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

import java.util.Arrays;
import java.util.List;

/**
 * Authorization Filter - Enforces role-based access control
 * Apply this filter AFTER AuthenticationFilter in the filter chain
 */
@Component
@Slf4j
public class AuthorizationFilter extends AbstractGatewayFilterFactory<AuthorizationFilter.Config> {

    public AuthorizationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            
            // Extract role from headers (set by AuthenticationFilter)
            String userRole = request.getHeaders().getFirst("X-User-Role");
            String username = request.getHeaders().getFirst("X-User-Username");
            
            if (userRole == null || userRole.isEmpty()) {
                log.warn("🚫 Missing user role in headers for path: {}", request.getPath());
                return onError(exchange, "Authorization failed: Missing user role", HttpStatus.FORBIDDEN);
            }

            // Check if user has any of the required roles
            List<String> allowedRoles = config.getAllowedRoles();
            
            if (allowedRoles == null || allowedRoles.isEmpty()) {
                log.debug("✅ No role restriction for path: {} - allowing authenticated user", request.getPath());
                return chain.filter(exchange); // No role restriction, allow all authenticated users
            }

            boolean hasRequiredRole = allowedRoles.stream()
                    .anyMatch(role -> role.equalsIgnoreCase(userRole));

            if (!hasRequiredRole) {
                log.warn("🚫 Access denied for user '{}' with role '{}' to path: {}. Required roles: {}", 
                        username, userRole, request.getPath(), allowedRoles);
                return onError(exchange, 
                        String.format("Access denied. Required role(s): %s. Your role: %s", 
                                String.join(", ", allowedRoles), userRole), 
                        HttpStatus.FORBIDDEN);
            }

            log.debug("✅ Authorization successful for user '{}' with role '{}' on path: {}", 
                    username, userRole, request.getPath());
            return chain.filter(exchange);
        };
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

    /**
     * Configuration class for allowed roles
     */
    public static class Config {
        private List<String> allowedRoles;

        public Config() {
        }

        public Config(String... roles) {
            this.allowedRoles = Arrays.asList(roles);
        }

        public Config(List<String> roles) {
            this.allowedRoles = roles;
        }

        public List<String> getAllowedRoles() {
            return allowedRoles;
        }

        public void setAllowedRoles(List<String> allowedRoles) {
            this.allowedRoles = allowedRoles;
        }
    }
}