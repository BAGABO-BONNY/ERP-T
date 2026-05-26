package com.jva.ERP.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * EmailService — sends transactional emails via SMTP (Gmail).
 *
 * Sender: bmbmanzi@gmail.com (configured in application.properties)
 *
 * Email sending is non-blocking and failures are logged but never propagate
 * to the caller — a failed email must never roll back a payroll transaction.
 *
 * To enable: set app.mail.enabled=true and provide a valid Gmail App Password
 * in application.properties (spring.mail.password).
 */
@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Value("${app.mail.sender-name:ERP Payroll}")
    private String senderName;

    @Value("${app.mail.enabled:true}")
    private boolean emailEnabled;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Send a plain-text email.
     *
     * @param to      recipient email address
     * @param subject email subject line
     * @param body    plain-text body
     */
    public void sendEmail(String to, String subject, String body) {
        if (!emailEnabled) {
            logger.info("Email sending is disabled (app.mail.enabled=false). " +
                    "Would have sent to: {} | Subject: {}", to, subject);
            return;
        }
        if (to == null || to.isBlank()) {
            logger.warn("Skipping email — recipient address is blank");
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderName + " <" + senderEmail + ">");
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            logger.info("Email sent to {} | Subject: {}", to, subject);
        } catch (MailException e) {
            // Log but never throw — email failure must not roll back payroll
            logger.error("Failed to send email to {} | Subject: {} | Error: {}",
                    to, subject, e.getMessage());
        }
    }

    /**
     * Send a salary credit notification email.
     *
     * Message format:
     *   Dear FIRSTNAME, your salary of MONTH/YEAR from INSTITUTION
     *   amount AMOUNT has been credited successfully.
     *
     * @param to          recipient email address
     * @param firstName   employee's first name
     * @param payrollMonth YYYY-MM format (e.g. "2025-05")
     * @param institution institution/company name
     * @param amount      net salary amount with currency (e.g. "USD 4,500.00")
     */
    public void sendSalaryCreditNotification(String to, String firstName,
                                              String payrollMonth, String institution,
                                              String amount) {
        // Convert YYYY-MM → "May 2025" for readability
        String formattedMonth = formatPayrollMonth(payrollMonth);

        String subject = "Salary Credit Notification – " + formattedMonth;
        String body = buildSalaryCreditBody(firstName, formattedMonth, institution, amount);
        sendEmail(to, subject, body);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Builds the salary credit message body.
     * Format: Dear FIRSTNAME, your salary of MONTH/YEAR from INSTITUTION
     *         amount AMOUNT has been credited successfully.
     */
    public String buildSalaryCreditBody(String firstName, String formattedMonth,
                                         String institution, String amount) {
        return String.format(
                "Dear %s\n\n" +
                "Your salary of %s from %s %s has been credited to your account Successfully.\n\n" +
                "Please log in to the ERP portal to view your detailed payslip.\n\n" +
                "Regards,\n%s",
                firstName, formattedMonth, institution, amount, senderName);
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
            return payrollMonth; // fallback to raw value
        }
    }
}
