package com.jva.ERP.controller;

import com.jva.ERP.dto.ApiResponse;
import com.jva.ERP.dto.PayrollRequest;
import com.jva.ERP.dto.PayrollResult;
import com.jva.ERP.service.PayrollService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * PayrollController — MANAGER/ADMIN endpoint to process monthly payroll.
 */
@Tag(name = "Payroll Processing",
     description = "MANAGER/ADMIN endpoint to run monthly payroll for all active employees. " +
                   "Calculates gross salary, allowances, deductions, and net salary. " +
                   "Saves payslips with status PENDING. " +
                   "Duplicate detection: throws 400 if a payslip already exists for the same employee+month.")
@RestController
@RequestMapping("/api/manager/payroll")
@CrossOrigin(origins = "*", maxAge = 3600)
public class PayrollController {

    @Autowired private PayrollService payrollService;

    @Operation(
        summary = "Process payroll for a month (MANAGER/ADMIN)",
        description = """
            Runs payroll for all active employees (or a single employee if employeeId is provided).

            **Calculation per employee:**
            - grossSalary = baseSalary + housingAllowance(14%) + transportAllowance(14%) + mealAllowance + otherAllowance
            - deductions  = EmployeeTax(30%) + Pension(6%) + MedicalInsurance(5%) + Others(5%) of baseSalary
            - netSalary   = grossSalary − totalDeductions
            - Deductions are capped at baseSalary if they exceed it.

            **Duplicate detection:** throws 400 if a payslip already exists for the same employee+month.

            **Returns:** PayrollResult with totals and list of generated payslips.
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
            description = "Payroll processed — result includes per-employee payslips and totals"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
            description = "Validation error, future month, or duplicate payslip detected"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
            description = "Access denied — MANAGER or ADMIN role required")
    })
    @PostMapping("/process")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<?>> processPayroll(
            @Valid @RequestBody PayrollRequest request) {
        PayrollResult result = payrollService.processPayroll(request);
        return ResponseEntity.ok(
                new ApiResponse<>(HttpStatus.OK.value(), "Payroll processed", result));
    }
}
