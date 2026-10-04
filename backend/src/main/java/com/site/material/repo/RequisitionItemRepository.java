package com.site.material.repo;

import com.site.material.domain.RequisitionItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RequisitionItemRepository extends JpaRepository<RequisitionItem, Long> {
    List<RequisitionItem> findByRequisitionId(Long requisitionId);
    List<RequisitionItem> findByMaterialId(Long materialId);
}
