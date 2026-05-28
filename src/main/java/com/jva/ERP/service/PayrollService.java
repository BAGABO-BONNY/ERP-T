package com.jva.ERP.service;

import com.jva.ERP.dto.PayrollRequest;
import com.jva.ERP.dto.PayrollResult;
import com.jva.ERP.dto.PayslipResponse;
import com.jva.ERP.entity.DeductionType;
import com.jva.ERP.entity.Employee;
import com.jva.ERP.entity.Employment;
import com.jva.ERP.entity.Payslip;
import com.jva.ERP.exception.BusinessException;
import com.jva.ERP.exception.ResourceNotFoundException;
import com.jva.ERP.repository.DeductionTypeRepository;
import com.jva.ERP.repository.EmployeeRepository;
import com.jva.ERP.repository.PayslipRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * PayrollService — processes monthly payroll for all active employees.
 *
 * Payroll calculation flow per employee:
 * ─────────────────────────────────────
 * 1. Fetch base salary from Employment record.
 *
 * 2. Compute ALLOWANCES (added to gross, NOT deducted):
 *      House Allowance     = baseSalary × House%      (default 14%)
 *      Transport Allowance = baseSalary × Transport%  (default 14%)
 *      Meal / Other allowances from Employment record (fixed amounts)
 *
 * 3. Gross Salary = baseSalary + houseAllowance + transportAllowance
 *                 + mealAllowance + otherAllowance
 *
 * 4. Compute DEDUCTIONS (all applied to baseSalary, NOT gross):
 *      EmployeeTax      = baseSalary × 30%
 *      Pension          = baseSalary × 6%
 *      MedicalInsurance = baseSalary × 5%
 *      Others           = baseSalary × 5%
 *
 * 5. Safety cap: totalDeductions must not exceed baseSalary.
 *    If they do, each deduction is scaled down proportionally.
 *
 * 6. Net Salary = baseSalary − totalDeductions
 *    (per exam formula: net is computed from BASE salary, not gross)
 *    Example: baseSalary=70,000 → deductions=32,200 → net=37,800
 *             grossSalary=89,600 (shown on payslip for reference only)
 *
 * 7. Payslip is saved with status "PENDING".
 *    Duplicate payslip detection: a BusinessException is thrown if a payslip
 *    already exists for the same employee+payrollMonth to prevent duplicates.
 */
@Service
@Transactional
public class PayrollService {

    private static final Logger logger = LoggerFactory.getLogger(PayrollService.class);
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final int SCALE = 2;
    private static final RoundingMode RM = RoundingMode.HALF_UP;

    // Known deduction type names (must match DataSeeder seeds)
    private static final String TYPE_TAX       = "EmployeeTax";
    private static final String TYPE_PENSION   = "Pension";
    private static final String TYPE_MEDICAL   = "MedicalInsurance";
    private static final String TYPE_OTHERS    = "Others";
    private static final String TYPE_HOUSE     = "House";
    private static final String TYPE_TRANSPORT = "Transport";

