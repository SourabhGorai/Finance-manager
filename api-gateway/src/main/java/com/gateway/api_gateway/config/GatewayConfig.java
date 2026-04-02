package com.gateway.api_gateway.config;

import com.gateway.api_gateway.filter.AuthenticationFilter;
import com.gateway.api_gateway.filter.AuthorizationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gateway Configuration with Role-Based Authorization
 * <p>
 * Available Roles:
 * - USERS: Regular students
 * - FACULTY: Faculty members
 * - SUPER_ADMIN: System administrators
 */
@Configuration
public class GatewayConfig {

    private final AuthenticationFilter authenticationFilter;
    private final AuthorizationFilter authorizationFilter;

    @Autowired
    public GatewayConfig(AuthenticationFilter authenticationFilter,
                         AuthorizationFilter authorizationFilter) {
        this.authenticationFilter = authenticationFilter;
        this.authorizationFilter = authorizationFilter;
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()

                // ==================== SPRINGDOC API-DOCS PROXY ROUTES ====================

                .route("user-service-api-docs", r -> r
                        .path("/user-service/v3/api-docs")
                        .filters(f -> f.rewritePath("/user-service/v3/api-docs", "/v3/api-docs"))
                        .uri("lb://USER-SERVICE"))

                .route("finance-service-api-docs", r -> r
                        .path("/finance-service/v3/api-docs")
                        .filters(f -> f.rewritePath("/finance-service/v3/api-docs", "/v3/api-docs"))
                        .uri("lb://FINANCE-SERVICE"))


                // ==================== USER SERVICE ====================

                // Public auth endpoints (no authentication required)
                .route("user-auth-public", r -> r
                        .path(
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/verify-otp",
                                "/api/auth/resend-verify-otp",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password",
                                "/api/auth/validate-credentials"
                        )
                        .uri("lb://USER-SERVICE"))

                // Bulk operations - SUPER_ADMIN only
                .route("user-auth-admin", r -> r
                        .path(
                                "/api/auth/bulk-register",
                                "/api/auth/bulk-verify-otp"
                        )
                        .filters(f -> f
                                .filter(authenticationFilter.apply(new AuthenticationFilter.Config()))
                                .filter(authorizationFilter.apply(
                                        new AuthorizationFilter.Config("ADMIN"))))
                        .uri("lb://USER-SERVICE"))

                // Token validation - all authenticated users
                .route("user-auth-validate-token", r -> r
                        .path("/api/auth/validate-token")
                        .filters(f -> f.filter(authenticationFilter.apply(new AuthenticationFilter.Config())))
                        .uri("lb://USER-SERVICE"))

                // Get all users - ADMIN only
                .route("user-get-all", r -> r
                        .path(
                                "/api/users/getAll",
                                "/api/users/getAll/paged",
                                "/api/users/changeRole/{usn}/{role}")
                        .and()
                        .method("GET")
                        .filters(f -> f
                                .filter(authenticationFilter.apply(new AuthenticationFilter.Config()))
                                .filter(authorizationFilter.apply(
                                        new AuthorizationFilter.Config("ADMIN"))))
                        .uri("lb://USER-SERVICE"))

                // User validation endpoint - internal use (all authenticated)
                .route("user-validate", r -> r
                        .path(
                                "/api/users/validate/**",
                                "/api/users/getUsers"
                        )
                        .filters(f -> f.filter(authenticationFilter.apply(new AuthenticationFilter.Config())))
                        .uri("lb://USER-SERVICE"))

                // Get/Update/Delete specific user - User can access their own, SUPER_ADMIN can access all
                .route("user-management", r -> r
                        .path("/api/users/**")
                        .filters(f -> f
                                .filter(authenticationFilter.apply(new AuthenticationFilter.Config()))
                                .filter(authorizationFilter.apply(
                                        new AuthorizationFilter.Config("VIEWER", "ANALYST", "ADMIN"))))
                        .uri("lb://USER-SERVICE"))

                // ==================== PROFILE MANAGEMENT SERVICE ====================

                // everyone is allowed
                .route("finance-all", r -> r
                        .path("/api/finance/v/**")
                        .and()
                        .method("POST", "GET", "PUT", "DELETE")
                        .filters(f -> f
                                .filter(authenticationFilter.apply(new AuthenticationFilter.Config()))
                                .filter(authorizationFilter.apply(
                                        new AuthorizationFilter.Config("VIEWER", "ANALYST", "ADMIN"))))
                        .uri("lb://FINANCE-SERVICE"))


                // admin and analyst only
                .route("profile-bulk-operations", r -> r
                        .path(
                                "/api/finance/a/**"
                        )
                        .and()
                        .method("GET")
                        .filters(f -> f
                                .filter(authenticationFilter.apply(new AuthenticationFilter.Config()))
                                .filter(authorizationFilter.apply(
                                        new AuthorizationFilter.Config("ADMIN", "ANALYST"))))
                        .uri("lb://FINANCE-SERVICE"))


                .build();
    }
}