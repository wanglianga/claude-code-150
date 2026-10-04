package com.site.material.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 损耗记录：瓷砖破损、砂浆过期等。
 * 归属原因：DESIGN_CHANGE 设计变更 / CONSTRUCTION_ERROR 施工错误 /
 *           POOR_STORAGE 保管不当 / SUPPLIER_QUALITY 供应商质量 / NORMAL 合理损耗。
 * 责任承担：CREW 班组扣款 / SUPPLIER 供应商索赔 / PROJECT 项目承担。
 */
@Getter
@Setter
@Entity
@Table(name = "loss_record")
public class LossRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loss_no", nullable = false, unique = true, length = 40)
    private String lossNo;

    @Column(name = "material_id", nullable = false)
    private Long materialId;

    @Column(name = "batch_id")
    private Long batchId;

    @Column(name = "crew_id")
    private Long crewId;

    @Column(name = "building_id")
    private Long buildingId;

    @Column(name = "process_id")
    private Long processId;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal qty = BigDecimal.ZERO;

    @Column(name = "unit_price", precision = 14, scale = 2)
    private BigDecimal unitPrice;

    /** DESIGN_CHANGE / CONSTRUCTION_ERROR / POOR_STORAGE / SUPPLIER_QUALITY / NORMAL */
    @Column(name = "loss_reason", nullable = false, length = 20)
    private String lossReason;

    /** CREW / SUPPLIER / PROJECT */
    @Column(name = "responsibility", nullable = false, length = 12)
    private String responsibility = "PROJECT";

    @Column(name = "supplier_id")
    private Long supplierId;

    /** 关联变更签证号（若因变更产生） */
    @Column(name = "change_id")
    private Long changeId;

    @Column(name = "loss_date")
    private LocalDate lossDate;

    @Column(length = 255)
    private String remark;

    @Column(name = "created_by")
    private Long createdBy;

    /** 是否调减仓库批次库存：仓内损耗/盘点盘亏=true；已发料到现场后的损耗=false */
    @Column(name = "stock_adjusted")
    private Boolean stockAdjusted = false;
}
