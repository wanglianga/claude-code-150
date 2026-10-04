package com.site.material.service;

import com.site.material.common.AuthContext;
import com.site.material.common.BusinessException;
import com.site.material.common.DocNoGenerator;
import com.site.material.domain.LossRecord;
import com.site.material.domain.MaterialBatch;
import com.site.material.domain.StockCheck;
import com.site.material.domain.StockCheckItem;
import com.site.material.repo.LossRecordRepository;
import com.site.material.repo.MaterialBatchRepository;
import com.site.material.repo.StockCheckItemRepository;
import com.site.material.repo.StockCheckRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 盘点：按批次快照账面数量 -> 录入实盘 -> 确认时盘亏调账并自动生成损耗（账实不符可追溯）。 */
@Service
public class StockCheckService {

    private final StockCheckRepository checkRepository;
    private final StockCheckItemRepository itemRepository;
    private final MaterialBatchRepository batchRepository;
    private final LossRecordRepository lossRepository;
    private final InventoryService inventoryService;
    private final NameResolver names;
    private final DocNoGenerator docNo;

    public StockCheckService(StockCheckRepository checkRepository, StockCheckItemRepository itemRepository,
                             MaterialBatchRepository batchRepository, LossRecordRepository lossRepository,
                             InventoryService inventoryService, NameResolver names, DocNoGenerator docNo) {
        this.checkRepository = checkRepository;
        this.itemRepository = itemRepository;
        this.batchRepository = batchRepository;
        this.lossRepository = lossRepository;
        this.inventoryService = inventoryService;
        this.names = names;
        this.docNo = docNo;
    }

    @Transactional
    public StockCheck createDraft(LocalDate checkDate, String remark) {
        StockCheck sc = new StockCheck();
        sc.setCheckNo(docNo.next("PD"));
        sc.setCheckDate(checkDate == null ? LocalDate.now() : checkDate);
        sc.setStatus("DRAFT");
        sc.setRemark(remark);
        sc.setCreatedBy(AuthContext.userId());
        StockCheck saved = checkRepository.save(sc);
        // 按全部批次建立账面快照
        for (MaterialBatch b : batchRepository.findAll()) {
            StockCheckItem item = new StockCheckItem();
            item.setStockCheckId(saved.getId());
            item.setBatchId(b.getId());
            item.setMaterialId(b.getMaterialId());
            item.setBookQty(b.getQtyOnHand());
            item.setActualQty(b.getQtyOnHand());
            item.setResponsibility("PROJECT");
            saved.getItems().add(itemRepository.save(item));
        }
        return saved;
    }

    public record ActualLine(Long itemId, BigDecimal actualQty, String responsibility, String remark) {
    }

    @Transactional
    public StockCheck enterActuals(Long id, List<ActualLine> lines) {
        StockCheck sc = mustGet(id);
        if (!"DRAFT".equals(sc.getStatus())) {
            throw new BusinessException("盘点单已确认，不能修改");
        }
        for (ActualLine line : lines) {
            StockCheckItem item = itemRepository.findById(line.itemId())
                    .orElseThrow(() -> new BusinessException("盘点明细不存在：" + line.itemId()));
            item.setActualQty(line.actualQty());
            if (line.responsibility() != null) {
                item.setResponsibility(line.responsibility());
            }
            item.setRemark(line.remark());
            itemRepository.save(item);
        }
        return sc;
    }

    @Transactional
    public StockCheck confirm(Long id) {
        StockCheck sc = mustGet(id);
        if (!"DRAFT".equals(sc.getStatus())) {
            throw new BusinessException("盘点单已确认");
        }
        for (StockCheckItem item : itemRepository.findByStockCheckId(id)) {
            if (item.getActualQty() == null) {
                throw new BusinessException("存在未录入实盘数量的批次");
            }
            BigDecimal diff = item.getActualQty().subtract(item.getBookQty());
            if (diff.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            MaterialBatch batch = inventoryService.mustGet(item.getBatchId());
            inventoryService.adjust(batch.getId(), diff);
            if (diff.compareTo(BigDecimal.ZERO) < 0) {
                // 盘亏：账实不符 -> 生成损耗记录，默认保管不当，按盘点录入责任归属
                LossRecord loss = new LossRecord();
                loss.setLossNo(docNo.next("SH"));
                loss.setMaterialId(item.getMaterialId());
                loss.setBatchId(batch.getId());
                loss.setQty(diff.negate());
                loss.setUnitPrice(batch.getUnitPrice());
                loss.setLossReason("POOR_STORAGE");
                loss.setResponsibility(item.getResponsibility() == null ? "PROJECT" : item.getResponsibility());
                loss.setLossDate(sc.getCheckDate());
                loss.setRemark("盘点单 " + sc.getCheckNo() + " 盘亏，" + nv(item.getRemark()));
                loss.setCreatedBy(AuthContext.userId());
                if ("SUPPLIER".equals(item.getResponsibility())) {
                    loss.setSupplierId(batch.getSupplierId());
                    loss.setLossReason("SUPPLIER_QUALITY");
                }
                lossRepository.save(loss);
            }
        }
        sc.setStatus("CONFIRMED");
        sc.setConfirmedAt(LocalDateTime.now());
        return checkRepository.save(sc);
    }

    private String nv(String s) {
        return s == null ? "" : s;
    }

    public StockCheck mustGet(Long id) {
        return checkRepository.findById(id)
                .orElseThrow(() -> new BusinessException("盘点单不存在：" + id));
    }

    public Map<String, Object> view(StockCheck sc) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", sc.getId());
        m.put("checkNo", sc.getCheckNo());
        m.put("checkDate", sc.getCheckDate());
        m.put("status", sc.getStatus());
        m.put("remark", sc.getRemark());
        m.put("confirmedAt", sc.getConfirmedAt());
        List<Map<String, Object>> items = itemRepository.findByStockCheckId(sc.getId()).stream().map(i -> {
            Map<String, Object> im = new HashMap<>();
            im.put("id", i.getId());
            im.put("batchId", i.getBatchId());
            MaterialBatch b = batchRepository.findById(i.getBatchId()).orElse(null);
            im.put("batchNo", b == null ? "" : b.getBatchNo());
            im.put("materialId", i.getMaterialId());
            im.put("materialName", names.materialName(i.getMaterialId()));
            im.put("unit", names.materialUnit(i.getMaterialId()));
            im.put("bookQty", i.getBookQty());
            im.put("actualQty", i.getActualQty());
            BigDecimal actual = i.getActualQty() == null ? i.getBookQty() : i.getActualQty();
            im.put("diffQty", actual.subtract(i.getBookQty()));
            im.put("responsibility", i.getResponsibility());
            im.put("remark", i.getRemark());
            return im;
        }).toList();
        m.put("items", items);
        return m;
    }
}
