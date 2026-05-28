package com.jva.ERP.controller;

import com.jva.ERP.dto.ApiResponse;
import com.jva.ERP.entity.Payslip;
import com.jva.ERP.repository.PayslipRepository;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AdminPayrollController — ADMIN-only payroll approval.
 *
 * POST /api/admin/payroll/approve?year=2025&month=5
 *
 * Flow:
 *   1. Find all PENDING payslips for the month.
 *   2. Set payment_status = 'PAID', is_finalized = true, payment_date = today.
 *   3. Save — the DB trigger fn_payslip_paid_notification fires on commit
 *      and inserts a salary-credit message into the messages table automatically.
 *
 * The Java-side PayslipNotificationService is intentionally NOT called here.
 * The PostgreSQL trigger is the single source of truth for message insertion,
 * which satisfies the exam requirement for a DB-level routine.
 */
@Tag(name = "Admin – Payroll Approval",
     description = "ADMIN-only endpoint to approve monthly payroll. " +
                   "Updates all PENDING payslips to PAID. " +
                   "The PostgreSQL trigger automatically inserts salary-credit " +
                   "messages into the messages table on commit.")
@RestController
@RequestMapping("/api/admin/payroll")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AdminPayrollController {

    private static final Logger logger = LoggerFactory.getLogger(AdminPayrollController.class);

    @Autowired
    private PayslipRepository payslipRepository;

    @Operation(
        summary = "Approve payroll for a month (ADMIN only)",
        description = """
            Marks all PENDING payslips for the given year/month as PAID.
            Sets isFinalized=true and paymentDate=today.

            The PostgreSQL trigger `trg_payslip_paid_notification` fires automatically
            on the UPDATE and inserts a salary-credit message into the messages table:

            "Dear FIRSTNAME Your salary of MONTH/YEAR from INSTITUTION AMOUNT
             has been credited to your EMPLOYEE_ID account Successfully."

            Already-paid payslips are skipped.
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
            description = "Payroll approved — messages inserted by DB trigger"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
            description = "No payslips found for the given month"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
            description = "Access denied — ADMIN role required")
    })
    @PostMapping("/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional  // single transaction — trigger fires on commit
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

        int approved    = 0;
        int alreadyPaid = 0;
        LocalDate today = LocalDate.now();

        for (Payslip p : slips) {
            if ("PAID".equals(p.getPaymentStatus()) || p.getIsFinalized()) {
                alreadyPaid++;
            } else {
                p.setPaymentStatus("PAID");
                p.setIsFinalized(true);
                p.setPaymentDate(today);
                payslipRepository.save(p);
                // ↑ The DB trigger fires on this UPDATE when the transaction commits.
                // No Java-side notification call needed here.
                approved++;
            }
        }

        String msg = String.format(
                "Payroll approved for %s: %d payslip(s) marked as PAID, %d already paid.",
                payrollMonth, approved, alreadyPaid);
        logger.info(msg);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("payrollMonth",  payrollMonth);
        summary.put("totalPayslips", slips.size());
        summary.put("newlyApproved", approved);
        summary.put("alreadyPaid",   alreadyPaid);
        summary.put("approvalDate",  today.toString());

        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), msg, summary));
    }
}
