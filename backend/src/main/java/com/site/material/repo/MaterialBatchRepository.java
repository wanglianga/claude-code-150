package com.site.material.repo;

import com.site.material.domain.MaterialBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MaterialBatchRepository extends JpaRepository<MaterialBatch, Long> {
    Optional<MaterialBatch> findByBatchNo(String batchNo);
    List<MaterialBatch> findByMaterialIdOrderByInboundDateAscIdAsc(Long materialId);
    List<MaterialBatch> findByQcStatus(String qcStatus);
}
