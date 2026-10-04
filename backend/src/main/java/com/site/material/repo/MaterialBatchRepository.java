package com.site.material.repo;

import com.site.material.domain.MaterialBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaterialBatchRepository extends JpaRepository<MaterialBatch, Long> {
    List<MaterialBatch> findByMaterialIdOrderByInboundTimeDesc(Long materialId);
    List<MaterialBatch> findByQcStatusNotOrderByInboundTimeDesc(String qcStatus);
}
