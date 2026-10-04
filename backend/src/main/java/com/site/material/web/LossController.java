package com.site.material.web;

import com.site.material.domain.LossRecord;
import com.site.material.repo.LossRecordRepository;
import com.site.material.service.LossService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/losses")
public class LossController {

    private final LossService lossService;
    private final LossRecordRepository lossRepository;

    public LossController(LossService lossService, LossRecordRepository lossRepository) {
        this.lossService = lossService;
        this.lossRepository = lossRepository;
    }

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam(required = false) String lossReason,
                                          @RequestParam(required = false) String responsibility) {
        return lossRepository.findAllByOrderByIdDesc().stream()
                .filter(l -> lossReason == null || lossReason.isBlank() || lossReason.equals(l.getLossReason()))
                .filter(l -> responsibility == null || responsibility.isBlank()
                        || responsibility.equals(l.getResponsibility()))
                .map(lossService::view)
                .toList();
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody LossRecord loss) {
        return lossService.view(lossService.create(loss));
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody LossRecord form) {
        return lossService.view(lossService.update(id, form));
    }
}
