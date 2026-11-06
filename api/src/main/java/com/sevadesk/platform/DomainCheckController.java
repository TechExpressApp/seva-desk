package com.sevadesk.platform;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Caddy's on-demand TLS "ask" endpoint: Caddy calls it before requesting an HTTPS certificate for a new host name,
 * so certificates are only issued for registered temples (radhagovinda.sevadesk.app, admin.mytemple.org).
 * Reachable without a tenant; see TenantResolverFilter.
 */
@RestController
class DomainCheckController {

    private final TenantResolver resolver;

    DomainCheckController(TenantResolver resolver) {
        this.resolver = resolver;
    }

    @GetMapping("/api/public/domain-allowed")
    ResponseEntity<Void> allowed(@RequestParam String domain) {
        return resolver.resolve(domain).isPresent() ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }
}
