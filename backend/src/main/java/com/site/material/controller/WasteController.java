package com.site.material.controller;

import com.site.material.domain.WasteRecord;
import com.site.material.dto.Requests.WasteRequest;
import com.site.material.service.WasteService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wastes")
public class WasteController {

    private final WasteService service;

    public WasteController(WasteService service) {
        this.service = service;
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        return service.list();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('KEEPER','PM')")
    public WasteRecord create(@Valid @RequestBody WasteRequest req) {
        return service.create(req);
    }
}
