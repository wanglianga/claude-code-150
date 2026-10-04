package com.site.material.service;

import com.site.material.common.AuthContext;
import com.site.material.common.BusinessException;
import com.site.material.common.DocNoGenerator;
import com.site.material.domain.RequisitionItem;
import com.site.material.domain.ReturnOrder;
import com.site.material.repo.RequisitionItemRepository;
import com.site.material.repo.ReturnOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/** 退库：规格领错/施工变更/临时追加未用等；可复用回补库存并冲减班组成本，报废不回库。 */
@Service
public class ReturnService {

    private final ReturnOrderRepository returnRepository;
    private final RequisitionItemRepository itemRepository;
    private final InventoryService inventoryService;
    private final NameResolver names;
    private final DocNoGenerator docNo;

    public ReturnService(ReturnOrderRepository returnRepository, RequisitionItemRepository itemRepository,
                         InventoryService inventoryService, NameResolver names, DocNoGenerator docNo) {
        this.returnRepository = returnRepository;
        this.itemRepository = itemRepository;
        this.inventoryService = inventoryService;
        this.names = names;
        this.docNo = docNo;
    }

    @Transactional
    public ReturnOrder create(ReturnOrder ro) {
        if (ro.getQty() == null || ro.getQty().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("退库数量必须大于 0");
        }
        ro.setReturnNo(docNo.next("TK"));
        ro.setStatus("DRAFT");
        ro.setCreatedBy(AuthContext.userId());
        ro.setCreatedAt(LocalDateTime.now());
        // 单价优先取来源领料明细锁定价
        if (ro.getRequisitionId() != null) {
            itemRepository.findByRequisitionId(ro.getRequisitionId()).stream()
                    .filter(i -> i.getMaterialId().equals(ro.getMaterialId()))
                    .findFirst()
                    .ifPresent(i -> {
                        ro.setBatchId(i.getBatchId());
                        ro.setUnitPrice(i.getUnitPrice());
                    });
        }
        if (ro.getUnitPrice() == null && ro.getBatchId() != null) {
            ro.setUnitPrice(inventoryService.mustGet(ro.getBatchId()).getUnitPrice());
        }
        return returnRepository.save(ro);
    }

    @Transactional
    public ReturnOrder confirm(Long id) {
        ReturnOrder ro = mustGet(id);
        if (!"DRAFT".equals(ro.getStatus())) {
            throw new BusinessException("退库单已确认，请勿重复操作");
        }
        if ("REUSE".equals(ro.getDisposition()) && ro.getBatchId() != null) {
            inventoryService.addBack(ro.getBatchId(), ro.getQty());
        }
        ro.setStatus("CONFIRMED");
        ro.setConfirmedBy(AuthContext.userId());
        ro.setConfirmedAt(LocalDateTime.now());
        return returnRepository.save(ro);
    }

    public ReturnOrder mustGet(Long id) {
        return returnRepository.findById(id)
                .orElseThrow(() -> new BusinessException("退库单不存在：" + id));
    }

    public Map<String, Object> view(ReturnOrder ro) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", ro.getId());
        m.put("returnNo", ro.getReturnNo());
        m.put("requisitionId", ro.getRequisitionId());
        m.put("crewId", ro.getCrewId());
        m.put("crewName", names.crewName(ro.getCrewId()));
        m.put("buildingId", ro.getBuildingId());
        m.put("buildingName", names.buildingName(ro.getBuildingId()));
        m.put("processId", ro.getProcessId());
        m.put("processName", names.processName(ro.getProcessId()));
        m.put("materialId", ro.getMaterialId());
        m.put("materialName", names.materialName(ro.getMaterialId()));
        m.put("unit", names.materialUnit(ro.getMaterialId()));
        m.put("batchId", ro.getBatchId());
        m.put("qty", ro.getQty());
        m.put("unitPrice", ro.getUnitPrice());
        m.put("amount", (ro.getUnitPrice() == null ? BigDecimal.ZERO : ro.getUnitPrice()).multiply(ro.getQty()));
        m.put("reason", ro.getReason());
        m.put("disposition", ro.getDisposition());
        m.put("status", ro.getStatus());
        m.put("remark", ro.getRemark());
        m.put("createdAt", ro.getCreatedAt());
        m.put("confirmedAt", ro.getConfirmedAt());
        return m;
    }
}
