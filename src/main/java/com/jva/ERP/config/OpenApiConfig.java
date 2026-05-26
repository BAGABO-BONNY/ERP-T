package com.jva.ERP.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenApiConfig — configures Swagger / OpenAPI 3 documentation.
 *
 * Swagger UI:  http://localhost:8080/swagger-ui.html
 * API docs:    http://localhost:8080/v3/api-docs
 *
 * All protected endpoints require a Bearer JWT token.
 * Click "Authorize" in Swagger UI and enter: Bearer <your-token>
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI erpOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local development server")
                ))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, jwtSecurityScheme()));
    }

    private Info apiInfo() {
        return new Info()
                .title("ERP System REST API")
                .version("1.0.0")
                .description("""
                        Enterprise Resource Planning (ERP) backend API.

                        **Authentication:**
                        All protected endpoints require a JWT Bearer token.
                        1. Call `POST /api/auth/login` with your credentials.
                        2. Copy the `token` from the response.
                        3. Click **Authorize** (top right) and enter: `Bearer <token>`

                        **Roles & Access:**
                        | Role     | Capabilities |
                        |----------|-------------|
                        | ADMIN    | Approve payroll, manage deduction types, manage employees |
                        | MANAGER  | Process payroll, manage employees, view all payslips |
                        | EMPLOYEE | View own payslips only |

                        **Seeded accounts (from application.properties):**
                        - Admin: `admin` / `Admin@2026!`
                        - Manager: `manager` / `Manager@2026!`
                        """)
                .contact(new Contact()
                        .name("ERP Support")
                        .email("bmbmanzi@gmail.com"))
                .license(new License()
                        .name("Private")
                        .url("https://example.com"));
    }

    private SecurityScheme jwtSecurityScheme() {
        return new SecurityScheme()
                .name(SECURITY_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Enter your JWT token. Obtain it from POST /api/auth/login.");
    }
}
