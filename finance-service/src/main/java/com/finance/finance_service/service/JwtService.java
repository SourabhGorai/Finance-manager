package com.finance.finance_service.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

@Service
@Slf4j
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secret;

    /**
     * Extract PRN from gateway headers (preferred method)
     */
    public String extractUsnFromHeaders(HttpServletRequest request) {
        String userId = request.getHeader("X-User-Id");

        // ✅ IMPROVED: Use info-level logging to see what's being extracted
        if (userId == null || userId.equals("null")) {
            log.warn("⚠️ X-User-Id header is missing or null! Gateway may not have USN in JWT.");
            log.warn("Available headers: X-User-Username={}, X-User-Role={}",
                    request.getHeader("X-User-Username"),
                    request.getHeader("X-User-Role"));
        } else {
            log.info("✅ Extracted USN from X-User-Id header: {}", userId);
        }

        return userId;
    }

    /**
     * Extract username from gateway headers
     */
    public String extractUsernameFromHeaders(HttpServletRequest request) {
        String username = request.getHeader("X-User-Username");
        log.debug("Extracted username from headers: {}", username);
        return username;
    }

    /**
     * Extract role from gateway headers
     */
    public String extractRoleFromHeaders(HttpServletRequest request) {
        String role = request.getHeader("X-User-Role");
        log.debug("Extracted role from headers: {}", role);
        return role;
    }

    // ========== FALLBACK: Direct JWT token extraction ==========
    // These methods are kept for backward compatibility or direct token parsing

    public String extractUsn(String token) {
        return extractClaim(token, claims -> claims.get("usn", String.class));
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(extractAllClaims(token));
    }
}