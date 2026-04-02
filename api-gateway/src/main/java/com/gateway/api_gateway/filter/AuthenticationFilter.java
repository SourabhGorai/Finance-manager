package com.gateway.api_gateway.filter;


import com.gateway.api_gateway.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final JwtUtil jwtUtil;

    public AuthenticationFilter(JwtUtil jwtUtil) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            log.debug("Processing authentication for path: {}", request.getPath());

            // Check if Authorization header exists
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                log.warn("Missing Authorization header for path: {}", request.getPath());
                return onError(exchange, "Missing Authorization header", HttpStatus.UNAUTHORIZED);
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("Invalid Authorization header format");
                return onError(exchange, "Invalid Authorization header format. Expected: Bearer <token>",
                        HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            try {
                // Validate JWT token
                jwtUtil.validateToken(token);

                // Extract user information
                String username = jwtUtil.extractUsername(token);
                String role = jwtUtil.extractRole(token);
                String usn = jwtUtil.extractUsn(token);

                // ⚠️ FALLBACK: Use username if USN is not available
                String userId = (usn != null && !usn.isEmpty()) ? usn : username;

                if (usn == null || usn.isEmpty()) {
                    log.warn("⚠️ USN claim missing in token for user: {}. Using username as fallback. " +
                            "UPDATE USER-SERVICE to include 'usn' claim in JWT!", username);
                }

                log.debug("Authenticated user: {} with role: {}, userId: {}", username, role, userId);

                // Add user information to request headers for downstream services
                ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                        .header("X-User-Username", username)
                        .header("X-User-Role", role)
                        .header("X-User-Id", userId)  // Pass user ID (USN or username fallback)
                        .build();

                return chain.filter(exchange.mutate().request(modifiedRequest).build());

            } catch (Exception e) {
                log.error("JWT validation failed: {}", e.getMessage());
                return onError(exchange, "Invalid or expired JWT token", HttpStatus.UNAUTHORIZED);
            }
        };
    }

    /**
     * Handle authentication errors
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
    }
}