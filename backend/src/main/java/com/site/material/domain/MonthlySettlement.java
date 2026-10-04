package com.site.material.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 月度班组材料核算结果（按 月份+楼栋+工序+班组+材料 落一行）：
 * 预算成本、实际领用、可复用退库冲减、报废/损耗（区分责任）、
 * 班组应承担成本、供应商索赔、项目承担、偏差。
 */
@Getter
@Setter
@Entity
@Table(name = "monthly_settlement",
        uniqueConstraints = @UniqueConstraint(name = "uk_settlement",
                columnNames = {"period", "building_id", "process_id", "crew_id", "material_id"}))
public class MonthlySettlement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** yyyy-MM */
    @Column(nullable = false, length = 7)
    private String period;

    @Column(name = "building_id", nullable = false)
    private Long buildingId;

    @Column(name = "process_id", nullable = false)
    private Long processId;

    @Column(name = "crew_id", nullable = false)
    private Long crewId;

    @Column(name = "material_id", nullable = false)
    private Long materialId;

    @Column(name = "budget_qty", precision = 14, scale = 2)
    private BigDecimal budgetQty = BigDecimal.ZERO;

    @Column(name = "issued_qty", precision = 14, scale = 2)
    private BigDecimal issuedQty = BigDecimal.ZERO;

    @Column(name = "returned_qty", precision = 14, scale = 2)
    private BigDecimal returnedQty = BigDecimal.ZERO;

    /** 净耗 = 实发 - 可复用退库 */
    @Column(name = "net_qty", precision = 14, scale = 2)
    private BigDecimal netQty = BigDecimal.ZERO;

    @Column(name = "budget_cost", precision = 16, scale = 2)
    private BigDecimal budgetCost = BigDecimal.ZERO;

    @Column(name = "issued_cost", precision = 16, scale = 2)
    private BigDecimal issuedCost = BigDecimal.ZERO;

    @Column(name = "returned_cost", precision = 16, scale = 2)
    private BigDecimal returnedCost = BigDecimal.ZERO;

    /** 班组责任损耗（施工错误/保管不当等） */
    @Column(name = "crew_loss_cost", precision = 16, scale = 2)
    private BigDecimal crewLossCost = BigDecimal.ZERO;

    /** 供应商质量索赔 */
    @Column(name = "supplier_claim_cost", precision = 16, scale = 2)
    private BigDecimal supplierClaimCost = BigDecimal.ZERO;

    /** 项目承担（合理损耗/设计变更） */
    @Column(name = "project_loss_cost", precision = 16, scale = 2)
    private BigDecimal projectLossCost = BigDecimal.ZERO;

    /** 班组材料成本 = 实发 - 可复用退库 + 班组责任损耗 */
    @Column(name = "crew_cost", precision = 16, scale = 2)
    private BigDecimal crewCost = BigDecimal.ZERO;

    /** 偏差 = 班组成本 - 预算成本 */
    @Column(name = "variance", precision = 16, scale = 2)
    private BigDecimal variance = BigDecimal.ZERO;
}
