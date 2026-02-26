package com.booking.auth.model;

/**
 * Application roles for role-based access control.
 * Stored in DB with ROLE_ prefix (e.g. ROLE_USER, ROLE_ADMIN).
 */
public enum Role {

    USER("ROLE_USER"),
    ADMIN("ROLE_ADMIN");

    private final String authority;

    Role(String authority) {
        this.authority = authority;
    }

    public String getAuthority() {
        return authority;
    }
}
