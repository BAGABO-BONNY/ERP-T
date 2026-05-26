package com.jva.ERP.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO to trigger payroll processing for a given month and year.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayrollRequest {

    @NotNull(message = "Year is required")
    @Min(value = 2000, message = "Year must be 2000 or later")
    @Max(value = 2100, message = "Year must be 2100 or earlier")
    private Integer year;

    @NotNull(message = "Month is required")
    @Min(value = 1, message = "Month must be between 1 and 12")
    @Max(value = 12, message = "Month must be between 1 and 12")
    private Integer month;

    /**
     * Optional: process payroll for a single employee only.
     * When null, all active employees are processed.
     */
    private Long employeeId;
}
