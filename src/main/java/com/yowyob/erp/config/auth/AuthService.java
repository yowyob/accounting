package com.yowyob.erp.config.auth;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

/**
 * Service for interacting with external authentication API
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final WebClient webClient;

    @Value("${auth.api.url}")
    private String authApiUrl;

    @Value("${auth.api.timeout:5000}")
    private int timeout;

    /**
     * Validates a JWT token via external API
     * Result cached to avoid repeated calls
     */
    @Cacheable(value = "jwt-validation", key = "#token")
    public Mono<AuthValidationResponse> validateToken(String token) {
        log.debug("Validating JWT token via external API");

        return webClient
                .post()
                .uri(authApiUrl + "/validate") // Token validation endpoint to be implemented by this service
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(this::parseAuthResponse)
                .timeout(Duration.ofMillis(timeout))
                .doOnError(error -> log.error("Error validating token", error))
                .onErrorReturn(AuthValidationResponse.invalid());
    }

    private AuthValidationResponse parseAuthResponse(JsonNode response) {
        if (response.has("valid") && response.get("valid").asBoolean()) {
            return AuthValidationResponse.builder()
                    .valid(true)
                    .userId(response.get("userId").asText())
                    .organizationId(response.get("organizationId").asText())
                    .roles(response.get("roles").asText().split(","))
                    .build();
        }
        return AuthValidationResponse.invalid();
    }

    /**
     * Retrieves user information
     */
    @Cacheable(value = "user-info", key = "#userEmail")
    public Mono<UserInfo> getUserInfo(String userEmail, String token) {
        return webClient
                .get()
                .uri(authApiUrl + "/users/email/" + userEmail)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(UserInfo.class)
                .timeout(Duration.ofMillis(timeout))
                .doOnError(error -> log.error("Error retrieving user info", error));
    }

    /**
     * Retrieves all employees (members) of the organization
     */
    public Flux<OrganizationMember> getOrganizationMembers(UUID organizationId) {
        return webClient
                .get()
                .uri(authApiUrl + "/employees")
                .header("X-Tenant-ID", organizationId.toString())
                .retrieve()
                .bodyToFlux(OrganizationMember.class)
                .timeout(Duration.ofMillis(timeout))
                .doOnError(error -> log.error("Error retrieving members for organization {}", organizationId, error))
                .onErrorResume(e -> Flux.empty());
    }
}
