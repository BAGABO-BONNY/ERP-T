package com.jva.ERP.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.math.BigDecimal;

/**
 * Employment Entity - Represents employment-related information
 * Has One-to-One relationship with Employee
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "employment")
public class Employment extends BaseEntity {

    @Column(name = "job_title", nullable = false, length = 100)
    private String jobTitle;

    @Column(name = "department", nullable = false, length = 100)
    private String department;

    @Column(name = "position_level", length = 50)
    private String positionLevel;

    @Column(name = "employment_type", nullable = false, length = 30)
    private String employmentType; // Full-time, Part-time, Contract, etc.

    @Column(name = "employment_status", nullable = false, length = 30)
    private String employmentStatus; // Active, Inactive, On Leave, etc.

    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    @Column(name = "probation_end_date")
    private LocalDate probationEndDate;

    @Column(name = "promotion_date")
    private LocalDate promotionDate;

    @Column(name = "termination_date")
    private LocalDate terminationDate;

    @Column(name = "termination_reason", length = 255)
    private String terminationReason;

    // Compensation
    @Column(name = "base_salary", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseSalary;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency; // USD, EUR, etc.

    @Column(name = "pay_frequency", nullable = false, length = 20)
    private String payFrequency; // Monthly, Bi-weekly, etc.

    @Column(name = "salary_effective_date")
    private LocalDate salaryEffectiveDate;

    // Benefits and Allowances
    @Column(name = "housing_allowance", precision = 10, scale = 2)
    private BigDecimal housingAllowance;

    @Column(name = "transport_allowance", precision = 10, scale = 2)
    private BigDecimal transportAllowance;

    @Column(name = "meal_allowance", precision = 10, scale = 2)
    private BigDecimal mealAllowance;

    @Column(name = "other_allowance", precision = 10, scale = 2)
    private BigDecimal otherAllowance;

    // Leave Information
    @Column(name = "annual_leave_days")
    private Integer annualLeaveDays;

    @Column(name = "sick_leave_days")
    private Integer sickLeaveDays;

    @Column(name = "maternity_leave_days")
    private Integer maternityLeaveDays;

    @Column(name = "used_leave_days")
    private Integer usedLeaveDays = 0;

    // Manager/Supervisor
    @Column(name = "manager_name", length = 100)
    private String managerName;

    @Column(name = "manager_email", length = 100)
    private String managerEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private Employee manager;

    // One-to-One relationship with Employee
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false, unique = true)
    private Employee employee;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Override
    public String toString() {
        return "Employment{" +
                "id=" + this.getId() +
                ", jobTitle='" + jobTitle + '\'' +
                ", department='" + department + '\'' +
                ", employmentStatus='" + employmentStatus + '\'' +
                ", hireDate=" + hireDate +
                ", baseSalary=" + baseSalary +
                '}';
    }
}

