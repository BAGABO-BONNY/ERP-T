package com.jva.ERP.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for creating or updating a DeductionType.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeductionTypeRequest {

    @NotBlank(message = "Type name is required")
    @Size(max = 100, message = "Type name must not exceed 100 characters")
    private String typeName;

    @NotBlank(message = "Display name is required")
    @Size(max = 100, message = "Display name must not exceed 100 characters")
    private String displayName;

    @NotNull(message = "Percentage is required")
    @DecimalMin(value = "0.01", message = "Percentage must be greater than 0")
    @DecimalMax(value = "100.00", message = "Percentage must not exceed 100")
    @Digits(integer = 3, fraction = 2, message = "Percentage must have at most 3 integer digits and 2 decimal places")
    private BigDecimal percentage;

    /** true = allowance (added to gross), false = deduction (subtracted from gross) */
    @NotNull(message = "isAllowance flag is required")
    private Boolean isAllowance;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;
}
