package com.site.material.service;

import com.site.material.common.AuthContext;
import com.site.material.common.BusinessException;
import com.site.material.common.DocNoGenerator;
import com.site.material.domain.*;
import com.site.material.repo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 领料单业务：申请 -> 仓管核对（施工进度/预算量/已领量/现场库存）发料 -> 班组长确认进入现场消耗。
 */
@Service
public class RequisitionService {

    private final RequisitionRepository requisitionRepository;
    private final RequisitionItemRepository itemRepository;
    private final BudgetRepository budgetRepository;
    private final InventoryService inventoryService;
    private final NameResolver names;
    private final DocNoGenerator docNo;

    public RequisitionService(RequisitionRepository requisitionRepository,
                              RequisitionItemRepository itemRepository,
                              BudgetRepository budgetRepository,
                              InventoryService inventoryService,
                              NameResolver names, DocNoGenerator docNo) {
        this.requisitionRepository = requisitionRepository;
        this.itemRepository = itemRepository;
        this.budgetRepository = budgetRepository;
        this.inventoryService = inventoryService;
        this.names = names;
        this.docNo = docNo;
    }

    public record ItemReq(Long materialId, Long batchId, BigDecimal applyQty, String remark) {
    }

    public record ReqRequest(Long crewId, Long buildingId, Long processId, String workArea,
                             String drawingNo, String priority, String applyRemark, List<ItemReq> items) {
    }

    @Transactional
    public Requisition apply(ReqRequest req) {
        if (req.items() == null || req.items().isEmpty()) {
            throw new BusinessException("领料明细不能为空");
        }
        Requisition r = new Requisition();
        r.setReqNo(docNo.next("LL"));
        r.setCrewId(req.crewId());
        r.setBuildingId(req.buildingId());
        r.setProcessId(req.processId());
        r.setWorkArea(req.workArea());
        r.setDrawingNo(req.drawingNo());
        r.setPriority("URGENT".equals(req.priority()) ? "URGENT" : "NORMAL");
        r.setApplyRemark(req.applyRemark());
        r.setStatus("APPLIED");
        r.setAppliedBy(AuthContext.userId());
        r.setAppliedAt(LocalDateTime.now());
        Requisition saved = requisitionRepository.save(r);
        for (ItemReq it : req.items()) {
            if (it.applyQty() == null || it.applyQty().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("领料数量必须大于 0");
            }
            RequisitionItem item = new RequisitionItem();
            item.setRequisitionId(saved.getId());
            item.setMaterialId(it.materialId());
            item.setBatchId(it.batchId());
            item.setApplyQty(it.applyQty());
            item.setRemark(it.remark());
            saved.getItems().add(itemRepository.save(item));
        }
        return saved;
    }

    /** 发料前核对：预算量、本班组已领量、现场库存、可用批次 */
    public Map<String, Object> precheck(Long reqId) {
        Requisition r = mustGet(reqId);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (RequisitionItem item : itemRepository.findByRequisitionId(reqId)) {
            Map<String, Object> row = new HashMap<>();
            row.put("itemId", item.getId());
            row.put("materialId", item.getMaterialId());
            row.put("materialName", names.materialName(item.getMaterialId()));
            row.put("unit", names.materialUnit(item.getMaterialId()));
            row.put("applyQty", item.getApplyQty());

            BigDecimal budgetQty = budgetRepository
                    .findByBuildingIdAndProcessIdAndMaterialId(r.getBuildingId(), r.getProcessId(), item.getMaterialId())
                    .map(Budget::getBudgetQty).orElse(BigDecimal.ZERO);
            BigDecimal alreadyIssued = alreadyIssued(r, item.getMaterialId());
            BigDecimal onHand = inventoryService.availableBatches(item.getMaterialId()).stream()
                    .map(MaterialBatch::getQtyOnHand).reduce(BigDecimal.ZERO, BigDecimal::add);

            row.put("budgetQty", budgetQty);
            row.put("alreadyIssuedQty", alreadyIssued);
            row.put("remainingBudgetQty", budgetQty.subtract(alreadyIssued).max(BigDecimal.ZERO));
            row.put("onHandQty", onHand);
            row.put("overBudget", alreadyIssued.add(item.getApplyQty()).compareTo(budgetQty) > 0
                    && budgetQty.compareTo(BigDecimal.ZERO) > 0);
            row.put("shortStock", onHand.compareTo(item.getApplyQty()) < 0);
            row.put("batchId", item.getBatchId());
            row.put("availableBatches", inventoryService.availableBatches(item.getMaterialId())
                    .stream().map(names::batchView).toList());
            rows.add(row);
        }
        return Map.of("requisition", view(r), "items", rows);
    }

