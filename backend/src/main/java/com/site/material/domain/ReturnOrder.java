package com.site.material.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退库单：临时追加未用、规格领错、施工变更等原因退回仓库。
 * 责任类型决定成本去向：REUSE 可复用（冲减班组成本）/ SCRAP 报废（计入责任成本）。
 */
@Getter
@Setter
@Entity
@Table(name = "return_order")
public class ReturnOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "return_no", nullable = false, unique = true, length = 40)
    private String returnNo;

    /** 来源领料单，可为空（现场余料退库） */
    @Column(name = "requisition_id")
    private Long requisitionId;

    @Column(name = "crew_id", nullable = false)
    private Long crewId;

    @Column(name = "building_id", nullable = false)
    private Long buildingId;

    @Column(name = "process_id", nullable = false)
    private Long processId;

    @Column(name = "material_id", nullable = false)
    private Long materialId;

    @Column(name = "batch_id")
    private Long batchId;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal qty = BigDecimal.ZERO;

    @Column(name = "unit_price", precision = 14, scale = 2)
    private BigDecimal unitPrice;

    /** WRONG_SPEC 规格领错 / CHANGE 施工变更 / EXTRA_APPEND 临时追加未用 / SURPLUS 现场余料 */
    @Column(nullable = false, length = 20)
    private String reason;

    /** REUSE 可复用回库 / SCRAP 报废不回库存价值（数量单独登记） */
    @Column(nullable = false, length = 10)
    private String disposition = "REUSE";

    /** DRAFT / CONFIRMED */
    @Column(nullable = false, length = 12)
    private String status = "DRAFT";

    @Column(length = 255)
    private String remark;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "confirmed_by")
    private Long confirmedBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;
}
