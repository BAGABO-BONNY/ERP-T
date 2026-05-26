package com.jva.ERP.service;

import com.jva.ERP.dto.DeductionTypeRequest;
import com.jva.ERP.dto.DeductionTypeResponse;
import com.jva.ERP.entity.DeductionType;
import com.jva.ERP.exception.BusinessException;
import com.jva.ERP.exception.ResourceNotFoundException;
import com.jva.ERP.repository.DeductionTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * DeductionTypeService — manages system-wide deduction/allowance rate definitions.
 *
 * Default types are seeded at startup by DeductionTypeSeeder.
 * ADMIN can create, update, and toggle active status via this service.
 *
 * Default rates:
 *   EmployeeTax       30%  deduction  → incomeTax on payslip
 *   Pension            6%  deduction  → socialSecurity on payslip
 *   MedicalInsurance   5%  deduction  → healthInsurance on payslip
 *   Others             5%  deduction  → otherDeductions on payslip
 *   House             14%  allowance  → housingAllowance on payslip (added to gross)
 *   Transport         14%  allowance  → transportAllowance on payslip (added to gross)
 */
@Service
@Transactional
public class DeductionTypeService {

    private static final Logger logger = LoggerFactory.getLogger(DeductionTypeService.class);

    @Autowired
    private DeductionTypeRepository deductionTypeRepository;

    // ── Read ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<DeductionTypeResponse> getAllActive() {
        return deductionTypeRepository.findByIsActiveTrue()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DeductionTypeResponse> getAll() {
        return deductionTypeRepository.findAll()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DeductionTypeResponse getById(Long id) {
        return toResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public DeductionTypeResponse getByTypeName(String typeName) {
        return toResponse(deductionTypeRepository.findByTypeName(typeName)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "DeductionType not found: " + typeName)));
    }

    /** Returns only allowance types (House, Transport). */
    @Transactional(readOnly = true)
    public List<DeductionTypeResponse> getAllowances() {
        return deductionTypeRepository.findByIsActiveTrueAndIsAllowanceTrue()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    /** Returns only deduction types (EmployeeTax, Pension, MedicalInsurance, Others). */
    @Transactional(readOnly = true)
    public List<DeductionTypeResponse> getDeductions() {
        return deductionTypeRepository.findByIsActiveTrueAndIsAllowanceFalse()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ── Create ────────────────────────────────────────────────────────────────

    public DeductionTypeResponse create(DeductionTypeRequest request) {
        if (deductionTypeRepository.existsByTypeName(request.getTypeName())) {
            throw new BusinessException(
                    "A deduction type with name '" + request.getTypeName() + "' already exists");
        }
        DeductionType dt = new DeductionType();
        applyRequest(dt, request);
        DeductionType saved = deductionTypeRepository.save(dt);
        logger.info("Created DeductionType '{}' at {}%", saved.getTypeName(), saved.getPercentage());
        return toResponse(saved);
    }

    /** Alias used by DeductionTypeController. */
    public DeductionTypeResponse createDeductionType(DeductionTypeRequest request) {
        return create(request);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    public DeductionTypeResponse update(Long id, DeductionTypeRequest request) {
        DeductionType dt = findById(id);

        // Allow rename only if the new name is not taken by another record
        if (!dt.getTypeName().equals(request.getTypeName()) &&
                deductionTypeRepository.existsByTypeName(request.getTypeName())) {
            throw new BusinessException(
                    "A deduction type with name '" + request.getTypeName() + "' already exists");
        }

        applyRequest(dt, request);
        DeductionType saved = deductionTypeRepository.save(dt);
        logger.info("Updated DeductionType '{}' to {}%", saved.getTypeName(), saved.getPercentage());
        return toResponse(saved);
    }

    /** Alias used by DeductionTypeController. */
    public DeductionTypeResponse updateDeductionType(Long id, DeductionTypeRequest request) {
        return update(id, request);
    }

    // ── Toggle active ─────────────────────────────────────────────────────────

    public DeductionTypeResponse setActive(Long id, boolean active) {
        DeductionType dt = findById(id);
        dt.setIsActive(active);
        DeductionType saved = deductionTypeRepository.save(dt);
        logger.info("DeductionType '{}' set isActive={}", saved.getTypeName(), active);
        return toResponse(saved);
    }

    /** Deactivate a deduction type (ADMIN only). */
    public DeductionTypeResponse deactivate(Long id) {
        return setActive(id, false);
    }

    /** Activate a deduction type (ADMIN only). */
    public DeductionTypeResponse activate(Long id) {
        return setActive(id, true);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private DeductionType findById(Long id) {
        return deductionTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "DeductionType not found with id: " + id));
    }

    private void applyRequest(DeductionType dt, DeductionTypeRequest r) {
        dt.setTypeName(r.getTypeName().trim());
        dt.setDisplayName(r.getDisplayName().trim());
        dt.setPercentage(r.getPercentage());
        dt.setIsAllowance(r.getIsAllowance());
        dt.setDescription(r.getDescription());
        dt.setIsActive(true);
    }

    public DeductionTypeResponse toResponse(DeductionType dt) {
        return new DeductionTypeResponse(
                dt.getId(),
                dt.getTypeName(),
                dt.getDisplayName(),
                dt.getPercentage(),
                dt.getIsAllowance(),
                dt.getDescription(),
                dt.getIsActive()
        );
    }
}
