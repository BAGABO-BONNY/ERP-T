package com.jva.ERP.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO for a DeductionType record.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeductionTypeResponse {
    private Long id;
    private String typeName;
    private String displayName;
    private BigDecimal percentage;
    private Boolean isAllowance;
    private String description;
    private Boolean isActive;
}
