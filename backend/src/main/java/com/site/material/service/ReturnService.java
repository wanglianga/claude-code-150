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
 * 退库链路：规格领错/临时追加多余/施工变更退料 -> 仓管确认入库 -> 回补批次库存。
 * 损坏退回（DAMAGED）不回补库存，按损耗口径核销，责任在班组。
 */
@Service
public class ReturnService {

    private final MaterialReturnRepository returnRepo;
    private final MaterialBatchRepository batchRepo;
    private final MaterialRepository materialRepo;
    private final CrewRepository crewRepo;
    private final CodeGenerator codes;

    public ReturnService(MaterialReturnRepository returnRepo, MaterialBatchRepository batchRepo,
                         MaterialRepository materialRepo, CrewRepository crewRepo, CodeGenerator codes) {
        this.returnRepo = returnRepo;
        this.batchRepo = batchRepo;
        this.materialRepo = materialRepo;
        this.crewRepo = crewRepo;
        this.codes = codes;
    }

    @Transactional
    public MaterialReturn create(ReturnRequest req) {
        if (req.items() == null || req.items().isEmpty()) {
            throw new BusinessException("退库明细不能为空");
        }
        crewRepo.findById(req.crewId()).orElseThrow(() -> new BusinessException("班组不存在"));
        MaterialReturn r = new MaterialReturn();
        r.setCode(codes.next("TK"));
        r.setCrewId(req.crewId());
        r.setRequisitionId(req.requisitionId());
        r.setBuilding(req.building());
        r.setArea(req.area());
        r.setProcess(req.process());
        r.setReasonType(req.reasonType());
        r.setRemark(req.remark());
        r.setApplicant(SecurityUtil.currentName());
        r.setApplyTime(LocalDateTime.now());
        r.setStatus("SUBMITTED");
        for (ItemIn it : req.items()) {
            if (it.qty() == null || it.qty() <= 0) {
                throw new BusinessException("退库数量必须大于 0");
            }
            ReturnItem item = new ReturnItem();
            item.setMaterialReturn(r);
            item.setMaterialId(it.materialId());
            item.setBatchId(it.batchId());
            item.setQty(it.qty());
            item.setConditionType(it.conditionType() == null ? "GOOD" : it.conditionType());
            item.setPrice(it.price() == null ? 0d : it.price());
            r.getItems().add(item);
        }
        return returnRepo.save(r);
    }

    @Transactional
    public MaterialReturn confirm(Long id) {
        MaterialReturn r = returnRepo.findById(id).orElseThrow(() -> new BusinessException("退库单不存在"));
        if (!"SUBMITTED".equals(r.getStatus())) {
            throw new BusinessException("仅待确认退库单可确认，当前状态: " + r.getStatus());
        }
        Map<Long, MaterialBatch> batchMap = batchRepo.findAll().stream()
                .collect(Collectors.toMap(MaterialBatch::getId, b -> b));
        for (ReturnItem it : r.getItems()) {
            if ("GOOD".equals(it.getConditionType())) {
                // 完好退库：回补指定批次库存（累加 qtyReturned）
                MaterialBatch batch = pickBatch(it, batchMap);
                if (batch == null) {
                    throw new BusinessException("退库材料缺少有效批次，请联系材料员");
                }
                if (it.getPrice() == null || it.getPrice() == 0d) {
                    it.setPrice(batch.getUnitPrice());
                }
                batch.setQtyReturned(batch.getQtyReturned() + it.getQty());
            }
            // DAMAGED 损坏退回：不回补，由损耗登记环节按班组责任核销
        }
        r.setStatus("CONFIRMED");
        r.setConfirmedBy(SecurityUtil.currentName());
        r.setConfirmTime(LocalDateTime.now());
        return returnRepo.save(r);
    }

    private MaterialBatch pickBatch(ReturnItem it, Map<Long, MaterialBatch> batchMap) {
        if (it.getBatchId() != null) {
            return batchMap.get(it.getBatchId());
        }
        return batchMap.values().stream()
                .filter(b -> b.getMaterialId().equals(it.getMaterialId()))
                .findFirst().orElse(null);
    }

    @Transactional
    public MaterialReturn reject(Long id) {
        MaterialReturn r = returnRepo.findById(id).orElseThrow(() -> new BusinessException("退库单不存在"));
        if (!"SUBMITTED".equals(r.getStatus())) {
            throw new BusinessException("仅待确认退库单可驳回");
        }
        r.setStatus("REJECTED");
        return r;
    }

    public List<Map<String, Object>> list(String status, Long crewId) {
        return returnRepo.findAllWithItems().stream()
                .filter(r -> status == null || status.equals(r.getStatus()))
                .filter(r -> crewId == null || crewId.equals(r.getCrewId()))
                .map(this::view).toList();
    }

    public Map<String, Object> view(MaterialReturn r) {
        Map<Long, String> matNames = materialRepo.findAll().stream()
                .collect(Collectors.toMap(Material::getId, Material::getName));
        Map<Long, String> units = materialRepo.findAll().stream()
                .collect(Collectors.toMap(Material::getId, Material::getUnit));
        Map<Long, String> crewNames = crewRepo.findAll().stream()
                .collect(Collectors.toMap(Crew::getId, Crew::getName));
        List<Map<String, Object>> items = r.getItems().stream().map(i -> {
            Map<String, Object> x = new LinkedHashMap<>();
            x.put("materialId", i.getMaterialId());
            x.put("materialName", matNames.get(i.getMaterialId()));
            x.put("unit", units.get(i.getMaterialId()));
            x.put("batchId", i.getBatchId());
            x.put("qty", i.getQty());
            x.put("conditionType", i.getConditionType());
            x.put("price", i.getPrice());
            x.put("amount", MasterDataService.round(i.getQty() * i.getPrice()));
            return x;
        }).toList();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("code", r.getCode());
        m.put("crewId", r.getCrewId());
        m.put("crewName", crewNames.get(r.getCrewId()));
        m.put("requisitionId", r.getRequisitionId());
        m.put("building", r.getBuilding());
        m.put("area", r.getArea());
        m.put("process", r.getProcess());
        m.put("reasonType", r.getReasonType());
        m.put("status", r.getStatus());
        m.put("applicant", r.getApplicant());
        m.put("confirmedBy", r.getConfirmedBy());
        m.put("applyTime", r.getApplyTime());
        m.put("confirmTime", r.getConfirmTime());
        m.put("remark", r.getRemark());
        m.put("items", items);
        m.put("totalAmount", MasterDataService.round(items.stream()
                .mapToDouble(x -> (double) x.get("amount")).sum()));
        return m;
    }
}
