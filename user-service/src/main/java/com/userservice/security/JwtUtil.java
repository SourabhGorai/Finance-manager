package com.userservice.security;

import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    /**
     * ✅ UPDATED: Generate JWT token with username, role, and PRN (user ID)
     * @param username User's username
     * @param role User's role
     * @param usn User's PRN (unique identifier)
     * @return JWT token
     */
    public String generateToken(String username, String role, String usn) {
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .claim("usn", usn)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(SignatureAlgorithm.HS256, jwtSecret)
                .compact();
    }

    /**
     * 🔄 BACKWARD COMPATIBILITY: Keep old method for existing code
     * @deprecated Use generateToken(username, role, prn) instead
     */
    @Deprecated
    public String generateToken(String username, String role) {
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(SignatureAlgorithm.HS256, jwtSecret)
                .compact();
    }

    public String extractUsername(String token) {
        return Jwts.parser().setSigningKey(jwtSecret)
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public String extractRole(String token) {
        return (String) Jwts.parser().setSigningKey(jwtSecret)
                .parseClaimsJws(token)
                .getBody()
                .get("role");
    }

    /**
     * ✅ NEW: Extract PRN from token
     * @param token JWT token
     * @return PRN (user ID) or null if not present
     */
    public String extractUsn(String token) {
        try {
            return (String) Jwts.parser().setSigningKey(jwtSecret)
                    .parseClaimsJws(token)
                    .getBody()
                    .get("usn");
        } catch (Exception e) {
            return null;
        }
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}