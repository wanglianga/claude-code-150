package com.site.material.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** 领料单明细，指定批次、申请量、实发量；实发按批次移动平均单价记成本 */
@Getter
@Setter
@Entity
@Table(name = "requisition_item")
public class RequisitionItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "requisition_id", nullable = false)
    private Long requisitionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requisition_id", insertable = false, updatable = false)
    private Requisition requisition;

    @Column(name = "material_id", nullable = false)
    private Long materialId;

    @Column(name = "batch_id")
    private Long batchId;

    @Column(name = "apply_qty", nullable = false, precision = 14, scale = 2)
    private BigDecimal applyQty = BigDecimal.ZERO;

    @Column(name = "issued_qty", precision = 14, scale = 2)
    private BigDecimal issuedQty;

    /** 发料时锁定的单价（批次单价） */
    @Column(name = "unit_price", precision = 14, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "remark", length = 255)
    private String remark;
}
