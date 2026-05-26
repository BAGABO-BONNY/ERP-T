package com.jva.ERP.repository;

import com.jva.ERP.entity.User;
import com.jva.ERP.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * User Repository
 * Provides CRUD operations and custom query methods for User entity
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by username
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by email
     */
    Optional<User> findByEmail(String email);

    /**
     * Find user by username or email
     */
    @Query("SELECT u FROM User u WHERE u.username = :identifier OR u.email = :identifier")
    Optional<User> findByUsernameOrEmail(@Param("identifier") String identifier);

    /**
     * Find users by role
     */
    List<User> findByRole(UserRole role);

    /**
     * Find all active users
     */
    List<User> findByIsActiveTrue();

    /**
     * Find all non-locked users
     */
    List<User> findByIsLockedFalse();

    /**
     * Find users by role and active status
     */
    List<User> findByRoleAndIsActiveTrue(UserRole role);

    /**
     * Find users associated with employees
     */
    @Query("SELECT u FROM User u WHERE u.employee IS NOT NULL")
    List<User> findUsersWithEmployee();

    /**
     * Check if username exists
     */
    boolean existsByUsername(String username);

    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Count active users
     */
    long countByIsActiveTrue();

    /**
     * Find users by first name and last name
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.firstName) LIKE LOWER(CONCAT('%', :firstName, '%')) " +
            "AND LOWER(u.lastName) LIKE LOWER(CONCAT('%', :lastName, '%')) AND u.isActive = true")
    List<User> findByFirstNameAndLastName(@Param("firstName") String firstName, @Param("lastName") String lastName);
}

