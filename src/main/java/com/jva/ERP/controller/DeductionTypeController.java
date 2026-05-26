package com.jva.ERP.controller;

import com.jva.ERP.dto.ApiResponse;
import com.jva.ERP.dto.DeductionTypeRequest;
import com.jva.ERP.dto.DeductionTypeResponse;
import com.jva.ERP.exception.BusinessException;
import com.jva.ERP.exception.ResourceNotFoundException;
import com.jva.ERP.service.DeductionTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * DeductionTypeController — manage system-wide deduction/allowance type definitions.
 *
 * Default rates seeded at startup:
 *   EmployeeTax 30%, Pension 6%, MedicalInsurance 5%, Others 5%  (deductions)
 *   House 14%, Transport 14%                                       (allowances)
 */
@Tag(name = "Deduction Types",
     description = "Manage system-wide deduction and allowance rate definitions. " +
                   "MANAGER can read. ADMIN can create, update, and toggle active status.")
@RestController
@RequestMapping("/api/deduction-types")
@CrossOrigin(origins = "*", maxAge = 3600)
public class DeductionTypeController {

    private static final Logger logger = LoggerFactory.getLogger(DeductionTypeController.class);

    @Autowired
    private DeductionTypeService deductionTypeService;

    @Operation(summary = "List all active deduction types (MANAGER/ADMIN)",
               security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<?>> getAllActive() {
        List<DeductionTypeResponse> list = deductionTypeService.getAllActive();
        return ok("Deduction types retrieved", list);
    }

    @Operation(summary = "List all deduction types including inactive (ADMIN only)",
               security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> getAll() {
        return ok("All deduction types retrieved", deductionTypeService.getAll());
    }

    @Operation(summary = "Get a deduction type by ID (MANAGER/ADMIN)",
               security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<?>> getById(
            @Parameter(description = "Deduction type database ID") @PathVariable Long id) {
        try {
            return ok("Deduction type retrieved", deductionTypeService.getById(id));
        } catch (ResourceNotFoundException e) {
            return notFound(e.getMessage());
        }
    }

    @Operation(summary = "List allowance types only — House, Transport (MANAGER/ADMIN)",
               security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/allowances")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<?>> getAllowances() {
        return ok("Allowance types retrieved", deductionTypeService.getAllowances());
    }

    @Operation(summary = "List deduction types only — Tax, Pension, Medical, Others (MANAGER/ADMIN)",
               security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/deductions")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<?>> getDeductions() {
        return ok("Deduction types retrieved", deductionTypeService.getDeductions());
    }

    @Operation(summary = "Create a new deduction type (ADMIN only)",
               description = "Set isAllowance=true for allowances (added to gross), false for deductions (subtracted).",
               security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> create(
            @Valid @RequestBody DeductionTypeRequest request) {
        try {
            DeductionTypeResponse response = deductionTypeService.createDeductionType(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(HttpStatus.CREATED.value(),
                            "Deduction type created", response));
        } catch (BusinessException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            logger.error("Error creating deduction type", e);
            return serverError("Failed to create deduction type");
        }
    }

    @Operation(summary = "Update a deduction type (ADMIN only)",
               security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> update(
            @Parameter(description = "Deduction type database ID") @PathVariable Long id,
            @Valid @RequestBody DeductionTypeRequest request) {
        try {
            return ok("Deduction type updated", deductionTypeService.updateDeductionType(id, request));
        } catch (ResourceNotFoundException e) {
            return notFound(e.getMessage());
        } catch (BusinessException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            logger.error("Error updating deduction type id={}", id, e);
            return serverError("Failed to update deduction type");
        }
    }

    @Operation(summary = "Deactivate a deduction type (ADMIN only)",
               security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> deactivate(
            @Parameter(description = "Deduction type database ID") @PathVariable Long id) {
        try {
            return ok("Deduction type deactivated", deductionTypeService.deactivate(id));
        } catch (ResourceNotFoundException e) {
            return notFound(e.getMessage());
        } catch (BusinessException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "Activate a deduction type (ADMIN only)",
               security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> activate(
            @Parameter(description = "Deduction type database ID") @PathVariable Long id) {
        try {
            return ok("Deduction type activated", deductionTypeService.activate(id));
        } catch (ResourceNotFoundException e) {
            return notFound(e.getMessage());
        } catch (BusinessException e) {
            return badRequest(e.getMessage());
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private <T> ResponseEntity<ApiResponse<?>> ok(String msg, T data) {
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), msg, data));
    }
    private ResponseEntity<ApiResponse<?>> badRequest(String msg) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), msg, null));
    }
    private ResponseEntity<ApiResponse<?>> notFound(String msg) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(HttpStatus.NOT_FOUND.value(), msg, null));
    }
    private ResponseEntity<ApiResponse<?>> serverError(String msg) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), msg, null));
    }
}