    private BigDecimal alreadyIssued(Requisition current, Long materialId) {
        return requisitionRepository.findByCrewIdOrderByIdDesc(current.getCrewId()).stream()
                .filter(x -> !x.getId().equals(current.getId()))
                .filter(x -> x.getBuildingId().equals(current.getBuildingId()))
                .filter(x -> x.getProcessId().equals(current.getProcessId()))
                .filter(x -> List.of("ISSUED", "CONFIRMED").contains(x.getStatus()))
                .flatMap(x -> itemRepository.findByRequisitionId(x.getId()).stream())
                .filter(i -> i.getMaterialId().equals(materialId))
                .map(RequisitionItem::getIssuedQty)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public record IssueLine(Long itemId, Long batchId, BigDecimal issuedQty) {
    }

    public record IssueRequest(List<IssueLine> lines, String issueRemark, Boolean allowOverBudget) {
    }

    @Transactional
    public Requisition issue(Long reqId, IssueRequest req) {
        Requisition r = mustGet(reqId);
        if (!"APPLIED".equals(r.getStatus())) {
            throw new BusinessException("只有已申请状态的领料单才能发料，当前状态：" + r.getStatus());
        }
        Map<Long, RequisitionItem> itemMap = new HashMap<>();
        itemRepository.findByRequisitionId(reqId).forEach(i -> itemMap.put(i.getId(), i));

        boolean force = Boolean.TRUE.equals(req.allowOverBudget());
        for (IssueLine line : req.lines()) {
            RequisitionItem item = itemMap.get(line.itemId());
            if (item == null) {
                throw new BusinessException("明细不存在：" + line.itemId());
            }
            if (line.batchId() == null) {
                throw new BusinessException(names.materialName(item.getMaterialId()) + " 未选择发料批次");
            }
            if (line.issuedQty() == null || line.issuedQty().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("实发数量必须大于 0");
            }
            if (line.issuedQty().compareTo(item.getApplyQty()) > 0) {
                throw new BusinessException(names.materialName(item.getMaterialId()) + " 实发量不能大于申请量");
            }
            // 预算核对：超出预算且未强制放行则拦截
            BigDecimal budgetQty = budgetRepository
                    .findByBuildingIdAndProcessIdAndMaterialId(r.getBuildingId(), r.getProcessId(), item.getMaterialId())
                    .map(Budget::getBudgetQty).orElse(BigDecimal.ZERO);
            BigDecimal afterIssue = alreadyIssued(r, item.getMaterialId()).add(line.issuedQty());
            if (!force && budgetQty.compareTo(BigDecimal.ZERO) > 0 && afterIssue.compareTo(budgetQty) > 0) {
                throw new BusinessException(names.materialName(item.getMaterialId())
                        + " 发料后累计 " + afterIssue + " 超过预算量 " + budgetQty
                        + "，请先追加预算或勾选超预算放行");
            }
            // 库存核对：扣减合格批次（不足会抛错）
            MaterialBatch batch = inventoryService.deduct(line.batchId(), line.issuedQty());
            item.setBatchId(line.batchId());
            item.setIssuedQty(line.issuedQty());
            item.setUnitPrice(batch.getUnitPrice());
            itemRepository.save(item);
        }
        r.setStatus("ISSUED");
        r.setIssuedBy(AuthContext.userId());
        r.setIssuedAt(LocalDateTime.now());
        r.setIssueRemark(req.issueRemark());
        return requisitionRepository.save(r);
    }

    @Transactional
    public Requisition confirm(Long reqId) {
        Requisition r = mustGet(reqId);
        if (!"ISSUED".equals(r.getStatus())) {
            throw new BusinessException("只有已发料的领料单才能由班组长确认，当前状态：" + r.getStatus());
        }
        r.setStatus("CONFIRMED");
        r.setConfirmedBy(AuthContext.userId());
        r.setConfirmedAt(LocalDateTime.now());
        return requisitionRepository.save(r);
    }

    @Transactional
    public Requisition reject(Long reqId, String reason) {
        Requisition r = mustGet(reqId);
        if (!"APPLIED".equals(r.getStatus())) {
            throw new BusinessException("只有已申请状态的领料单可以驳回");
        }
        r.setStatus("REJECTED");
        r.setRejectReason(reason);
        return requisitionRepository.save(r);
    }

    public Requisition mustGet(Long id) {
        return requisitionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("领料单不存在：" + id));
    }

    public Map<String, Object> view(Requisition r) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", r.getId());
        m.put("reqNo", r.getReqNo());
        m.put("crewId", r.getCrewId());
        m.put("crewName", names.crewName(r.getCrewId()));
        m.put("buildingId", r.getBuildingId());
        m.put("buildingName", names.buildingName(r.getBuildingId()));
        m.put("processId", r.getProcessId());
        m.put("processName", names.processName(r.getProcessId()));
        m.put("workArea", r.getWorkArea());
        m.put("drawingNo", r.getDrawingNo());
        m.put("priority", r.getPriority());
        m.put("status", r.getStatus());
        m.put("applyRemark", r.getApplyRemark());
        m.put("issueRemark", r.getIssueRemark());
        m.put("rejectReason", r.getRejectReason());
        m.put("appliedAt", r.getAppliedAt());
        m.put("issuedAt", r.getIssuedAt());
        m.put("confirmedAt", r.getConfirmedAt());
        List<Map<String, Object>> items = new ArrayList<>();
        for (RequisitionItem it : itemRepository.findByRequisitionId(r.getId())) {
            Map<String, Object> im = new HashMap<>();
            im.put("id", it.getId());
            im.put("materialId", it.getMaterialId());
            im.put("materialName", names.materialName(it.getMaterialId()));
            im.put("unit", names.materialUnit(it.getMaterialId()));
            im.put("batchId", it.getBatchId());
            im.put("applyQty", it.getApplyQty());
            im.put("issuedQty", it.getIssuedQty());
            im.put("unitPrice", it.getUnitPrice());
            im.put("remark", it.getRemark());
            if (it.getBatchId() != null) {
                im.put("batchNo", inventoryService.mustGet(it.getBatchId()).getBatchNo());
            }
            BigDecimal issued = it.getIssuedQty() == null ? BigDecimal.ZERO : it.getIssuedQty();
            BigDecimal price = it.getUnitPrice() == null ? BigDecimal.ZERO : it.getUnitPrice();
            im.put("lineCost", issued.multiply(price));
            items.add(im);
        }
        m.put("items", items);
        return m;
    }
}
