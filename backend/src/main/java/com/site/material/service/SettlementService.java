package com.site.material.service;

import com.site.material.common.BusinessException;
import com.site.material.domain.*;
import com.site.material.repo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/**
 * 月底核算引擎：按 预算 / 实际领用 / 退库 / 损耗 / 责任归属
 * 生成 月份+楼栋+工序+班组+材料 维度的班组材料成本。
 */
@Service
public class SettlementService {

    private final MonthlySettlementRepository settlementRepository;
    private final RequisitionRepository requisitionRepository;
    private final RequisitionItemRepository itemRepository;
    private final ReturnOrderRepository returnRepository;
    private final LossRecordRepository lossRepository;
    private final BudgetRepository budgetRepository;
    private final MaterialBatchRepository batchRepository;
    private final MaterialRepository materialRepository;

    public SettlementService(MonthlySettlementRepository settlementRepository,
                             RequisitionRepository requisitionRepository,
                             RequisitionItemRepository itemRepository,
                             ReturnOrderRepository returnRepository,
                             LossRecordRepository lossRepository,
                             BudgetRepository budgetRepository,
                             MaterialBatchRepository batchRepository,
                             MaterialRepository materialRepository) {
        this.settlementRepository = settlementRepository;
        this.requisitionRepository = requisitionRepository;
        this.itemRepository = itemRepository;
        this.returnRepository = returnRepository;
        this.lossRepository = lossRepository;
        this.budgetRepository = budgetRepository;
        this.batchRepository = batchRepository;
        this.materialRepository = materialRepository;
    }

    private record Key(Long buildingId, Long processId, Long crewId, Long materialId) {
    }

