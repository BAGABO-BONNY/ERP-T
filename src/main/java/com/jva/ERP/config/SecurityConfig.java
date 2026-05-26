package com.jva.ERP.config;

import com.jva.ERP.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * SecurityConfig — stateless JWT security configuration.
 *
 * Role-based URL access:
 *   /api/auth/**          → public
 *   /health, /swagger-ui/**, /v3/api-docs/** → public
 *   /api/employees/**     → MANAGER, ADMIN
 *   /api/admin/**         → ADMIN only
 *   /api/manager/**       → MANAGER, ADMIN
 *   /api/employee/**      → EMPLOYEE, MANAGER, ADMIN
 *   everything else       → authenticated
 *
 * Fine-grained method-level control is applied via @PreAuthorize on each endpoint.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService customUserDetailsService;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          CustomUserDetailsService customUserDetailsService) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    /**
     * Explicit DaoAuthenticationProvider wired with our UserDetailsService and encoder.
     * Declaring this bean explicitly suppresses the Spring Boot auto-config warning.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                // ── Public — no token required ────────────────────────────────
                .requestMatchers(
                    "/api/auth/**",
                    "/api/public/**",
                    "/health",
                    "/error",
                    "/swagger-ui/**",
                    "/swagger-ui/index.html",
                    "/v3/api-docs",
                    "/v3/api-docs/**",
                    "/v3/api-docs.yaml",
                    "/webjars/**"
                ).permitAll()

                // ── Role-based URL guards ──────────────────────────────────────
                .requestMatchers("/api/employees/**").hasAnyRole("MANAGER", "ADMIN")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/manager/**").hasAnyRole("MANAGER", "ADMIN")
                .requestMatchers("/api/employee/**")
                    .hasAnyRole("EMPLOYEE", "MANAGER", "ADMIN")
                .requestMatchers("/api/deduction-types/**")
                    .hasAnyRole("MANAGER", "ADMIN")

                // ── Everything else requires a valid JWT ──────────────────────
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
