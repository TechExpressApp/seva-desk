package com.sevadesk.platform;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    Optional<Tenant> findBySlugAndActiveTrue(String slug);
    Optional<Tenant> findByCustomDomainAndActiveTrue(String customDomain);
    List<Tenant> findByActiveTrue();
}
