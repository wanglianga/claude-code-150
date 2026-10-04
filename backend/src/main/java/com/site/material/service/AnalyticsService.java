package com.site.material.service;

import com.site.material.domain.*;
import com.site.material.repo.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 角色分析：
 *  - 项目经理：按楼栋/工序/班组的材料偏差
 *  - 财务：供应商结算、班组扣款、变更索赔
 *  - 预算员：用实际消耗反推后续采购计划与分包结算
 *  - 材料员：损耗复盘（设计变更/施工错误/保管不当/供应商质量）
 */
@Service
public class AnalyticsService {

    private final MonthlySettlementRepository settlementRepository;
    private final MaterialBatchRepository batchRepository;
    private final LossRecordRepository lossRepository;
    private final ChangeOrderRepository changeRepository;
    private final RequisitionRepository requisitionRepository;
    private final RequisitionItemRepository itemRepository;
    private final BudgetRepository budgetRepository;
    private final NameResolver names;

    public AnalyticsService(MonthlySettlementRepository settlementRepository,
                            MaterialBatchRepository batchRepository,
                            LossRecordRepository lossRepository,
                            ChangeOrderRepository changeRepository,
                            RequisitionRepository requisitionRepository,
                            RequisitionItemRepository itemRepository,
                            BudgetRepository budgetRepository,
                            NameResolver names) {
        this.settlementRepository = settlementRepository;
        this.batchRepository = batchRepository;
        this.lossRepository = lossRepository;
        this.changeRepository = changeRepository;
        this.itemRepository = itemRepository;
        this.requisitionRepository = requisitionRepository;
        this.budgetRepository = budgetRepository;
        this.names = names;
    }

    /* ---------------- 项目经理：材料偏差 ---------------- */

    public List<Map<String, Object>> deviation(String period) {
        // 维度键：楼栋+工序+班组（汇总各材料）
        record K(Long building, Long process, Long crew) {
        }
        Map<K, Map<String, Object>> agg = new LinkedHashMap<>();
        for (MonthlySettlement s : settlementRepository.findByPeriod(period)) {
            K k = new K(s.getBuildingId(), s.getProcessId(), s.getCrewId());
            Map<String, Object> row = agg.computeIfAbsent(k, x -> {
                Map<String, Object> m = new HashMap<>();
                m.put("buildingId", x.building());
                m.put("buildingName", x.building() == 0 ? "未指定" : names.buildingName(x.building()));
                m.put("processId", x.process());
                m.put("processName", x.process() == 0 ? "未指定" : names.processName(x.process()));
                m.put("crewId", x.crew());
                m.put("crewName", x.crew() == 0 ? "公共/未指定" : names.crewName(x.crew()));
                m.put("budgetCost", BigDecimal.ZERO);
                m.put("crewCost", BigDecimal.ZERO);
                m.put("issuedCost", BigDecimal.ZERO);
                m.put("crewLossCost", BigDecimal.ZERO);
                m.put("variance", BigDecimal.ZERO);
                return m;
            });
            add(row, "budgetCost", s.getBudgetCost());
            add(row, "crewCost", s.getCrewCost());
            add(row, "issuedCost", s.getIssuedCost());
            add(row, "crewLossCost", s.getCrewLossCost());
            add(row, "variance", s.getVariance());
        }
        List<Map<String, Object>> rows = new ArrayList<>(agg.values());
        for (Map<String, Object> row : rows) {
            BigDecimal budget = (BigDecimal) row.get("budgetCost");
            BigDecimal variance = (BigDecimal) row.get("variance");
            BigDecimal rate = budget.compareTo(BigDecimal.ZERO) > 0
                    ? variance.multiply(HUNDRED).divide(budget, 1, RoundingMode.HALF_UP)
                    : (variance.compareTo(BigDecimal.ZERO) > 0 ? HUNDRED : BigDecimal.ZERO);
            row.put("varianceRate", rate);
        }
        rows.sort((a, b) -> ((BigDecimal) b.get("variance")).compareTo((BigDecimal) a.get("variance")));
        return rows;
    }

    /* ---------------- 财务：供应商结算 / 班组扣款 / 变更索赔 ---------------- */

