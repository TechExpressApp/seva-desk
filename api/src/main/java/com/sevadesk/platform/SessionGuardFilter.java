package com.sevadesk.platform;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

/**
 * Runs after the sign-in token is verified, on every API call:
 * <ul>
 *   <li>ends the session if the user was deactivated, or their password or roles changed since they signed in;</li>
 *   <li>while a temporary password is in use, allows only changing it (and signing out).</li>
 * </ul>
 */
class SessionGuardFilter extends OncePerRequestFilter {

    private static final Set<String> ALLOWED_WHILE_PASSWORD_CHANGE_REQUIRED =
            Set.of("/api/auth/me", "/api/auth/change-password", "/api/auth/logout");

    private final AppUserRepository users;

    SessionGuardFilter(AppUserRepository users) {
        this.users = users;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken auth) {
            var jwt = auth.getToken();
            UUID userId;
            try {
                userId = UUID.fromString(jwt.getSubject());
            } catch (IllegalArgumentException e) {
                reject(response, 401, "Your session has ended. Please sign in again.");
                return;
            }
            Object tv = jwt.getClaims().get("tv");
            int tokenVersion = tv instanceof Number n ? n.intValue() : 0;
            var user = users.findById(userId).orElse(null);
            if (user == null || !user.isActive() || user.getTokenVersion() != tokenVersion) {
                SecurityContextHolder.clearContext();
                reject(response, 401, "Your session has ended. Please sign in again.");
                return;
            }
            if (user.isMustChangePassword() && !ALLOWED_WHILE_PASSWORD_CHANGE_REQUIRED.contains(request.getRequestURI())) {
                reject(response, 403, "Please choose a new password before continuing.");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private static void reject(HttpServletResponse response, int status, String detail) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write("{\"status\":" + status + ",\"detail\":\"" + detail + "\"}");
    }
}