    @Autowired private EmployeeRepository      employeeRepository;
    @Autowired private PayslipRepository       payslipRepository;
    @Autowired private DeductionTypeRepository deductionTypeRepository;
    @Autowired private PayslipNotificationService notificationService;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Process payroll for all active employees for the given month/year.
     * Throws BusinessException if a duplicate payslip is detected for any employee.
     */
    public PayrollResult processPayroll(PayrollRequest request) {
        validateRequest(request);

        String payrollMonth = buildPayrollMonth(request.getYear(), request.getMonth());
        logger.info("Starting payroll run for {}", payrollMonth);

        // Load active deduction type rates into a map for fast lookup
        Map<String, BigDecimal> rates = loadRates();

        List<Employee> employees;
        if (request.getEmployeeId() != null) {
            // Single-employee run
            Employee emp = employeeRepository.findById(request.getEmployeeId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Employee not found: " + request.getEmployeeId()));
            if (!emp.getIsActive()) {
                throw new BusinessException(
                        "Employee " + emp.getEmployeeIdNumber() + " is not active");
            }
            employees = List.of(emp);
        } else {
            employees = employeeRepository.findByIsActiveTrue();
        }

        PayrollResult result = new PayrollResult();
        result.setPayrollMonth(payrollMonth);
        result.setTotalGrossSalary(BigDecimal.ZERO);
        result.setTotalDeductions(BigDecimal.ZERO);
        result.setTotalNetSalary(BigDecimal.ZERO);

        for (Employee employee : employees) {
            try {
                processOneEmployee(employee, request.getYear(), request.getMonth(),
                        payrollMonth, rates, result);
            } catch (Exception e) {
                logger.error("Payroll failed for employee {}: {}",
                        employee.getEmployeeIdNumber(), e.getMessage(), e);
                result.getErrors().add(employee.getEmployeeIdNumber() + ": " + e.getMessage());
                result.setTotalFailed(result.getTotalFailed() + 1);
            }
        }

        logger.info("Payroll run complete for {}. Processed={}, Skipped={}, Failed={}",
                payrollMonth, result.getTotalProcessed(),
                result.getTotalSkipped(), result.getTotalFailed());
        return result;
    }

    /**
     * Fetch all payslips for a given month (YYYY-MM).
     */
    @Transactional(readOnly = true)
    public List<PayslipResponse> getPayslipsByMonth(String payrollMonth) {
        return payslipRepository.findByPayrollMonth(payrollMonth)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * Fetch all payslips for a specific employee.
     */
    @Transactional(readOnly = true)
    public List<PayslipResponse> getPayslipsByEmployee(Long employeeId) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employee not found: " + employeeId);
        }
        return payslipRepository.findByEmployeeId(employeeId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * Fetch a single payslip by its ID.
     */
    @Transactional(readOnly = true)
    public PayslipResponse getPayslipById(Long id) {
        Payslip payslip = payslipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payslip not found: " + id));
        return toResponse(payslip);
    }

    /**
     * Mark a payslip as PAID and finalized.
     * The DB trigger fn_payslip_paid_notification fires on the UPDATE
     * and inserts the salary-credit message into the messages table.
     */
    public PayslipResponse finalizePayslip(Long id) {
        Payslip payslip = payslipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payslip not found: " + id));
        if (payslip.getIsFinalized()) {
            throw new BusinessException("Payslip " + payslip.getPayslipNumber() + " is already finalized");
        }
        payslip.setPaymentStatus("PAID");
        payslip.setIsFinalized(true);
        payslip.setPaymentDate(LocalDate.now());
        return toResponse(payslipRepository.save(payslip));
    }

    // ── Core calculation ──────────────────────────────────────────────────────

    private void processOneEmployee(Employee employee, int year, int month,
                                    String payrollMonth,
                                    Map<String, BigDecimal> rates,
                                    PayrollResult result) {

        // Duplicate detection: throw exception if payslip already exists for this month
        if (payslipRepository.existsByEmployeeIdAndPayrollMonth(employee.getId(), payrollMonth)) {
            throw new BusinessException("Payslip already exists for employee "
                    + employee.getEmployeeIdNumber() + " in " + payrollMonth);
        }

        Employment employment = employee.getEmployment();
        if (employment == null || !employment.getIsActive()) {
            throw new BusinessException("No active employment record found");
        }

        BigDecimal baseSalary = employment.getBaseSalary();
        if (baseSalary == null || baseSalary.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Invalid base salary: " + baseSalary);
        }

        // ── Step 2: Allowances ────────────────────────────────────────────────
        BigDecimal houseAllowance = computeAllowance(
                employment.getHousingAllowance(), baseSalary, rates.get(TYPE_HOUSE));
        BigDecimal transportAllowance = computeAllowance(
                employment.getTransportAllowance(), baseSalary, rates.get(TYPE_TRANSPORT));
        BigDecimal mealAllowance    = orZero(employment.getMealAllowance());
        BigDecimal otherAllowance   = orZero(employment.getOtherAllowance());

        // ── Step 3: Gross Salary ──────────────────────────────────────────────
        BigDecimal grossSalary = baseSalary
                .add(houseAllowance)
                .add(transportAllowance)
                .add(mealAllowance)
                .add(otherAllowance)
                .setScale(SCALE, RM);

        // ── Step 4: Deductions (applied to baseSalary) ────────────────────────
        BigDecimal taxDeduction     = pct(baseSalary, rates.get(TYPE_TAX));
        BigDecimal pensionDeduction = pct(baseSalary, rates.get(TYPE_PENSION));
        BigDecimal medicalDeduction = pct(baseSalary, rates.get(TYPE_MEDICAL));
        BigDecimal othersDeduction  = pct(baseSalary, rates.get(TYPE_OTHERS));

        BigDecimal totalDeductions = taxDeduction
                .add(pensionDeduction)
                .add(medicalDeduction)
                .add(othersDeduction)
                .setScale(SCALE, RM);

        // ── Step 5: Safety cap — deductions must not exceed baseSalary ────────
        if (totalDeductions.compareTo(baseSalary) > 0) {
            logger.warn("Total deductions ({}) exceed base salary ({}) for {} — capping proportionally",
                    totalDeductions, baseSalary, employee.getEmployeeIdNumber());
            BigDecimal scale = baseSalary.divide(totalDeductions, 10, RM);
            taxDeduction     = taxDeduction.multiply(scale).setScale(SCALE, RM);
            pensionDeduction = pensionDeduction.multiply(scale).setScale(SCALE, RM);
            medicalDeduction = medicalDeduction.multiply(scale).setScale(SCALE, RM);
            othersDeduction  = othersDeduction.multiply(scale).setScale(SCALE, RM);
            totalDeductions  = baseSalary;
        }

        // ── Step 6: Net Salary (exam formula: baseSalary − totalDeductions) ──
        // Deductions are computed from baseSalary and subtracted from baseSalary.
        // Gross salary is shown on the payslip for reference but is NOT used here.
        BigDecimal netSalary = baseSalary.subtract(totalDeductions).setScale(SCALE, RM);

        // ── Step 7: Build and save payslip ────────────────────────────────────
        YearMonth ym = YearMonth.of(year, month);
        LocalDate periodStart = ym.atDay(1);
        LocalDate periodEnd   = ym.atEndOfMonth();

        Payslip payslip = new Payslip();
        payslip.setPayslipNumber(generatePayslipNumber(employee.getEmployeeIdNumber(), payrollMonth));
        payslip.setEmployee(employee);
        payslip.setPayrollMonth(payrollMonth);
        payslip.setPayrollPeriodStart(periodStart);
        payslip.setPayrollPeriodEnd(periodEnd);
        payslip.setBasicSalary(baseSalary);
        payslip.setHousingAllowance(houseAllowance);
        payslip.setTransportAllowance(transportAllowance);
        payslip.setMealAllowance(mealAllowance);
        payslip.setBonus(BigDecimal.ZERO);
        payslip.setOtherEarnings(otherAllowance);
        payslip.setGrossSalary(grossSalary);
        payslip.setIncomeTax(taxDeduction);
        payslip.setSocialSecurity(pensionDeduction);
        payslip.setHealthInsurance(medicalDeduction);
        payslip.setLoanDeduction(BigDecimal.ZERO);
        payslip.setOtherDeductions(othersDeduction);
        payslip.setTotalDeductions(totalDeductions);
        payslip.setNetSalary(netSalary);
        payslip.setCurrency(employment.getCurrency());
        payslip.setPaymentStatus("PENDING");
        payslip.setIsFinalized(false);

        Payslip saved = payslipRepository.save(payslip);

        // Accumulate totals
        result.setTotalGrossSalary(result.getTotalGrossSalary().add(grossSalary));
        result.setTotalDeductions(result.getTotalDeductions().add(totalDeductions));
        result.setTotalNetSalary(result.getTotalNetSalary().add(netSalary));
        result.setTotalProcessed(result.getTotalProcessed() + 1);
        result.getPayslips().add(toResponse(saved));

        logger.info("Payslip generated for {} | gross={} deductions={} net={}",
                employee.getEmployeeIdNumber(), grossSalary, totalDeductions, netSalary);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Resolve allowance: use the fixed amount from Employment if set and > 0,
     * otherwise fall back to percentage of base salary.
     */
    private BigDecimal computeAllowance(BigDecimal fixedAmount,
                                        BigDecimal baseSalary,
                                        BigDecimal fallbackPct) {
        if (fixedAmount != null && fixedAmount.compareTo(BigDecimal.ZERO) > 0) {
            return fixedAmount.setScale(SCALE, RM);
        }
        return pct(baseSalary, fallbackPct);
    }

    /** percentage of base: base × (pct / 100) */
    private BigDecimal pct(BigDecimal base, BigDecimal percentage) {
        if (percentage == null || percentage.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return base.multiply(percentage)
                .divide(BigDecimal.valueOf(100), SCALE, RM);
    }

    private BigDecimal orZero(BigDecimal value) {
        return value != null ? value.setScale(SCALE, RM) : BigDecimal.ZERO;
    }

    private String buildPayrollMonth(int year, int month) {
        return String.format("%04d-%02d", year, month);
    }

    private String generatePayslipNumber(String employeeIdNumber, String payrollMonth) {
        return "PS-" + employeeIdNumber + "-" + payrollMonth;
    }

    private Map<String, BigDecimal> loadRates() {
        List<DeductionType> all = deductionTypeRepository.findByIsActiveTrue();
        if (all.isEmpty()) {
            throw new BusinessException(
                    "No active deduction types found. Ensure DataSeeder has run.");
        }
        return all.stream().collect(
                Collectors.toMap(DeductionType::getTypeName, DeductionType::getPercentage));
    }

    private void validateRequest(PayrollRequest request) {
        YearMonth requested = YearMonth.of(request.getYear(), request.getMonth());
        YearMonth current   = YearMonth.now();
        if (requested.isAfter(current)) {
            throw new BusinessException(
                    "Cannot process payroll for a future month: " + requested);
        }
    }

    // ── Response mapping ──────────────────────────────────────────────────────

    private PayslipResponse toResponse(Payslip p) {
        PayslipResponse r = new PayslipResponse();
        r.setId(p.getId());
        r.setPayslipNumber(p.getPayslipNumber());
        r.setPayrollMonth(p.getPayrollMonth());
        r.setPayrollPeriodStart(p.getPayrollPeriodStart());
        r.setPayrollPeriodEnd(p.getPayrollPeriodEnd());
        r.setPaymentDate(p.getPaymentDate());

        Employee emp = p.getEmployee();
        r.setEmployeeId(emp.getId());
        r.setEmployeeIdNumber(emp.getEmployeeIdNumber());
        r.setEmployeeName(emp.getFirstName() + " " + emp.getLastName());
        if (emp.getEmployment() != null) {
            r.setDepartment(emp.getEmployment().getDepartment());
            r.setJobTitle(emp.getEmployment().getJobTitle());
        }

        r.setBasicSalary(p.getBasicSalary());
        r.setHousingAllowance(p.getHousingAllowance());
        r.setTransportAllowance(p.getTransportAllowance());
        r.setMealAllowance(p.getMealAllowance());
        r.setBonus(p.getBonus());
        r.setOtherEarnings(p.getOtherEarnings());
        r.setGrossSalary(p.getGrossSalary());

        r.setEmployeeTax(p.getIncomeTax());
        r.setPension(p.getSocialSecurity());
        r.setMedicalInsurance(p.getHealthInsurance());
        r.setOtherDeductions(p.getOtherDeductions());
        r.setTotalDeductions(p.getTotalDeductions());
        r.setNetSalary(p.getNetSalary());

        r.setCurrency(p.getCurrency());
        r.setPaymentStatus(p.getPaymentStatus());
        r.setIsFinalized(p.getIsFinalized());
        r.setRemarks(p.getRemarks());
        return r;
    }
}
