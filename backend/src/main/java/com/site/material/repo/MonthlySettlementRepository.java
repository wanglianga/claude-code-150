package com.site.material.repo;

import com.site.material.domain.MonthlySettlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MonthlySettlementRepository extends JpaRepository<MonthlySettlement, Long> {
    List<MonthlySettlement> findByPeriod(String period);
    List<MonthlySettlement> findByPeriodAndBuildingId(String period, Long buildingId);
    List<MonthlySettlement> findByPeriodAndCrewId(String period, Long crewId);
    long deleteByPeriod(String period);
}
