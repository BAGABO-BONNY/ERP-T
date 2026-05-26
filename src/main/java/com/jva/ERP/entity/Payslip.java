package com.jva.ERP.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.math.BigDecimal;

/**
 * Payslip Entity - Represents a salary slip for an employee
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payslips")
public class Payslip extends BaseEntity {

    @Column(name = "payslip_number", nullable = false, length = 50, unique = true)
    private String payslipNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "payroll_period_start", nullable = false)
    private LocalDate payrollPeriodStart;

    @Column(name = "payroll_period_end", nullable = false)
    private LocalDate payrollPeriodEnd;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(name = "payroll_month", nullable = false, length = 10)
    private String payrollMonth; // YYYY-MM format

    // Earnings
    @Column(name = "basic_salary", nullable = false, precision = 10, scale = 2)
    private BigDecimal basicSalary;

    @Column(name = "housing_allowance", precision = 10, scale = 2)
    private BigDecimal housingAllowance = BigDecimal.ZERO;

    @Column(name = "transport_allowance", precision = 10, scale = 2)
    private BigDecimal transportAllowance = BigDecimal.ZERO;

    @Column(name = "meal_allowance", precision = 10, scale = 2)
    private BigDecimal mealAllowance = BigDecimal.ZERO;

    @Column(name = "bonus", precision = 10, scale = 2)
    private BigDecimal bonus = BigDecimal.ZERO;

    @Column(name = "other_earnings", precision = 10, scale = 2)
    private BigDecimal otherEarnings = BigDecimal.ZERO;

    @Column(name = "gross_salary", nullable = false, precision = 10, scale = 2)
    private BigDecimal grossSalary;

    // Deductions
    @Column(name = "income_tax", precision = 10, scale = 2)
    private BigDecimal incomeTax = BigDecimal.ZERO;

    @Column(name = "social_security", precision = 10, scale = 2)
    private BigDecimal socialSecurity = BigDecimal.ZERO;

    @Column(name = "health_insurance", precision = 10, scale = 2)
    private BigDecimal healthInsurance = BigDecimal.ZERO;

    @Column(name = "loan_deduction", precision = 10, scale = 2)
    private BigDecimal loanDeduction = BigDecimal.ZERO;

    @Column(name = "other_deductions", precision = 10, scale = 2)
    private BigDecimal otherDeductions = BigDecimal.ZERO;

    @Column(name = "total_deductions", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalDeductions = BigDecimal.ZERO;

    // Net Pay
    @Column(name = "net_salary", nullable = false, precision = 10, scale = 2)
    private BigDecimal netSalary;

    // Additional Information
    @Column(name = "working_days")
    private Integer workingDays;

    @Column(name = "working_hours", precision = 8, scale = 2)
    private BigDecimal workingHours;

    @Column(name = "leave_days")
    private Integer leaveDays;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency; // USD, EUR, etc.

    @Column(name = "payment_method", length = 50)
    private String paymentMethod; // Bank Transfer, Check, Cash, etc.

    @Column(name = "payment_status", nullable = false, length = 20)
    private String paymentStatus; // Pending, Paid, Failed, etc.

    @Column(name = "remarks", length = 255)
    private String remarks;

    @Column(name = "is_finalized", nullable = false)
    private Boolean isFinalized = false;

    @Override
    public String toString() {
        return "Payslip{" +
                "id=" + this.getId() +
                ", payslipNumber='" + payslipNumber + '\'' +
                ", payrollMonth='" + payrollMonth + '\'' +
                ", grossSalary=" + grossSalary +
                ", netSalary=" + netSalary +
                ", paymentStatus='" + paymentStatus + '\'' +
                '}';
    }
}

