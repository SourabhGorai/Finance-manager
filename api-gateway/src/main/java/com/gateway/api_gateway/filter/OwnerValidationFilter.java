package com.gateway.api_gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Owner Validation Filter - Ensures users can only access their own resources
 * This filter checks if the USN in the URL matches the authenticated user's USN
 * SUPER_ADMIN can access any resource
 */
@Component
@Slf4j
public class OwnerValidationFilter extends AbstractGatewayFilterFactory<OwnerValidationFilter.Config> {

    public OwnerValidationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            
            // Extract user info from headers (set by AuthenticationFilter)
            String userUsn = request.getHeaders().getFirst("X-User-Usn");
            String userRole = request.getHeaders().getFirst("X-User-Role");
            
            if (userUsn == null || userRole == null) {
                log.warn("⚠️ Missing user context in headers for owner validation");
                return onError(exchange, "Missing user context", HttpStatus.FORBIDDEN);
            }

            // SUPER_ADMIN can access any resource
            if ("SUPER_ADMIN".equalsIgnoreCase(userRole)) {
                log.debug("✅ SUPER_ADMIN bypass for owner validation on path: {}", request.getPath());
                return chain.filter(exchange);
            }

            // Extract USN from path
            String pathUsn = extractUsnFromPath(request.getPath().toString());
            
            if (pathUsn == null) {
                log.warn("⚠️ Could not extract USN from path: {}", request.getPath());
                // If we can't extract USN, let it through (might be a list endpoint)
                return chain.filter(exchange);
            }

            // Check if user is accessing their own resource
            if (!userUsn.equals(pathUsn)) {
                log.warn("🚫 User '{}' attempted to access resource belonging to '{}'", 
                        userUsn, pathUsn);
                return onError(exchange, 
                        "Access denied. You can only access your own resources.", 
                        HttpStatus.FORBIDDEN);
            }

            log.debug("✅ Owner validation successful for user '{}' accessing path: {}", 
                    userUsn, request.getPath());
            return chain.filter(exchange);
        };
    }

    /**
     * Extract Usn from URL path
     * Supports patterns like:
     * - /api/profiles/USN12345
     * - /api/profiles/usn/USN12345
     * - /api/users/USN12345
     */
    private String extractUsnFromPath(String path) {
        // Pattern to match USN in path (after /api/*/usn/ or /api/*/{usn})
        Pattern pattern = Pattern.compile("/(?:usn/)?([A-Z0-9]+)(?:/|$)");
        Matcher matcher = pattern.matcher(path);
        
        if (matcher.find()) {
            String usn = matcher.group(1);
            // Basic validation - USN should start with letters
            if (usn.matches("^[A-Z]+[0-9]+.*")) {
                return usn;
            }
        }
        
        return null;
    }

    /**
     * Handle authorization errors
     */
    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add("Content-Type", "application/json");

        String errorResponse = String.format(
                "{\"error\": \"%s\", \"status\": %d, \"timestamp\": \"%s\", \"path\": \"%s\"}",
                message,
                status.value(),
                java.time.Instant.now().toString(),
                exchange.getRequest().getPath()
        );

        byte[] bytes = errorResponse.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }

    public static class Config {
        // Configuration properties can be added here if needed
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}