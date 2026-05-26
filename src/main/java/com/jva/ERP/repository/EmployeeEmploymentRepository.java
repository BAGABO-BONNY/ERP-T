package com.jva.ERP.repository;

import com.jva.ERP.entity.Employment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Employment Repository
 * Provides CRUD operations and custom query methods for Employment entity
 */
@Repository
public interface EmployeeEmploymentRepository extends JpaRepository<Employment, Long> {

    /**
     * Find employment by employee ID
     */
    Optional<Employment> findByEmployeeId(Long employeeId);

    /**
     * Find employment records by department
     */
    List<Employment> findByDepartment(String department);

    /**
     * Find employment records by job title
     */
    List<Employment> findByJobTitle(String jobTitle);

    /**
     * Find employment records by employment type
     */
    List<Employment> findByEmploymentType(String employmentType);

    /**
     * Find employment records by employment status
     */
    List<Employment> findByEmploymentStatus(String employmentStatus);

    /**
     * Find all active employment records
     */
    List<Employment> findByIsActiveTrue();

    /**
     * Find employment records with specific position level
     */
    List<Employment> findByPositionLevel(String positionLevel);

    /**
     * Find employment records hired between two dates
     */
    @Query("SELECT e FROM Employment e WHERE e.hireDate BETWEEN :startDate AND :endDate ORDER BY e.hireDate DESC")
    List<Employment> findEmployeeHiredBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Find terminated employment records
     */
    @Query("SELECT e FROM Employment e WHERE e.terminationDate IS NOT NULL ORDER BY e.terminationDate DESC")
    List<Employment> findTerminatedEmployees();

    /**
     * Find employment records by manager ID
     */
    List<Employment> findByManagerId(Long managerId);

    /**
     * Find employment records with salary above specified amount
     */
    @Query("SELECT e FROM Employment e WHERE e.baseSalary > :salary ORDER BY e.baseSalary DESC")
    List<Employment> findByBaseSalaryGreaterThan(@Param("salary") java.math.BigDecimal salary);

    /**
     * Count employees by department
     */
    @Query("SELECT COUNT(e) FROM Employment e WHERE e.department = :department AND e.isActive = true")
    long countByDepartment(@Param("department") String department);

    /**
     * Count employees by employment status
     */
    @Query("SELECT COUNT(e) FROM Employment e WHERE e.employmentStatus = :status AND e.isActive = true")
    long countByEmploymentStatus(@Param("status") String status);
}

