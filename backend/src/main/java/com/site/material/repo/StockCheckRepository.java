package com.site.material.repo;

import com.site.material.domain.StockCheck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockCheckRepository extends JpaRepository<StockCheck, Long> {
    List<StockCheck> findAllByOrderByIdDesc();
}
