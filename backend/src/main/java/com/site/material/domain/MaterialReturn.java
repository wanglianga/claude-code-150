package com.site.material.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 退库单：临时追加多余、规格领错、施工变更等原因退回。
 * 状态：SUBMITTED -> CONFIRMED(仓管确认入库) / REJECTED
 */
@Getter
@Setter
@Entity
@Table(name = "material_return")
public class MaterialReturn {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32, unique = true)
    private String code;

    private Long crewId;

    /** 退库关联的原领料单（可空，说明无单退库） */
    private Long requisitionId;

    @Column(nullable = false, length = 32)
    private String building;

    @Column(length = 64)
    private String area;

    @Column(length = 64)
    private String process;

    /** SPEC_WRONG 规格领错 / SURPLUS 临时追加多余 / CHANGE 施工变更 / OTHER */
    @Column(nullable = false, length = 16)
    private String reasonType;

    /** SUBMITTED / CONFIRMED / REJECTED */
    @Column(nullable = false, length = 16)
    private String status = "SUBMITTED";

    private String applicant;
    private String confirmedBy;
    private LocalDateTime applyTime;
    private LocalDateTime confirmTime;
    private String remark;

    @OneToMany(mappedBy = "materialReturn", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReturnItem> items = new ArrayList<>();
}
