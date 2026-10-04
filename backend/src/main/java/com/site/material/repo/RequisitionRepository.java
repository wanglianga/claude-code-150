package com.site.material.repo;

import com.site.material.domain.Requisition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RequisitionRepository extends JpaRepository<Requisition, Long> {

    @Query("select distinct r from Requisition r left join fetch r.items where r.id = :id")
    Optional<Requisition> findByIdWithItems(@Param("id") Long id);

    List<Requisition> findByCrewIdOrderByApplyTimeDesc(Long crewId);

    @Query("select distinct r from Requisition r left join fetch r.items where r.status = :status order by r.applyTime desc")
    List<Requisition> findByStatus(@Param("status") String status);

    @Query("select distinct r from Requisition r left join fetch r.items order by r.applyTime desc")
    List<Requisition> findAllWithItems();

    @Query("select distinct r from Requisition r left join fetch r.items where r.issueTime >= :from and r.issueTime < :to order by r.applyTime desc")
    List<Requisition> findIssuedBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select distinct r from Requisition r left join fetch r.items where r.crewId = :crewId and r.issueTime >= :from and r.issueTime < :to")
    List<Requisition> findIssuedByCrewBetween(@Param("crewId") Long crewId,
                                              @Param("from") LocalDateTime from,
                                              @Param("to") LocalDateTime to);
}
