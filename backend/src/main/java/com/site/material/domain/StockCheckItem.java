package com.site.material.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "stock_check_item")
public class StockCheckItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_check_id", nullable = false)
    private Long stockCheckId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_check_id", insertable = false, updatable = false)
    private StockCheck stockCheck;

    @Column(name = "batch_id", nullable = false)
    private Long batchId;

    @Column(name = "material_id", nullable = false)
    private Long materialId;

    @Column(name = "book_qty", nullable = false, precision = 14, scale = 2)
    private BigDecimal bookQty = BigDecimal.ZERO;

    @Column(name = "actual_qty", precision = 14, scale = 2)
    private BigDecimal actualQty;

    /** 盘亏责任归属：CREW / SUPPLIER / PROJECT，默认保管不当由项目承担，财务可调整 */
    @Column(name = "responsibility", length = 12)
    private String responsibility = "PROJECT";

    @Column(length = 255)
    private String remark;
}
