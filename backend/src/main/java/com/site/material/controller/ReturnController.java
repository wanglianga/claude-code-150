package com.site.material.controller;

import com.site.material.domain.MaterialReturn;
import com.site.material.dto.Requests.ReturnRequest;
import com.site.material.service.ReturnService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/returns")
public class ReturnController {

    private final ReturnService service;

    public ReturnController(ReturnService service) {
        this.service = service;
    }

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam(required = false) String status,
                                          @RequestParam(required = false) Long crewId) {
        return service.list(status, crewId);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('FOREMAN','KEEPER','PM')")
    public MaterialReturn create(@Valid @RequestBody ReturnRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('KEEPER','PM')")
    public MaterialReturn confirm(@PathVariable Long id) {
        return service.confirm(id);
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('KEEPER','PM')")
    public MaterialReturn reject(@PathVariable Long id) {
        return service.reject(id);
    }
}
