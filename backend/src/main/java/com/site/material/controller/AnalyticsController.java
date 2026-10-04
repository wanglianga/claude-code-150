package com.site.material.controller;

import com.site.material.service.AnalyticsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService service;

    public AnalyticsController(AnalyticsService service) {
        this.service = service;
    }

    /** 项目经理：按楼栋/工序/班组查看真实材料消耗与偏差 */
    @GetMapping("/consumption")
    public List<Map<String, Object>> consumption(@RequestParam(required = false) String building,
                                                 @RequestParam(required = false) String process,
                                                 @RequestParam(required = false) Long crewId) {
        return service.consumption(building, process, crewId);
    }

    /** 材料员月底复盘：损耗原因（设计变更/施工错误/保管不当/供应商质量） */
    @GetMapping("/waste-review")
    @PreAuthorize("hasAnyRole('KEEPER','PM','FINANCE')")
    public Map<String, Object> wasteReview() {
        return service.wasteReview();
    }

    /** 预算员：实际消耗反推后续采购计划与分包结算 */
    @GetMapping("/procurement-plan")
    @PreAuthorize("hasAnyRole('BUDGET','PM')")
    public Map<String, Object> procurementPlan(@RequestParam(required = false) String building) {
        return service.procurementPlan(building);
    }

    /** 财务：供应商结算 / 班组扣款 / 变更索赔 */
    @GetMapping("/finance")
    @PreAuthorize("hasAnyRole('FINANCE','PM')")
    public Map<String, Object> finance() {
        return service.financeSummary();
    }
}
