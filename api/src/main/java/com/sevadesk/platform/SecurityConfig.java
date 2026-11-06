package com.sevadesk.platform;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Stateless JWT auth. The token lives in an httpOnly, SameSite=Strict cookie, so page scripts can't read it and
 * other sites can't send it (which is why CSRF tokens are not needed). Each token is bound to one temple:
 * a token issued for one tenant is rejected on another tenant's address.
 */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
class SecurityConfig {

    static final String TOKEN_COOKIE = "SD_TOKEN";

    @Bean
    SecurityFilterChain api(HttpSecurity http, AppUserRepository users) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**", "/api/docs/**", "/actuator/health/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/logout",
                        "/api/auth/forgot-password", "/api/auth/reset-password").permitAll()
                .requestMatchers("/actuator/**").denyAll()
                .requestMatchers("/api/**").authenticated()
                // Everything else is the temple's public website.
                .anyRequest().permitAll())
            .oauth2ResourceServer(rs -> rs
                .bearerTokenResolver(cookieOrHeaderResolver())
                .jwt(jwt -> jwt.jwtAuthenticationConverter(new TenantBoundJwtConverter())))
            // ends sessions of deactivated users / changed passwords, and enforces "change your temporary password"
            .addFilterAfter(new SessionGuardFilter(users), BearerTokenAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    SecretKey jwtKey(@Value("${sevadesk.jwt.secret}") String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("sevadesk.jwt.secret (JWT_SECRET) must be at least 32 bytes");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtKey));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtKey) {
        return NimbusJwtDecoder.withSecretKey(jwtKey).macAlgorithm(MacAlgorithm.HS256).build();
    }

    private static BearerTokenResolver cookieOrHeaderResolver() {
        var header = new DefaultBearerTokenResolver();
        return (HttpServletRequest request) -> {
            // Only the API looks at the sign-in token; an expired cookie must never break the public website.
            // Public endpoints and sign-in/out ignore it too, so a stale or other-temple cookie can't block them.
            String uri = request.getRequestURI();
            if (!uri.startsWith("/api/") || uri.startsWith("/api/public/")
                    || uri.equals("/api/auth/login") || uri.equals("/api/auth/logout")
                    || uri.equals("/api/auth/forgot-password") || uri.equals("/api/auth/reset-password")) return null;
            String fromHeader = header.resolve(request);
            if (fromHeader != null) return fromHeader;
            if (request.getCookies() == null) return null;
            for (Cookie c : request.getCookies()) {
                if (TOKEN_COOKIE.equals(c.getName()) && !c.getValue().isBlank()) return c.getValue();
            }
            return null;
        };
    }

    /** Turns the "roles" claim into ROLE_* authorities and refuses tokens issued for another tenant. */
    static final class TenantBoundJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {
        @Override
        public AbstractAuthenticationToken convert(Jwt jwt) {
            String tokenTenant = jwt.getClaimAsString("tid");
            var current = TenantContext.currentId();
            if (current == null || !Objects.equals(tokenTenant, current.toString())) {
                throw new InvalidBearerTokenException("This sign-in belongs to a different temple");
            }
            List<String> roles = jwt.getClaimAsStringList("roles");
            Collection<GrantedAuthority> authorities = roles == null ? List.of()
                    : roles.stream().<GrantedAuthority>map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList();
            return new JwtAuthenticationToken(jwt, authorities, jwt.getClaimAsString("email"));
        }
    }
}
