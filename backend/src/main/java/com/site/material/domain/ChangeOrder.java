package com.site.material.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 变更签证：设计/施工变更引起材料增减，是损耗与成本归属（变更索赔）的依据。
 * 状态：DRAFT 草稿 / APPROVED 已批准（进入索赔与核算）/ REJECTED。
 */
@Getter
@Setter
@Entity
@Table(name = "change_order")
public class ChangeOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "change_no", nullable = false, unique = true, length = 40)
    private String changeNo;

    @Column(name = "building_id", nullable = false)
    private Long buildingId;

    @Column(name = "process_id")
    private Long processId;

    @Column(name = "material_id")
    private Long materialId;

    @Column(name = "crew_id")
    private Long crewId;

    /** DESIGN 设计变更 / CONSTRUCTION 施工变更 */
    @Column(nullable = false, length = 15)
    private String type;

    @Column(name = "change_qty", precision = 14, scale = 2)
    private BigDecimal changeQty = BigDecimal.ZERO;

    @Column(name = "claim_amount", precision = 14, scale = 2)
    private BigDecimal claimAmount = BigDecimal.ZERO;

    /** DRAFT / APPROVED / REJECTED */
    @Column(nullable = false, length = 10)
    private String status = "DRAFT";

    @Column(name = "change_date")
    private LocalDate changeDate;

    @Column(length = 255)
    private String description;

    @Column(name = "created_by")
    private Long createdBy;
}
