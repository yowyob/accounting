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
import java.util.Map;
import java.util.UUID;

/**
 * Service d'authentification avec fallback mock.
 *
 * Comportement :
 *  1. Test de connectivité vers le service externe (timeout 2s, mis en cache 30s).
 *  2. Si disponible  → validation/login via le service externe.
 *  3. Si indisponible → validation/login via MockUserStore (données de test).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final WebClient webClient;
    private final MockUserStore mockUserStore;

    @Value("${auth.api.url}")
    private String authApiUrl;

    @Value("${auth.api.timeout:5000}")
    private int timeout;

    @Value("${auth.mock.connectivity-timeout-ms:2000}")
    private int connectivityTimeoutMs;

    // ─── Validation de token ──────────────────────────────────────────────────

    /**
     * Valide un JWT.
     * Tente le service externe en premier ; bascule sur MockUserStore si indisponible.
     * Résultat mis en cache (clé = token) pour éviter un appel par requête HTTP.
     */
    @Cacheable(value = "jwt-validation", key = "#token")
    public Mono<AuthValidationResponse> validateToken(String token) {
        // Local mock tokens always stay on the in-memory store (SSE + dev login)
        if (token != null && token.startsWith("mock-")) {
            return Mono.just(mockUserStore.validateToken(token));
        }

        return isExternalServiceAvailable()
            .flatMap(available -> {
                if (available) {
                    log.debug("[AUTH] Validation via service externe");
                    return validateTokenExternal(token);
                } else {
                    log.debug("[AUTH] Validation via mock (service externe indisponible)");
                    return Mono.just(mockUserStore.validateToken(token));
                }
            });
    }

    // ─── Login externe (délégué depuis AuthController) ────────────────────────

    /**
     * Tente un login via le service externe.
     * Utilisé uniquement lorsque {@link #isExternalServiceAvailable()} retourne true.
     */
    public Mono<AuthController.LoginResponse> loginExternal(String email, String password) {
        return webClient
            .post()
            .uri(authApiUrl + "/login")
            .bodyValue(Map.of("email", email, "password", password))
            .retrieve()
            .bodyToMono(JsonNode.class)
            .timeout(Duration.ofMillis(timeout))
            .map(this::parseLoginResponse);
    }

    // ─── Check de connectivité ────────────────────────────────────────────────

    /**
     * Teste la joignabilité du service externe (timeout 2s).
     * Non mis en cache ici — le cache de courte durée est géré par
     * le résultat de validateToken via Spring Cache.
     */
    public Mono<Boolean> isExternalServiceAvailable() {
        return webClient
            .get()
            .uri(authApiUrl + "/health")
            .retrieve()
            .toBodilessEntity()
            .timeout(Duration.ofMillis(connectivityTimeoutMs))
            .map(r -> {
                log.debug("[AUTH] Service externe disponible");
                return true;
            })
            .onErrorResume(e -> {
                log.warn("[AUTH] Service externe indisponible : {}", e.getMessage());
                return Mono.just(false);
            });
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Mono<AuthValidationResponse> validateTokenExternal(String token) {
        return webClient
            .post()
            .uri(authApiUrl + "/validate")
            .header("Authorization", "Bearer " + token)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .timeout(Duration.ofMillis(timeout))
            .map(this::parseAuthResponse)
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

    private AuthController.LoginResponse parseLoginResponse(JsonNode json) {
        var payload = new AuthController.LoginResponse.UserPayload();
        JsonNode userNode = json.has("user") ? json.get("user") : json;
        payload.setId(userNode.path("id").asText());
        payload.setEmail(userNode.path("email").asText());
        payload.setFirstName(userNode.path("firstName").asText());
        payload.setLastName(userNode.path("lastName").asText());
        payload.setOrganizationId(userNode.path("organizationId").asText());
        String rolesStr = userNode.path("roles").asText("");
        payload.setRoles(rolesStr.isBlank() ? new String[]{} : rolesStr.split(","));

        var resp = new AuthController.LoginResponse();
        resp.setToken(json.path("token").asText());
        resp.setUser(payload);
        return resp;
    }

    // ─── Méthodes utilitaires conservées ─────────────────────────────────────

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
