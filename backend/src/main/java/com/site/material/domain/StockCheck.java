package com.site.material.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 盘点单：月底/不定期盘点，发现账实不符。
 * 明细按批次记录账面数量与实盘数量；确认后盘亏生成损耗（POOR_STORAGE 默认，可改归属），盘盈冲回库存。
 */
@Getter
@Setter
@Entity
@Table(name = "stock_check")
public class StockCheck {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "check_no", nullable = false, unique = true, length = 40)
    private String checkNo;

    @Column(name = "check_date", nullable = false)
    private LocalDate checkDate;

    /** DRAFT / CONFIRMED */
    @Column(nullable = false, length = 12)
    private String status = "DRAFT";

    @Column(length = 255)
    private String remark;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @OneToMany(mappedBy = "stockCheck", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StockCheckItem> items = new ArrayList<>();
}
