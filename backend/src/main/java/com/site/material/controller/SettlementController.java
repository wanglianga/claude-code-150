package com.site.material.controller;

import com.site.material.domain.Settlement;
import com.site.material.dto.Requests.GenerateSettlementRequest;
import com.site.material.service.SettlementService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/settlements")
public class SettlementController {

    private final SettlementService service;

    public SettlementController(SettlementService service) {
        this.service = service;
    }

    @GetMapping
    public List<Settlement> list(@RequestParam(required = false) String period) {
        return service.list(period);
    }

    @GetMapping("/{id}")
    public Settlement detail(@PathVariable Long id) {
        return service.detail(id);
    }

    /** 生成某班组（不传 crewId 则全部班组）某月核算单 */
    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('PM','BUDGET','FINANCE')")
    public Object generate(@Valid @RequestBody GenerateSettlementRequest req) {
        if (req.crewId() == null) {
            return service.generateAll(req.period());
        }
        return service.generate(req.period(), req.crewId());
    }

    @PutMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('FINANCE','PM')")
    public Settlement confirm(@PathVariable Long id) {
        return service.confirm(id);
    }
}
