package com.jva.ERP.service;

import com.jva.ERP.dto.PayslipResponse;
import com.jva.ERP.entity.User;
import com.jva.ERP.exception.ResourceNotFoundException;
import com.jva.ERP.repository.PayslipRepository;
import com.jva.ERP.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * PayslipService — read-only payslip access for employees and managers.
 *
 * Role-based access:
 *   EMPLOYEE  → can only view their own payslips (enforced in controller + here)
 *   MANAGER   → can view all payslips
 *   ADMIN     → can view all payslips
 */
@Service
@Transactional(readOnly = true)
public class PayslipService {

    private static final Logger logger = LoggerFactory.getLogger(PayslipService.class);

    @Autowired private PayslipRepository payslipRepository;
    @Autowired private UserRepository    userRepository;
    @Autowired private PayrollService    payrollService;

    /**
     * Get all payslips for a specific employee by their DB id.
     */
    public List<PayslipResponse> getPayslipsForEmployee(Long employeeId) {
        return payslipRepository.findByEmployeeId(employeeId)
                .stream()
                .map(p -> payrollService.getPayslipById(p.getId()))
                .collect(Collectors.toList());
    }

    /**
     * Get all payslips in the system (MANAGER / ADMIN only).
     */
    public List<PayslipResponse> getAllPayslips() {
        return payslipRepository.findAll()
                .stream()
                .map(p -> payrollService.getPayslipById(p.getId()))
                .collect(Collectors.toList());
    }

    /**
     * Get a single payslip by its DB id.
     */
    public PayslipResponse getPayslip(Long id) {
        return payrollService.getPayslipById(id);
    }

    /**
     * Get all payslips for the currently authenticated user.
     * Resolves the username → User → Employee chain.
     */
    public List<PayslipResponse> getPayslipsForCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        if (user.getEmployee() == null) {
            throw new ResourceNotFoundException(
                    "No employee record linked to user: " + username);
        }
        Long empId = user.getEmployee().getId();
        logger.debug("Fetching payslips for user '{}' (employeeId={})", username, empId);
        return getPayslipsForEmployee(empId);
    }

    /**
     * Get all payslips for a given payroll month (YYYY-MM).
     * Used by managers to review a specific month's payroll.
     */
    public List<PayslipResponse> getPayslipsByMonth(String payrollMonth) {
        return payrollService.getPayslipsByMonth(payrollMonth);
    }
}
