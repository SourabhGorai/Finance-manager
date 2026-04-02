package com.gateway.api_gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@Slf4j
public class GlobalLoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        String requestId = UUID.randomUUID().toString();
        String path = request.getPath().toString();
        String method = request.getMethod().toString();
        String clientIp = getClientIp(request);

        log.info("🔵 [{}] {} {} - Client IP: {}",
                requestId, method, path, clientIp);

        long startTime = System.currentTimeMillis();

        // Add request ID to exchange attributes for tracking
        exchange.getAttributes().put("requestId", requestId);

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            ServerHttpResponse response = exchange.getResponse();
            long duration = System.currentTimeMillis() - startTime;
            int statusCode = response.getStatusCode() != null ?
                    response.getStatusCode().value() : 0;

            String statusEmoji = getStatusEmoji(statusCode);

            log.info("{} [{}] {} {} - Status: {} - Duration: {}ms",
                    statusEmoji, requestId, method, path, statusCode, duration);
        }));
    }

    /**
     * Get client IP address from request
     */
    private String getClientIp(ServerHttpRequest request) {
        String ip = request.getHeaders().getFirst("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeaders().getFirst("X-Real-IP");
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddress() != null ?
                    request.getRemoteAddress().getAddress().getHostAddress() : "unknown";
        }
        return ip;
    }

    /**
     * Get emoji based on HTTP status code
     */
    private String getStatusEmoji(int statusCode) {
        if (statusCode >= 200 && statusCode < 300) {
            return "✅"; // Success
        } else if (statusCode >= 300 && statusCode < 400) {
            return "🔄"; // Redirect
        } else if (statusCode >= 400 && statusCode < 500) {
            return "⚠️"; // Client error
        } else if (statusCode >= 500) {
            return "❌"; // Server error
        }
        return "⚪"; // Unknown
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}