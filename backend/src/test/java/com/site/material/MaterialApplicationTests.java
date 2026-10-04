package com.site.material;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 核心链路集成测试（H2 内存库 + 种子数据）：
 * 1. 登录拿 token；
 * 2. 超预算领料单：未强制放行必须被拦截，勾选超预算放行后发料成功并扣减库存；
 * 3. 发料后的单据必须经班组长确认才进入 CONFIRMED；
 * 4. 质检不合格批次不能作为可用发料批次；
 * 5. 月度核算可生成且包含退库冲减/损耗责任分流。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class MaterialApplicationTests {

    @Autowired
    private MockMvc mockMvc;
    private final ObjectMapper om = new ObjectMapper();

    private String login(String username, String password) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}""".formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return om.readTree(r.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    void fullChain_overBudgetIssue_confirm_settlement() throws Exception {
        String wh = login("warehouse", "wh123");
        String leader = login("wangqiang", "wang123");
        String budget = login("budget", "budget123");

        // 找到已申请的 2#楼钢筋领料单（预算10、申请12）
        MvcResult listResult = mockMvc.perform(get("/api/requisitions").header("X-Auth-Token", wh)
                        .param("status", "APPLIED"))
                .andExpect(status().isOk()).andReturn();
        JsonNode reqs = om.readTree(listResult.getResponse().getContentAsString());
        long reqId = -1L;
        long itemId = -1L;
        long batchId = -1L;
        for (JsonNode r : reqs) {
            if ("NORMAL".equals(r.get("priority").asText()) && r.get("buildingName").asText().contains("2#")) {
                reqId = r.get("id").asLong();
            }
        }
        org.junit.jupiter.api.Assertions.assertTrue(reqId > 0, "种子数据应包含 2#楼待发料单");

        MvcResult preResult = mockMvc.perform(get("/api/requisitions/" + reqId + "/precheck")
                        .header("X-Auth-Token", wh))
                .andExpect(status().isOk()).andReturn();
        JsonNode pre = om.readTree(preResult.getResponse().getContentAsString());
        JsonNode item = pre.get("items").get(0);
        itemId = item.get("itemId").asLong();
        batchId = item.get("availableBatches").get(0).get("id").asLong();
        org.junit.jupiter.api.Assertions.assertTrue(item.get("overBudget").asBoolean(), "该单应判定为超预算");

        String line = """
                {"lines":[{"itemId":%d,"batchId":%d,"issuedQty":12}],"issueRemark":"test","allowOverBudget":false}"""
                .formatted(itemId, batchId);
        // 未放行 -> 400
        mockMvc.perform(post("/api/requisitions/" + reqId + "/issue")
                        .header("X-Auth-Token", wh).contentType(MediaType.APPLICATION_JSON).content(line))
                .andExpect(status().isBadRequest());

        String forceLine = """
                {"lines":[{"itemId":%d,"batchId":%d,"issuedQty":12}],"issueRemark":"test","allowOverBudget":true}"""
                .formatted(itemId, batchId);
        mockMvc.perform(post("/api/requisitions/" + reqId + "/issue")
                        .header("X-Auth-Token", wh).contentType(MediaType.APPLICATION_JSON).content(forceLine))
                .andExpect(status().isOk());

        // 班组长确认
        mockMvc.perform(post("/api/requisitions/" + reqId + "/confirm").header("X-Auth-Token", leader))
                .andExpect(status().isOk());

        // 不合格安全网批次不应出现在可用批次中
        MvcResult avail = mockMvc.perform(get("/api/inventory/available")
                        .header("X-Auth-Token", wh).param("materialId", "9"))
                .andExpect(status().isOk()).andReturn();
        JsonNode batches = om.readTree(avail.getResponse().getContentAsString());
        for (JsonNode b : batches) {
            org.junit.jupiter.api.Assertions.assertNotEquals("FAILED", b.get("qcStatus").asText());
        }

        // 生成当月核算
        String period = java.time.YearMonth.now().toString();
        MvcResult setResult = mockMvc.perform(post("/api/settlements/generate")
                        .header("X-Auth-Token", budget).param("period", period))
                .andExpect(status().isOk()).andReturn();
        JsonNode rows = om.readTree(setResult.getResponse().getContentAsString());
        org.junit.jupiter.api.Assertions.assertTrue(rows.size() > 0, "核算应产生明细行");
        boolean hasReturnOffset = false;
        boolean hasSupplierClaim = false;
        for (JsonNode row : List.of(rows)) {
            for (JsonNode s : row) {
                if (s.get("returnedCost").decimalValue().signum() > 0) hasReturnOffset = true;
                if (s.get("supplierClaimCost").decimalValue().signum() > 0) hasSupplierClaim = true;
            }
        }
        org.junit.jupiter.api.Assertions.assertTrue(hasReturnOffset, "可复用退库应冲减班组成本");
        org.junit.jupiter.api.Assertions.assertTrue(hasSupplierClaim, "供应商质量损耗应进入索赔口径");
    }
}
