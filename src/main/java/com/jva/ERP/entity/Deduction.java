package com.jva.ERP.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.math.BigDecimal;

/**
 * Deduction Entity - Represents salary deductions for employees
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "deductions")
public class Deduction extends BaseEntity {

    @Column(name = "deduction_name", nullable = false, length = 100)
    private String deductionName;

    @Column(name = "deduction_type", nullable = false, length = 50)
    private String deductionType; // Tax, Insurance, Loan, etc.

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "percentage")
    private BigDecimal percentage; // For percentage-based deductions

    @Column(name = "is_percentage_based", nullable = false)
    private Boolean isPercentageBased = false;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "is_recurring", nullable = false)
    private Boolean isRecurring = true;

    @Column(name = "recurrence_frequency", length = 20)
    private String recurrenceFrequency; // Monthly, Yearly, etc.

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "remarks", length = 255)
    private String remarks;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Override
    public String toString() {
        return "Deduction{" +
                "id=" + this.getId() +
                ", deductionName='" + deductionName + '\'' +
                ", deductionType='" + deductionType + '\'' +
                ", amount=" + amount +
                ", isActive=" + isActive +
                '}';
    }
}

