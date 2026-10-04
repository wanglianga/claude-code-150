package com.site.material.service;

import com.site.material.domain.*;
import com.site.material.repo.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 小表名称解析，供列表视图拼装冗余名称字段，避免前端多次请求 */
@Component
public class NameResolver {

    private final MaterialRepository materialRepository;
    private final SupplierRepository supplierRepository;
    private final CrewRepository crewRepository;
    private final BuildingRepository buildingRepository;
    private final WorkProcessRepository processRepository;

    public NameResolver(MaterialRepository materialRepository, SupplierRepository supplierRepository,
                        CrewRepository crewRepository, BuildingRepository buildingRepository,
                        WorkProcessRepository processRepository) {
        this.materialRepository = materialRepository;
        this.supplierRepository = supplierRepository;
        this.crewRepository = crewRepository;
        this.buildingRepository = buildingRepository;
        this.processRepository = processRepository;
    }

    public Map<String, Object> batchView(MaterialBatch b) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", b.getId());
        m.put("batchNo", b.getBatchNo());
        m.put("materialId", b.getMaterialId());
        m.put("supplierId", b.getSupplierId());
        m.put("spec", b.getSpec());
        m.put("qtyIn", b.getQtyIn());
        m.put("qtyOnHand", b.getQtyOnHand());
        m.put("unitPrice", b.getUnitPrice());
        m.put("qcStatus", b.getQcStatus());
        m.put("qcNote", b.getQcNote());
        m.put("inboundDate", b.getInboundDate());
        Material mat = materialRepository.findById(b.getMaterialId()).orElse(null);
        if (mat != null) {
            m.put("materialCode", mat.getCode());
            m.put("materialName", mat.getName());
            m.put("category", mat.getCategory());
            m.put("unit", mat.getUnit());
            if (b.getSpec() == null) {
                m.put("spec", mat.getSpec());
            }
        }
        if (b.getSupplierId() != null) {
            supplierRepository.findById(b.getSupplierId())
                    .ifPresent(s -> m.put("supplierName", s.getName()));
        }
        BigDecimal price = b.getUnitPrice() == null ? BigDecimal.ZERO : b.getUnitPrice();
        BigDecimal onHand = b.getQtyOnHand() == null ? BigDecimal.ZERO : b.getQtyOnHand();
        m.put("stockValue", price.multiply(onHand));
        return m;
    }

    public Map<String, Long> materialIds() {
        Map<String, Long> map = new HashMap<>();
        materialRepository.findAll().forEach(m -> map.put(m.getName(), m.getId()));
        return map;
    }

    public String materialName(Long id) {
        return materialRepository.findById(id).map(Material::getName).orElse("");
    }

    public String materialUnit(Long id) {
        return materialRepository.findById(id).map(Material::getUnit).orElse("");
    }

    public String supplierName(Long id) {
        return id == null ? "" : supplierRepository.findById(id).map(Supplier::getName).orElse("");
    }

    public String crewName(Long id) {
        return id == null ? "" : crewRepository.findById(id).map(Crew::getName).orElse("");
    }

    public String buildingName(Long id) {
        return id == null ? "" : buildingRepository.findById(id).map(Building::getName).orElse("");
    }

    public String processName(Long id) {
        return id == null ? "" : processRepository.findById(id).map(WorkProcess::getName).orElse("");
    }

    public List<Material> materials() {
        return materialRepository.findAll();
    }
}
