package com.site.material.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "stock_check_item")
public class StockCheckItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "check_id")
    private StockCheck stockCheck;

    @Column(nullable = false)
    private Long materialId;

    private Long batchId;

    /** 账面数量 */
    @Column(nullable = false)
    private Double bookQty;

    /** 实盘数量 */
    @Column(nullable = false)
    private Double actualQty;

    /** 单价（核算盘盈盘亏金额） */
    private Double unitPrice = 0d;

    /** 盘亏责任归属，同 WasteRecord.responsibility */
    @Column(length = 16)
    private String responsibility = "COMPANY";

    private String remark;

    public double diffQty() {
        return actualQty - bookQty;
    }
}
