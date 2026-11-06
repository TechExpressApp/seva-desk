package com.sevadesk.platform;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Staff sign-in. Issues a tenant-bound JWT in an httpOnly cookie, carrying the user's token version so that
 * deactivation, role changes and password changes end existing sessions (see SessionGuardFilter).
 * TODO: TOTP second factor for ADMIN/TREASURER, login rate limiting.
 */
@RestController
@RequestMapping("/api/auth")
class AuthController {

    record LoginRequest(@NotBlank String email, @NotBlank String password) {}
    record ChangePasswordRequest(@NotBlank String currentPassword, @NotBlank String newPassword) {}
    record ForgotRequest(String email) {}
    record ResetRequest(@NotBlank String token, @NotBlank String newPassword) {}

    /** The signed-in user, with the admin-app modules their roles open. */
    record Me(String email, String displayName, List<String> roles, Set<String> modules, boolean mustChangePassword) {
        static Me of(AppUser u) {
            Set<String> modules = new LinkedHashSet<>();
            u.getRoles().stream().filter(Role::isValid).map(Role::valueOf).forEach(r -> modules.addAll(r.modules()));
            return new Me(u.getEmail(), u.getDisplayName(), u.getRoles(), modules, u.isMustChangePassword());
        }
    }

    private static final String WRONG_LOGIN = "Email or password is incorrect";

    private final AppUserRepository users;
    private final TenantRepository tenants;
    private final Passwords passwords;
    private final JwtEncoder jwtEncoder;
    private final Duration ttl;
    private final boolean secureCookie;

    AuthController(AppUserRepository users, TenantRepository tenants, Passwords passwords, JwtEncoder jwtEncoder,
                   @Value("${sevadesk.jwt.ttl}") Duration ttl,
                   @Value("${sevadesk.cookie.secure}") boolean secureCookie) {
        this.users = users;
        this.tenants = tenants;
        this.passwords = passwords;
        this.jwtEncoder = jwtEncoder;
        this.ttl = ttl;
        this.secureCookie = secureCookie;
    }

    @PostMapping("/login")
    @Transactional
    ResponseEntity<Me> login(@Valid @RequestBody LoginRequest req) {
        AppUser user = users.findByEmailIgnoreCase(req.email().strip())
                .filter(AppUser::isActive)
                .filter(u -> passwords.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, WRONG_LOGIN));
        user.recordLogin();
        return signedIn(user);
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString()).build();
    }

    @GetMapping("/me")
    Me me(@AuthenticationPrincipal Jwt jwt) {
        return Me.of(current(jwt));
    }

    /** Change your own password (also the way to replace a temporary one). Signs out your other sessions. */
    @PostMapping("/change-password")
    @Transactional
    ResponseEntity<Me> changePassword(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ChangePasswordRequest req) {
        AppUser user = current(jwt);
        if (!passwords.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Your current password is incorrect.");
        }
        if (req.newPassword().equals(req.currentPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a password different from the current one.");
        }
        String problem = Passwords.problemWith(req.newPassword(), user.getEmail());
        if (problem != null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, problem);
        user.changePassword(passwords.encode(req.newPassword()), false);
        return signedIn(user);
    }

    /** Always answers the same way, so it never reveals whether an email has an account. */
    @PostMapping("/forgot-password")
    ResponseEntity<Void> forgotPassword(@RequestBody ForgotRequest req) {
        passwords.forgot(req.email(), templeName(), baseUrl());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/reset-password")
    ResponseEntity<?> resetPassword(@Valid @RequestBody ResetRequest req) {
        String problem = passwords.reset(req.token(), req.newPassword());
        if (problem != null) return ResponseEntity.badRequest().body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, problem));
        return ResponseEntity.noContent().build();
    }

    private AppUser current(Jwt jwt) {
        return users.findById(UUID.fromString(jwt.getSubject()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in again."));
    }

    private ResponseEntity<Me> signedIn(AppUser user) {
        var tenant = TenantContext.require();
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder()
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .claim("tid", tenant.id().toString())
                .claim("email", user.getEmail())
                .claim("name", user.getDisplayName())
                .claim("roles", user.getRoles())
                .claim("tv", user.getTokenVersion())
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie(token, ttl).toString())
                .body(Me.of(user));
    }

    private String templeName() {
        return tenants.findById(TenantContext.require().id()).map(Tenant::getName).orElse("your temple");
    }

    static String baseUrl() {
        return ServletUriComponentsBuilder.fromCurrentContextPath().toUriString();
    }

    private ResponseCookie cookie(String value, Duration maxAge) {
        return ResponseCookie.from(SecurityConfig.TOKEN_COOKIE, value)
                .httpOnly(true).secure(secureCookie).sameSite("Strict").path("/").maxAge(maxAge).build();
    }
}
