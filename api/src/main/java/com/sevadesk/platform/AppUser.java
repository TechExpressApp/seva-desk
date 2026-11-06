package com.sevadesk.platform;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/** A staff login for one temple. Access is controlled by {@link Role}s. */
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
    @Column(name = "must_change_password")
    private boolean mustChangePassword;
    /** Sign-in tokens carry this number; changing it signs the user out everywhere. */
    @Column(name = "token_version")
    private int tokenVersion;
    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    protected AppUser() {}

    public AppUser(String email, String passwordHash, String displayName, List<String> roles) {
        this.email = email.strip().toLowerCase();
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.roles = String.join(",", roles);
    }

    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getDisplayName() { return displayName; }
    public List<String> getRoles() {
        return roles == null || roles.isBlank() ? List.of() : Arrays.stream(roles.split(",")).map(String::trim).toList();
    }
    public boolean isActive() { return active; }
    public boolean isMustChangePassword() { return mustChangePassword; }
    public int getTokenVersion() { return tokenVersion; }
    public Instant getLastLoginAt() { return lastLoginAt; }

    public void setDisplayName(String displayName) { this.displayName = displayName; }

    /** New roles take effect immediately: existing sign-ins are ended so the next one carries the new access. */
    public void setRoles(Collection<String> newRoles) {
        String joined = String.join(",", newRoles);
        if (!joined.equals(roles)) {
            roles = joined;
            tokenVersion++;
        }
    }

    public void setActive(boolean active) {
        if (this.active != active) {
            this.active = active;
            tokenVersion++;
        }
    }

    /** Sets a new password and signs the user out of every other session. */
    public void changePassword(String newHash, boolean mustChangeAtNextLogin) {
        passwordHash = newHash;
        mustChangePassword = mustChangeAtNextLogin;
        tokenVersion++;
    }

    public void recordLogin() {
        lastLoginAt = Instant.now();
    }
}
