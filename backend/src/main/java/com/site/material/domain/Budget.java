package com.site.material.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** 楼栋+工序+材料维度的预算量 */
@Getter
@Setter
@Entity
@Table(name = "budget",
        uniqueConstraints = @UniqueConstraint(name = "uk_budget", columnNames = {"building_id", "process_id", "material_id"}))
public class Budget {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "building_id", nullable = false)
    private Long buildingId;

    @Column(name = "process_id", nullable = false)
    private Long processId;

    @Column(name = "material_id", nullable = false)
    private Long materialId;

    @Column(name = "budget_qty", nullable = false, precision = 14, scale = 2)
    private BigDecimal budgetQty = BigDecimal.ZERO;

    /** 形象进度%（0-100）：预算员据此用实际消耗反推后续采购量 */
    @Column(name = "progress_percent", precision = 5, scale = 1)
    private BigDecimal progressPercent = new BigDecimal("100");
}
