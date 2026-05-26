package com.jva.ERP.controller;

import com.jva.ERP.dto.ApiResponse;
import com.jva.ERP.entity.Payslip;
import com.jva.ERP.repository.PayslipRepository;
import com.jva.ERP.service.PayslipNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AdminPayrollController — ADMIN-only payroll approval endpoint.
 *
 * POST /api/admin/payroll/approve?year=2025&month=5
 *   → Finds all PENDING payslips for the given month and marks them as PAID.
 *   → Sends salary credit notifications (Message + email) for each approved payslip.
 *   → Returns a summary of how many payslips were approved.
 *
 * Role: ADMIN only (enforced by SecurityConfig path rule + @PreAuthorize).
 */
@Tag(name = "Admin – Payroll Approval",
     description = "ADMIN-only endpoint to approve monthly payroll. " +
                   "Updates all PENDING payslips to PAID and sends salary credit notifications.")
@RestController
@RequestMapping("/api/admin/payroll")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AdminPayrollController {

    private static final Logger logger = LoggerFactory.getLogger(AdminPayrollController.class);

    @Autowired private PayslipRepository          payslipRepository;
    @Autowired private PayslipNotificationService notificationService;

    /**
     * Approve payroll for a given month and year.
     * Updates all PENDING payslips to PAID and fires salary credit notifications.
     */
    @Operation(
        summary = "Approve payroll for a month (ADMIN only)",
        description = "Marks all PENDING payslips for the given year/month as PAID. " +
                      "Sets isFinalized=true and paymentDate=today. " +
                      "Sends a salary credit message and email to each employee. " +
                      "Already-paid payslips are skipped.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
            description = "Payroll approved successfully",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
            description = "No payslips found for the given month"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
            description = "Access denied — ADMIN role required")
    })
    @PostMapping("/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> approvePayroll(
            @Parameter(description = "4-digit year, e.g. 2025", example = "2025")
            @RequestParam int year,
            @Parameter(description = "Month number 1–12, e.g. 5 for May", example = "5")
            @RequestParam int month) {

        String payrollMonth = String.format("%04d-%02d", year, month);
        logger.info("ADMIN approving payroll for {}", payrollMonth);

        List<Payslip> slips = payslipRepository.findByPayrollMonth(payrollMonth);
        if (slips.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(HttpStatus.NOT_FOUND.value(),
                            "No payslips found for " + payrollMonth, null));
        }

        int approved   = 0;
        int alreadyPaid = 0;
        LocalDate today = LocalDate.now();

        for (Payslip p : slips) {
            if (p.getIsFinalized()) {
                alreadyPaid++;
            } else {
                p.setPaymentStatus("Paid");
                p.setIsFinalized(true);
                p.setPaymentDate(today);
                payslipRepository.save(p);
                approved++;

                // Send salary credit message + email (runs in its own transaction)
                notificationService.notifyPayslipApproved(p);
            }
        }

        String message = String.format(
                "Payroll approved for %s: %d payslip(s) marked as PAID, %d already paid.",
                payrollMonth, approved, alreadyPaid);
        logger.info(message);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("payrollMonth",  payrollMonth);
        summary.put("totalPayslips", slips.size());
        summary.put("newlyApproved", approved);
        summary.put("alreadyPaid",   alreadyPaid);
        summary.put("approvalDate",  today.toString());

        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), message, summary));
    }
}