    public Map<String, Object> finance(String period) {
        // 供应商结算：按批次入库金额 + 质量索赔扣减
        Map<Long, Map<String, Object>> supplierMap = new LinkedHashMap<>();
        for (MaterialBatch b : batchRepository.findAll()) {
            Long sid = b.getSupplierId() == null ? 0L : b.getSupplierId();
            Map<String, Object> row = supplierMap.computeIfAbsent(sid, x -> {
                Map<String, Object> m = new HashMap<>();
                m.put("supplierId", x);
                m.put("supplierName", x == 0 ? "未指定供应商" : names.supplierName(x));
                m.put("inboundAmount", BigDecimal.ZERO);
                m.put("qualityClaim", BigDecimal.ZERO);
                return m;
            });
            add(row, "inboundAmount", b.getQtyIn().multiply(b.getUnitPrice()));
        }
        BigDecimal totalClaim = BigDecimal.ZERO;
        for (LossRecord l : inPeriodLosses(period)) {
            if ("SUPPLIER".equals(l.getResponsibility())) {
                Long sid = l.getSupplierId() == null ? 0L : l.getSupplierId();
                Map<String, Object> row = supplierMap.computeIfAbsent(sid, x -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("supplierId", x);
                    m.put("supplierName", x == 0 ? "未指定供应商" : names.supplierName(x));
                    m.put("inboundAmount", BigDecimal.ZERO);
                    m.put("qualityClaim", BigDecimal.ZERO);
                    return m;
                });
                BigDecimal amount = l.getQty().multiply(l.getUnitPrice() == null ? BigDecimal.ZERO : l.getUnitPrice());
                add(row, "qualityClaim", amount);
                totalClaim = totalClaim.add(amount);
            }
        }
        List<Map<String, Object>> supplierRows = new ArrayList<>(supplierMap.values());
        supplierRows.forEach(r -> {
            BigDecimal payable = ((BigDecimal) r.get("inboundAmount")).subtract((BigDecimal) r.get("qualityClaim"));
            r.put("payableAmount", payable);
        });

        // 班组扣款：本期班组材料成本 + 责任损耗
        Map<Long, Map<String, Object>> crewMap = new LinkedHashMap<>();
        for (MonthlySettlement s : settlementRepository.findByPeriod(period)) {
            if (s.getCrewId() == 0L) {
                continue;
            }
            Map<String, Object> row = crewMap.computeIfAbsent(s.getCrewId(), x -> {
                Map<String, Object> m = new HashMap<>();
                m.put("crewId", x);
                m.put("crewName", names.crewName(x));
                m.put("materialCost", BigDecimal.ZERO);
                m.put("crewLossDeduction", BigDecimal.ZERO);
                return m;
            });
            add(row, "materialCost", s.getCrewCost());
            add(row, "crewLossDeduction", s.getCrewLossCost());
        }
        List<Map<String, Object>> crewRows = new ArrayList<>(crewMap.values());
        crewRows.forEach(r -> r.put("totalDeduction",
                ((BigDecimal) r.get("materialCost")).add((BigDecimal) r.get("crewLossDeduction"))));

