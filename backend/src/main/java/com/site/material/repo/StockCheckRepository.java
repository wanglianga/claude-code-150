package com.site.material.repo;

import com.site.material.domain.StockCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StockCheckRepository extends JpaRepository<StockCheck, Long> {

    @Query("select distinct s from StockCheck s left join fetch s.items order by s.createdTime desc")
    List<StockCheck> findAllWithItems();
}
