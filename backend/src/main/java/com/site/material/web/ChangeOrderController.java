package com.site.material.web;

import com.site.material.common.AuthContext;
import com.site.material.common.BusinessException;
import com.site.material.common.DocNoGenerator;
import com.site.material.domain.ChangeOrder;
import com.site.material.repo.ChangeOrderRepository;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** 变更签证：批准后进入变更索赔口径，财务据此对业主索赔/对班组调整 */
@RestController
@RequestMapping("/api/changes")
public class ChangeOrderController {

    private final ChangeOrderRepository changeRepository;
    private final DocNoGenerator docNo;

    public ChangeOrderController(ChangeOrderRepository changeRepository, DocNoGenerator docNo) {
        this.changeRepository = changeRepository;
        this.docNo = docNo;
    }

    @GetMapping
    public List<ChangeOrder> list(@RequestParam(required = false) String status) {
        return changeRepository.findAllByOrderByIdDesc().stream()
                .filter(c -> status == null || status.isBlank() || status.equals(c.getStatus()))
                .toList();
    }

    @PostMapping
    public ChangeOrder create(@RequestBody ChangeOrder c) {
        if (c.getBuildingId() == null) {
            throw new BusinessException("请选择楼栋");
        }
        c.setId(null);
        c.setChangeNo(docNo.next("BG"));
        c.setStatus("DRAFT");
        if (c.getChangeDate() == null) {
            c.setChangeDate(LocalDate.now());
        }
        c.setCreatedBy(AuthContext.userId());
        return changeRepository.save(c);
    }

    @PostMapping("/{id}/approve")
    public ChangeOrder approve(@PathVariable Long id) {
        ChangeOrder c = mustGet(id);
        c.setStatus("APPROVED");
        return changeRepository.save(c);
    }

    @PostMapping("/{id}/reject")
    public ChangeOrder reject(@PathVariable Long id) {
        ChangeOrder c = mustGet(id);
        c.setStatus("REJECTED");
        return changeRepository.save(c);
    }

    private ChangeOrder mustGet(Long id) {
        return changeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("变更签证不存在：" + id));
    }
}
