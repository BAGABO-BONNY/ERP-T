package com.jva.ERP.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Role Entity - Represents a role with permissions in the system
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "roles", uniqueConstraints = {
    @UniqueConstraint(columnNames = "role_name")
})
public class Role extends BaseEntity {

    @Column(name = "role_name", nullable = false, length = 50, unique = true)
    private String roleName;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "permissions", columnDefinition = "TEXT")
    private String permissions; // JSON format or comma-separated values

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Override
    public String toString() {
        return "Role{" +
                "id=" + this.getId() +
                ", roleName='" + roleName + '\'' +
                ", description='" + description + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}

