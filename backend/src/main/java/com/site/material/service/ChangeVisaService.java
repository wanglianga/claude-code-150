package com.site.material.service;

import com.site.material.common.BusinessException;
import com.site.material.common.CodeGenerator;
import com.site.material.domain.ChangeVisa;
import com.site.material.dto.Requests.*;
import com.site.material.repo.ChangeVisaRepository;
import com.site.material.repo.MaterialRepository;
import com.site.material.security.SecurityUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 变更签证：施工/设计变更引起材料量变化与索赔，财务据此做变更索赔。
 */
@Service
public class ChangeVisaService {

    private final ChangeVisaRepository visaRepo;
    private final MaterialRepository materialRepo;
    private final CodeGenerator codes;

    public ChangeVisaService(ChangeVisaRepository visaRepo, MaterialRepository materialRepo,
                             CodeGenerator codes) {
        this.visaRepo = visaRepo;
        this.materialRepo = materialRepo;
        this.codes = codes;
    }

    @Transactional
    public ChangeVisa create(VisaRequest req) {
        if (req.materialId() != null) {
            materialRepo.findById(req.materialId()).orElseThrow(() -> new BusinessException("材料不存在"));
        }
        ChangeVisa v = new ChangeVisa();
        v.setCode(codes.next("QZ"));
        v.setBuilding(req.building());
        v.setArea(req.area());
        v.setProcess(req.process());
        v.setDrawingNo(req.drawingNo());
        v.setMaterialId(req.materialId());
        v.setCrewId(req.crewId());
        v.setChangeType(req.changeType());
        v.setDescription(req.description());
        v.setQtyChange(req.qtyChange() == null ? 0d : req.qtyChange());
        v.setUnitPrice(req.unitPrice() == null ? 0d : req.unitPrice());
        v.setClaimAmount(req.claimAmount() == null
                ? MasterDataService.round(v.getQtyChange() * v.getUnitPrice()) : req.claimAmount());
        v.setStatus("DRAFT");
        v.setApplicant(SecurityUtil.currentName());
        v.setCreatedTime(LocalDateTime.now());
        return visaRepo.save(v);
    }

    @Transactional
    public ChangeVisa confirm(Long id) {
        ChangeVisa v = visaRepo.findById(id).orElseThrow(() -> new BusinessException("签证不存在"));
        if (!"DRAFT".equals(v.getStatus())) {
            throw new BusinessException("仅草稿签证可确认，当前状态: " + v.getStatus());
        }
        v.setStatus("CONFIRMED");
        v.setConfirmer(SecurityUtil.currentName());
        v.setConfirmTime(LocalDateTime.now());
        return v;
    }

    @Transactional
    public ChangeVisa markClaimed(Long id) {
        ChangeVisa v = visaRepo.findById(id).orElseThrow(() -> new BusinessException("签证不存在"));
        if (!"CONFIRMED".equals(v.getStatus())) {
            throw new BusinessException("仅已确认签证可标记索赔");
        }
        v.setStatus("CLAIMED");
        return v;
    }

    public List<ChangeVisa> list() {
        return visaRepo.findAllByOrderByCreatedTimeDesc();
    }
}
