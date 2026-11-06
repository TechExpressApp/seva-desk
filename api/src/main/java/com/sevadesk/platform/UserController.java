package com.sevadesk.platform;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

/**
 * Staff logins for one temple: add people, choose which modules they can use, reset passwords, deactivate.
 * Admins only. The last active admin can never be removed, so a temple can't lock itself out.
 */
@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
class UserController {

    record RoleView(String role, String label, String description, List<String> modules) {}

    record UserView(UUID id, String email, String displayName, List<String> roles, boolean active,
                    boolean mustChangePassword, Instant lastLoginAt, Instant createdAt) {
        static UserView of(AppUser u) {
            return new UserView(u.getId(), u.getEmail(), u.getDisplayName(), u.getRoles(), u.isActive(),
                    u.isMustChangePassword(), u.getLastLoginAt(), u.getCreatedAt());
        }
    }

    record NewUser(@NotBlank @Size(max = 120) String displayName, @NotBlank @Email @Size(max = 200) String email,
                   @NotEmpty List<String> roles) {}

    record UserUpdate(@NotBlank @Size(max = 120) String displayName, @NotEmpty List<String> roles, boolean active) {}

    /** emailSent: a "set your password" link went out. Otherwise temporaryPassword is shown to the admin once. */
    record PasswordIssued(UserView user, boolean emailSent, String temporaryPassword) {}

    private final AppUserRepository users;
    private final TenantRepository tenants;
    private final Passwords passwords;

    UserController(AppUserRepository users, TenantRepository tenants, Passwords passwords) {
        this.users = users;
        this.tenants = tenants;
        this.passwords = passwords;
    }

    @GetMapping
    List<UserView> list() {
        return users.findAll().stream()
                .sorted(Comparator.comparing(AppUser::isActive).reversed()
                        .thenComparing(u -> u.getDisplayName().toLowerCase()))
                .map(UserView::of).toList();
    }

    @GetMapping("/roles")
    List<RoleView> roles() {
        return Arrays.stream(Role.values())
                .map(r -> new RoleView(r.name(), r.label(), r.description(), r.modules())).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    PasswordIssued create(@Valid @RequestBody NewUser req) {
        String email = Passwords.normalizeEmail(req.email());
        if (users.findByEmailIgnoreCase(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Someone with this email already has a login.");
        }
        // Unusable until a link is used or a temporary password is issued just below.
        var user = users.save(new AppUser(email, passwords.encode(UUID.randomUUID().toString()),
                req.displayName().strip(), validRoles(req.roles())));
        var issued = passwords.issue(user, PasswordToken.Purpose.INVITE, templeName(), AuthController.baseUrl());
        return new PasswordIssued(UserView.of(user), issued.emailSent(), issued.temporaryPassword());
    }

    @PutMapping("/{id}")
    @Transactional
    UserView update(@PathVariable UUID id, @Valid @RequestBody UserUpdate req, @AuthenticationPrincipal Jwt jwt) {
        var user = find(id);
        var roles = validRoles(req.roles());
        boolean wasActiveAdmin = user.isActive() && user.getRoles().contains(Role.ADMIN.name());
        boolean staysActiveAdmin = req.active() && roles.contains(Role.ADMIN.name());
        if (wasActiveAdmin && !staysActiveAdmin && activeAdmins() <= 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This is the only active administrator. Make someone else an administrator first.");
        }
        if (!req.active() && id.toString().equals(jwt.getSubject())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You can't deactivate your own login.");
        }
        user.setDisplayName(req.displayName().strip());
        user.setRoles(roles);
        user.setActive(req.active());
        return UserView.of(user);
    }

    /** Emails a reset link if email is set up; otherwise sets a temporary password that must be changed at sign-in. */
    @PostMapping("/{id}/reset-password")
    @Transactional
    PasswordIssued resetPassword(@PathVariable UUID id) {
        var user = find(id);
        if (!user.isActive()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Activate this login first.");
        var issued = passwords.issue(user, PasswordToken.Purpose.RESET, templeName(), AuthController.baseUrl());
        return new PasswordIssued(UserView.of(user), issued.emailSent(), issued.temporaryPassword());
    }

    private AppUser find(UUID id) {
        return users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private long activeAdmins() {
        return users.findAll().stream().filter(u -> u.isActive() && u.getRoles().contains(Role.ADMIN.name())).count();
    }

    private static List<String> validRoles(List<String> requested) {
        var roles = new LinkedHashSet<String>();
        for (String r : requested) {
            if (!Role.isValid(r)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown role: " + r);
            roles.add(r);
        }
        if (roles.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose at least one role.");
        return List.copyOf(roles);
    }

    private String templeName() {
        return tenants.findById(TenantContext.require().id()).map(Tenant::getName).orElse("your temple");
    }
}
