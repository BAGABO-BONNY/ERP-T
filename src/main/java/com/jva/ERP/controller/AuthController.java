package com.jva.ERP.controller;

import com.jva.ERP.dto.*;
import com.jva.ERP.entity.User;
import com.jva.ERP.exception.BusinessException;
import com.jva.ERP.repository.UserRepository;
import com.jva.ERP.security.JwtUtil;
import com.jva.ERP.service.impl.UserServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Authentication Controller
 *
 * Public endpoints (no token required):
 *   POST /api/auth/register  — employee self-registration (EMPLOYEE role only)
 *   POST /api/auth/login     — obtain JWT token
 *
 * Protected endpoints (token required):
 *   GET  /api/auth/me              — current user info
 *   GET  /api/auth/validate-token  — token validity check
 *
 * ADMIN and MANAGER accounts are seeded at startup and cannot be created
 * through this controller.
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Authentication", description = "Public login and registration endpoints. " +
        "ADMIN and MANAGER accounts are seeded at startup. " +
        "POST /register creates EMPLOYEE accounts only.")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private UserServiceImpl userService;

    // ── Public endpoints ──────────────────────────────────────────────────────

    /**
     * Employee self-registration.
     * Always creates an EMPLOYEE account — role cannot be specified by the caller.
     * ADMIN and MANAGER accounts are seeded at startup only.
     */
    @Operation(summary = "Register a new employee account (public)",
               description = "Creates an EMPLOYEE user account. Role is always EMPLOYEE — cannot be overridden.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Employee registered successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or duplicate username/email")
    })
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<?>> register(
            @Valid @RequestBody RegistrationRequest request) {
        try {
            logger.info("Employee registration request: {}", request.getUsername());
            RegistrationResponse response = userService.registerUser(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(HttpStatus.CREATED.value(),
                            "Employee registered successfully", response));
        } catch (BusinessException e) {
            logger.warn("Registration failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Registration error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "Registration failed", null));
        }
    }

    /**
     * Login — accepts username or email, returns a JWT token.
     */
    @Operation(summary = "Login and obtain JWT token (public)",
               description = "Accepts username or email + password. Returns a Bearer JWT token valid for 24 hours.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful — token returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials, inactive or locked account")
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<?>> login(
            @Valid @RequestBody LoginRequest loginRequest) {
        try {
            logger.info("Login attempt: {}", loginRequest.getUsername());

            // Resolve user by username or email
            Optional<User> userOptional =
                    userRepository.findByUsernameOrEmail(loginRequest.getUsername());

            if (userOptional.isEmpty()) {
                return unauthorized("Invalid username or password");
            }

            User user = userOptional.get();

            if (!user.getIsActive()) {
                return unauthorized("Account is inactive");
            }
            if (user.getIsLocked()) {
                return unauthorized("Account is locked");
            }

            // Delegate credential check to Spring Security
            try {
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                user.getUsername(), loginRequest.getPassword()));
            } catch (BadCredentialsException e) {
                return unauthorized("Invalid username or password");
            } catch (DisabledException e) {
                return unauthorized("Account is disabled");
            } catch (LockedException e) {
                return unauthorized("Account is locked");
            }

            // Generate token
            UserDetails userDetails =
                    userDetailsService.loadUserByUsername(user.getUsername());
            String token = jwtUtil.generateToken(userDetails);

            LoginResponse loginResponse = new LoginResponse();
            loginResponse.setToken(token);
            loginResponse.setTokenType("Bearer");
            loginResponse.setUsername(user.getUsername());
            loginResponse.setEmail(user.getEmail());
            loginResponse.setFirstName(user.getFirstName());
            loginResponse.setLastName(user.getLastName());
            loginResponse.setRole(user.getRole().name());

            logger.info("Login successful: {} ({})", user.getUsername(), user.getRole());
            return ResponseEntity.ok(
                    new ApiResponse<>(HttpStatus.OK.value(), "Login successful", loginResponse));

        } catch (Exception e) {
            logger.error("Login error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "Login failed", null));
        }
    }

    // ── Protected endpoints ───────────────────────────────────────────────────

    /**
     * Returns info about the currently authenticated user.
     */
    @Operation(summary = "Get current user profile", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User info returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<?>> getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth == null || !auth.isAuthenticated()
                    || "anonymousUser".equals(auth.getPrincipal())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>(HttpStatus.UNAUTHORIZED.value(),
                                "Not authenticated", null));
            }

            String username = auth.getName();
            Optional<User> userOptional = userRepository.findByUsername(username);

            if (userOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(HttpStatus.NOT_FOUND.value(),
                                "User not found", null));
            }

            User user = userOptional.get();
            Map<String, Object> data = new HashMap<>();
            data.put("id", user.getId());
            data.put("username", user.getUsername());
            data.put("email", user.getEmail());
            data.put("firstName", user.getFirstName());
            data.put("lastName", user.getLastName());
            data.put("role", user.getRole().name());
            data.put("isActive", user.getIsActive());

            return ResponseEntity.ok(
                    new ApiResponse<>(HttpStatus.OK.value(), "User info retrieved", data));

        } catch (Exception e) {
            logger.error("Get current user error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "Failed to retrieve user info", null));
        }
    }

    /**
     * Validates a Bearer token passed in the Authorization header.
     */
    @Operation(summary = "Validate a JWT token", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Token is valid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Token is invalid or expired")
    })
    @GetMapping("/validate-token")
    public ResponseEntity<ApiResponse<?>> validateToken(
            @RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiResponse<>(HttpStatus.BAD_REQUEST.value(),
                                "Authorization header must start with 'Bearer '", null));
            }

            String token = authHeader.substring(7);
            String username = jwtUtil.extractUsername(token);

            if (jwtUtil.validateToken(token, username)) {
                Map<String, Object> data = new HashMap<>();
                data.put("username", username);
                data.put("valid", true);
                return ResponseEntity.ok(
                        new ApiResponse<>(HttpStatus.OK.value(), "Token is valid", data));
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(HttpStatus.UNAUTHORIZED.value(),
                            "Token is invalid or expired", null));

        } catch (Exception e) {
            logger.error("Token validation error", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(HttpStatus.UNAUTHORIZED.value(),
                            "Token validation failed", null));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ResponseEntity<ApiResponse<?>> unauthorized(String message) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse<>(HttpStatus.UNAUTHORIZED.value(), message, null));
    }
}
