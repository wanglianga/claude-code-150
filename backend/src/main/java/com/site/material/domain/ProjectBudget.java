package com.site.material.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 材料预算：按 楼栋/施工区域/工序/图纸 + 材料 维度编制，
 * 作为领料核对（预算量、已领量）和班组核算的基准。
 */
@Getter
@Setter
@Entity
@Table(name = "project_budget")
public class ProjectBudget {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String building;

    @Column(length = 64)
    private String area;

    @Column(nullable = false, length = 64)
    private String process;

    @Column(length = 64)
    private String drawingNo;

    @Column(nullable = false)
    private Long materialId;

    @Column(nullable = false)
    private Double budgetQty;

    /** 预算单价（预算成本口径，可与采购单价不同） */
    private Double budgetPrice = 0d;

    /** 归口班组（可空，表示跨班组共用预算） */
    private Long crewId;
}
