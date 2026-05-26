package com.jva.ERP.service;

import com.jva.ERP.entity.User;
import com.jva.ERP.repository.UserRepository;
import com.jva.ERP.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

/**
 * Custom UserDetailsService implementation for Spring Security
 * Loads user details from the database for authentication
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.info("Loading user by username: {}", username);

        Optional<User> userOptional = userRepository.findByUsername(username);

        if (userOptional.isEmpty()) {
            logger.error("User not found: {}", username);
            throw new UsernameNotFoundException("User not found with username: " + username);
        }

        User user = userOptional.get();

        if (!user.getIsActive()) {
            logger.warn("User account is inactive: {}", username);
            throw new UsernameNotFoundException("User account is inactive: " + username);
        }

        if (user.getIsLocked()) {
            logger.warn("User account is locked: {}", username);
            throw new UsernameNotFoundException("User account is locked: " + username);
        }

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                true, // enabled
                true, // accountNonExpired
                true, // credentialsNonExpired
                true, // accountNonLocked
                getAuthorities(user)
        );
    }

    /**
     * Get authorities for the user based on their role
     */
    private Collection<? extends GrantedAuthority> getAuthorities(User user) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        String role = user.getRole().name();

        // Add the primary role with ROLE_ prefix
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role));

        // Add hierarchical permissions based on role
        switch (user.getRole()) {
            case ADMIN:
                // ADMIN has all permissions
                authorities.add(new SimpleGrantedAuthority("PERMISSION_READ"));
                authorities.add(new SimpleGrantedAuthority("PERMISSION_WRITE"));
                authorities.add(new SimpleGrantedAuthority("PERMISSION_DELETE"));
                authorities.add(new SimpleGrantedAuthority("PERMISSION_ADMIN"));
                break;

            case MANAGER:
                // MANAGER has read, write permissions
                authorities.add(new SimpleGrantedAuthority("PERMISSION_READ"));
                authorities.add(new SimpleGrantedAuthority("PERMISSION_WRITE"));
                break;

            case EMPLOYEE:
                // EMPLOYEE has read permission only
                authorities.add(new SimpleGrantedAuthority("PERMISSION_READ"));
                break;

            case USER:
                // USER has limited read permission
                authorities.add(new SimpleGrantedAuthority("PERMISSION_READ"));
                break;

            default:
                authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        logger.debug("User: {} has authorities: {}", user.getUsername(), authorities);
        return authorities;
    }
}

