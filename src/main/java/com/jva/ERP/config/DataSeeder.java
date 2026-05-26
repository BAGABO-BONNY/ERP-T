package com.jva.ERP.config;

import com.jva.ERP.entity.DeductionType;
import com.jva.ERP.entity.User;
import com.jva.ERP.enums.UserRole;
import com.jva.ERP.repository.DeductionTypeRepository;
import com.jva.ERP.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * DataSeeder — runs once at application startup (idempotent).
 *
 * Seeds:
 *  1. ADMIN and MANAGER user accounts (credentials from application.properties)
 *  2. Default system DeductionTypes:
 *       EmployeeTax       30%  (deduction)
 *       Pension            6%  (deduction)
 *       MedicalInsurance   5%  (deduction)
 *       Others             5%  (deduction)
 *       House             14%  (allowance — added to gross salary)
 *       Transport         14%  (allowance — added to gross salary)
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SeedProperties seedProperties;
    private final DeductionTypeRepository deductionTypeRepository;

    public DataSeeder(UserRepository userRepository,
                      PasswordEncoder passwordEncoder,
                      SeedProperties seedProperties,
                      DeductionTypeRepository deductionTypeRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.seedProperties = seedProperties;
        this.deductionTypeRepository = deductionTypeRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // 1. Seed privileged user accounts
        seedUser(seedProperties.getAdmin(), UserRole.ADMIN);
        seedUser(seedProperties.getManager(), UserRole.MANAGER);

        // 2. Seed default deduction types
        seedDeductionTypes();
    }

    // ── User seeding ──────────────────────────────────────────────────────────

    private void seedUser(SeedProperties.UserSeed seed, UserRole role) {
        if (seed.getUsername() == null || seed.getUsername().isBlank()) {
            logger.warn("Seed username for role {} is not configured — skipping.", role);
            return;
        }
        if (userRepository.existsByUsername(seed.getUsername())) {
            logger.info("{} account '{}' already exists — skipping.", role, seed.getUsername());
            return;
        }

        User user = new User();
        user.setUsername(seed.getUsername());
        user.setEmail(seed.getEmail());
        user.setPassword(passwordEncoder.encode(seed.getPassword()));
        user.setFirstName(seed.getFirstName());
        user.setLastName(seed.getLastName());
        user.setRole(role);
        user.setIsActive(true);
        user.setIsLocked(false);

        userRepository.save(user);
        logger.info("Seeded {} account: '{}'", role, seed.getUsername());
    }

    // ── Deduction type seeding ────────────────────────────────────────────────

    private void seedDeductionTypes() {
        // typeName, displayName, description, percentage, isAllowance
        Object[][] defaults = {
            { "EmployeeTax",      "Employee Tax",        "Statutory employee income tax",          new BigDecimal("30.00"), false },
            { "Pension",          "Pension",             "Pension / retirement fund contribution",  new BigDecimal("6.00"),  false },
            { "MedicalInsurance", "Medical Insurance",   "Medical / health insurance premium",      new BigDecimal("5.00"),  false },
            { "Others",           "Other Deductions",    "Miscellaneous deductions",                new BigDecimal("5.00"),  false },
            { "House",            "Housing Allowance",   "Housing allowance (% of base salary)",    new BigDecimal("14.00"), true  },
            { "Transport",        "Transport Allowance", "Transport allowance (% of base salary)",  new BigDecimal("14.00"), true  },
        };

        for (Object[] row : defaults) {
            String name = (String) row[0];
            if (deductionTypeRepository.existsByTypeName(name)) {
                logger.info("DeductionType '{}' already exists — skipping.", name);
                continue;
            }
            DeductionType dt = new DeductionType();
            dt.setTypeName(name);
            dt.setDisplayName((String) row[1]);
            dt.setDescription((String) row[2]);
            dt.setPercentage((BigDecimal) row[3]);
            dt.setIsAllowance((Boolean) row[4]);
            dt.setIsActive(true);
            deductionTypeRepository.save(dt);
            logger.info("Seeded DeductionType: {} @ {}%{}", name, row[3],
                    (Boolean) row[4] ? " [allowance]" : " [deduction]");
        }
    }
}
