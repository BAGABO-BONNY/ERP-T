package com.jva.ERP.service;

import com.jva.ERP.entity.Employee;
import com.jva.ERP.entity.Message;
import com.jva.ERP.entity.Payslip;
import com.jva.ERP.repository.MessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * PayslipNotificationService — triggered after a payslip is approved (status → Paid).
 *
 * For each approved payslip this service:
 *   1. Inserts a Message record into the messages table with the salary credit text.
 *   2. Sends a salary credit email to the employee's registered email address.
 *
 * Message format:
 *   "Dear FIRSTNAME, your salary of MONTH/YEAR from INSTITUTION
 *    amount AMOUNT has been credited successfully."
 *
 * This service runs in its own transaction (REQUIRES_NEW) so that a notification
 * failure never rolls back the payroll approval transaction.
 */
@Service
public class PayslipNotificationService {

    private static final Logger logger =
            LoggerFactory.getLogger(PayslipNotificationService.class);

    // System sender ID — 0 means "system" (no real user)
    private static final long SYSTEM_SENDER_ID = 0L;
    private static final String SYSTEM_SENDER_NAME = "ERP Payroll System";

    @Autowired private MessageRepository messageRepository;
    @Autowired private EmailService       emailService;

    @Value("${app.mail.institution-name:ERP Payroll System}")
    private String institutionName;

    /**
     * Notify an employee that their payslip has been approved and salary credited.
     *
     * Runs in a separate transaction so failures here never affect the caller.
     *
     * @param payslip the approved (Paid) payslip
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyPayslipApproved(Payslip payslip) {
        try {
            Employee employee = payslip.getEmployee();
            if (employee == null) {
                logger.warn("Cannot notify — payslip {} has no employee", payslip.getId());
                return;
            }

            String firstName    = employee.getFirstName();
            String payrollMonth = payslip.getPayrollMonth();          // YYYY-MM
            String formattedMonth = formatPayrollMonth(payrollMonth); // e.g. "May 2025"
            String amount       = formatAmount(payslip.getNetSalary(), payslip.getCurrency());

            // ── 1. Build message text ─────────────────────────────────────────
            String messageText = buildMessageText(firstName, formattedMonth, amount);

            // ── 2. Persist Message record ─────────────────────────────────────
            Message msg = new Message();
            msg.setSenderId(SYSTEM_SENDER_ID);
            msg.setSenderName(SYSTEM_SENDER_NAME);
            msg.setReceiverId(employee.getId());
            msg.setReceiverName(firstName + " " + employee.getLastName());
            msg.setSubject("Salary Credit – " + formattedMonth);
            msg.setMessage(messageText);
            msg.setMessageType("Notification");
            msg.setPriority("High");
            msg.setIsRead(false);
            msg.setIsArchived(false);
            msg.setRelatedEntityType("Payslip");
            msg.setRelatedEntityId(payslip.getId());
            msg.setSentAt(LocalDateTime.now());
            messageRepository.save(msg);
            logger.info("Salary credit message saved for employee {} payslip {}",
                    employee.getEmployeeIdNumber(), payslip.getPayslipNumber());

            // ── 3. Send email ─────────────────────────────────────────────────
            String recipientEmail = resolveEmail(employee);
            if (recipientEmail != null) {
                emailService.sendSalaryCreditNotification(
                        recipientEmail, firstName, payrollMonth, institutionName, amount);
            } else {
                logger.warn("No email address found for employee {} — skipping email",
                        employee.getEmployeeIdNumber());
            }

        } catch (Exception e) {
            // Log but never propagate — notification failure must not affect payroll
            logger.error("Failed to send payslip notification for payslip {}: {}",
                    payslip.getId(), e.getMessage(), e);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Builds the salary credit message.
     * Format: Dear FIRSTNAME, your salary of MONTH/YEAR from INSTITUTION
     *         amount AMOUNT has been credited successfully.
     */
    private String buildMessageText(String firstName, String formattedMonth, String amount) {
        return String.format(
                "Dear %s Your salary of %s from %s %s has been credited to your account Successfully.",
                firstName, formattedMonth, institutionName, amount);
    }

    /**
     * Resolves the best email address for the employee.
     * Prefers the work email; falls back to personal email.
     */
    private String resolveEmail(Employee employee) {
        if (employee.getEmail() != null && !employee.getEmail().isBlank()) {
            return employee.getEmail();
        }
        if (employee.getPersonalEmail() != null && !employee.getPersonalEmail().isBlank()) {
            return employee.getPersonalEmail();
        }
        // Try linked user account email
        if (employee.getUser() != null && employee.getUser().getEmail() != null) {
            return employee.getUser().getEmail();
        }
        return null;
    }

    /**
     * Formats a BigDecimal amount with currency prefix, e.g. "USD 4,500.00".
     */
    private String formatAmount(BigDecimal amount, String currency) {
        if (amount == null) return "0.00";
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        String formatted = nf.format(amount);
        return (currency != null ? currency + " " : "") + formatted;
    }

    /**
     * Converts "2025-05" → "May 2025".
     */
    private String formatPayrollMonth(String payrollMonth) {
        try {
            String[] parts = payrollMonth.split("-");
            int year  = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            String[] months = { "January","February","March","April","May","June",
                                 "July","August","September","October","November","December" };
            return months[month - 1] + " " + year;
        } catch (Exception e) {
            return payrollMonth;
        }
    }
}
