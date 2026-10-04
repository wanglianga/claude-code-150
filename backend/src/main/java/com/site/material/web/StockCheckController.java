package com.site.material.web;

import com.site.material.repo.StockCheckRepository;
import com.site.material.service.StockCheckService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stock-checks")
public class StockCheckController {

    private final StockCheckService stockCheckService;
    private final StockCheckRepository checkRepository;

    public StockCheckController(StockCheckService stockCheckService, StockCheckRepository checkRepository) {
        this.stockCheckService = stockCheckService;
        this.checkRepository = checkRepository;
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        return checkRepository.findAllByOrderByIdDesc().stream().map(stockCheckService::view).toList();
    }

    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable Long id) {
        return stockCheckService.view(stockCheckService.mustGet(id));
    }

    public record CreateRequest(LocalDate checkDate, String remark) {
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody CreateRequest req) {
        return stockCheckService.view(stockCheckService.createDraft(req.checkDate(), req.remark()));
    }

    @PutMapping("/{id}/actuals")
    public Map<String, Object> enterActuals(@PathVariable Long id,
                                            @RequestBody List<StockCheckService.ActualLine> lines) {
        return stockCheckService.view(stockCheckService.enterActuals(id, lines));
    }

    @PostMapping("/{id}/confirm")
    public Map<String, Object> confirm(@PathVariable Long id) {
        return stockCheckService.view(stockCheckService.confirm(id));
    }
}
