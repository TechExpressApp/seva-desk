package com.sevadesk.platform;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Maps a host name to a tenant:
 * <ul>
 *   <li>{@code radhagovinda.sevadesk.app} or {@code radhagovinda.localhost} → slug {@code radhagovinda}</li>
 *   <li>{@code admin.mytemple.org} → tenant whose custom_domain matches</li>
 *   <li>a bare base domain ({@code localhost}) → the configured default slug, if any (dev only)</li>
 * </ul>
 */
@Component
public class TenantResolver {

    private final TenantRepository tenants;
    private final List<String> baseDomains;
    private final String defaultSlug;

    TenantResolver(TenantRepository tenants,
                   @Value("${sevadesk.tenancy.base-domains}") String baseDomains,
                   @Value("${sevadesk.tenancy.default-slug:}") String defaultSlug) {
        this.tenants = tenants;
        this.baseDomains = Arrays.stream(baseDomains.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        this.defaultSlug = defaultSlug;
    }

    public Optional<Tenant> resolve(String host) {
        if (host == null || host.isBlank()) return Optional.empty();
        String h = host.toLowerCase(Locale.ROOT);
        for (String base : baseDomains) {
            if (h.equals(base)) {
                return defaultSlug.isBlank() ? Optional.empty() : tenants.findBySlugAndActiveTrue(defaultSlug);
            }
            if (h.endsWith("." + base)) {
                String label = h.substring(0, h.length() - base.length() - 1);
                if (label.contains(".")) return Optional.empty();
                return tenants.findBySlugAndActiveTrue(label);
            }
        }
        // www.iskconsomerset.org and iskconsomerset.org are the same temple
        var byDomain = tenants.findByCustomDomainAndActiveTrue(h);
        if (byDomain.isEmpty() && h.startsWith("www.")) {
            return tenants.findByCustomDomainAndActiveTrue(h.substring(4));
        }
        return byDomain;
    }
}
