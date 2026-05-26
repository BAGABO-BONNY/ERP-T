package com.jva.ERP.controller;

import com.jva.ERP.dto.ApiResponse;
import com.jva.ERP.dto.EmployeeRequest;
import com.jva.ERP.dto.EmployeeResponse;
import com.jva.ERP.exception.BusinessException;
import com.jva.ERP.exception.ResourceNotFoundException;
import com.jva.ERP.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * EmployeeController — REST API for employee lifecycle management.
 *
 * Base path : /api/employees
 *
 * Access rules (enforced via @PreAuthorize — SecurityConfig allows any
 * authenticated user to reach /api/employees/**, fine-grained control is here):
 *
 *   POST   /                          → MANAGER, ADMIN   (create)
 *   GET    /                          → MANAGER, ADMIN   (list all)
 *   GET    /{id}                      → MANAGER, ADMIN   (get by DB id)
 *   GET    /by-id-number/{idNumber}   → MANAGER, ADMIN   (get by EMP-xxx)
 *   GET    /search?q=                 → MANAGER, ADMIN   (name/email search)
 *   GET    /department/{dept}         → MANAGER, ADMIN   (filter by dept)
 *   GET    /count                     → MANAGER, ADMIN   (headcount)
 *   PUT    /{id}                      → ADMIN only       (update)
 *   DELETE /{id}                      → ADMIN only       (soft deactivate)
 *   PATCH  /{id}/reactivate           → ADMIN only       (reactivate)
 */
@RestController
@RequestMapping("/api/employees")
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Employee Management",
     description = "CRUD operations for employee records. " +
                   "MANAGER can create/read. ADMIN can also update/delete/reactivate.")
public class EmployeeController {

    private static final Logger logger = LoggerFactory.getLogger(EmployeeController.class);

    @Autowired
    private EmployeeService employeeService;

    // ── Create ────────────────────────────────────────────────────────────────

