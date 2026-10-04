package com.site.material.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 盘点单：账实不符时登记，逐项盘盈/盘亏，经确认后调整批次账面库存。
 * 状态：COUNTING(盘点中) -> ADJUSTED(已调账)
 */
@Getter
@Setter
@Entity
@Table(name = "stock_check")
public class StockCheck {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32, unique = true)
    private String code;

    @Column(length = 64)
    private String scope;

    /** COUNTING / ADJUSTED */
    @Column(nullable = false, length = 16)
    private String status = "COUNTING";

    private String recorder;
    private String confirmer;
    private LocalDateTime createdTime;
    private LocalDateTime adjustTime;
    private String remark;

    @OneToMany(mappedBy = "stockCheck", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StockCheckItem> items = new ArrayList<>();
}
