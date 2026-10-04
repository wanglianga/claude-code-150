package com.site.material.repo;

import com.site.material.domain.StockCheckItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockCheckItemRepository extends JpaRepository<StockCheckItem, Long> {
    List<StockCheckItem> findByStockCheckId(Long stockCheckId);
}
