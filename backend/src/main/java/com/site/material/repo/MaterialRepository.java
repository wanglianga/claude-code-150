package com.site.material.repo;

import com.site.material.domain.Material;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaterialRepository extends JpaRepository<Material, Long> {
    List<Material> findByActiveTrueOrderByCategoryAscNameAsc();
}
