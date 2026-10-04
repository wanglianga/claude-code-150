package com.site.material.controller;

import com.site.material.domain.StockCheck;
import com.site.material.dto.Requests.StockCheckRequest;
import com.site.material.service.StockCheckService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stock-checks")
public class StockCheckController {

    private final StockCheckService service;

    public StockCheckController(StockCheckService service) {
        this.service = service;
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        return service.list();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('KEEPER','PM')")
    public StockCheck create(@Valid @RequestBody StockCheckRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}/adjust")
    @PreAuthorize("hasAnyRole('KEEPER','PM')")
    public StockCheck adjust(@PathVariable Long id) {
        return service.adjust(id);
    }
}
