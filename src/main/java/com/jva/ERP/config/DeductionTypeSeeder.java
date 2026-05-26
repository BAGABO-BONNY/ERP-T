package com.jva.ERP.config;

import com.jva.ERP.entity.DeductionType;
import com.jva.ERP.repository.DeductionTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * DeductionTypeSeeder — seeds the default system-wide deduction/allowance rates.
 *
 * Runs at startup (after DataSeeder, order = 2).
 * Idempotent: skips any type that already exists by typeName.
 *
 * Default rates:
 *   EmployeeTax       30%  deduction  → incomeTax on payslip
 *   Pension            6%  deduction  → socialSecurity on payslip
 *   MedicalInsurance   5%  deduction  → healthInsurance on payslip
 *   Others             5%  deduction  → otherDeductions on payslip
 *   House             14%  allowance  → housingAllowance on payslip (added to gross)
 *   Transport         14%  allowance  → transportAllowance on payslip (added to gross)
 */
@Component
@Order(2)
public class DeductionTypeSeeder implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DeductionTypeSeeder.class);

    private final DeductionTypeRepository deductionTypeRepository;

    public DeductionTypeSeeder(DeductionTypeRepository deductionTypeRepository) {
        this.deductionTypeRepository = deductionTypeRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<DeductionType> defaults = List.of(
                type("EmployeeTax",       "Employee Tax",        new BigDecimal("30.00"), false,
                        "Income tax deducted from gross salary"),
                type("Pension",           "Pension",             new BigDecimal("6.00"),  false,
                        "Pension / social security contribution"),
                type("MedicalInsurance",  "Medical Insurance",   new BigDecimal("5.00"),  false,
                        "Medical / health insurance deduction"),
                type("Others",            "Other Deductions",    new BigDecimal("5.00"),  false,
                        "Miscellaneous deductions"),
                type("House",             "Housing Allowance",   new BigDecimal("14.00"), true,
                        "Housing allowance — percentage of base salary added to gross"),
                type("Transport",         "Transport Allowance", new BigDecimal("14.00"), true,
                        "Transport allowance — percentage of base salary added to gross")
        );

        for (DeductionType dt : defaults) {
            if (deductionTypeRepository.existsByTypeName(dt.getTypeName())) {
                logger.debug("DeductionType '{}' already exists — skipping.", dt.getTypeName());
            } else {
                deductionTypeRepository.save(dt);
                logger.info("Seeded DeductionType '{}' at {}%{}",
                        dt.getTypeName(), dt.getPercentage(),
                        dt.getIsAllowance() ? " (allowance)" : " (deduction)");
            }
        }
    }

    private DeductionType type(String name, String display, BigDecimal pct,
                               boolean isAllowance, String description) {
        DeductionType dt = new DeductionType();
        dt.setTypeName(name);
        dt.setDisplayName(display);
        dt.setPercentage(pct);
        dt.setIsAllowance(isAllowance);
        dt.setDescription(description);
        dt.setIsActive(true);
        return dt;
    }
}
