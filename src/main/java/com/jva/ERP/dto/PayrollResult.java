package com.jva.ERP.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Summary result returned after running payroll for a month.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayrollResult {

    private String payrollMonth;
    private int totalProcessed;
    private int totalSkipped;       // already had a payslip for this month
    private int totalFailed;

    private BigDecimal totalGrossSalary;
    private BigDecimal totalDeductions;
    private BigDecimal totalNetSalary;

    private List<PayslipResponse> payslips = new ArrayList<>();
    private List<String> skippedEmployees = new ArrayList<>();
    private List<String> errors = new ArrayList<>();
}
