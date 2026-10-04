package com.site.material.service;

import com.site.material.common.BusinessException;
import com.site.material.common.DocNoGenerator;
import com.site.material.domain.MaterialBatch;
import com.site.material.repo.MaterialBatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/** 入库批次与库存：登记入库、质检冻结/放行、批次库存扣减/回补 */
@Service
public class InventoryService {

    private final MaterialBatchRepository batchRepository;
    private final DocNoGenerator docNo;

    public InventoryService(MaterialBatchRepository batchRepository, DocNoGenerator docNo) {
        this.batchRepository = batchRepository;
        this.docNo = docNo;
    }

    @Transactional
    public MaterialBatch inbound(MaterialBatch batch) {
        if (batch.getBatchNo() == null || batch.getBatchNo().isBlank()) {
            batch.setBatchNo(docNo.next("RK"));
        }
        if (batchRepository.findByBatchNo(batch.getBatchNo()).isPresent()) {
            throw new BusinessException("批次号已存在：" + batch.getBatchNo());
        }
        if (batch.getInboundDate() == null) {
            batch.setInboundDate(LocalDate.now());
        }
        if (batch.getQcStatus() == null) {
            batch.setQcStatus("PENDING");
        }
        batch.setQtyOnHand(batch.getQtyIn());
        return batchRepository.save(batch);
    }

    @Transactional
    public MaterialBatch qc(Long batchId, String result, String note) {
        MaterialBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new BusinessException("批次不存在"));
        if (!"PASSED".equals(result) && !"FAILED".equals(result)) {
            throw new BusinessException("质检结论只能是 PASSED / FAILED");
        }
        batch.setQcStatus(result);
        batch.setQcNote(note);
        return batchRepository.save(batch);
    }

    /** 从指定批次扣减；若为质检不合格或库存不足则拒绝 */
    @Transactional
    public MaterialBatch deduct(Long batchId, BigDecimal qty) {
        MaterialBatch batch = mustGet(batchId);
        if ("FAILED".equals(batch.getQcStatus())) {
            throw new BusinessException("批次 " + batch.getBatchNo() + " 质检不合格，禁止发料");
        }
        if (!"PASSED".equals(batch.getQcStatus())) {
            throw new BusinessException("批次 " + batch.getBatchNo() + " 尚未质检合格，禁止发料");
        }
        if (batch.getQtyOnHand().compareTo(qty) < 0) {
            throw new BusinessException("批次 " + batch.getBatchNo() + " 库存不足：在账 "
                    + batch.getQtyOnHand() + "，申请 " + qty);
        }
        batch.setQtyOnHand(batch.getQtyOnHand().subtract(qty));
        return batchRepository.save(batch);
    }

    /** 退库回补（只回补合格批次） */
    @Transactional
    public void addBack(Long batchId, BigDecimal qty) {
        if (batchId == null) {
            return;
        }
        MaterialBatch batch = mustGet(batchId);
        batch.setQtyOnHand(batch.getQtyOnHand().add(qty));
        batchRepository.save(batch);
    }

    @Transactional
    public void adjust(Long batchId, BigDecimal delta) {
        MaterialBatch batch = mustGet(batchId);
        BigDecimal next = batch.getQtyOnHand().add(delta);
        if (next.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("批次 " + batch.getBatchNo() + " 调减后库存为负");
        }
        batch.setQtyOnHand(next);
        batchRepository.save(batch);
    }

    public MaterialBatch mustGet(Long batchId) {
        return batchRepository.findById(batchId)
                .orElseThrow(() -> new BusinessException("批次不存在：" + batchId));
    }

    /** 自动分配：同材料按入库时间先后（先进先出）从合格批次中挑库存 */
    public List<MaterialBatch> availableBatches(Long materialId) {
        return batchRepository.findByMaterialIdOrderByInboundDateAscIdAsc(materialId).stream()
                .filter(b -> "PASSED".equals(b.getQcStatus()))
                .filter(b -> b.getQtyOnHand().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(MaterialBatch::getInboundDate)
                        .thenComparing(MaterialBatch::getId))
                .toList();
    }
}
