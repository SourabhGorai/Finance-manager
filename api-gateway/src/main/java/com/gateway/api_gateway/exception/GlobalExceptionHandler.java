package com.gateway.api_gateway.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
@Order(-1)
@Slf4j
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        log.error("❌ Global error occurred: ", ex);

        HttpStatus status = determineHttpStatus(ex);
        String message = determineErrorMessage(ex);
        String path = exchange.getRequest().getPath().toString();

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", Instant.now().toString());
        errorResponse.put("path", path);
        errorResponse.put("status", status.value());
        errorResponse.put("error", status.getReasonPhrase());
        errorResponse.put("message", message);
        errorResponse.put("requestId", exchange.getAttribute("requestId"));

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("Error serializing error response", e);
            return exchange.getResponse().writeWith(
                    Mono.just(exchange.getResponse().bufferFactory()
                            .wrap(getDefaultErrorMessage(status).getBytes(StandardCharsets.UTF_8)))
            );
        }
    }

    /**
     * Determine HTTP status from exception
     */
    private HttpStatus determineHttpStatus(Throwable ex) {
        if (ex instanceof ResponseStatusException) {
            return (HttpStatus) ((ResponseStatusException) ex).getStatusCode();
        } else if (ex.getCause() instanceof ResponseStatusException) {
            return (HttpStatus) ((ResponseStatusException) ex.getCause()).getStatusCode();
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    /**
     * Extract error message from exception
     */
    private String determineErrorMessage(Throwable ex) {
        if (ex instanceof ResponseStatusException) {
            ResponseStatusException rse = (ResponseStatusException) ex;
            return rse.getReason() != null ? rse.getReason() : "An error occurred";
        } else if (ex.getCause() instanceof ResponseStatusException) {
            ResponseStatusException rse = (ResponseStatusException) ex.getCause();
            return rse.getReason() != null ? rse.getReason() : "An error occurred";
        }

        // Don't expose internal error details to clients
        return "An unexpected error occurred. Please try again later.";
    }

    /**
     * Fallback error message if JSON serialization fails
     */
    private String getDefaultErrorMessage(HttpStatus status) {
        return String.format(
                "{\"error\": \"%s\", \"status\": %d, \"timestamp\": \"%s\"}",
                status.getReasonPhrase(), status.value(), Instant.now().toString()
        );
    }
}
