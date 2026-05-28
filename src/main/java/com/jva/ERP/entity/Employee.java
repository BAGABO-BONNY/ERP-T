package com.jva.ERP.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.List;

/**
 * Employee Entity - Represents an employee's personal information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "employees", uniqueConstraints = {
    @UniqueConstraint(columnNames = "employee_id_number")
})
public class Employee extends BaseEntity {

    @Column(name = "employee_id_number", nullable = false, length = 20, unique = true)
    private String employeeIdNumber;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "middle_name", length = 50)
    private String middleName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "email", nullable = false, length = 100, unique = true)
    private String email;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "personal_email", length = 100)
    private String personalEmail;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "gender", length = 10)
    private String gender;

    @Column(name = "marital_status", length = 20)
    private String maritalStatus;

    @Column(name = "nationality", length = 50)
    private String nationality;

    @Column(name = "national_id", length = 20)
    private String nationalId;

    // Address Information
    @Column(name = "street_address", length = 255)
    private String streetAddress;

    @Column(name = "city", length = 50)
    private String city;

    @Column(name = "district", length = 100)
    private String district;

    @Column(name = "state", length = 50)
    private String state;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "country", length = 50)
    private String country;

    // Emergency Contact
    @Column(name = "emergency_contact_name", length = 100)
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone", length = 20)
    private String emergencyContactPhone;

    @Column(name = "emergency_contact_email", length = 100)
    private String emergencyContactEmail;

    @Column(name = "emergency_contact_relationship", length = 50)
    private String emergencyContactRelationship;

    // Bank Information
    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "bank_account_number", length = 30)
    private String bankAccountNumber;

    @Column(name = "bank_branch", length = 100)
    private String bankBranch;

    // Tax Information
    @Column(name = "tax_id", length = 30)
    private String taxId;

    @Column(name = "tax_filing_status", length = 30)
    private String taxFilingStatus;

    @Column(name = "number_of_dependents")
    private Integer numberOfDependents;

    // One-to-One relationship with Employment
    @OneToOne(mappedBy = "employee", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Employment employment;

    // One-to-One relationship with User
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    // One-to-Many relationship with Payslip
    @OneToMany(mappedBy = "employee", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Payslip> payslips;

    // One-to-Many relationship with Deduction
    @OneToMany(mappedBy = "employee", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Deduction> deductions;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Override
    public String toString() {
        return "Employee{" +
                "id=" + this.getId() +
                ", employeeIdNumber='" + employeeIdNumber + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}
