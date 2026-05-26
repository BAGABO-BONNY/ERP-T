package com.jva.ERP.repository;

import com.jva.ERP.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Employee Repository
 * Provides CRUD operations and custom query methods for Employee entity
 */
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    /**
     * Find employee by email
     */
    Optional<Employee> findByEmail(String email);

    /**
     * Find employee by employee ID number
     */
    Optional<Employee> findByEmployeeIdNumber(String employeeIdNumber);

    /**
     * Find employee by username (via associated User)
     */
    @Query("SELECT e FROM Employee e JOIN e.user u WHERE u.username = :username")
    Optional<Employee> findByUsername(@Param("username") String username);

    /**
     * Find all active employees
     */
    List<Employee> findByIsActiveTrue();

    /**
     * Find employees by department (via Employment relationship)
     */
    @Query("SELECT e FROM Employee e WHERE e.employment.department = :department AND e.isActive = true")
    List<Employee> findByDepartment(@Param("department") String department);

    /**
     * Find employees by job title
     */
    @Query("SELECT e FROM Employee e WHERE e.employment.jobTitle = :jobTitle AND e.isActive = true")
    List<Employee> findByJobTitle(@Param("jobTitle") String jobTitle);

    /**
     * Find employees by first and last name
     */
    @Query("SELECT e FROM Employee e WHERE LOWER(e.firstName) LIKE LOWER(CONCAT('%', :firstName, '%')) " +
            "AND LOWER(e.lastName) LIKE LOWER(CONCAT('%', :lastName, '%')) AND e.isActive = true")
    List<Employee> findByFirstNameAndLastName(@Param("firstName") String firstName, @Param("lastName") String lastName);

    /**
     * Find employees by city
     */
    List<Employee> findByCity(String city);

    /**
     * Find employees with no user account
     */
    @Query("SELECT e FROM Employee e WHERE e.user IS NULL AND e.isActive = true")
    List<Employee> findEmployeesWithoutUser();

    /**
     * Check if an employee email already exists (excluding a given employee ID for updates)
     */
    @Query("SELECT COUNT(e) > 0 FROM Employee e WHERE e.email = :email AND e.id <> :excludeId")
    boolean existsByEmailAndIdNot(@Param("email") String email, @Param("excludeId") Long excludeId);

    /**
     * Check if employee ID number exists (excluding a given employee ID for updates)
     */
    @Query("SELECT COUNT(e) > 0 FROM Employee e WHERE e.employeeIdNumber = :idNumber AND e.id <> :excludeId")
    boolean existsByEmployeeIdNumberAndIdNot(@Param("idNumber") String idNumber, @Param("excludeId") Long excludeId);

    /**
     * Check if email exists at all
     */
    boolean existsByEmail(String email);

    /**
     * Check if employee ID number exists at all
     */
    boolean existsByEmployeeIdNumber(String employeeIdNumber);

    /**
     * Find employees by department and employment status
     */
    @Query("SELECT e FROM Employee e WHERE e.employment.department = :department AND e.employment.employmentStatus = :status AND e.isActive = true")
    List<Employee> findByDepartmentAndStatus(@Param("department") String department, @Param("status") String status);

    /**
     * Count active employees.
     */
    long countByIsActiveTrue();

    /**
     * Search employees by name fragment (first or last name)
     */
    @Query("SELECT e FROM Employee e WHERE e.isActive = true AND (" +
           "LOWER(e.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(e.lastName)  LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(e.email)     LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Employee> searchByNameOrEmail(@Param("query") String query);
}

