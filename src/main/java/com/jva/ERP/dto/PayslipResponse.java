package com.jva.ERP.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Response DTO for a Payslip record.
 *
 * Field naming is kept consistent with what PayrollService.toResponse() sets.
 * Aliases are provided via extra getters so both naming conventions work.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayslipResponse {

    private Long id;
    private String payslipNumber;
    private String payrollMonth;

    // Employee summary
    private Long employeeId;
    private String employeeIdNumber;
    /** Full name of the employee — also accessible via getEmployeeName() for compatibility. */
    private String employeeFullName;
    private String department;
    private String jobTitle;

    // Period
    private LocalDate payrollPeriodStart;
    private LocalDate payrollPeriodEnd;
    private LocalDate paymentDate;

    // Earnings
    private BigDecimal basicSalary;
    private BigDecimal housingAllowance;
    private BigDecimal transportAllowance;
    private BigDecimal mealAllowance;
    private BigDecimal bonus;
    private BigDecimal otherEarnings;
    private BigDecimal grossSalary;

    // Deductions breakdown
    /** EmployeeTax 30% of base salary. Also accessible via getEmployeeTax(). */
    private BigDecimal incomeTax;
    /** Pension 6% of base salary. Also accessible via getPension(). */
    private BigDecimal socialSecurity;
    /** MedicalInsurance 5% of base salary. Also accessible via getMedicalInsurance(). */
    private BigDecimal healthInsurance;
    private BigDecimal otherDeductions;
    private BigDecimal totalDeductions;

    // Net
    private BigDecimal netSalary;

    private String currency;
    private String paymentStatus;
    private Boolean isFinalized;
    private String remarks;

    // ── Compatibility aliases (used by PayrollService.toResponse) ─────────────

    /** Alias for {@link #employeeFullName}. */
    public void setEmployeeName(String name) {
        this.employeeFullName = name;
    }

    /** Alias for {@link #employeeFullName}. */
    public String getEmployeeName() {
        return this.employeeFullName;
    }

    /** Alias for {@link #incomeTax}. */
    public void setEmployeeTax(BigDecimal value) {
        this.incomeTax = value;
    }

    /** Alias for {@link #incomeTax}. */
    public BigDecimal getEmployeeTax() {
        return this.incomeTax;
    }

    /** Alias for {@link #socialSecurity}. */
    public void setPension(BigDecimal value) {
        this.socialSecurity = value;
    }

    /** Alias for {@link #socialSecurity}. */
    public BigDecimal getPension() {
        return this.socialSecurity;
    }

    /** Alias for {@link #healthInsurance}. */
    public void setMedicalInsurance(BigDecimal value) {
        this.healthInsurance = value;
    }

    /** Alias for {@link #healthInsurance}. */
    public BigDecimal getMedicalInsurance() {
        return this.healthInsurance;
    }
}