    /** 材料加权平均单价（按入库数量），预算成本与无单价明细兜底使用 */
    public Map<Long, BigDecimal> avgPriceIndex() {
        Map<Long, BigDecimal[]> agg = new HashMap<>(); // [amount, qty]
        for (MaterialBatch b : batchRepository.findAll()) {
            BigDecimal[] a = agg.computeIfAbsent(b.getMaterialId(), k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            a[0] = a[0].add(b.getQtyIn().multiply(b.getUnitPrice()));
            a[1] = a[1].add(b.getQtyIn());
        }
        Map<Long, BigDecimal> price = new HashMap<>();
        agg.forEach((mid, a) -> {
            if (a[1].compareTo(BigDecimal.ZERO) > 0) {
                price.put(mid, a[0].divide(a[1], 2, RoundingMode.HALF_UP));
            }
        });
        // 零入库批次兜底：取最新批次单价
        batchRepository.findAll().stream()
                .sorted(Comparator.comparing(MaterialBatch::getId).reversed())
                .forEach(b -> price.putIfAbsent(b.getMaterialId(), b.getUnitPrice()));
        return price;
    }

    @Transactional
    public List<MonthlySettlement> generate(String period) {
        YearMonth ym;
        try {
            ym = YearMonth.parse(period);
        } catch (Exception e) {
            throw new BusinessException("月份格式应为 yyyy-MM");
        }
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        Map<Long, BigDecimal> priceIndex = avgPriceIndex();

        settlementRepository.deleteByPeriod(period);
        settlementRepository.flush();

        Map<Key, MonthlySettlement> rows = new HashMap<>();

        // 1) 实际领用（本期发料的领料单明细）
        requisitionRepository.findAllByOrderByIdDesc().stream()
                .filter(r -> List.of("ISSUED", "CONFIRMED").contains(r.getStatus()))
                .filter(r -> r.getIssuedAt() != null
                        && !r.getIssuedAt().toLocalDate().isBefore(start)
                        && !r.getIssuedAt().toLocalDate().isAfter(end))
                .forEach(r -> itemRepository.findByRequisitionId(r.getId()).forEach(it -> {
                    if (it.getIssuedQty() == null || it.getIssuedQty().compareTo(BigDecimal.ZERO) == 0) {
                        return;
                    }
                    Key key = new Key(r.getBuildingId(), r.getProcessId(), r.getCrewId(), it.getMaterialId());
                    MonthlySettlement s = rows.computeIfAbsent(key, k -> newRow(period, k, priceIndex));
                    BigDecimal price = it.getUnitPrice() != null ? it.getUnitPrice()
                            : priceIndex.getOrDefault(it.getMaterialId(), BigDecimal.ZERO);
                    s.setIssuedQty(s.getIssuedQty().add(it.getIssuedQty()));
                    s.setIssuedCost(s.getIssuedCost().add(it.getIssuedQty().multiply(price)));
                }));

        // 2) 退库：可复用冲减班组成本与数量；报废计为班组责任损耗成本
        returnRepository.findAllByOrderByIdDesc().stream()
                .filter(r -> "CONFIRMED".equals(r.getStatus()))
                .filter(r -> r.getConfirmedAt() != null
                        && !r.getConfirmedAt().toLocalDate().isBefore(start)
                        && !r.getConfirmedAt().toLocalDate().isAfter(end))
                .forEach(r -> {
                    Key key = new Key(r.getBuildingId(), r.getProcessId(), r.getCrewId(), r.getMaterialId());
                    MonthlySettlement s = rows.computeIfAbsent(key, k -> newRow(period, k, priceIndex));
                    BigDecimal price = r.getUnitPrice() != null ? r.getUnitPrice()
                            : priceIndex.getOrDefault(r.getMaterialId(), BigDecimal.ZERO);
                    if ("REUSE".equals(r.getDisposition())) {
                        s.setReturnedQty(s.getReturnedQty().add(r.getQty()));
                        s.setReturnedCost(s.getReturnedCost().add(r.getQty().multiply(price)));
                    } else {
                        s.setCrewLossCost(s.getCrewLossCost().add(r.getQty().multiply(price)));
                    }
                });

        // 3) 损耗：按责任归属分流（本期）
        lossRepository.findAllByOrderByIdDesc().stream()
                .filter(l -> l.getLossDate() != null
                        && !l.getLossDate().isBefore(start) && !l.getLossDate().isAfter(end))
                .forEach(l -> {
                    Long crewId = l.getCrewId() == null ? 0L : l.getCrewId();
                    Long buildingId = l.getBuildingId() == null ? 0L : l.getBuildingId();
                    Long processId = l.getProcessId() == null ? 0L : l.getProcessId();
                    Key key = new Key(buildingId, processId, crewId, l.getMaterialId());
                    MonthlySettlement s = rows.computeIfAbsent(key, k -> newRow(period, k, priceIndex));
                    BigDecimal price = l.getUnitPrice() != null ? l.getUnitPrice()
                            : priceIndex.getOrDefault(l.getMaterialId(), BigDecimal.ZERO);
                    BigDecimal amount = l.getQty().multiply(price);
                    switch (l.getResponsibility() == null ? "" : l.getResponsibility()) {
                        case "CREW" -> s.setCrewLossCost(s.getCrewLossCost().add(amount));
                        case "SUPPLIER" -> s.setSupplierClaimCost(s.getSupplierClaimCost().add(amount));
                        default -> s.setProjectLossCost(s.getProjectLossCost().add(amount));
                    }
                });

        // 4) 预算：按楼栋+工序+材料，在本期有领用的班组间按实发量比例分摊；无领用则挂班组 0 作为基线行
        for (Budget b : budgetRepository.findAll()) {
            List<MonthlySettlement> crewRows = rows.values().stream()
                    .filter(s -> s.getBuildingId().equals(b.getBuildingId()))
                    .filter(s -> s.getProcessId().equals(b.getProcessId()))
                    .filter(s -> s.getMaterialId().equals(b.getMaterialId()))
                    .filter(s -> s.getCrewId() != 0L)
                    .toList();
            BigDecimal price = priceIndex.getOrDefault(b.getMaterialId(), BigDecimal.ZERO);
            if (crewRows.isEmpty()) {
                Key key = new Key(b.getBuildingId(), b.getProcessId(), 0L, b.getMaterialId());
                MonthlySettlement s = rows.computeIfAbsent(key, k -> newRow(period, k, priceIndex));
                s.setBudgetQty(b.getBudgetQty());
                s.setBudgetCost(b.getBudgetQty().multiply(price));
            } else {
                BigDecimal totalIssued = crewRows.stream().map(MonthlySettlement::getIssuedQty)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal allocated = BigDecimal.ZERO;
                for (int i = 0; i < crewRows.size(); i++) {
                    MonthlySettlement s = crewRows.get(i);
                    BigDecimal share;
                    if (i == crewRows.size() - 1) {
                        share = b.getBudgetQty().subtract(allocated); // 末行兜底，避免尾差
                    } else if (totalIssued.compareTo(BigDecimal.ZERO) > 0) {
                        share = b.getBudgetQty().multiply(s.getIssuedQty())
                                .divide(totalIssued, 2, RoundingMode.HALF_UP);
                        allocated = allocated.add(share);
                    } else {
                        share = BigDecimal.ZERO;
                    }
                    s.setBudgetQty(s.getBudgetQty().add(share));
                    s.setBudgetCost(s.getBudgetCost().add(share.multiply(price)));
                }
            }
        }

        // 5) 汇总计算：净耗、班组成本、偏差
        List<MonthlySettlement> result = new ArrayList<>();
        for (MonthlySettlement s : rows.values()) {
            s.setNetQty(s.getIssuedQty().subtract(s.getReturnedQty()));
            // 班组材料成本 = 实发 - 可复用退库 + 班组责任损耗（报废退库/现场责任损耗扣款参考）
            BigDecimal crewCost = s.getIssuedCost().subtract(s.getReturnedCost()).add(s.getCrewLossCost());
            s.setCrewCost(crewCost);
            s.setVariance(crewCost.subtract(s.getBudgetCost()));
            normalize(s);
            result.add(settlementRepository.save(s));
        }
        result.sort(Comparator.comparing(MonthlySettlement::getBuildingId)
                .thenComparing(MonthlySettlement::getProcessId)
                .thenComparing(MonthlySettlement::getCrewId)
                .thenComparing(MonthlySettlement::getMaterialId));
        return result;
    }

    private MonthlySettlement newRow(String period, Key k, Map<Long, BigDecimal> priceIndex) {
        MonthlySettlement s = new MonthlySettlement();
        s.setPeriod(period);
        s.setBuildingId(k.buildingId());
        s.setProcessId(k.processId());
        s.setCrewId(k.crewId());
        s.setMaterialId(k.materialId());
        return s;
    }

    private void normalize(MonthlySettlement s) {
        s.setIssuedCost(scale(s.getIssuedCost()));
        s.setReturnedCost(scale(s.getReturnedCost()));
        s.setBudgetCost(scale(s.getBudgetCost()));
        s.setCrewLossCost(scale(s.getCrewLossCost()));
        s.setSupplierClaimCost(scale(s.getSupplierClaimCost()));
        s.setProjectLossCost(scale(s.getProjectLossCost()));
        s.setCrewCost(scale(s.getCrewCost()));
        s.setVariance(scale(s.getVariance()));
    }

    private BigDecimal scale(BigDecimal v) {
        return v == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : v.setScale(2, RoundingMode.HALF_UP);
    }
}
