package com.sevadesk.platform;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.Arrays;
import java.util.List;

/** A staff login for one temple. Roles: ADMIN, TREASURER, FRONT_DESK, EVENT_COORDINATOR, PRIEST, GUEST_HOUSE, VOLUNTEER_COORDINATOR. */
@Entity
@Table(name = "app_user")
public class AppUser extends TenantScopedEntity {

    private String email;
    @Column(name = "password_hash")
    private String passwordHash;
    @Column(name = "display_name")
    private String displayName;
    private String roles;
    private boolean active = true;

    protected AppUser() {}

    public AppUser(String email, String passwordHash, String displayName, List<String> roles) {
        this.email = email.toLowerCase();
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.roles = String.join(",", roles);
    }

    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getDisplayName() { return displayName; }
    public List<String> getRoles() { return Arrays.stream(roles.split(",")).map(String::trim).toList(); }
    public boolean isActive() { return active; }
}
