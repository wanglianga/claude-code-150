package com.site.material.repo;

import com.site.material.domain.ProjectBudget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectBudgetRepository extends JpaRepository<ProjectBudget, Long> {
    List<ProjectBudget> findByBuildingAndProcessAndMaterialId(String building, String process, Long materialId);
    List<ProjectBudget> findByBuildingOrderByProcessAsc(String building);
}
