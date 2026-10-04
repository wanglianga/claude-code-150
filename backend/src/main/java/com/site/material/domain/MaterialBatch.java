package com.site.material.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 入库批次：仓库登记的批次、规格、供应商、入库数量、质检和单价。
 * 数量字段随领料/退库/损耗联动，是现场库存的账面包容器。
 */
@Getter
@Setter
@Entity
@Table(name = "material_batch")
public class MaterialBatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String batchNo;

    @Column(nullable = false)
    private Long materialId;

    @Column(length = 64)
    private String spec;

    private Long supplierId;

    /** 入库数量 */
    @Column(nullable = false)
    private Double qtyInbound = 0d;

    /** 累计实发数量 */
    @Column(nullable = false)
    private Double qtyIssued = 0d;

    /** 累计退库数量 */
    @Column(nullable = false)
    private Double qtyReturned = 0d;

    /** 累计损耗数量（破损/过期/保管不当等，直接从该批次核销） */
    @Column(nullable = false)
    private Double qtyWasted = 0d;

    /** 盘点调整数量（盘盈正、盘亏负） */
    @Column(nullable = false)
    private Double qtyAdjust = 0d;

    private Double unitPrice = 0d;

    /** PENDING / PASSED / FAILED / CONDITIONAL（让步接收） */
    @Column(length = 16)
    private String qcStatus = "PENDING";

    private LocalDateTime inboundTime;

    private String remark;

    /** 账面库存 = 入库 - 已发 + 退库 - 损耗 + 盘点调整 */
    public double onHand() {
        return qtyInbound - qtyIssued + qtyReturned - qtyWasted + qtyAdjust;
    }
}
