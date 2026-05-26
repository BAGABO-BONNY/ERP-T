package com.jva.ERP.enums;

/**
 * User roles enumeration
 */
public enum UserRole {
    ADMIN("Admin"),
    USER("User"),
    MANAGER("Manager"),
    EMPLOYEE("Employee");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

