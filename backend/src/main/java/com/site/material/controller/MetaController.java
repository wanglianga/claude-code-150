package com.site.material.controller;

import com.site.material.repo.CrewRepository;
import com.site.material.repo.MaterialRepository;
import com.site.material.repo.ProjectBudgetRepository;
import com.site.material.repo.SupplierRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 下拉选项等公共元数据（登录前也可访问，便于登录页展示演示信息） */
@RestController
@RequestMapping("/api/meta")
public class MetaController {

    private final MaterialRepository materialRepo;
    private final SupplierRepository supplierRepo;
    private final CrewRepository crewRepo;
    private final ProjectBudgetRepository budgetRepo;

    public MetaController(MaterialRepository materialRepo, SupplierRepository supplierRepo,
                         CrewRepository crewRepo, ProjectBudgetRepository budgetRepo) {
        this.materialRepo = materialRepo;
        this.supplierRepo = supplierRepo;
        this.crewRepo = crewRepo;
        this.budgetRepo = budgetRepo;
    }

    @GetMapping("/options")
    public Map<String, Object> options() {
        List<String> buildings = budgetRepo.findAll().stream()
                .map(b -> b.getBuilding()).distinct().sorted().toList();
        List<String> processes = budgetRepo.findAll().stream()
                .map(b -> b.getProcess()).distinct().sorted().toList();
        return Map.of(
                "materials", materialRepo.findByActiveTrueOrderByCategoryAscNameAsc(),
                "suppliers", supplierRepo.findAll(),
                "crews", crewRepo.findAll(),
                "buildings", buildings,
                "processes", processes);
    }
}
