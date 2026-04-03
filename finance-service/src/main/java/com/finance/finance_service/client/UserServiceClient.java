package com.finance.finance_service.client;

import com.finance.finance_service.dto.ApiResponse;
import com.finance.finance_service.dto.response.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final WebClient.Builder webClientBuilder;
    private final HttpServletRequest request;

    @Value("${app.user-service.url:http://USER-SERVICE/api}")
    private String userServiceUrl;

    public UserResponse getUser(String usn) {
        String authHeader = request.getHeader("Authorization");

        log.info("Calling user service to fetch user for USN: {}", usn);

        try {
            ApiResponse<UserResponse> response = webClientBuilder.build()
                    .get()
                    .uri(userServiceUrl + "/users/{usn}", usn)
                    .header("Authorization", authHeader)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference
                            <ApiResponse<UserResponse>>() {})
                    .block();

            if (response == null || Boolean.FALSE.equals(response.getSuccess())) {
                log.warn("User service returned failure for USN: {}", usn);
                return null;
            }

            return response.getData();

        } catch (WebClientResponseException.NotFound e) {
            log.warn("User not found for USN: {}", usn);
            return null;

        } catch (WebClientResponseException e) {
            log.error("Error fetching user {}: {} - {}", usn, e.getStatusCode(), e.getMessage());
            throw new RuntimeException("Failed to fetch user: " + usn, e);

        } catch (Exception e) {
            log.error("Unexpected error fetching user {}: {}", usn, e.getMessage());
            throw new RuntimeException("Failed to fetch user: " + usn, e);
        }
    }

    public List<UserResponse> getUsers(List<String> usns) {
        String authHeader = request.getHeader("Authorization");

        log.info("Calling user service to fetch users for USNs: {}", usns);

        try {
            ApiResponse<List<UserResponse>> response = webClientBuilder.build()
                    .post()
                    .uri(userServiceUrl + "/users/getUsers")
                    .bodyValue(usns)
                    .header("Authorization", authHeader)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference
                            <ApiResponse<List<UserResponse>>>() {})
                    .block();

            if (response == null || Boolean.FALSE.equals(response.getSuccess())) {
                log.warn("User service returned failure for USNs: {}", usns);
                return Collections.emptyList();
            }

            return response.getData() != null ? response.getData() : Collections.emptyList();

        } catch (WebClientResponseException.NotFound e) {
            log.warn("Users not found for USNs: {}", usns);
            return Collections.emptyList();

        } catch (WebClientResponseException e) {
            log.error("Error fetching users: {} - {}", e.getStatusCode(), e.getMessage());
            throw new RuntimeException("Failed to fetch users", e);

        } catch (Exception e) {
            log.error("Unexpected error: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch users", e);
        }
    }
}