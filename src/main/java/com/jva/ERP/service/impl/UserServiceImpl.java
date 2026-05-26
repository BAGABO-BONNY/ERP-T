package com.jva.ERP.service.impl;

import com.jva.ERP.dto.RegistrationRequest;
import com.jva.ERP.dto.RegistrationResponse;
import com.jva.ERP.entity.User;
import com.jva.ERP.enums.UserRole;
import com.jva.ERP.exception.BusinessException;
import com.jva.ERP.exception.ResourceNotFoundException;
import com.jva.ERP.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * User Service Implementation.
 *
 * The public registration path always creates EMPLOYEE accounts.
 * ADMIN and MANAGER accounts are created exclusively by DataSeeder at startup.
 */
@Service
@Transactional
public class UserServiceImpl {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Register a new EMPLOYEE user via the public endpoint.
     * Role is always EMPLOYEE — no caller can override this.
     */
    public RegistrationResponse registerUser(RegistrationRequest request) {
        logger.info("Employee self-registration request: {}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhoneNumber(request.getPhoneNumber());

        // Role is always EMPLOYEE for public registration — non-negotiable
        user.setRole(UserRole.EMPLOYEE);

        user.setIsActive(true);
        user.setIsLocked(false);

        User saved = userRepository.save(user);
        logger.info("Employee registered successfully: {}", saved.getUsername());

        return mapToRegistrationResponse(saved);
    }

    /**
     * Get user by username.
     */
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    /**
     * Get user by email.
     */
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    /**
     * Verify a raw password against its BCrypt hash.
     */
    public boolean verifyPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private RegistrationResponse mapToRegistrationResponse(User user) {
        RegistrationResponse response = new RegistrationResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setRole(user.getRole().name());
        response.setIsActive(user.getIsActive());
        response.setMessage("Employee registered successfully");
        return response;
    }
}
