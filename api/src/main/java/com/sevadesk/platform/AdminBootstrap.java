package com.sevadesk.platform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Creates a temple's first admin login on startup from BOOTSTRAP_TENANT_SLUG / BOOTSTRAP_ADMIN_EMAIL /
 * BOOTSTRAP_ADMIN_PASSWORD, but only while that temple has no users at all. After the first login,
 * remove the password from the server's .env file.
 */
@Component
@Order(0)
class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final TenantRepository tenants;
    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String slug;
    private final String email;
    private final String password;

    AdminBootstrap(TenantRepository tenants, AppUserRepository users, PasswordEncoder passwordEncoder,
                   @Value("${sevadesk.bootstrap.tenant-slug:}") String slug,
                   @Value("${sevadesk.bootstrap.admin-email:}") String email,
                   @Value("${sevadesk.bootstrap.admin-password:}") String password) {
        this.tenants = tenants;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.slug = slug;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (slug.isBlank() || email.isBlank() || password.isBlank()) return;
        if (password.length() < 12) {
            log.warn("BOOTSTRAP_ADMIN_PASSWORD must be at least 12 characters; no admin created");
            return;
        }
        var tenant = tenants.findBySlugAndActiveTrue(slug);
        if (tenant.isEmpty()) {
            log.warn("BOOTSTRAP_TENANT_SLUG '{}' does not match any active temple; no admin created", slug);
            return;
        }
        TenantContext.runAs(tenant.get().toRef(), () -> {
            if (users.count() > 0) return;
            users.save(new AppUser(email, passwordEncoder.encode(password), "Temple Admin", List.of("ADMIN")));
            log.info("Created the first admin login for {}", tenant.get().getName());
        });
    }
}
