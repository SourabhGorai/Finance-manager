package com.userservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Filter to extract user authentication details from API Gateway headers
 * 
 * This filter checks if the request comes through the API Gateway by looking for
 * X-User-Username, X-User-Role, X-User-Id headers.
 * 
 * If these headers are present, the Gateway has already validated the JWT,
 * so we trust the headers and set the authentication.
 * 
 * If headers are NOT present, this filter does nothing and lets JwtAuthFilter
 * handle authentication (for direct service calls during testing).
 */
@Component
@Slf4j
public class GatewayAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Extract user details from Gateway headers
        String username = request.getHeader("X-User-Username");
        String role = request.getHeader("X-User-Role");
        String userId = request.getHeader("X-User-Id");

        // If Gateway headers are present, use them (Gateway already validated JWT)
        if (username != null && role != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            log.debug("🌐 Request from API Gateway - User: {}, Role: {}, UserId: {}", username, role, userId);

            // Create authentication token with ROLE_ prefix for Spring Security
            SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(username, null, Collections.singletonList(authority));

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // Set authentication in SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Store additional user details in request attributes for easy access
            request.setAttribute("userId", userId);
            request.setAttribute("userRole", role);
            request.setAttribute("username", username);
            
            log.debug("✅ Gateway authentication successful for user: {}", username);
        } else if (username == null && role == null) {
            // No Gateway headers - this is a direct service call
            // JwtAuthFilter will handle authentication
            log.debug("📱 Direct service call - JwtAuthFilter will handle authentication");
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Don't filter public auth endpoints
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/register") ||
               path.startsWith("/api/auth/login") ||
               path.startsWith("/api/auth/verify-otp") ||
               path.startsWith("/api/auth/bulk-verify-otp") ||
               path.startsWith("/api/auth/resend-verify-otp") ||
               path.startsWith("/api/auth/forgot-password") ||
               path.startsWith("/api/auth/reset-password") ||
               path.startsWith("/api/auth/validate-credentials");
    }
}