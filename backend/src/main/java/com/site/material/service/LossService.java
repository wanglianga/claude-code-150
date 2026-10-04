package com.site.material.service;

import com.site.material.common.AuthContext;
import com.site.material.common.BusinessException;
import com.site.material.common.DocNoGenerator;
import com.site.material.domain.LossRecord;
import com.site.material.repo.LossRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 损耗登记：瓷砖破损、砂浆过期等。
 * 责任归属 CREW（班组扣款）/ SUPPLIER（供应商索赔）/ PROJECT（项目承担），月底进入对应成本口径。
 */
@Service
public class LossService {

    private final LossRecordRepository lossRepository;
    private final InventoryService inventoryService;
    private final NameResolver names;
    private final DocNoGenerator docNo;

    public LossService(LossRecordRepository lossRepository, InventoryService inventoryService,
                       NameResolver names, DocNoGenerator docNo) {
        this.lossRepository = lossRepository;
        this.inventoryService = inventoryService;
        this.names = names;
        this.docNo = docNo;
    }

    @Transactional
    public LossRecord create(LossRecord loss) {
        if (loss.getQty() == null || loss.getQty().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("损耗数量必须大于 0");
        }
        loss.setLossNo(docNo.next("SH"));
        if (loss.getLossDate() == null) {
            loss.setLossDate(LocalDate.now());
        }
        if (loss.getBatchId() != null) {
            var batch = inventoryService.mustGet(loss.getBatchId());
            if (loss.getUnitPrice() == null) {
                loss.setUnitPrice(batch.getUnitPrice());
            }
            // 仅仓内损耗/盘点盘亏调减批次在账数量；现场（已发料）损耗只计责任成本，不动仓库库存
            if (Boolean.TRUE.equals(loss.getStockAdjusted())) {
                inventoryService.adjust(loss.getBatchId(), loss.getQty().negate());
            }
        }
        loss.setCreatedBy(AuthContext.userId());
        return lossRepository.save(loss);
    }

    /** 财务/材料员复盘时修正责任归属与原因 */
    @Transactional
    public LossRecord update(Long id, LossRecord form) {
        LossRecord loss = lossRepository.findById(id)
                .orElseThrow(() -> new BusinessException("损耗记录不存在：" + id));
        loss.setLossReason(form.getLossReason());
        loss.setResponsibility(form.getResponsibility());
        loss.setSupplierId(form.getSupplierId());
        loss.setRemark(form.getRemark());
        return lossRepository.save(loss);
    }

    public Map<String, Object> view(LossRecord l) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", l.getId());
        m.put("lossNo", l.getLossNo());
        m.put("materialId", l.getMaterialId());
        m.put("materialName", names.materialName(l.getMaterialId()));
        m.put("unit", names.materialUnit(l.getMaterialId()));
        m.put("batchId", l.getBatchId());
        m.put("crewId", l.getCrewId());
        m.put("crewName", names.crewName(l.getCrewId()));
        m.put("buildingId", l.getBuildingId());
        m.put("buildingName", names.buildingName(l.getBuildingId()));
        m.put("processId", l.getProcessId());
        m.put("processName", names.processName(l.getProcessId()));
        m.put("qty", l.getQty());
        m.put("unitPrice", l.getUnitPrice());
        m.put("amount", (l.getUnitPrice() == null ? BigDecimal.ZERO : l.getUnitPrice()).multiply(l.getQty()));
        m.put("lossReason", l.getLossReason());
        m.put("responsibility", l.getResponsibility());
        m.put("supplierId", l.getSupplierId());
        m.put("supplierName", names.supplierName(l.getSupplierId()));
        m.put("changeId", l.getChangeId());
        m.put("lossDate", l.getLossDate());
        m.put("remark", l.getRemark());
        return m;
    }
}
