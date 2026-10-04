package com.site.material.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 领料单：班组按施工区域、楼栋、工序和图纸申请领料。
 * 状态：DRAFT 草稿 -> APPLIED 已申请 -> ISSUED 已发料 -> CONFIRMED 班组长已确认（材料进入现场消耗）
 *       -> REJECTED 驳回；紧急单 NORMAL/URGENT，夜间紧急领料直接发料后补确认。
 */
@Getter
@Setter
@Entity
@Table(name = "requisition")
public class Requisition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "req_no", nullable = false, unique = true, length = 40)
    private String reqNo;

    @Column(name = "crew_id", nullable = false)
    private Long crewId;

    @Column(name = "building_id", nullable = false)
    private Long buildingId;

    @Column(name = "process_id", nullable = false)
    private Long processId;

    /** 施工区域（楼层/轴线） */
    @Column(name = "work_area", length = 100)
    private String workArea;

    /** 图纸编号 */
    @Column(name = "drawing_no", length = 50)
    private String drawingNo;

    /** NORMAL 正常 / URGENT 夜间紧急 */
    @Column(nullable = false, length = 10)
    private String priority = "NORMAL";

    /** DRAFT / APPLIED / ISSUED / CONFIRMED / REJECTED */
    @Column(nullable = false, length = 12)
    private String status = "DRAFT";

    @Column(name = "apply_remark", length = 255)
    private String applyRemark;

    @Column(name = "issue_remark", length = 255)
    private String issueRemark;

    @Column(name = "reject_reason", length = 255)
    private String rejectReason;

    @Column(name = "applied_by")
    private Long appliedBy;

    @Column(name = "issued_by")
    private Long issuedBy;

    @Column(name = "confirmed_by")
    private Long confirmedBy;

    @Column(name = "applied_at")
    private LocalDateTime appliedAt;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @OneToMany(mappedBy = "requisition", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RequisitionItem> items = new ArrayList<>();
}
