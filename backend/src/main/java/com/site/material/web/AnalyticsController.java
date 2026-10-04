package com.site.material.web;

import com.site.material.service.AnalyticsService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 角色分析看板：偏差、财务、采购反推、损耗复盘 */
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analytics;

    public AnalyticsController(AnalyticsService analytics) {
        this.analytics = analytics;
    }

    /** 项目经理：按楼栋/工序/班组的材料偏差 */
    @GetMapping("/deviation")
    public List<Map<String, Object>> deviation(@RequestParam String period) {
        return analytics.deviation(period);
    }

    /** 财务：供应商结算、班组扣款、变更索赔 */
    @GetMapping("/finance")
    public Map<String, Object> finance(@RequestParam String period) {
        return analytics.finance(period);
    }

    /** 预算员：实际消耗反推采购计划与分包结算 */
    @GetMapping("/procurement")
    public Map<String, Object> procurement(@RequestParam String period) {
        return analytics.procurement(period);
    }

    /** 材料员：损耗原因复盘 */
    @GetMapping("/loss-review")
    public Map<String, Object> lossReview(@RequestParam String period) {
        return analytics.lossReview(period);
    }
}
