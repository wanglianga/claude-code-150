package com.site.material.repo;

import com.site.material.domain.ChangeOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChangeOrderRepository extends JpaRepository<ChangeOrder, Long> {
    Optional<ChangeOrder> findByChangeNo(String changeNo);
    List<ChangeOrder> findAllByOrderByIdDesc();
}
