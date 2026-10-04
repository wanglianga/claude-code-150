package com.site.material.repo;

import com.site.material.domain.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    Optional<Settlement> findByCrewIdAndPeriod(Long crewId, String period);
    List<Settlement> findByPeriodOrderByIdAsc(String period);
    List<Settlement> findAllByOrderByPeriodDescIdAsc();
}
