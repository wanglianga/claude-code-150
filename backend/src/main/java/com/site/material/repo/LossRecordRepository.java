package com.site.material.repo;

import com.site.material.domain.LossRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LossRecordRepository extends JpaRepository<LossRecord, Long> {
    Optional<LossRecord> findByLossNo(String lossNo);
    List<LossRecord> findAllByOrderByIdDesc();
    List<LossRecord> findByMaterialId(Long materialId);
}
