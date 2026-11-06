package com.sevadesk.platform;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Email + password sign-in for staff. Issues a tenant-bound JWT in an httpOnly cookie.
 * TODO (phase 0): TOTP second factor for ADMIN/TREASURER, login rate limiting, password reset.
 */
@RestController
@RequestMapping("/api/auth")
class AuthController {

    record LoginRequest(@NotBlank String email, @NotBlank String password) {}
    record Me(String email, String displayName, List<String> roles) {}

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final Duration ttl;
    private final boolean secureCookie;

    AuthController(AppUserRepository users, PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder,
                   @Value("${sevadesk.jwt.ttl}") Duration ttl,
                   @Value("${sevadesk.cookie.secure}") boolean secureCookie) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.ttl = ttl;
        this.secureCookie = secureCookie;
    }

    @PostMapping("/login")
    ResponseEntity<Me> login(@Valid @RequestBody LoginRequest req) {
        var tenant = TenantContext.require();
        AppUser user = users.findByEmailIgnoreCase(req.email())
                .filter(AppUser::isActive)
                .filter(u -> passwordEncoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email or password is incorrect"));

        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder()
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .claim("tid", tenant.id().toString())
                .claim("email", user.getEmail())
                .claim("name", user.getDisplayName())
                .claim("roles", user.getRoles())
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie(token, ttl).toString())
                .body(new Me(user.getEmail(), user.getDisplayName(), user.getRoles()));
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString()).build();
    }

    @GetMapping("/me")
    Me me(@AuthenticationPrincipal Jwt jwt) {
        return new Me(jwt.getClaimAsString("email"), jwt.getClaimAsString("name"), jwt.getClaimAsStringList("roles"));
    }

    private ResponseCookie cookie(String value, Duration maxAge) {
        return ResponseCookie.from(SecurityConfig.TOKEN_COOKIE, value)
                .httpOnly(true).secure(secureCookie).sameSite("Strict").path("/").maxAge(maxAge).build();
    }
}
