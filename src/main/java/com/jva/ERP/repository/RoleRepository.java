package com.jva.ERP.repository;

import com.jva.ERP.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Role Repository
 * Provides CRUD operations and custom query methods for Role entity
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Find role by role name
     */
    Optional<Role> findByRoleName(String roleName);

    /**
     * Find all active roles
     */
    List<Role> findByIsActiveTrue();

    /**
     * Check if role exists by name
     */
    boolean existsByRoleName(String roleName);

    /**
     * Find roles by partial name match
     */
    @Query("SELECT r FROM Role r WHERE LOWER(r.roleName) LIKE LOWER(CONCAT('%', :name, '%')) ORDER BY r.roleName")
    List<Role> searchByName(@Param("name") String name);

    /**
     * Find roles by description
     */
    @Query("SELECT r FROM Role r WHERE LOWER(r.description) LIKE LOWER(CONCAT('%', :description, '%')) ORDER BY r.roleName")
    List<Role> findByDescriptionContaining(@Param("description") String description);

    /**
     * Count active roles
     */
    long countByIsActiveTrue();
}

