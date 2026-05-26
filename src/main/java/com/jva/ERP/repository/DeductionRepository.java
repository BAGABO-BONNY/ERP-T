package com.jva.ERP.repository;

import com.jva.ERP.entity.Deduction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Deduction Repository
 * Provides CRUD operations and custom query methods for Deduction entity
 */
@Repository
public interface DeductionRepository extends JpaRepository<Deduction, Long> {

    /**
     * Find deductions by employee ID
     */
    List<Deduction> findByEmployeeId(Long employeeId);

    /**
     * Find deductions by deduction type
     */
    List<Deduction> findByDeductionType(String deductionType);

    /**
     * Find deductions by deduction name
     */
    List<Deduction> findByDeductionName(String deductionName);

    /**
     * Find active deductions for an employee
     */
    @Query("SELECT d FROM Deduction d WHERE d.employee.id = :employeeId AND d.isActive = true ORDER BY d.deductionName")
    List<Deduction> findActiveDeductionsByEmployee(@Param("employeeId") Long employeeId);

    /**
     * Find recurring deductions for an employee
     */
    @Query("SELECT d FROM Deduction d WHERE d.employee.id = :employeeId AND d.isRecurring = true AND d.isActive = true")
    List<Deduction> findRecurringDeductionsByEmployee(@Param("employeeId") Long employeeId);

    /**
     * Find percentage-based deductions
     */
    List<Deduction> findByIsPercentageBasedTrue();

    /**
     * Find amount-based deductions
     */
    List<Deduction> findByIsPercentageBasedFalse();

    /**
     * Find active deductions
     */
    List<Deduction> findByIsActiveTrue();

    /**
     * Find deductions by effective date
     */
    @Query("SELECT d FROM Deduction d WHERE d.effectiveDate <= :date AND (d.endDate IS NULL OR d.endDate >= :date) ORDER BY d.effectiveDate DESC")
    List<Deduction> findEffectiveDeductionsOnDate(@Param("date") LocalDate date);

    /**
     * Find deductions by employee and type
     */
    @Query("SELECT d FROM Deduction d WHERE d.employee.id = :employeeId AND d.deductionType = :type AND d.isActive = true")
    List<Deduction> findByEmployeeAndType(@Param("employeeId") Long employeeId, @Param("type") String type);

    /**
     * Count active deductions for an employee
     */
    @Query("SELECT COUNT(d) FROM Deduction d WHERE d.employee.id = :employeeId AND d.isActive = true")
    long countActiveDeductionsByEmployee(@Param("employeeId") Long employeeId);

    /**
     * Find expired deductions
     */
    @Query("SELECT d FROM Deduction d WHERE d.endDate IS NOT NULL AND d.endDate < :date ORDER BY d.endDate DESC")
    List<Deduction> findExpiredDeductions(@Param("date") LocalDate date);

    /**
     * Find deductions between date range
     */
    @Query("SELECT d FROM Deduction d WHERE d.effectiveDate BETWEEN :startDate AND :endDate ORDER BY d.effectiveDate DESC")
    List<Deduction> findByEffectiveDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}

