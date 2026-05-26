package com.jva.ERP.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Summary returned after a payroll run.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayrollSummary {

    private String payrollMonth;          // YYYY-MM
    private int totalEmployeesProcessed;
    private int skippedAlreadyProcessed;
    private int skippedNoEmployment;

    private BigDecimal totalGrossSalary;
    private BigDecimal totalDeductions;
    private BigDecimal totalNetSalary;

    private List<PayslipResponse> payslips;
}
