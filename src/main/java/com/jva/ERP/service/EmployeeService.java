package com.jva.ERP.service;

import com.jva.ERP.dto.EmployeeRequest;
import com.jva.ERP.dto.EmployeeResponse;
import com.jva.ERP.entity.Employee;
import com.jva.ERP.entity.Employment;
import com.jva.ERP.entity.User;
import com.jva.ERP.enums.UserRole;
import com.jva.ERP.exception.BusinessException;
import com.jva.ERP.exception.ResourceNotFoundException;
import com.jva.ERP.repository.EmployeeRepository;
import com.jva.ERP.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * EmployeeService — business logic for Employee lifecycle management.
 *
 * Validation layers:
 *  1. Bean validation (@Valid on controller) — field-level constraints from EmployeeRequest
 *  2. Business validation (this class) — cross-field rules, uniqueness, date logic
 *
 * All write operations are transactional. Read operations use readOnly transactions
 * to avoid unnecessary dirty-checking overhead.
 */
@Service
@Transactional
public class EmployeeService {

    private static final Logger logger = LoggerFactory.getLogger(EmployeeService.class);

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ── Create ────────────────────────────────────────────────────────────────

    /**
     * Register a new employee.
     *
     * Business rules enforced:
     * - employeeIdNumber must be unique
     * - email must be unique across all employees
     * - hireDate must not be in the future
     * - probationEndDate (if provided) must be after hireDate
     * - if username is provided, it must be unique and password must be supplied
     *
     * @param request validated EmployeeRequest DTO
     * @return EmployeeResponse with the persisted data
     * @throws BusinessException on any business rule violation
     */
    public EmployeeResponse registerEmployee(EmployeeRequest request) {
        logger.info("Registering new employee: {}", request.getEmployeeIdNumber());

        validateNewEmployee(request);

        Employee employee = buildEmployee(request);

        // Link to an existing registered User (must exist; validated in validateNewEmployee)
        if (hasText(request.getUsername())) {
            userRepository.findByUsername(request.getUsername())
                    .ifPresent(employee::setUser);
        }

        employee.setEmployment(buildEmployment(request, employee));

        Employee saved = employeeRepository.save(employee);
        logger.info("Employee registered successfully: {} (id={})",
                saved.getEmployeeIdNumber(), saved.getId());
        return toResponse(saved);
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    /**
     * Returns all active employees.
     */
    @Transactional(readOnly = true)
    public List<EmployeeResponse> getAllEmployees() {
        return employeeRepository.findByIsActiveTrue()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Returns a single employee by database primary key.
     *
     * @throws ResourceNotFoundException if no employee exists with that id
     */
    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeById(Long id) {
        return toResponse(findActiveById(id));
    }

    /**
     * Returns a single employee by their human-readable employee ID number (e.g. EMP-001).
     *
     * @throws ResourceNotFoundException if not found
     */
    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeByIdNumber(String employeeIdNumber) {
        Employee employee = employeeRepository.findByEmployeeIdNumber(employeeIdNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found with ID number: " + employeeIdNumber));
        return toResponse(employee);
    }

    /**
     * Returns employees whose first name, last name, or email contains the query string.
     * Case-insensitive. Only active employees are returned.
     */
    @Transactional(readOnly = true)
    public List<EmployeeResponse> searchEmployees(String query) {
        if (query == null || query.isBlank()) {
            return getAllEmployees();
        }
        return employeeRepository.searchByNameOrEmail(query.trim())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Returns all active employees in a given department.
     */
    @Transactional(readOnly = true)
    public List<EmployeeResponse> getEmployeesByDepartment(String department) {
        if (department == null || department.isBlank()) {
            throw new BusinessException("Department name must not be blank");
        }
        return employeeRepository.findByDepartment(department)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Returns the count of currently active employees.
     */
    @Transactional(readOnly = true)
    public long countActiveEmployees() {
        return employeeRepository.countByIsActiveTrue();
    }

    // ── Update ────────────────────────────────────────────────────────────────

    /**
     * Updates an existing employee's personal and employment details.
     *
     * Business rules enforced:
     * - Employee must exist and be active
     * - If email is changed, the new email must not belong to another employee
     * - If employeeIdNumber is changed, the new ID must not belong to another employee
     * - hireDate must not be in the future
     * - probationEndDate (if provided) must be after hireDate
     *
     * @param id      database primary key of the employee to update
     * @param request validated EmployeeRequest DTO with updated values
     * @return updated EmployeeResponse
     * @throws ResourceNotFoundException if employee not found
     * @throws BusinessException         on any business rule violation
     */
    public EmployeeResponse updateEmployee(Long id, EmployeeRequest request) {
        Employee employee = findActiveById(id);

        validateUpdateEmployee(id, request, employee);

        updateEmployeeFields(employee, request);

        if (employee.getEmployment() != null) {
            updateEmploymentFields(employee.getEmployment(), request);
        } else {
            employee.setEmployment(buildEmployment(request, employee));
        }

        Employee saved = employeeRepository.save(employee);
        logger.info("Employee updated: {} (id={})", saved.getEmployeeIdNumber(), saved.getId());
        return toResponse(saved);
    }

    // ── Deactivate / Reactivate ───────────────────────────────────────────────

    /**
     * Soft-deletes an employee by setting isActive = false.
     * Also deactivates the linked Employment record and User account.
     * The database record is retained for audit purposes.
     *
     * @throws ResourceNotFoundException if employee not found
     * @throws BusinessException         if employee is already inactive
     */
    public void deactivateEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found with id: " + id));

        if (!employee.getIsActive()) {
            throw new BusinessException(
                    "Employee is already inactive: " + employee.getEmployeeIdNumber());
        }

        employee.setIsActive(false);

        if (employee.getEmployment() != null) {
            employee.getEmployment().setIsActive(false);
        }
        if (employee.getUser() != null) {
            employee.getUser().setIsActive(false);
        }

        employeeRepository.save(employee);
        logger.info("Employee deactivated: {} (id={})", employee.getEmployeeIdNumber(), id);
    }

    /**
     * Reactivates a previously deactivated employee.
     * Also reactivates the linked Employment record and User account.
     *
     * @throws ResourceNotFoundException if employee not found
     * @throws BusinessException         if employee is already active
     */
    public EmployeeResponse reactivateEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found with id: " + id));

        if (employee.getIsActive()) {
            throw new BusinessException(
                    "Employee is already active: " + employee.getEmployeeIdNumber());
        }

        employee.setIsActive(true);

        if (employee.getEmployment() != null) {
            employee.getEmployment().setIsActive(true);
        }
        if (employee.getUser() != null) {
            employee.getUser().setIsActive(true);
        }

        Employee saved = employeeRepository.save(employee);
        logger.info("Employee reactivated: {} (id={})", saved.getEmployeeIdNumber(), id);
        return toResponse(saved);
    }

    // ── Business validation ───────────────────────────────────────────────────

    /**
     * Validates all business rules for a new employee registration.
     */
    private void validateNewEmployee(EmployeeRequest r) {
        // Uniqueness checks
        if (employeeRepository.existsByEmployeeIdNumber(r.getEmployeeIdNumber())) {
            throw new BusinessException(
                    "Employee ID number already exists: " + r.getEmployeeIdNumber());
        }
        if (employeeRepository.existsByEmail(r.getEmail())) {
            throw new BusinessException(
                    "An employee with this email already exists: " + r.getEmail());
        }

        // Date logic
        validateDates(r);

        // Linked user account — employee must correspond to an existing registered User
        if (!hasText(r.getUsername())) {
            throw new BusinessException("Username is required and must belong to an existing registered user");
        }

        var userOpt = userRepository.findByUsername(r.getUsername());
        if (userOpt.isEmpty()) {
            throw new BusinessException("No registered user found with username: " + r.getUsername());
        }

        var user = userOpt.get();

        // Ensure the user is active and has the EMPLOYEE role
        if (user.getRole() != UserRole.EMPLOYEE) {
            throw new BusinessException("User must have role EMPLOYEE to be linked to an employee");
        }
        if (user.getIsActive() == null || !user.getIsActive()) {
            throw new BusinessException("User account is not active: " + r.getUsername());
        }

        // Ensure no existing employee is already linked to this user
        if (employeeRepository.findByUsername(r.getUsername()).isPresent()) {
            throw new BusinessException("An employee is already linked to user: " + r.getUsername());
        }

        // Ensure provided email matches the user's email to avoid mismatches
        if (user.getEmail() != null && !user.getEmail().equalsIgnoreCase(r.getEmail())) {
            throw new BusinessException("Provided employee email does not match user's email");
        }
    }

    /**
     * Validates all business rules for an employee update.
     */
    private void validateUpdateEmployee(Long id, EmployeeRequest r, Employee existing) {
        // Email uniqueness — allow same email on the same record
        if (!existing.getEmail().equalsIgnoreCase(r.getEmail()) &&
                employeeRepository.existsByEmailAndIdNot(r.getEmail(), id)) {
            throw new BusinessException(
                    "Another employee already uses this email: " + r.getEmail());
        }

        // Employee ID number uniqueness — allow same ID on the same record
        if (!existing.getEmployeeIdNumber().equals(r.getEmployeeIdNumber()) &&
                employeeRepository.existsByEmployeeIdNumberAndIdNot(r.getEmployeeIdNumber(), id)) {
            throw new BusinessException(
                    "Another employee already uses this ID number: " + r.getEmployeeIdNumber());
        }

        // Date logic
        validateDates(r);
    }

    /**
     * Shared date validation for both create and update.
     */
    private void validateDates(EmployeeRequest r) {
        if (r.getHireDate().isAfter(LocalDate.now())) {
            throw new BusinessException("Hire date cannot be in the future");
        }
        if (r.getProbationEndDate() != null &&
                !r.getProbationEndDate().isAfter(r.getHireDate())) {
            throw new BusinessException(
                    "Probation end date must be after hire date");
        }
        if (r.getDateOfBirth() != null && r.getDateOfBirth().isAfter(LocalDate.now())) {
            throw new BusinessException("Date of birth cannot be in the future");
        }
        if (r.getDateOfBirth() != null &&
                r.getDateOfBirth().isAfter(LocalDate.now().minusYears(16))) {
            throw new BusinessException(
                    "Employee must be at least 16 years old");
        }
    }

    // ── Entity builders ───────────────────────────────────────────────────────

    private Employee buildEmployee(EmployeeRequest r) {
        Employee e = new Employee();
        updateEmployeeFields(e, r);
        return e;
    }

    private void updateEmployeeFields(Employee e, EmployeeRequest r) {
        e.setEmployeeIdNumber(r.getEmployeeIdNumber());
        e.setFirstName(r.getFirstName().trim());
        e.setMiddleName(r.getMiddleName() != null ? r.getMiddleName().trim() : null);
        e.setLastName(r.getLastName().trim());
        e.setEmail(r.getEmail().trim().toLowerCase());
        e.setPhoneNumber(r.getPhoneNumber());
        e.setPersonalEmail(r.getPersonalEmail() != null
                ? r.getPersonalEmail().trim().toLowerCase() : null);
        e.setDateOfBirth(r.getDateOfBirth());
        e.setGender(r.getGender());
        e.setMaritalStatus(r.getMaritalStatus());
        e.setNationality(r.getNationality());
        e.setNationalId(r.getNationalId());
        e.setStreetAddress(r.getStreetAddress());
        e.setCity(r.getCity());
        e.setState(r.getState());
        e.setPostalCode(r.getPostalCode());
        e.setCountry(r.getCountry());
        e.setEmergencyContactName(r.getEmergencyContactName());
        e.setEmergencyContactPhone(r.getEmergencyContactPhone());
        e.setEmergencyContactEmail(r.getEmergencyContactEmail());
        e.setEmergencyContactRelationship(r.getEmergencyContactRelationship());
        e.setBankName(r.getBankName());
        e.setBankAccountNumber(r.getBankAccountNumber());
        e.setBankBranch(r.getBankBranch());
        e.setTaxId(r.getTaxId());
        e.setTaxFilingStatus(r.getTaxFilingStatus());
        e.setNumberOfDependents(r.getNumberOfDependents());
        e.setIsActive(true);
    }

    private Employment buildEmployment(EmployeeRequest r, Employee employee) {
        Employment emp = new Employment();
        updateEmploymentFields(emp, r);
        emp.setEmployee(employee);
        return emp;
    }

    private void updateEmploymentFields(Employment emp, EmployeeRequest r) {
        emp.setJobTitle(r.getJobTitle().trim());
        emp.setDepartment(r.getDepartment().trim());
        emp.setPositionLevel(r.getPositionLevel());
        emp.setEmploymentType(r.getEmploymentType());
        emp.setEmploymentStatus(r.getEmploymentStatus());
        emp.setHireDate(r.getHireDate());
        emp.setProbationEndDate(r.getProbationEndDate());
        emp.setBaseSalary(r.getBaseSalary());
        emp.setCurrency(r.getCurrency().toUpperCase());
        emp.setPayFrequency(r.getPayFrequency());
        emp.setSalaryEffectiveDate(r.getSalaryEffectiveDate());
        emp.setHousingAllowance(r.getHousingAllowance());
        emp.setTransportAllowance(r.getTransportAllowance());
        emp.setMealAllowance(r.getMealAllowance());
        emp.setOtherAllowance(r.getOtherAllowance());
        emp.setAnnualLeaveDays(r.getAnnualLeaveDays());
        emp.setSickLeaveDays(r.getSickLeaveDays());
        emp.setMaternityLeaveDays(r.getMaternityLeaveDays());
        emp.setManagerName(r.getManagerName());
        emp.setManagerEmail(r.getManagerEmail());
        emp.setIsActive(true);
    }

    private User buildLinkedUser(EmployeeRequest r) {
        User user = new User();
        user.setUsername(r.getUsername().trim());
        user.setEmail(r.getEmail().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(r.getPassword()));
        user.setFirstName(r.getFirstName().trim());
        user.setLastName(r.getLastName().trim());
        user.setPhoneNumber(r.getPhoneNumber());
        user.setRole(UserRole.EMPLOYEE); // always EMPLOYEE — non-negotiable
        user.setIsActive(true);
        user.setIsLocked(false);
        return user;
    }

    // ── Response mapping ──────────────────────────────────────────────────────

    private EmployeeResponse toResponse(Employee e) {
        EmployeeResponse res = new EmployeeResponse();
        res.setId(e.getId());
        res.setEmployeeIdNumber(e.getEmployeeIdNumber());
        res.setFirstName(e.getFirstName());
        res.setMiddleName(e.getMiddleName());
        res.setLastName(e.getLastName());
        res.setEmail(e.getEmail());
        res.setPhoneNumber(e.getPhoneNumber());
        res.setPersonalEmail(e.getPersonalEmail());
        res.setDateOfBirth(e.getDateOfBirth());
        res.setGender(e.getGender());
        res.setMaritalStatus(e.getMaritalStatus());
        res.setNationality(e.getNationality());
        res.setNationalId(e.getNationalId());
        res.setStreetAddress(e.getStreetAddress());
        res.setCity(e.getCity());
        res.setState(e.getState());
        res.setPostalCode(e.getPostalCode());
        res.setCountry(e.getCountry());
        res.setEmergencyContactName(e.getEmergencyContactName());
        res.setEmergencyContactPhone(e.getEmergencyContactPhone());
        res.setEmergencyContactEmail(e.getEmergencyContactEmail());
        res.setEmergencyContactRelationship(e.getEmergencyContactRelationship());
        res.setBankName(e.getBankName());
        res.setBankAccountNumber(e.getBankAccountNumber());
        res.setBankBranch(e.getBankBranch());
        res.setTaxId(e.getTaxId());
        res.setTaxFilingStatus(e.getTaxFilingStatus());
        res.setNumberOfDependents(e.getNumberOfDependents());
        res.setIsActive(e.getIsActive());

        if (e.getEmployment() != null) {
            Employment emp = e.getEmployment();
            res.setJobTitle(emp.getJobTitle());
            res.setDepartment(emp.getDepartment());
            res.setPositionLevel(emp.getPositionLevel());
            res.setEmploymentType(emp.getEmploymentType());
            res.setEmploymentStatus(emp.getEmploymentStatus());
            res.setHireDate(emp.getHireDate());
            res.setBaseSalary(emp.getBaseSalary());
            res.setCurrency(emp.getCurrency());
            res.setPayFrequency(emp.getPayFrequency());
        }

        if (e.getUser() != null) {
            res.setUsername(e.getUser().getUsername());
            res.setRole(e.getUser().getRole().name());
        }

        return res;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Finds an active employee by id, throws ResourceNotFoundException if missing. */
    private Employee findActiveById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found with id: " + id));
        if (!employee.getIsActive()) {
            throw new ResourceNotFoundException(
                    "Employee with id " + id + " is inactive");
        }
        return employee;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
