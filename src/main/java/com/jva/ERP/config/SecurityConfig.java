package com.jva.ERP.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jva.ERP.dto.ApiResponse;
import com.jva.ERP.service.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
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
 * SecurityConfig — stateless JWT security.
 *
 * Key design decisions:
 * - No redirect to /error on 401/403 — JSON is returned directly via custom entry points.
 *   This eliminates the /error → 403 loop entirely.
 * - All Swagger/OpenAPI paths are permitted without a token.
 * - JwtAuthenticationFilter.shouldNotFilter() also skips public paths at the filter level.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService customUserDetailsService;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          CustomUserDetailsService customUserDetailsService,
                          ObjectMapper objectMapper) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.customUserDetailsService = customUserDetailsService;
        this.objectMapper = objectMapper;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

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

            // ── Return JSON on 401/403 — never redirect to /error ─────────────
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write(objectMapper.writeValueAsString(
                        new ApiResponse<>(401, "Unauthorized: " + authException.getMessage(), null)
                    ));
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write(objectMapper.writeValueAsString(
                        new ApiResponse<>(403, "Access denied: insufficient role", null)
                    ));
                })
            )

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
