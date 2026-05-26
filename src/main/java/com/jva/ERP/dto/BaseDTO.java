package com.jva.ERP.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Base DTO (Data Transfer Object) class
 * Used for API responses and requests
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseDTO {
    private Long id;
}

