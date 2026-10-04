package com.site.material.repo;

import com.site.material.domain.Requisition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RequisitionRepository extends JpaRepository<Requisition, Long> {
    Optional<Requisition> findByReqNo(String reqNo);
    List<Requisition> findByCrewIdOrderByIdDesc(Long crewId);
    List<Requisition> findByBuildingIdOrderByIdDesc(Long buildingId);
    List<Requisition> findAllByOrderByIdDesc();
}
