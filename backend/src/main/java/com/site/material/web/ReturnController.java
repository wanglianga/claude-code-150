package com.site.material.web;

import com.site.material.domain.ReturnOrder;
import com.site.material.repo.ReturnOrderRepository;
import com.site.material.service.ReturnService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/returns")
public class ReturnController {

    private final ReturnService returnService;
    private final ReturnOrderRepository returnRepository;

    public ReturnController(ReturnService returnService, ReturnOrderRepository returnRepository) {
        this.returnService = returnService;
        this.returnRepository = returnRepository;
    }

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam(required = false) Long crewId) {
        return (crewId == null ? returnRepository.findAllByOrderByIdDesc()
                : returnRepository.findByCrewIdOrderByIdDesc(crewId))
                .stream().map(returnService::view).toList();
    }

    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable Long id) {
        return returnService.view(returnService.mustGet(id));
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody ReturnOrder ro) {
        return returnService.view(returnService.create(ro));
    }

    @PostMapping("/{id}/confirm")
    public Map<String, Object> confirm(@PathVariable Long id) {
        return returnService.view(returnService.confirm(id));
    }
}
