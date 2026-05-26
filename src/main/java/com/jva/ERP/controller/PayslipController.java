package com.jva.ERP.controller;

import com.jva.ERP.dto.ApiResponse;
import com.jva.ERP.dto.PayslipResponse;
import com.jva.ERP.entity.User;
import com.jva.ERP.exception.ResourceNotFoundException;
import com.jva.ERP.repository.UserRepository;
import com.jva.ERP.service.PayslipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * PayslipController — payslip read endpoints with role-based access.
 *
 *   EMPLOYEE  → own payslips only
 *   MANAGER   → all payslips, filter by month or employee
 *   ADMIN     → same as MANAGER
 */
@Tag(name = "Payslips",
     description = "View payslips with role-based access. " +
                   "EMPLOYEE sees only their own payslips. " +
                   "MANAGER/ADMIN can view all payslips and filter by month or employee.")
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*", maxAge = 3600)
public class PayslipController {

    private static final Logger logger = LoggerFactory.getLogger(PayslipController.class);

    @Autowired private PayslipService payslipService;
    @Autowired private UserRepository userRepository;

    // ── EMPLOYEE endpoints ────────────────────────────────────────────────────

    @Operation(
        summary = "Get my payslips (EMPLOYEE/MANAGER/ADMIN)",
        description = "Returns all payslips for the currently authenticated user's employee record. " +
                      "Includes full salary breakdown: earnings, deductions, and net salary.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/employee/payslips")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<?>> getMyPayslips(Authentication authentication) {
        List<PayslipResponse> list =
                payslipService.getPayslipsForCurrentUser(authentication.getName());
        return ok("Payslips retrieved", list);
    }

    @Operation(
        summary = "Get a single payslip by ID",
        description = "EMPLOYEE: returns 403 if the payslip does not belong to them. " +
                      "MANAGER/ADMIN: can access any payslip.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/payslips/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<?>> getPayslip(
            @Parameter(description = "Payslip database ID") @PathVariable Long id,
            Authentication authentication) {

        PayslipResponse resp = payslipService.getPayslip(id);

        boolean isEmployee = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"));

        if (isEmployee) {
            Long ownerEmployeeId = resolveEmployeeId(authentication.getName());
            if (!resp.getEmployeeId().equals(ownerEmployeeId)) {
                logger.warn("EMPLOYEE '{}' attempted to access payslip {} belonging to employee {}",
                        authentication.getName(), id, resp.getEmployeeId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse<>(HttpStatus.FORBIDDEN.value(),
                                "Access denied: this payslip does not belong to you", null));
            }
        }

        return ok("Payslip retrieved", resp);
    }

    // ── MANAGER / ADMIN endpoints ─────────────────────────────────────────────

    @Operation(
        summary = "List all payslips (MANAGER/ADMIN)",
        description = "Returns every payslip in the system with full salary breakdown.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/manager/payslips")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<?>> getAllPayslips() {
        List<PayslipResponse> list = payslipService.getAllPayslips();
        return ok("All payslips retrieved", list);
    }

    @Operation(
        summary = "List payslips for a specific payroll month (MANAGER/ADMIN)",
        description = "Filter payslips by payroll month in YYYY-MM format, e.g. 2025-05.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/manager/payslips/month/{payrollMonth}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<?>> getPayslipsByMonth(
            @Parameter(description = "Payroll month in YYYY-MM format, e.g. 2025-05")
            @PathVariable String payrollMonth) {
        List<PayslipResponse> list = payslipService.getPayslipsByMonth(payrollMonth);
        return ok("Payslips for " + payrollMonth + " retrieved", list);
    }

    @Operation(
        summary = "List all payslips for a specific employee (MANAGER/ADMIN)",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/manager/payslips/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<?>> getPayslipsByEmployee(
            @Parameter(description = "Employee database ID") @PathVariable Long employeeId) {
        List<PayslipResponse> list = payslipService.getPayslipsForEmployee(employeeId);
        return ok("Payslips for employee " + employeeId + " retrieved", list);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Long resolveEmployeeId(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        if (user.getEmployee() == null) {
            throw new ResourceNotFoundException(
                    "No employee record linked to user: " + username);
        }
        return user.getEmployee().getId();
    }

    private <T> ResponseEntity<ApiResponse<?>> ok(String msg, T data) {
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), msg, data));
    }
}
