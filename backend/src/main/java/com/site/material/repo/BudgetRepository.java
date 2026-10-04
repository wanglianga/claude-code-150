package com.site.material.repo;

import com.site.material.domain.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByBuildingId(Long buildingId);
    List<Budget> findByBuildingIdAndProcessId(Long buildingId, Long processId);
    Optional<Budget> findByBuildingIdAndProcessIdAndMaterialId(Long buildingId, Long processId, Long materialId);
}
