package com.jva.ERP.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User Registration Response DTO
 * Contains user information after successful registration
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationResponse {

    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String role;
    private Boolean isActive;
    private String message;
}

