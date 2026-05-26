package com.jva.ERP.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO returned when reading Employee data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeResponse {

    private Long id;
    private String employeeIdNumber;
    private String firstName;
    private String middleName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String personalEmail;
    private LocalDate dateOfBirth;
    private String gender;
    private String maritalStatus;
    private String nationality;
    private String nationalId;

    // Address
    private String streetAddress;
    private String city;
    private String state;
    private String postalCode;
    private String country;

    // Emergency Contact
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactEmail;
    private String emergencyContactRelationship;

    // Bank
    private String bankName;
    private String bankAccountNumber;
    private String bankBranch;

    // Tax
    private String taxId;
    private String taxFilingStatus;
    private Integer numberOfDependents;

    // Employment summary
    private String jobTitle;
    private String department;
    private String positionLevel;
    private String employmentType;
    private String employmentStatus;
    private LocalDate hireDate;
    private BigDecimal baseSalary;
    private String currency;
    private String payFrequency;

    // Linked user
    private String username;
    private String role;

    private Boolean isActive;
}
