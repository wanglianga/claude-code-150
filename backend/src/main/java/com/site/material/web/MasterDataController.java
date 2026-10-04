package com.site.material.web;

import com.site.material.domain.*;
import com.site.material.repo.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** 主数据：班组、楼栋、工序、供应商、材料目录、预算 */
@RestController
@RequestMapping("/api/masters")
public class MasterDataController {

    private final CrewRepository crewRepository;
    private final BuildingRepository buildingRepository;
    private final WorkProcessRepository processRepository;
    private final SupplierRepository supplierRepository;
    private final MaterialRepository materialRepository;
    private final BudgetRepository budgetRepository;

    public MasterDataController(CrewRepository crewRepository, BuildingRepository buildingRepository,
                                WorkProcessRepository processRepository, SupplierRepository supplierRepository,
                                MaterialRepository materialRepository, BudgetRepository budgetRepository) {
        this.crewRepository = crewRepository;
        this.buildingRepository = buildingRepository;
        this.processRepository = processRepository;
        this.supplierRepository = supplierRepository;
        this.materialRepository = materialRepository;
        this.budgetRepository = budgetRepository;
    }

    @GetMapping("/crews")
    public List<Crew> crews() {
        return crewRepository.findAll();
    }

    @PostMapping("/crews")
    public Crew saveCrew(@RequestBody Crew crew) {
        return crewRepository.save(crew);
    }

    @GetMapping("/buildings")
    public List<Building> buildings() {
        return buildingRepository.findAll();
    }

    @PostMapping("/buildings")
    public Building saveBuilding(@RequestBody Building building) {
        return buildingRepository.save(building);
    }

    @GetMapping("/processes")
    public List<WorkProcess> processes() {
        return processRepository.findAll();
    }

    @PostMapping("/processes")
    public WorkProcess saveProcess(@RequestBody WorkProcess process) {
        return processRepository.save(process);
    }

    @GetMapping("/suppliers")
    public List<Supplier> suppliers() {
        return supplierRepository.findAll();
    }

    @PostMapping("/suppliers")
    public Supplier saveSupplier(@RequestBody Supplier supplier) {
        return supplierRepository.save(supplier);
    }

    @GetMapping("/materials")
    public List<Material> materials() {
        return materialRepository.findAll();
    }

    @PostMapping("/materials")
    public Material saveMaterial(@RequestBody Material material) {
        return materialRepository.save(material);
    }

    @GetMapping("/budgets")
    public List<Budget> budgets(@RequestParam(required = false) Long buildingId,
                                @RequestParam(required = false) Long processId) {
        if (buildingId != null && processId != null) {
            return budgetRepository.findByBuildingIdAndProcessId(buildingId, processId);
        }
        if (buildingId != null) {
            return budgetRepository.findByBuildingId(buildingId);
        }
        return budgetRepository.findAll();
    }

    public record BudgetRequest(Long buildingId, Long processId, Long materialId,
                                BigDecimal budgetQty, BigDecimal progressPercent) {
    }

    @PostMapping("/budgets")
    public Budget saveBudget(@RequestBody BudgetRequest req) {
        Budget budget = budgetRepository
                .findByBuildingIdAndProcessIdAndMaterialId(req.buildingId(), req.processId(), req.materialId())
                .orElseGet(Budget::new);
        budget.setBuildingId(req.buildingId());
        budget.setProcessId(req.processId());
        budget.setMaterialId(req.materialId());
        budget.setBudgetQty(req.budgetQty() == null ? BigDecimal.ZERO : req.budgetQty());
        if (req.progressPercent() != null) {
            budget.setProgressPercent(req.progressPercent());
        }
        return budgetRepository.save(budget);
    }
}
