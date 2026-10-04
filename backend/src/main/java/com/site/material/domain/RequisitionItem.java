package com.site.material.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "requisition_item")
public class RequisitionItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requisition_id")
    private Requisition requisition;

    @Column(nullable = false)
    private Long materialId;

    /** 指定批次发料，可空（由仓管分配） */
    private Long batchId;

    /** 申请数量 */
    @Column(nullable = false)
    private Double applyQty;

    /** 实发数量（发料时填写，用于追加/少发场景） */
    private Double issuedQty = 0d;

    /** 发料单价（取批次移动口径单价快照） */
    private Double issuePrice = 0d;
}
