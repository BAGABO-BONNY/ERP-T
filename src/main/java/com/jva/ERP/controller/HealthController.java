package com.jva.ERP.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HealthController — public liveness check endpoint.
 */
@Tag(name = "Health", description = "Public health check — no authentication required.")
@RestController
public class HealthController {

    @Operation(summary = "System health check (public)",
               description = "Returns a plain-text confirmation that the server is running. No token required.")
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("ERP System is running!");
    }
}
