package com.gateway.api_gateway.util;

import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * Validates the JWT token
     * @param token JWT token to validate
     * @throws RuntimeException if token is invalid
     */
    public void validateToken(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(jwtSecret)
                    .parseClaimsJws(token);
            log.debug("Token validated successfully");
        } catch (SignatureException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
            throw new RuntimeException("Invalid JWT signature");
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            throw new RuntimeException("Invalid JWT token");
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
            throw new RuntimeException("JWT token is expired");
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
            throw new RuntimeException("JWT token is unsupported");
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
            throw new RuntimeException("JWT claims string is empty");
        }
    }

    /**
     * Extract username from JWT token
     * @param token JWT token
     * @return username
     */
    public String extractUsername(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(jwtSecret)
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
        } catch (Exception e) {
            log.error("Error extracting username from token: {}", e.getMessage());
            throw new RuntimeException("Error extracting username from token");
        }
    }

    /**
     * Extract role from JWT token
     * @param token JWT token
     * @return user role
     */
    public String extractRole(String token) {
        try {
            return (String) Jwts.parser()
                    .setSigningKey(jwtSecret)
                    .parseClaimsJws(token)
                    .getBody()
                    .get("role");
        } catch (Exception e) {
            log.error("Error extracting role from token: {}", e.getMessage());
            throw new RuntimeException("Error extracting role from token");
        }
    }

    /**
     * Extract user ID from JWT token (if available)
     * @param token JWT token
     * @return user ID or null
     */
    public String extractUsn(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(jwtSecret)
                    .parseClaimsJws(token)
                    .getBody();

            // Try different claim names
            Object usn = claims.get("usn");
            if (usn == null) {
                usn = claims.get("userId");  // Try userId
            }
            if (usn == null) {
                usn = claims.get("id");  // Try id
            }
            if (usn == null) {
                log.warn("No USN/userId/id claim found in token. Available claims: {}", claims.keySet());
                return null;
            }

            return usn.toString();
        } catch (Exception e) {
            log.error("Error extracting USN from token: {}", e.getMessage());
            return null;
        }
    }
}