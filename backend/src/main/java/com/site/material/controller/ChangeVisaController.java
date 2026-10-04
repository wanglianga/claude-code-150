package com.site.material.controller;

import com.site.material.domain.ChangeVisa;
import com.site.material.dto.Requests.VisaRequest;
import com.site.material.service.ChangeVisaService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/visas")
public class ChangeVisaController {

    private final ChangeVisaService service;

    public ChangeVisaController(ChangeVisaService service) {
        this.service = service;
    }

    @GetMapping
    public List<ChangeVisa> list() {
        return service.list();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PM','BUDGET','KEEPER')")
    public ChangeVisa create(@Valid @RequestBody VisaRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('PM','BUDGET')")
    public ChangeVisa confirm(@PathVariable Long id) {
        return service.confirm(id);
    }

    @PutMapping("/{id}/claim")
    @PreAuthorize("hasRole('FINANCE')")
    public ChangeVisa claim(@PathVariable Long id) {
        return service.markClaimed(id);
    }
}
