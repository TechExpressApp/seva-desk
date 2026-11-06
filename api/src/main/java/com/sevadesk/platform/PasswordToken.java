package com.sevadesk.platform;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** A one-time "set your password" link. Only the SHA-256 hash of the link's token is stored. */
@Entity
@Table(name = "password_token")
class PasswordToken extends TenantScopedEntity {

    enum Purpose { RESET, INVITE }

    @Column(name = "user_id") private UUID userId;
    @Column(name = "token_hash") private String tokenHash;
    @Enumerated(EnumType.STRING) private Purpose purpose;
    @Column(name = "expires_at") private Instant expiresAt;
    @Column(name = "used_at") private Instant usedAt;

    protected PasswordToken() {}

    PasswordToken(UUID userId, String tokenHash, Purpose purpose, Instant expiresAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.purpose = purpose;
        this.expiresAt = expiresAt;
    }

    boolean usable() {
        return usedAt == null && Instant.now().isBefore(expiresAt);
    }

    void markUsed() {
        usedAt = Instant.now();
    }

    UUID getUserId() { return userId; }
    Purpose getPurpose() { return purpose; }
}
