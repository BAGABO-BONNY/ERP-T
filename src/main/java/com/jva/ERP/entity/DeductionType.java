package com.jva.ERP.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DeductionType — system-wide deduction rate definitions.
 *
 * These are the global rates applied to every employee during payroll processing.
 * They are seeded at startup by DeductionTypeSeeder and can be managed by ADMIN.
 *
 * Default rates:
 *   EmployeeTax       30%  (maps to incomeTax on payslip)
 *   Pension            6%  (maps to socialSecurity on payslip)
 *   MedicalInsurance   5%  (maps to healthInsurance on payslip)
 *   Others             5%  (maps to otherDeductions on payslip)
 *   House             14%  (earnings allowance — % of baseSalary)
 *   Transport         14%  (earnings allowance — % of baseSalary)
 *
 * House and Transport are allowances (added to gross), not deductions from gross.
 * The isAllowance flag distinguishes them.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "deduction_types", uniqueConstraints = {
        @UniqueConstraint(columnNames = "type_name")
})
public class DeductionType extends BaseEntity {

    /** Unique name, e.g. "EmployeeTax", "Pension", "House" */
    @Column(name = "type_name", nullable = false, length = 100, unique = true)
    private String typeName;

    /** Human-readable label shown on payslips */
    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    /** Percentage of baseSalary (0–100). Stored as e.g. 30.00 for 30%. */
    @Column(name = "percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal percentage;

    /**
     * When true this entry is an earnings allowance (added to gross salary).
     * When false it is a deduction (subtracted from gross salary).
     */
    @Column(name = "is_allowance", nullable = false)
    private Boolean isAllowance = false;

    /** Optional description shown on payslips */
    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Override
    public String toString() {
        return "DeductionType{typeName='" + typeName + "', percentage=" + percentage
                + ", isAllowance=" + isAllowance + '}';
    }
}
