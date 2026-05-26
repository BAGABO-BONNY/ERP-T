package com.jva.ERP.repository;

import com.jva.ERP.entity.Payslip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Payslip Repository
 * Provides CRUD operations and custom query methods for Payslip entity
 */
@Repository
public interface PayslipRepository extends JpaRepository<Payslip, Long> {

    /**
     * Find payslip by payslip number
     */
    Optional<Payslip> findByPayslipNumber(String payslipNumber);

    /**
     * Find payslips by employee ID
     */
    List<Payslip> findByEmployeeId(Long employeeId);

    /**
     * Find payslips by employee ID and payroll month (YYYY-MM format)
     */
    @Query("SELECT p FROM Payslip p WHERE p.employee.id = :employeeId AND p.payrollMonth = :payrollMonth")
    Optional<Payslip> findByEmployeeAndMonth(@Param("employeeId") Long employeeId, @Param("payrollMonth") String payrollMonth);

    /**
     * Find payslips by employee and year-month (flexible format)
     */
    @Query("SELECT p FROM Payslip p WHERE p.employee.id = :employeeId AND YEAR(p.payrollPeriodStart) = :year AND MONTH(p.payrollPeriodStart) = :month")
    Optional<Payslip> findByEmployeeYearMonth(@Param("employeeId") Long employeeId, @Param("year") int year, @Param("month") int month);

    /**
     * Find payslips by employee and date range
     */
    @Query("SELECT p FROM Payslip p WHERE p.employee.id = :employeeId AND p.payrollPeriodStart BETWEEN :startDate AND :endDate ORDER BY p.payrollPeriodStart DESC")
    List<Payslip> findByEmployeeAndDateRange(@Param("employeeId") Long employeeId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Check if a payslip already exists for an employee in a given month (YYYY-MM)
     */
    boolean existsByEmployeeIdAndPayrollMonth(Long employeeId, String payrollMonth);

    /**
     * Find all payslips for a specific payroll month
     */
    List<Payslip> findByPayrollMonth(String payrollMonth);

    /**
     * Find payslips by payment status
     */
    List<Payslip> findByPaymentStatus(String paymentStatus);

    /**
     * Find unpaid payslips for an employee
     */
    @Query("SELECT p FROM Payslip p WHERE p.employee.id = :employeeId AND p.paymentStatus != 'Paid' ORDER BY p.payrollPeriodStart DESC")
    List<Payslip> findUnpaidPayslips(@Param("employeeId") Long employeeId);

    /**
     * Find payslips between two dates
     */
    @Query("SELECT p FROM Payslip p WHERE p.payrollPeriodStart BETWEEN :startDate AND :endDate ORDER BY p.payrollPeriodStart DESC")
    List<Payslip> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Find finalized payslips
     */
    @Query("SELECT p FROM Payslip p WHERE p.isFinalized = true ORDER BY p.payrollPeriodStart DESC")
    List<Payslip> findFinalizedPayslips();

    /**
     * Find finalized payslips for an employee
     */
    @Query("SELECT p FROM Payslip p WHERE p.employee.id = :employeeId AND p.isFinalized = true ORDER BY p.payrollPeriodStart DESC")
    List<Payslip> findFinalizedPayslipsForEmployee(@Param("employeeId") Long employeeId);

    /**
     * Find payslips by payment method
     */
    List<Payslip> findByPaymentMethod(String paymentMethod);

    /**
     * Count payslips by payment status
     */
    @Query("SELECT COUNT(p) FROM Payslip p WHERE p.paymentStatus = :status AND p.payrollMonth = :payrollMonth")
    long countByPaymentStatusAndMonth(@Param("status") String status, @Param("payrollMonth") String payrollMonth);

    /**
     * Find latest payslip for an employee
     */
    @Query("SELECT p FROM Payslip p WHERE p.employee.id = :employeeId ORDER BY p.payrollPeriodEnd DESC LIMIT 1")
    Optional<Payslip> findLatestPayslipForEmployee(@Param("employeeId") Long employeeId);

    /**
     * Find all payslips for an employee with specific month and year
     */
    @Query("SELECT p FROM Payslip p WHERE p.employee.id = :employeeId AND p.payrollMonth LIKE :monthYear% ORDER BY p.payrollPeriodStart DESC")
    List<Payslip> findByEmployeeAndMonthYear(@Param("employeeId") Long employeeId, @Param("monthYear") String monthYear);
}

