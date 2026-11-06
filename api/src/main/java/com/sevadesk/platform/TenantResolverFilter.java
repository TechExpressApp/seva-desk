package com.sevadesk.platform;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Resolves the tenant from the request host before Spring Security runs, and clears it afterwards.
 * API calls for an unknown host get 404; health checks and API docs work without a tenant.
 */
public class TenantResolverFilter extends OncePerRequestFilter {

    private final TenantResolver resolver;

    public TenantResolverFilter(TenantResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var tenant = resolver.resolve(request.getServerName());
        if (tenant.isEmpty() && needsTenant(request)) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            if (request.getRequestURI().startsWith("/api/")) {
                response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                response.getWriter().write("""
                    {"title":"Unknown temple","status":404,"detail":"No temple is registered for this address."}""");
            } else {
                response.setContentType(MediaType.TEXT_HTML_VALUE + ";charset=UTF-8");
                response.getWriter().write("<!doctype html><title>Not found</title><p>No temple website is registered for this address.</p>");
            }
            return;
        }
        try {
            tenant.ifPresent(t -> TenantContext.set(t.toRef()));
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private boolean needsTenant(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Everything is per temple (API and public website) except ops endpoints.
        return !path.startsWith("/actuator") && !path.startsWith("/api/docs") && !path.equals("/error")
                && !path.equals("/api/public/domain-allowed");
    }
}