        // 变更索赔：已批准签证
        List<Map<String, Object>> changeRows = changeRepository.findAllByOrderByIdDesc().stream()
                .filter(c -> "APPROVED".equals(c.getStatus()))
                .map(c -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("changeNo", c.getChangeNo());
                    m.put("type", c.getType());
                    m.put("buildingName", names.buildingName(c.getBuildingId()));
                    m.put("materialName", c.getMaterialId() == null ? "" : names.materialName(c.getMaterialId()));
                    m.put("changeQty", c.getChangeQty());
                    m.put("claimAmount", c.getClaimAmount());
                    m.put("changeDate", c.getChangeDate());
                    m.put("description", c.getDescription());
                    return m;
                }).toList();

        BigDecimal totalInbound = supplierRows.stream().map(r -> (BigDecimal) r.get("inboundAmount"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCrewDeduction = crewRows.stream().map(r -> (BigDecimal) r.get("totalDeduction"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalChangeClaim = changeRows.stream().map(r -> (BigDecimal) r.get("claimAmount"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return Map.of(
                "supplierSettlement", supplierRows,
                "crewDeduction", crewRows,
                "changeClaims", changeRows,
                "summary", Map.of(
                        "inboundTotal", totalInbound,
                        "qualityClaimTotal", totalClaim,
                        "payableTotal", totalInbound.subtract(totalClaim),
                        "crewDeductionTotal", totalCrewDeduction,
                        "changeClaimTotal", totalChangeClaim));
    }

    /* ---------------- 预算员：实际消耗反推采购计划 + 分包结算 ---------------- */

    public Map<String, Object> procurement(String period) {
        List<MonthlySettlement> all = new ArrayList<>(settlementRepository.findByPeriod(period));

        // 按材料汇总本期净耗
        Map<Long, BigDecimal> netByMat = new HashMap<>();
        for (MonthlySettlement s : all) {
            netByMat.merge(s.getMaterialId(), s.getNetQty(), BigDecimal::add);
        }

        // 预算（跨楼栋工序）：总预算量与形象进度（加权，按预算量）
        Map<Long, BigDecimal> budgetByMat = new HashMap<>();
        Map<Long, BigDecimal[]> progressWeight = new HashMap<>(); // [Σbudget*progress, Σbudget]
        for (Budget b : budgetRepository.findAll()) {
            budgetByMat.merge(b.getMaterialId(), b.getBudgetQty(), BigDecimal::add);
            BigDecimal pct = b.getProgressPercent() == null ? HUNDRED : b.getProgressPercent();
            BigDecimal[] w = progressWeight.computeIfAbsent(b.getMaterialId(),
                    k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            w[0] = w[0].add(b.getBudgetQty().multiply(pct));
            w[1] = w[1].add(b.getBudgetQty());
        }
        names.materials().forEach(m -> {
            budgetByMat.putIfAbsent(m.getId(), BigDecimal.ZERO);
            netByMat.putIfAbsent(m.getId(), BigDecimal.ZERO);
        });
        Map<Long, BigDecimal> avg = avgPriceMap();
        // 可用库存仅统计质检合格批次（待检/不合格冻结批次不可用于后续施工）
        Map<Long, BigDecimal> stockByMat = new HashMap<>();
        batchRepository.findAll().stream()
                .filter(b -> "PASSED".equals(b.getQcStatus()))
                .forEach(b -> stockByMat.merge(b.getMaterialId(), b.getQtyOnHand(), BigDecimal::add));

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Long mid : budgetByMat.keySet()) {
            BigDecimal budget = budgetByMat.get(mid);
            BigDecimal net = netByMat.get(mid);
            BigDecimal stock = stockByMat.getOrDefault(mid, BigDecimal.ZERO);
            BigDecimal price = avg.getOrDefault(mid, BigDecimal.ZERO);
            BigDecimal[] w = progressWeight.get(mid);
            BigDecimal progress = (w == null || w[1].compareTo(BigDecimal.ZERO) == 0)
                    ? HUNDRED
                    : w[0].divide(w[1], 1, RoundingMode.HALF_UP);
            // 当前进度下的预算应耗 = 总预算 × 进度%
            BigDecimal plannedAtProgress = budget.multiply(progress)
                    .divide(HUNDRED, 2, RoundingMode.HALF_UP);
            // 实际单耗系数 = 净耗 / 当前进度预算应耗
            BigDecimal factor = plannedAtProgress.compareTo(BigDecimal.ZERO) > 0
                    ? net.divide(plannedAtProgress, 4, RoundingMode.HALF_UP) : BigDecimal.ONE;
            // 反推完工总需 = 总预算 × 系数；后续尚需 = 总需 - 已净耗
            BigDecimal forecastTotal = budget.multiply(factor).setScale(2, RoundingMode.HALF_UP);
            BigDecimal remainingForecast = forecastTotal.subtract(net).max(BigDecimal.ZERO);
            BigDecimal suggestPurchase = remainingForecast.subtract(stock).max(BigDecimal.ZERO)
                    .setScale(0, RoundingMode.CEILING);
            Map<String, Object> m = new HashMap<>();
            m.put("materialId", mid);
            m.put("materialName", names.materialName(mid));
            m.put("unit", names.materialUnit(mid));
            m.put("budgetQty", budget);
            m.put("progressPercent", progress);
            m.put("netConsumedQty", net);
            m.put("consumeRate", plannedAtProgress.compareTo(BigDecimal.ZERO) > 0
                    ? net.multiply(HUNDRED).divide(plannedAtProgress, 1, RoundingMode.HALF_UP) : BigDecimal.ZERO);
            m.put("avgUnitPrice", price);
            m.put("onHandQty", stock);
            m.put("forecastRemainQty", remainingForecast);
            m.put("suggestPurchaseQty", suggestPurchase);
            m.put("suggestPurchaseAmount", suggestPurchase.multiply(price).setScale(2, RoundingMode.HALF_UP));
            rows.add(m);
        }
        rows.sort((a, b) -> ((BigDecimal) b.get("suggestPurchaseAmount"))
                .compareTo((BigDecimal) a.get("suggestPurchaseAmount")));

        // 分包结算建议：按班组汇总净耗成本+责任扣款
        Map<Long, Map<String, Object>> crewMap = new LinkedHashMap<>();
        for (MonthlySettlement s : all) {
            if (s.getCrewId() == 0L) {
                continue;
            }
            Map<String, Object> row = crewMap.computeIfAbsent(s.getCrewId(), x -> {
                Map<String, Object> m = new HashMap<>();
                m.put("crewId", x);
                m.put("crewName", names.crewName(x));
                m.put("netCost", BigDecimal.ZERO);
                m.put("crewLossCost", BigDecimal.ZERO);
                m.put("budgetCost", BigDecimal.ZERO);
                return m;
            });
            add(row, "netCost", s.getIssuedCost().subtract(s.getReturnedCost()));
            add(row, "crewLossCost", s.getCrewLossCost());
            add(row, "budgetCost", s.getBudgetCost());
        }
        List<Map<String, Object>> subcontract = new ArrayList<>(crewMap.values());
        subcontract.forEach(r -> r.put("settleAmount",
                ((BigDecimal) r.get("netCost")).add((BigDecimal) r.get("crewLossCost"))));

        return Map.of("procurementPlan", rows, "subcontractSettlement", subcontract);
    }

    /* ---------------- 材料员：损耗复盘 ---------------- */

    public Map<String, Object> lossReview(String period) {
        List<LossRecord> losses = inPeriodLosses(period);
        Map<String, Map<String, Object>> reasonMap = new LinkedHashMap<>();
        for (String reason : List.of("DESIGN_CHANGE", "CONSTRUCTION_ERROR", "POOR_STORAGE",
                "SUPPLIER_QUALITY", "NORMAL")) {
            Map<String, Object> m = new HashMap<>();
            m.put("lossReason", reason);
            m.put("qty", BigDecimal.ZERO);
            m.put("amount", BigDecimal.ZERO);
            m.put("count", 0);
            reasonMap.put(reason, m);
        }
        List<Map<String, Object>> details = new ArrayList<>();
        for (LossRecord l : losses) {
            String reason = l.getLossReason() == null ? "NORMAL" : l.getLossReason();
            Map<String, Object> row = reasonMap.computeIfAbsent(reason, x -> {
                Map<String, Object> m = new HashMap<>();
                m.put("lossReason", x);
                m.put("qty", BigDecimal.ZERO);
                m.put("amount", BigDecimal.ZERO);
                m.put("count", 0);
                return m;
            });
            BigDecimal amount = l.getQty().multiply(l.getUnitPrice() == null ? BigDecimal.ZERO : l.getUnitPrice());
            add(row, "qty", l.getQty());
            add(row, "amount", amount);
            row.put("count", (Integer) row.get("count") + 1);

            Map<String, Object> d = new HashMap<>();
            d.put("lossNo", l.getLossNo());
            d.put("materialName", names.materialName(l.getMaterialId()));
            d.put("qty", l.getQty());
            d.put("amount", amount);
            d.put("lossReason", reason);
            d.put("responsibility", l.getResponsibility());
            d.put("crewName", names.crewName(l.getCrewId()));
            d.put("supplierName", names.supplierName(l.getSupplierId()));
            d.put("lossDate", l.getLossDate());
            d.put("remark", l.getRemark());
            details.add(d);
        }
        List<Map<String, Object>> reasonRows = reasonMap.values().stream()
                .filter(m -> (Integer) m.get("count") > 0)
                .sorted((a, b) -> ((BigDecimal) b.get("amount")).compareTo((BigDecimal) a.get("amount")))
                .toList();
        BigDecimal total = reasonRows.stream().map(r -> (BigDecimal) r.get("amount"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Map.of("byReason", reasonRows, "details", details, "totalLoss", total);
    }

    private List<LossRecord> inPeriodLosses(String period) {
        var ym = java.time.YearMonth.parse(period);
        return lossRepository.findAllByOrderByIdDesc().stream()
                .filter(l -> l.getLossDate() != null
                        && !l.getLossDate().isBefore(ym.atDay(1))
                        && !l.getLossDate().isAfter(ym.atEndOfMonth()))
                .toList();
    }

    private Map<Long, BigDecimal> avgPriceMap() {
        Map<Long, BigDecimal[]> agg = new HashMap<>();
        for (MaterialBatch b : batchRepository.findAll()) {
            BigDecimal[] a = agg.computeIfAbsent(b.getMaterialId(),
                    k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            a[0] = a[0].add(b.getQtyIn().multiply(b.getUnitPrice()));
            a[1] = a[1].add(b.getQtyIn());
        }
        Map<Long, BigDecimal> price = new HashMap<>();
        agg.forEach((mid, a) -> {
            if (a[1].compareTo(BigDecimal.ZERO) > 0) {
                price.put(mid, a[0].divide(a[1], 2, RoundingMode.HALF_UP));
            }
        });
        return price;
    }

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    @SuppressWarnings("unchecked")
    private void add(Map<String, Object> row, String key, BigDecimal v) {
        if (v == null) {
            return;
        }
        row.put(key, ((BigDecimal) row.get(key)).add(v));
    }
}
