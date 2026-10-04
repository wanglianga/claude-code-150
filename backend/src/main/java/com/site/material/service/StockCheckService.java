package com.site.material.service;

import com.site.material.common.BusinessException;
import com.site.material.common.CodeGenerator;
import com.site.material.domain.*;
import com.site.material.dto.Requests.*;
import com.site.material.repo.*;
import com.site.material.security.SecurityUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 盘点链路：登记账面/实盘差异 -> 确认调账（批次 qtyAdjust）。
 * 盘亏按责任归属进入复盘与核算口径（盘亏数量在批次调整后不再计入现场库存）。
 */
@Service
public class StockCheckService {

    private final StockCheckRepository checkRepo;
    private final MaterialBatchRepository batchRepo;
    private final MaterialRepository materialRepo;
    private final CodeGenerator codes;

    public StockCheckService(StockCheckRepository checkRepo, MaterialBatchRepository batchRepo,
                             MaterialRepository materialRepo, CodeGenerator codes) {
        this.checkRepo = checkRepo;
        this.batchRepo = batchRepo;
        this.materialRepo = materialRepo;
        this.codes = codes;
    }

    @Transactional
    public StockCheck create(StockCheckRequest req) {
        if (req.items() == null || req.items().isEmpty()) {
            throw new BusinessException("盘点明细不能为空");
        }
        StockCheck sc = new StockCheck();
        sc.setCode(codes.next("PD"));
        sc.setScope(req.scope());
        sc.setRemark(req.remark());
        sc.setRecorder(SecurityUtil.currentName());
        sc.setCreatedTime(LocalDateTime.now());
        sc.setStatus("COUNTING");

        Map<Long, List<MaterialBatch>> batches = batchRepo.findAll().stream()
                .collect(Collectors.groupingBy(MaterialBatch::getMaterialId));
        for (CheckItemIn in : req.items()) {
            List<MaterialBatch> bs = batches.getOrDefault(in.materialId(), List.of());
            MaterialBatch target = in.batchId() != null
                    ? bs.stream().filter(b -> b.getId().equals(in.batchId())).findFirst().orElse(null)
                    : bs.stream().findFirst().orElse(null);
            if (target == null) {
                throw new BusinessException("材料 " + in.materialId() + " 无入库批次，无法盘点");
            }
            StockCheckItem item = new StockCheckItem();
            item.setStockCheck(sc);
            item.setMaterialId(in.materialId());
            item.setBatchId(target.getId());
            item.setBookQty(target.onHand());
            item.setActualQty(in.actualQty());
            item.setUnitPrice(target.getUnitPrice());
            item.setResponsibility(in.responsibility() == null ? "COMPANY" : in.responsibility());
            item.setRemark(in.remark());
            sc.getItems().add(item);
        }
        return checkRepo.save(sc);
    }

    @Transactional
    public StockCheck adjust(Long id) {
        StockCheck sc = checkRepo.findById(id).orElseThrow(() -> new BusinessException("盘点单不存在"));
        if (!"COUNTING".equals(sc.getStatus())) {
            throw new BusinessException("仅盘点中单据可调账");
        }
        for (StockCheckItem item : sc.getItems()) {
            MaterialBatch b = batchRepo.findById(item.getBatchId())
                    .orElseThrow(() -> new BusinessException("批次不存在"));
            double diff = item.diffQty();
            // 将差异写入批次调整量，使账面等于实盘
            b.setQtyAdjust(b.getQtyAdjust() + diff);
            batchRepo.save(b);
        }
        sc.setStatus("ADJUSTED");
        sc.setConfirmer(SecurityUtil.currentName());
        sc.setAdjustTime(LocalDateTime.now());
        return checkRepo.save(sc);
    }

    public List<Map<String, Object>> list() {
        Map<Long, String> matNames = materialRepo.findAll().stream()
                .collect(Collectors.toMap(Material::getId, Material::getName));
        Map<Long, String> units = materialRepo.findAll().stream()
                .collect(Collectors.toMap(Material::getId, Material::getUnit));
        return checkRepo.findAllWithItems().stream().map(sc -> {
            List<Map<String, Object>> items = sc.getItems().stream().map(i -> {
                Map<String, Object> x = new LinkedHashMap<>();
                x.put("materialId", i.getMaterialId());
                x.put("materialName", matNames.get(i.getMaterialId()));
                x.put("unit", units.get(i.getMaterialId()));
                x.put("batchId", i.getBatchId());
                x.put("bookQty", i.getBookQty());
                x.put("actualQty", i.getActualQty());
                x.put("diffQty", MasterDataService.round(i.diffQty()));
                x.put("unitPrice", i.getUnitPrice());
                x.put("diffAmount", MasterDataService.round(i.diffQty() * i.getUnitPrice()));
                x.put("responsibility", i.getResponsibility());
                x.put("remark", i.getRemark());
                return x;
            }).toList();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", sc.getId());
            m.put("code", sc.getCode());
            m.put("scope", sc.getScope());
            m.put("status", sc.getStatus());
            m.put("recorder", sc.getRecorder());
            m.put("confirmer", sc.getConfirmer());
            m.put("createdTime", sc.getCreatedTime());
            m.put("adjustTime", sc.getAdjustTime());
            m.put("remark", sc.getRemark());
            m.put("items", items);
            m.put("diffTotal", MasterDataService.round(items.stream()
                    .mapToDouble(x -> (double) x.get("diffAmount")).sum()));
            return m;
        }).toList();
    }
}
