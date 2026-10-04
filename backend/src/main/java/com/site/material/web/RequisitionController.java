package com.site.material.web;

import com.site.material.domain.Requisition;
import com.site.material.repo.RequisitionRepository;
import com.site.material.service.NameResolver;
import com.site.material.service.RequisitionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/requisitions")
public class RequisitionController {

    private final RequisitionService requisitionService;
    private final RequisitionRepository requisitionRepository;
    private final NameResolver names;

    public RequisitionController(RequisitionService requisitionService,
                                 RequisitionRepository requisitionRepository, NameResolver names) {
        this.requisitionService = requisitionService;
        this.requisitionRepository = requisitionRepository;
        this.names = names;
    }

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam(required = false) String status,
                                          @RequestParam(required = false) Long crewId,
                                          @RequestParam(required = false) Long buildingId) {
        return requisitionRepository.findAllByOrderByIdDesc().stream()
                .filter(r -> status == null || status.isBlank() || status.equals(r.getStatus()))
                .filter(r -> crewId == null || crewId.equals(r.getCrewId()))
                .filter(r -> buildingId == null || buildingId.equals(r.getBuildingId()))
                .map(requisitionService::view)
                .toList();
    }

    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable Long id) {
        return requisitionService.view(requisitionService.mustGet(id));
    }

    @PostMapping
    public Map<String, Object> apply(@RequestBody RequisitionService.ReqRequest req) {
        Requisition r = requisitionService.apply(req);
        return requisitionService.view(r);
    }

    @GetMapping("/{id}/precheck")
    public Map<String, Object> precheck(@PathVariable Long id) {
        return requisitionService.precheck(id);
    }

    @PostMapping("/{id}/issue")
    public Map<String, Object> issue(@PathVariable Long id, @RequestBody RequisitionService.IssueRequest req) {
        return requisitionService.view(requisitionService.issue(id, req));
    }

    @PostMapping("/{id}/confirm")
    public Map<String, Object> confirm(@PathVariable Long id) {
        return requisitionService.view(requisitionService.confirm(id));
    }

    public record RejectRequest(String reason) {
    }

    @PostMapping("/{id}/reject")
    public Map<String, Object> reject(@PathVariable Long id, @RequestBody RejectRequest req) {
        return requisitionService.view(requisitionService.reject(id, req.reason()));
    }
}
