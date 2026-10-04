package com.site.material.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "return_item")
public class ReturnItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_id")
    private MaterialReturn materialReturn;

    @Column(nullable = false)
    private Long materialId;

    /** 退回的来源批次 */
    private Long batchId;

    @Column(nullable = false)
    private Double qty;

    /** 退回时完好程度：GOOD 可重新发料 / DAMAGED 损坏降级 */
    @Column(length = 16)
    private String conditionType = "GOOD";

    private Double price = 0d;
}
