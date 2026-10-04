package com.site.material.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 材料入库批次：登记批次、规格、供应商、入库数量、质检结论和单价。
 * qtyOnHand 为当前仓库在账数量，发料扣减、退库回加、损耗/盘亏调减。
 */
@Getter
@Setter
@Entity
@Table(name = "material_batch")
public class MaterialBatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_no", nullable = false, unique = true, length = 40)
    private String batchNo;

    @Column(name = "material_id", nullable = false)
    private Long materialId;

    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(length = 100)
    private String spec;

    @Column(name = "qty_in", nullable = false, precision = 14, scale = 2)
    private BigDecimal qtyIn = BigDecimal.ZERO;

    @Column(name = "qty_on_hand", nullable = false, precision = 14, scale = 2)
    private BigDecimal qtyOnHand = BigDecimal.ZERO;

    @Column(name = "unit_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    /** PENDING 待检 / PASSED 合格 / FAILED 不合格（不合格批次冻结不得发料） */
    @Column(name = "qc_status", nullable = false, length = 10)
    private String qcStatus = "PENDING";

    @Column(name = "qc_note", length = 255)
    private String qcNote;

    @Column(name = "inbound_date")
    private LocalDate inboundDate;

    @Column(name = "created_by")
    private Long createdBy;
}
