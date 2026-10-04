package com.site.material.bootstrap;

import com.site.material.domain.*;
import com.site.material.repo.*;
import com.site.material.service.SettlementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 演示种子数据：覆盖入库质检、领料全链路、退库、损耗、变更、盘点等场景。 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Value("${app.seed.enabled:true}")
    private boolean seedEnabled;

    private final SysUserRepository userRepository;
    private final CrewRepository crewRepository;
    private final BuildingRepository buildingRepository;
    private final WorkProcessRepository processRepository;
    private final SupplierRepository supplierRepository;
    private final MaterialRepository materialRepository;
    private final MaterialBatchRepository batchRepository;
    private final BudgetRepository budgetRepository;
    private final RequisitionRepository requisitionRepository;
    private final RequisitionItemRepository itemRepository;
    private final ReturnOrderRepository returnRepository;
    private final LossRecordRepository lossRepository;
    private final ChangeOrderRepository changeRepository;
    private final StockCheckRepository checkRepository;
    private final StockCheckItemRepository checkItemRepository;
    private final SettlementService settlementService;

    public DataInitializer(SysUserRepository userRepository, CrewRepository crewRepository,
                           BuildingRepository buildingRepository, WorkProcessRepository processRepository,
                           SupplierRepository supplierRepository, MaterialRepository materialRepository,
                           MaterialBatchRepository batchRepository, BudgetRepository budgetRepository,
                           RequisitionRepository requisitionRepository, RequisitionItemRepository itemRepository,
                           ReturnOrderRepository returnRepository, LossRecordRepository lossRepository,
                           ChangeOrderRepository changeRepository, StockCheckRepository checkRepository,
                           StockCheckItemRepository checkItemRepository,
                           SettlementService settlementService) {
        this.userRepository = userRepository;
        this.crewRepository = crewRepository;
        this.buildingRepository = buildingRepository;
        this.processRepository = processRepository;
        this.supplierRepository = supplierRepository;
        this.materialRepository = materialRepository;
        this.batchRepository = batchRepository;
        this.budgetRepository = budgetRepository;
        this.requisitionRepository = requisitionRepository;
        this.itemRepository = itemRepository;
        this.returnRepository = returnRepository;
        this.lossRepository = lossRepository;
        this.changeRepository = changeRepository;
        this.checkRepository = checkRepository;
        this.checkItemRepository = checkItemRepository;
        this.settlementService = settlementService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedEnabled || userRepository.count() > 0) {
            return;
        }
        LocalDate today = LocalDate.now();
        LocalDateTime t0 = today.minusDays(2).atTime(9, 0);

        // ---------- 用户 ----------
        user("admin", "admin123", "系统管理员", "ADMIN", null);
        user("pm", "pm123", "项明（项目经理）", "PROJECT_MANAGER", null);
        user("warehouse", "wh123", "仓管老吴", "WAREHOUSE", null);
        user("finance", "fin123", "财务小周", "FINANCE", null);
        user("budget", "budget123", "预算员小钱", "BUDGET", null);

        // ---------- 班组 ----------
        Crew c1 = crew("钢筋一班", "钢筋", "王强", "13800000001");
        Crew c2 = crew("防水班组", "防水", "李水", "13800000002");
        Crew c3 = crew("镶贴班组", "镶贴", "张贴", "13800000003");
        Crew c4 = crew("水电安装班组", "水电", "陈电", "13800000004");
        Crew c5 = crew("架子安全班组", "架子安全", "赵安全", "13800000005");
        Crew c6 = crew("砌筑班组", "砌筑", "孙磊", "13800000006");
        user("wangqiang", "wang123", "王强", "CREW_LEADER", c1.getId());
        user("lishui", "li123", "李水", "CREW_LEADER", c2.getId());
        user("zhangtie", "zhang123", "张贴", "CREW_LEADER", c3.getId());
        user("chendian", "chen123", "陈电", "CREW_LEADER", c4.getId());
        user("zhaoanquan", "zhao123", "赵安全", "CREW_LEADER", c5.getId());
        user("sunlei", "sun123", "孙磊", "CREW_LEADER", c6.getId());

        // ---------- 楼栋 / 工序 ----------
        Building b1 = building("1#", "1#住宅楼", "阳光里安置房项目");
        Building b2 = building("2#", "2#住宅楼", "阳光里安置房项目");
        Building b3 = building("DX", "地下车库", "阳光里安置房项目");
        WorkProcess p1 = process("GJ", "钢筋绑扎");
        WorkProcess p2 = process("FS", "防水施工");
        WorkProcess p3 = process("XT", "墙地砖铺贴");
        WorkProcess p4 = process("XG", "线管预埋");
        WorkProcess p5 = process("QZ", "砌筑抹灰");
        WorkProcess p6 = process("AQ", "安全防护搭设");

        // ---------- 供应商 ----------
        Supplier s1 = supplier("宏发钢铁贸易有限公司", "刘经理", "13911110001", "钢筋及辅材");
        Supplier s2 = supplier("雨神防水材料有限公司", "孙经理", "13922220002", "防水卷材");
        Supplier s3 = supplier("金陶建材经营部", "周老板", "13933330003", "瓷砖");
        Supplier s4 = supplier("恒通水电材料公司", "吴经理", "13944440004", "线管五金");
        Supplier s5 = supplier("磐石砂浆站", "郑调度", "13955550005", "商品砂浆");
        Supplier s6 = supplier("安捷防护用品厂", "王厂长", "13966660006", "安全防护用品");

        // ---------- 材料 ----------
        Material m1 = material("GJ-16", "螺纹钢 HRB400E Φ16", "钢筋辅材", "Φ16mm", "吨");
        Material m2 = material("GJ-ZS", "绑扎扎丝 22#", "钢筋辅材", "22#", "卷");
        Material m3 = material("FS-SBS", "SBS改性沥青防水卷材 4mm", "防水卷材", "4mm×1m×10m", "卷");
        Material m4 = material("CZ-800", "通体瓷砖 800×800", "瓷砖", "800×800mm", "箱");
        Material m5 = material("XG-PVC20", "PVC阻燃线管 DN20", "线管", "DN20", "米");
        Material m6 = material("SJ-M5", "干混砌筑砂浆 M5", "砂浆", "M5 袋装", "吨");
        Material m7 = material("WJ-M10", "膨胀螺栓 M10×80", "五金", "M10×80", "套");
        Material m8 = material("AQ-AQM", "ABS安全帽 V型", "安全防护用品", "V型", "顶");
        Material m9 = material("AQ-AQW", "密目安全网 2000目", "安全防护用品", "1.8×6m", "张");

        // ---------- 入库批次（在账数量与后续业务保持一致） ----------
        MaterialBatch bm1 = batch("RK-20260905-001", m1, s1, "Φ16mm", bd("50"), bd("32"), bd("4200"), "PASSED", "复检合格", today.minusDays(28));
        MaterialBatch bm2 = batch("RK-20260906-001", m2, s1, "22#", bd("200"), bd("125"), bd("35"), "PASSED", "合格", today.minusDays(27));
        MaterialBatch bm3 = batch("RK-20260908-001", m3, s2, "4mm", bd("50"), bd("13"), bd("280"), "PASSED", "合格", today.minusDays(25));
        MaterialBatch bm4 = batch("RK-20260910-001", m4, s3, "800×800", bd("150"), bd("82"), bd("180"), "PASSED", "合格", today.minusDays(23));
        MaterialBatch bm5 = batch("RK-20260912-001", m5, s4, "DN20", bd("5000"), bd("3700"), bd("3.5"), "PASSED", "合格", today.minusDays(21));
        MaterialBatch bm6 = batch("RK-20260915-001", m6, s5, "M5", bd("60"), bd("47"), bd("320"), "PASSED", "合格", today.minusDays(18));
        MaterialBatch bm7 = batch("RK-20260918-001", m7, s4, "M10×80", bd("2000"), bd("2000"), bd("1.2"), "PASSED", "合格", today.minusDays(15));
        MaterialBatch bm8 = batch("RK-20260920-001", m8, s6, "V型", bd("100"), bd("80"), bd("25"), "PASSED", "合格", today.minusDays(13));
        MaterialBatch bm9 = batch("RK-20260922-001", m9, s6, "1.8×6m", bd("50"), bd("35"), bd("45"), "PASSED", "合格", today.minusDays(11));
        // 供应商补货延迟：刚到货待检
        batch("RK-20261001-001", m4, s3, "800×800", bd("100"), bd("100"), bd("180"), "PENDING",
                "供应商补货延迟14天，昨夜紧急到货，待质检", today.minusDays(1));
        // 质检不合格冻结
        batch("RK-20260925-001", m9, s6, "1.8×6m", bd("10"), bd("10"), bd("42"), "FAILED",
                "强力检测不达标，冻结并要求供应商退场", today.minusDays(8));
        batchRepository.flush();

        // ---------- 预算 ----------
        budget(b1, p1, m1, bd("20"), bd("100"));
        budget(b1, p1, m2, bd("100"), bd("100"));
        budget(b2, p1, m1, bd("10"), bd("100"));
        budget(b3, p2, m3, bd("50"), bd("70"));
        budget(b1, p3, m4, bd("80"), bd("70"));
        budget(b2, p3, m4, bd("60"), bd("0"));   // 2#楼瓷砖尚未开工，后续采购反推需考虑
        budget(b2, p4, m5, bd("1500"), bd("80"));
        budget(b1, p5, m6, bd("15"), bd("40"));
        budget(b1, p6, m8, bd("30"), bd("100"));
        budget(b1, p6, m9, bd("20"), bd("75"));

        // ---------- 领料单（本月内） ----------
        confirmedReq("LL-20260928-001", c1, b1, p1, "1#楼3层 A-C轴", "结施-03", "NORMAL", t0,
                List.of(line(m1, bm1, bd("18"), bd("18"), bd("4200")),
                        line(m2, bm2, bd("80"), bd("80"), bd("35"))));
        confirmedReq("LL-20260929-001", c2, b3, p2, "地下车库顶板南区", "建防-12", "NORMAL", t0.plusDays(1),
                List.of(line(m3, bm3, bd("40"), bd("40"), bd("280"))));
        confirmedReq("LL-20261001-001", c3, b1, p3, "1#楼2层东户", "建装-08", "NORMAL", t0.plusDays(3),
                List.of(line(m4, bm4, bd("70"), bd("60"), bd("180"))));
        confirmedReq("LL-20261002-001", c4, b2, p4, "2#楼5层墙板", "电施-05", "NORMAL", t0.plusDays(4),
                List.of(line(m5, bm5, bd("1200"), bd("1200"), bd("3.5"))));
        confirmedReq("LL-20261002-002", c5, b1, p6, "1#楼外架首层", "安施-02", "NORMAL", t0.plusDays(4),
                List.of(line(m8, bm8, bd("20"), bd("20"), bd("25")),
                        line(m9, bm9, bd("15"), bd("15"), bd("45"))));
        confirmedReq("LL-20261002-003", c6, b1, p5, "1#楼2层砌体", "建施-06", "NORMAL", t0.plusDays(4),
                List.of(line(m6, bm6, bd("10"), bd("10"), bd("320"))));
        // 待发料：超预算申请（2#楼钢筋预算10吨，申请12吨）——发料核对演示
        appliedReq("LL-20261003-001", c1, b2, p1, "2#楼2层 B轴", "结施-11", "NORMAL", t0.plusDays(5),
                List.of(line(m1, bm1, bd("12"), null, null)));
        // 夜间紧急领料（待发料）
        appliedReq("LL-20261003-002", c3, b1, p3, "1#楼3层西户抢工", "建装-09", "URGENT", t0.plusDays(5),
                List.of(line(m4, bm4, bd("30"), null, null)));

        // ---------- 退库 ----------
        // 临时追加未用，可复用回库（bm2 已含回补后在账 125）
        ReturnOrder rt1 = new ReturnOrder();
        rt1.setReturnNo("TK-20261002-001");
        rt1.setRequisitionId(null);
        rt1.setCrewId(c1.getId());
        rt1.setBuildingId(b1.getId());
        rt1.setProcessId(p1.getId());
        rt1.setMaterialId(m2.getId());
        rt1.setBatchId(bm2.getId());
        rt1.setQty(bd("5"));
        rt1.setUnitPrice(bd("35"));
        rt1.setReason("EXTRA_APPEND");
        rt1.setDisposition("REUSE");
        rt1.setStatus("CONFIRMED");
        rt1.setRemark("临时追加的扎丝未用完退库");
        rt1.setCreatedAt(t0.plusDays(6));
        rt1.setConfirmedAt(t0.plusDays(6));
        returnRepository.save(rt1);

        // 规格领错报废：计入班组责任成本，不回库存
        ReturnOrder rt2 = new ReturnOrder();
        rt2.setReturnNo("TK-20261002-002");
        rt2.setCrewId(c3.getId());
        rt2.setBuildingId(b1.getId());
        rt2.setProcessId(p3.getId());
        rt2.setMaterialId(m4.getId());
        rt2.setBatchId(bm4.getId());
        rt2.setQty(bd("5"));
        rt2.setUnitPrice(bd("180"));
        rt2.setReason("WRONG_SPEC");
        rt2.setDisposition("SCRAP");
        rt2.setStatus("CONFIRMED");
        rt2.setRemark("领成600×600规格，开箱后无法退货，作报废处理");
        rt2.setCreatedAt(t0.plusDays(7));
        rt2.setConfirmedAt(t0.plusDays(7));
        returnRepository.save(rt2);

        // 施工变更退库（草稿，待仓管确认）
        ReturnOrder rt3 = new ReturnOrder();
        rt3.setReturnNo("TK-20261003-001");
        rt3.setCrewId(c2.getId());
        rt3.setBuildingId(b3.getId());
        rt3.setProcessId(p2.getId());
        rt3.setMaterialId(m3.getId());
        rt3.setBatchId(bm3.getId());
        rt3.setQty(bd("3"));
        rt3.setUnitPrice(bd("280"));
        rt3.setReason("CHANGE");
        rt3.setDisposition("REUSE");
        rt3.setStatus("DRAFT");
        rt3.setRemark("车库顶板防水范围调整，余料待退");
        rt3.setCreatedAt(t0.plusDays(8));
        returnRepository.save(rt3);

        // ---------- 损耗 ----------
        // 瓷砖破损：施工错误，班组责任
        LossRecord l1 = new LossRecord();
        l1.setLossNo("SH-20261001-001");
        l1.setMaterialId(m4.getId());
        l1.setBatchId(bm4.getId());
        l1.setCrewId(c3.getId());
        l1.setBuildingId(b1.getId());
        l1.setProcessId(p3.getId());
        l1.setQty(bd("10"));
        l1.setUnitPrice(bd("180"));
        l1.setLossReason("CONSTRUCTION_ERROR");
        l1.setResponsibility("CREW");
        l1.setStockAdjusted(false);
        l1.setLossDate(today.minusDays(2));
        l1.setRemark("铺贴切割失误及搬运破损共10箱（已发料现场损耗，不扣仓库库存）");
        lossRepository.save(l1);

        // 砂浆过期：保管不当，项目承担
        LossRecord l2 = new LossRecord();
        l2.setLossNo("SH-20261002-001");
        l2.setMaterialId(m6.getId());
        l2.setBatchId(bm6.getId());
        l2.setLossReason("POOR_STORAGE");
        l2.setResponsibility("PROJECT");
        l2.setQty(bd("3"));
        l2.setUnitPrice(bd("320"));
        l2.setStockAdjusted(true);
        l2.setLossDate(today.minusDays(1));
        l2.setRemark("仓库堆放超过适用期结块报废3吨，保管不当，调减库存");
        lossRepository.save(l2);

        // 线管壁厚不达标：供应商质量索赔
        LossRecord l3 = new LossRecord();
        l3.setLossNo("SH-20261002-002");
        l3.setMaterialId(m5.getId());
        l3.setBatchId(bm5.getId());
        l3.setCrewId(c4.getId());
        l3.setBuildingId(b2.getId());
        l3.setProcessId(p4.getId());
        l3.setQty(bd("100"));
        l3.setUnitPrice(bd("3.5"));
        l3.setLossReason("SUPPLIER_QUALITY");
        l3.setResponsibility("SUPPLIER");
        l3.setSupplierId(s4.getId());
        l3.setStockAdjusted(true);
        l3.setLossDate(today.minusDays(1));
        l3.setRemark("抽检壁厚不达标，100米退场并向供应商索赔，调减库存");
        lossRepository.save(l3);

        // 设计变更：旧排版备料8箱无法用于新方案，项目承担，关联已批准签证
        LossRecord l4 = new LossRecord();
        l4.setLossNo("SH-20260930-001");
        l4.setMaterialId(m4.getId());
        l4.setBatchId(bm4.getId());
        l4.setCrewId(c3.getId());
        l4.setBuildingId(b1.getId());
        l4.setProcessId(p3.getId());
        l4.setQty(bd("8"));
        l4.setUnitPrice(bd("180"));
        l4.setLossReason("DESIGN_CHANGE");
        l4.setResponsibility("PROJECT");
        l4.setStockAdjusted(true);
        l4.setLossDate(today.minusDays(2));
        l4.setRemark("卫生间设计变更，旧排版备料8箱无法使用，项目承担，调减库存");
        lossRepository.save(l4);

        // 合理损耗：钢筋下料切割合理损耗0.3吨，项目承担，已在现场不扣库存
        LossRecord l5 = new LossRecord();
        l5.setLossNo("SH-20261001-002");
        l5.setMaterialId(m1.getId());
        l5.setCrewId(c1.getId());
        l5.setBuildingId(b1.getId());
        l5.setProcessId(p1.getId());
        l5.setQty(bd("0.3"));
        l5.setUnitPrice(bd("4200"));
        l5.setLossReason("NORMAL");
        l5.setResponsibility("PROJECT");
        l5.setStockAdjusted(false);
        l5.setLossDate(today.minusDays(2));
        l5.setRemark("钢筋下料切割合理损耗0.3吨，定额内项目承担");
        lossRepository.save(l5);

        // ---------- 变更签证 ----------
        ChangeOrder co1 = new ChangeOrder();
        co1.setChangeNo("BG-20260930-001");
        co1.setBuildingId(b1.getId());
        co1.setProcessId(p3.getId());
        co1.setMaterialId(m4.getId());
        co1.setCrewId(c3.getId());
        co1.setType("DESIGN");
        co1.setChangeQty(bd("15"));
        co1.setClaimAmount(bd("2700"));
        co1.setStatus("APPROVED");
        co1.setChangeDate(today.minusDays(2));
        co1.setDescription("设计变更：卫生间排版方案调整，增加瓷砖15箱");
        changeRepository.save(co1);

        ChangeOrder co2 = new ChangeOrder();
        co2.setChangeNo("BG-20261002-001");
        co2.setBuildingId(b3.getId());
        co2.setProcessId(p2.getId());
        co2.setMaterialId(m3.getId());
        co2.setCrewId(c2.getId());
        co2.setType("CONSTRUCTION");
        co2.setChangeQty(bd("8"));
        co2.setClaimAmount(bd("2240"));
        co2.setStatus("DRAFT");
        co2.setChangeDate(today.minusDays(1));
        co2.setDescription("施工变更：地下车库基层做法调整，签证待审批");
        changeRepository.save(co2);

        // 注：盘点单不预置（账面快照需在盘点发起时即时生成，避免与后续发料不一致），
        // 由仓管在“库存盘点”页一键创建。

        // 预生成当月班组材料核算，使各角色看板登录即有数据；业务发生后可在“月度核算”页重算覆盖
        int rows = settlementService.generate(java.time.YearMonth.now().toString()).size();
        log.info("种子数据初始化完成：用户/班组/楼栋/材料批次/预算/领料/退库/损耗/变更，并生成当月核算 {} 行", rows);
    }

    private void user(String username, String pwd, String realName, String role, Long crewId) {
        SysUser u = new SysUser();
        u.setUsername(username);
        u.setPassword(pwd);
        u.setRealName(realName);
        u.setRole(role);
        u.setCrewId(crewId);
        userRepository.save(u);
    }

    private Crew crew(String name, String trade, String leader, String phone) {
        Crew c = new Crew();
        c.setName(name);
        c.setTrade(trade);
        c.setLeaderName(leader);
        c.setPhone(phone);
        return crewRepository.save(c);
    }

    private Building building(String code, String name, String project) {
        Building b = new Building();
        b.setCode(code);
        b.setName(name);
        b.setProjectName(project);
        return buildingRepository.save(b);
    }

    private WorkProcess process(String code, String name) {
        WorkProcess p = new WorkProcess();
        p.setCode(code);
        p.setName(name);
        return processRepository.save(p);
    }

    private Supplier supplier(String name, String contact, String phone, String category) {
        Supplier s = new Supplier();
        s.setName(name);
        s.setContact(contact);
        s.setPhone(phone);
        s.setCategory(category);
        return supplierRepository.save(s);
    }

    private Material material(String code, String name, String category, String spec, String unit) {
        Material m = new Material();
        m.setCode(code);
        m.setName(name);
        m.setCategory(category);
        m.setSpec(spec);
        m.setUnit(unit);
        return materialRepository.save(m);
    }

    private MaterialBatch batch(String no, Material m, Supplier s, String spec,
                                BigDecimal qtyIn, BigDecimal onHand, BigDecimal price,
                                String qc, String qcNote, LocalDate date) {
        MaterialBatch b = new MaterialBatch();
        b.setBatchNo(no);
        b.setMaterialId(m.getId());
        b.setSupplierId(s.getId());
        b.setSpec(spec);
        b.setQtyIn(qtyIn);
        b.setQtyOnHand(onHand);
        b.setUnitPrice(price);
        b.setQcStatus(qc);
        b.setQcNote(qcNote);
        b.setInboundDate(date);
        return batchRepository.save(b);
    }

    private void budget(Building b, WorkProcess p, Material m, BigDecimal qty) {
        budget(b, p, m, qty, new BigDecimal("100"));
    }

    private void budget(Building b, WorkProcess p, Material m, BigDecimal qty, BigDecimal progress) {
        Budget bg = new Budget();
        bg.setBuildingId(b.getId());
        bg.setProcessId(p.getId());
        bg.setMaterialId(m.getId());
        bg.setBudgetQty(qty);
        bg.setProgressPercent(progress);
        budgetRepository.save(bg);
    }

    private RequisitionItem line(Material m, MaterialBatch b, BigDecimal apply, BigDecimal issued, BigDecimal price) {
        RequisitionItem it = new RequisitionItem();
        it.setMaterialId(m.getId());
        it.setBatchId(b == null ? null : b.getId());
        it.setApplyQty(apply);
        it.setIssuedQty(issued);
        it.setUnitPrice(price);
        return it;
    }

    private void confirmedReq(String no, Crew c, Building b, WorkProcess p, String area, String drawing,
                              String priority, LocalDateTime base, List<RequisitionItem> lines) {
        Requisition r = new Requisition();
        r.setReqNo(no);
        r.setCrewId(c.getId());
        r.setBuildingId(b.getId());
        r.setProcessId(p.getId());
        r.setWorkArea(area);
        r.setDrawingNo(drawing);
        r.setPriority(priority);
        r.setStatus("CONFIRMED");
        r.setAppliedAt(base);
        r.setIssuedAt(base.plusHours(2));
        r.setConfirmedAt(base.plusHours(5));
        requisitionRepository.save(r);
        requisitionRepository.flush();
        for (RequisitionItem it : lines) {
            it.setRequisitionId(r.getId());
            itemRepository.save(it);
        }
    }

    private void appliedReq(String no, Crew c, Building b, WorkProcess p, String area, String drawing,
                            String priority, LocalDateTime base, List<RequisitionItem> lines) {
        Requisition r = new Requisition();
        r.setReqNo(no);
        r.setCrewId(c.getId());
        r.setBuildingId(b.getId());
        r.setProcessId(p.getId());
        r.setWorkArea(area);
        r.setDrawingNo(drawing);
        r.setPriority(priority);
        r.setStatus("APPLIED");
        r.setAppliedAt(base);
        r.setApplyRemark(priority.equals("URGENT") ? "夜间紧急抢工领料，请仓管立即发料" : null);
        requisitionRepository.save(r);
        requisitionRepository.flush();
        for (RequisitionItem it : lines) {
            it.setRequisitionId(r.getId());
            itemRepository.save(it);
        }
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
