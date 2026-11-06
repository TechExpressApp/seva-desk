package com.sevadesk.platform;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface PasswordTokenRepository extends JpaRepository<PasswordToken, UUID> {

    Optional<PasswordToken> findByTokenHash(String tokenHash);

    long countByUserIdAndCreatedAtAfter(UUID userId, Instant since);

    /** Older links stop working when a new one is issued or the password changes. */
    @Modifying
    @Query("update PasswordToken t set t.usedAt = :now where t.userId = :userId and t.usedAt is null")
    void expireAllFor(@Param("userId") UUID userId, @Param("now") Instant now);
}