    /**
     * POST /api/employees
     * Register a new employee. Optionally creates a linked EMPLOYEE user account
     * when username + password are included in the request body.
     * Accessible by MANAGER and ADMIN.
     */
    @Operation(summary = "Create a new employee (MANAGER/ADMIN)",
               description = "Registers a new employee with employment details. Optionally creates a linked EMPLOYEE user account.",
               security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<?>> registerEmployee(
            @Valid @RequestBody EmployeeRequest request) {
        try {
            logger.info("Register employee request: {}", request.getEmployeeIdNumber());
            EmployeeResponse response = employeeService.registerEmployee(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(HttpStatus.CREATED.value(),
                            "Employee registered successfully", response));
        } catch (BusinessException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            logger.error("Employee registration error", e);
            return serverError("Registration failed: " + e.getMessage());
        }
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    /**
     * GET /api/employees
     * Returns all active employees.
     */
    @Operation(summary = "List all active employees (MANAGER/ADMIN)", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<?>> getAllEmployees() {
        try {
            List<EmployeeResponse> employees = employeeService.getAllEmployees();
            return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(),
                    "Employees retrieved successfully", employees));
        } catch (Exception e) {
            logger.error("Error fetching employees", e);
            return serverError("Failed to retrieve employees");
        }
    }

    /**
     * GET /api/employees/{id}
     * Returns a single active employee by database ID.
     */
    @Operation(summary = "Get employee by database ID (MANAGER/ADMIN)", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<?>> getEmployeeById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(),
                    "Employee retrieved successfully",
                    employeeService.getEmployeeById(id)));
        } catch (ResourceNotFoundException e) {
            return notFound(e.getMessage());
        } catch (Exception e) {
            logger.error("Error fetching employee id={}", id, e);
            return serverError("Failed to retrieve employee");
        }
    }

    /**
     * GET /api/employees/by-id-number/{employeeIdNumber}
     * Returns a single employee by their human-readable ID (e.g. EMP-001).
     */
    @Operation(summary = "Get employee by ID number e.g. EMP-001 (MANAGER/ADMIN)", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/by-id-number/{employeeIdNumber}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<?>> getEmployeeByIdNumber(
            @PathVariable String employeeIdNumber) {
        try {
            return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(),
                    "Employee retrieved successfully",
                    employeeService.getEmployeeByIdNumber(employeeIdNumber)));
        } catch (ResourceNotFoundException e) {
            return notFound(e.getMessage());
        } catch (Exception e) {
            logger.error("Error fetching employee idNumber={}", employeeIdNumber, e);
            return serverError("Failed to retrieve employee");
        }
    }

    /**
     * GET /api/employees/search?q={query}
     * Full-text search across first name, last name, and email.
     * Returns all active employees when query is blank.
     */
    @Operation(summary = "Search employees by name or email (MANAGER/ADMIN)", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<?>> searchEmployees(
            @RequestParam(name = "q", required = false, defaultValue = "") String query) {
        try {
            List<EmployeeResponse> results = employeeService.searchEmployees(query);
            return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(),
                    results.size() + " employee(s) found", results));
        } catch (Exception e) {
            logger.error("Employee search error, query={}", query, e);
            return serverError("Search failed");
        }
    }

    /**
     * GET /api/employees/department/{department}
     * Returns all active employees in the given department.
     */
    @Operation(summary = "Get employees by department (MANAGER/ADMIN)", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/department/{department}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<?>> getByDepartment(
            @PathVariable String department) {
        try {
            List<EmployeeResponse> employees =
                    employeeService.getEmployeesByDepartment(department);
            return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(),
                    "Employees in department '" + department + "' retrieved", employees));
        } catch (BusinessException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            logger.error("Error fetching employees by department={}", department, e);
            return serverError("Failed to retrieve employees");
        }
    }

    /**
     * GET /api/employees/count
     * Returns the count of currently active employees.
     */
    @Operation(summary = "Count active employees (MANAGER/ADMIN)", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/count")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<?>> countActiveEmployees() {
        try {
            long count = employeeService.countActiveEmployees();
            return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(),
                    "Active employee count retrieved", count));
        } catch (Exception e) {
            logger.error("Error counting employees", e);
            return serverError("Failed to count employees");
        }
    }

    // ── Update ────────────────────────────────────────────────────────────────

    /**
     * PUT /api/employees/{id}
     * Updates an existing employee's personal and employment details.
     * Restricted to ADMIN only.
     */
    @Operation(summary = "Update employee details (ADMIN only)", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeRequest request) {
        try {
            EmployeeResponse response = employeeService.updateEmployee(id, request);
            return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(),
                    "Employee updated successfully", response));
        } catch (ResourceNotFoundException e) {
            return notFound(e.getMessage());
        } catch (BusinessException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            logger.error("Error updating employee id={}", id, e);
            return serverError("Failed to update employee");
        }
    }

    // ── Deactivate / Reactivate ───────────────────────────────────────────────

    /**
     * DELETE /api/employees/{id}
     * Soft-deactivates an employee (isActive = false). Record is retained.
     * Restricted to ADMIN only.
     */
    @Operation(summary = "Soft-deactivate an employee (ADMIN only)", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> deactivateEmployee(@PathVariable Long id) {
        try {
            employeeService.deactivateEmployee(id);
            return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(),
                    "Employee deactivated successfully", null));
        } catch (ResourceNotFoundException e) {
            return notFound(e.getMessage());
        } catch (BusinessException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            logger.error("Error deactivating employee id={}", id, e);
            return serverError("Failed to deactivate employee");
        }
    }

    /**
     * PATCH /api/employees/{id}/reactivate
     * Reactivates a previously deactivated employee.
     * Restricted to ADMIN only.
     */
    @Operation(summary = "Reactivate a deactivated employee (ADMIN only)", security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping("/{id}/reactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> reactivateEmployee(@PathVariable Long id) {
        try {
            EmployeeResponse response = employeeService.reactivateEmployee(id);
            return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(),
                    "Employee reactivated successfully", response));
        } catch (ResourceNotFoundException e) {
            return notFound(e.getMessage());
        } catch (BusinessException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            logger.error("Error reactivating employee id={}", id, e);
            return serverError("Failed to reactivate employee");
        }
    }

    // ── Response helpers ──────────────────────────────────────────────────────

    private ResponseEntity<ApiResponse<?>> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), message, null));
    }

    private ResponseEntity<ApiResponse<?>> notFound(String message) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(HttpStatus.NOT_FOUND.value(), message, null));
    }

    private ResponseEntity<ApiResponse<?>> serverError(String message) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), message, null));
    }
}
