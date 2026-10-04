package com.site.material.repo;

import com.site.material.domain.ChangeVisa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ChangeVisaRepository extends JpaRepository<ChangeVisa, Long> {

    List<ChangeVisa> findAllByOrderByCreatedTimeDesc();

    @Query("select v from ChangeVisa v where v.crewId = :crewId and v.createdTime >= :from and v.createdTime < :to")
    List<ChangeVisa> findByCrewBetween(@Param("crewId") Long crewId,
                                       @Param("from") LocalDateTime from,
                                       @Param("to") LocalDateTime to);
}
