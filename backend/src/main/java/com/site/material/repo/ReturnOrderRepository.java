package com.site.material.repo;

import com.site.material.domain.ReturnOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReturnOrderRepository extends JpaRepository<ReturnOrder, Long> {
    Optional<ReturnOrder> findByReturnNo(String returnNo);
    List<ReturnOrder> findAllByOrderByIdDesc();
    List<ReturnOrder> findByCrewIdOrderByIdDesc(Long crewId);
}
