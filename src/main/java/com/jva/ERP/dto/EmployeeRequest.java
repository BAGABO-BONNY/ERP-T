package com.jva.ERP.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for creating or updating an Employee along with their Employment details.
 *
 * Validation rules enforced here (bean validation layer):
 *  - Required fields: employeeIdNumber, firstName, lastName, email, jobTitle,
 *    department, employmentType, employmentStatus, hireDate, baseSalary, currency, payFrequency
 *  - Email must be well-formed
 *  - baseSalary must be > 0
 *  - numberOfDependents must be >= 0 when provided
 *  - annualLeaveDays / sickLeaveDays / maternityLeaveDays must be >= 0 when provided
 *  - Allowances must be >= 0 when provided
 *
 * Additional business rules enforced in EmployeeService:
 *  - employeeIdNumber must be unique
 *  - email must be unique (across all employees)
 *  - hireDate must not be in the future
 *  - probationEndDate (if set) must be after hireDate
 *  - username (if set) must be unique and password must be provided
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeRequest {

    // ── Personal Information ──────────────────────────────────────────────────

    @NotBlank(message = "Employee ID number is required")
    @Size(max = 20, message = "Employee ID number must not exceed 20 characters")
    private String employeeIdNumber;

    @NotBlank(message = "First name is required")
    @Size(min = 1, max = 50, message = "First name must be between 1 and 50 characters")
    private String firstName;

    @Size(max = 50, message = "Middle name must not exceed 50 characters")
    private String middleName;

    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    private String phoneNumber;

    @Email(message = "Personal email must be a valid email address")
    @Size(max = 100, message = "Personal email must not exceed 100 characters")
    private String personalEmail;

    private LocalDate dateOfBirth;

    @Size(max = 10, message = "Gender must not exceed 10 characters")
    private String gender;

    @Size(max = 20, message = "Marital status must not exceed 20 characters")
    private String maritalStatus;

    @Size(max = 50, message = "Nationality must not exceed 50 characters")
    private String nationality;

    @Size(max = 20, message = "National ID must not exceed 20 characters")
    private String nationalId;

    // Address
    @Size(max = 255, message = "Street address must not exceed 255 characters")
    private String streetAddress;

    @Size(max = 50, message = "City must not exceed 50 characters")
    private String city;

    @Size(max = 50, message = "State must not exceed 50 characters")
    private String state;

    @Size(max = 20, message = "Postal code must not exceed 20 characters")
    private String postalCode;

    @Size(max = 50, message = "Country must not exceed 50 characters")
    private String country;

    // Emergency Contact
    @Size(max = 100, message = "Emergency contact name must not exceed 100 characters")
    private String emergencyContactName;

    @Size(max = 20, message = "Emergency contact phone must not exceed 20 characters")
    private String emergencyContactPhone;

    @Email(message = "Emergency contact email must be a valid email address")
    @Size(max = 100, message = "Emergency contact email must not exceed 100 characters")
    private String emergencyContactEmail;

    @Size(max = 50, message = "Emergency contact relationship must not exceed 50 characters")
    private String emergencyContactRelationship;

    // Bank Information
    @Size(max = 100, message = "Bank name must not exceed 100 characters")
    private String bankName;

    @Size(max = 30, message = "Bank account number must not exceed 30 characters")
    private String bankAccountNumber;

    @Size(max = 100, message = "Bank branch must not exceed 100 characters")
    private String bankBranch;

    // Tax Information
    @Size(max = 30, message = "Tax ID must not exceed 30 characters")
    private String taxId;

    @Size(max = 30, message = "Tax filing status must not exceed 30 characters")
    private String taxFilingStatus;

    @Min(value = 0, message = "Number of dependents must be 0 or greater")
    private Integer numberOfDependents;

    // ── Employment Information ────────────────────────────────────────────────

    @NotBlank(message = "Job title is required")
    @Size(max = 100, message = "Job title must not exceed 100 characters")
    private String jobTitle;

    @NotBlank(message = "Department is required")
    @Size(max = 100, message = "Department must not exceed 100 characters")
    private String department;

    @Size(max = 50, message = "Position level must not exceed 50 characters")
    private String positionLevel;

    @NotBlank(message = "Employment type is required")
    @Size(max = 30, message = "Employment type must not exceed 30 characters")
    private String employmentType;   // Full-time, Part-time, Contract

    @NotBlank(message = "Employment status is required")
    @Size(max = 30, message = "Employment status must not exceed 30 characters")
    private String employmentStatus; // Active, Inactive, On Leave

    @NotNull(message = "Hire date is required")
    private LocalDate hireDate;

    private LocalDate probationEndDate;

    @NotNull(message = "Base salary is required")
    @DecimalMin(value = "0.01", message = "Base salary must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Base salary must have at most 8 integer digits and 2 decimal places")
    private BigDecimal baseSalary;

    @NotBlank(message = "Currency is required")
    @Size(max = 10, message = "Currency must not exceed 10 characters")
    private String currency;

    @NotBlank(message = "Pay frequency is required")
    @Size(max = 20, message = "Pay frequency must not exceed 20 characters")
    private String payFrequency;

    private LocalDate salaryEffectiveDate;

    @DecimalMin(value = "0.00", message = "Housing allowance must be 0 or greater")
    @Digits(integer = 8, fraction = 2, message = "Housing allowance must have at most 8 integer digits and 2 decimal places")
    private BigDecimal housingAllowance;

    @DecimalMin(value = "0.00", message = "Transport allowance must be 0 or greater")
    @Digits(integer = 8, fraction = 2, message = "Transport allowance must have at most 8 integer digits and 2 decimal places")
    private BigDecimal transportAllowance;

    @DecimalMin(value = "0.00", message = "Meal allowance must be 0 or greater")
    @Digits(integer = 8, fraction = 2, message = "Meal allowance must have at most 8 integer digits and 2 decimal places")
    private BigDecimal mealAllowance;

    @DecimalMin(value = "0.00", message = "Other allowance must be 0 or greater")
    @Digits(integer = 8, fraction = 2, message = "Other allowance must have at most 8 integer digits and 2 decimal places")
    private BigDecimal otherAllowance;

    @Min(value = 0, message = "Annual leave days must be 0 or greater")
    private Integer annualLeaveDays;

    @Min(value = 0, message = "Sick leave days must be 0 or greater")
    private Integer sickLeaveDays;

    @Min(value = 0, message = "Maternity leave days must be 0 or greater")
    private Integer maternityLeaveDays;

    @Size(max = 100, message = "Manager name must not exceed 100 characters")
    private String managerName;

    @Email(message = "Manager email must be a valid email address")
    @Size(max = 100, message = "Manager email must not exceed 100 characters")
    private String managerEmail;

    // ── Optional: create a linked user account ────────────────────────────────

    /** If provided, a User account (EMPLOYEE role) will be created for this employee. */
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String password;
}
