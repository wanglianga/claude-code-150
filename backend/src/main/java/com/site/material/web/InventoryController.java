package com.site.material.web;

import com.site.material.domain.MaterialBatch;
import com.site.material.repo.MaterialBatchRepository;
import com.site.material.service.InventoryService;
import com.site.material.service.NameResolver;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 入库登记、质检、库存查询 */
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;
    private final MaterialBatchRepository batchRepository;
    private final NameResolver names;

    public InventoryController(InventoryService inventoryService,
                               MaterialBatchRepository batchRepository, NameResolver names) {
        this.inventoryService = inventoryService;
        this.batchRepository = batchRepository;
        this.names = names;
    }

    @GetMapping("/batches")
    public List<Map<String, Object>> batches(@RequestParam(required = false) Long materialId,
                                             @RequestParam(required = false) String qcStatus) {
        return batchRepository.findAll().stream()
                .filter(b -> materialId == null || materialId.equals(b.getMaterialId()))
                .filter(b -> qcStatus == null || qcStatus.isBlank() || qcStatus.equals(b.getQcStatus()))
                .map(names::batchView)
                .toList();
    }

    @PostMapping("/batches")
    public Map<String, Object> inbound(@RequestBody MaterialBatch batch) {
        return names.batchView(inventoryService.inbound(batch));
    }

    public record QcRequest(String result, String note) {
    }

    @PostMapping("/batches/{id}/qc")
    public Map<String, Object> qc(@PathVariable Long id, @RequestBody QcRequest req) {
        return names.batchView(inventoryService.qc(id, req.result(), req.note()));
    }

    /** 同材料可用（合格且有库存）批次，供发料选择 */
    @GetMapping("/available")
    public List<Map<String, Object>> available(@RequestParam Long materialId) {
        return inventoryService.availableBatches(materialId).stream().map(names::batchView).toList();
    }
}
