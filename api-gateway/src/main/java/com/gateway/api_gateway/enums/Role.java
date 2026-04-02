package com.gateway.api_gateway.enums;

/**
 * User Roles in the System
 * This enum ensures consistency across the application
 */
public enum Role {
    USERS("VIEWER"),           // Regular students
    FACULTY("ANALYST"),     // Faculty members
    SUPER_ADMIN("ADMIN"); // System administrators

    private final String value;

    Role(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    /**
     * Check if a string matches this role (case-insensitive)
     */
    public boolean matches(String role) {
        return this.value.equalsIgnoreCase(role);
    }

    /**
     * Get Role from string value
     */
    public static Role fromString(String role) {
        for (Role r : Role.values()) {
            if (r.value.equalsIgnoreCase(role)) {
                return r;
            }
        }
        throw new IllegalArgumentException("Invalid role: " + role);
    }

    /**
     * Check if role string is valid
     */
    public static boolean isValid(String role) {
        for (Role r : Role.values()) {
            if (r.value.equalsIgnoreCase(role)) {
                return true;
            }
        }
        return false;
    }
}