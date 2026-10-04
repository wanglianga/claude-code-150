package com.site.material.web;

import com.site.material.domain.MonthlySettlement;
import com.site.material.repo.MonthlySettlementRepository;
import com.site.material.service.NameResolver;
import com.site.material.service.SettlementService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/settlements")
public class SettlementController {

    private final SettlementService settlementService;
    private final MonthlySettlementRepository settlementRepository;
    private final NameResolver names;

    public SettlementController(SettlementService settlementService,
                                MonthlySettlementRepository settlementRepository, NameResolver names) {
        this.settlementService = settlementService;
        this.settlementRepository = settlementRepository;
        this.names = names;
    }

    @PostMapping("/generate")
    public List<Map<String, Object>> generate(@RequestParam String period) {
        return settlementService.generate(period).stream().map(this::view).toList();
    }

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam String period,
                                          @RequestParam(required = false) Long buildingId,
                                          @RequestParam(required = false) Long processId,
                                          @RequestParam(required = false) Long crewId) {
        return settlementRepository.findByPeriod(period).stream()
                .filter(s -> buildingId == null || buildingId.equals(s.getBuildingId()))
                .filter(s -> processId == null || processId.equals(s.getProcessId()))
                .filter(s -> crewId == null || crewId.equals(s.getCrewId()))
                .map(this::view)
                .toList();
    }

    private Map<String, Object> view(MonthlySettlement s) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", s.getId());
        m.put("period", s.getPeriod());
        m.put("buildingId", s.getBuildingId());
        m.put("buildingName", s.getBuildingId() == 0 ? "未指定" : names.buildingName(s.getBuildingId()));
        m.put("processId", s.getProcessId());
        m.put("processName", s.getProcessId() == 0 ? "未指定" : names.processName(s.getProcessId()));
        m.put("crewId", s.getCrewId());
        m.put("crewName", s.getCrewId() == 0 ? "公共/未指定" : names.crewName(s.getCrewId()));
        m.put("materialId", s.getMaterialId());
        m.put("materialName", names.materialName(s.getMaterialId()));
        m.put("unit", names.materialUnit(s.getMaterialId()));
        m.put("budgetQty", s.getBudgetQty());
        m.put("issuedQty", s.getIssuedQty());
        m.put("returnedQty", s.getReturnedQty());
        m.put("netQty", s.getNetQty());
        m.put("budgetCost", s.getBudgetCost());
        m.put("issuedCost", s.getIssuedCost());
        m.put("returnedCost", s.getReturnedCost());
        m.put("crewLossCost", s.getCrewLossCost());
        m.put("supplierClaimCost", s.getSupplierClaimCost());
        m.put("projectLossCost", s.getProjectLossCost());
        m.put("crewCost", s.getCrewCost());
        m.put("variance", s.getVariance());
        return m;
    }
}
