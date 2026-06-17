package com.yowyob.erp.config.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Service d'authentification qui valide les JWT émis par KSM_Kernel_Layer.
 *
 * Stratégie :
 *  1. Au démarrage, récupère la clé publique RSA du Kernel via GET /.well-known/jwks.json
 *  2. Valide chaque JWT localement (RS256 + expiration + issuer + audience)
 *  3. Extrait les claims : sub (userId), tid (tenantId), oid (organizationId), permissions
 *  4. Pour le login, délègue à POST /api/auth/login du Kernel
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final String KERNEL_AUDIENCE = "iwm-api";

    private final WebClient webClient;

    @Value("${auth.api.url}")
    private String authApiUrl;

    @Value("${auth.api.health-url:${auth.api.url}/actuator/health}")
    private String authApiHealthUrl;

    @Value("${auth.api.timeout:5000}")
    private int timeout;

    @Value("${auth.connectivity-timeout-ms:2000}")
    private int connectivityTimeoutMs;

    @Value("${auth.jwt.issuer:}")
    private String expectedIssuer;

    @Value("${auth.api.default-tenant-id:}")
    private String defaultTenantId;

    // Identification de l'application cliente backend auprès du Kernel.
    // Le Kernel (notamment en prod) exige X-Client-Id + X-Api-Key sur /api/**.
    @Value("${kernel.client.id:}")
    private String kernelClientId;

    @Value("${kernel.client.secret:}")
    private String kernelClientSecret;

    // Clé publique RSA du Kernel — chargée au démarrage et rechargeable
    private final AtomicReference<RSASSAVerifier> kernelVerifier = new AtomicReference<>();

    // ─── Initialisation — chargement de la clé publique du Kernel ─────────────

    @PostConstruct
    public void loadKernelPublicKey() {
        fetchKernelPublicKey()
            .doOnSuccess(verifier -> {
                kernelVerifier.set(verifier);
                log.info("[AUTH] Clé publique RS256 du Kernel chargée depuis {}/.well-known/jwks.json", authApiUrl);
            })
            .doOnError(e -> log.warn("[AUTH] Impossible de charger la clé publique du Kernel : {} — les tokens seront rejetés tant qu'elle n'est pas chargée",
                e.getMessage()))
            .onErrorResume(e -> Mono.empty())
            .subscribe();
    }

    private Mono<RSASSAVerifier> fetchKernelPublicKey() {
        return webClient
            .get()
            .uri(authApiUrl + "/.well-known/jwks.json")
            .retrieve()
            .bodyToMono(String.class)
            .timeout(Duration.ofMillis(connectivityTimeoutMs))
            .map(json -> {
                try {
                    JWKSet jwkSet = JWKSet.parse(json);
                    RSAKey rsaKey = (RSAKey) jwkSet.getKeys().get(0);
                    return new RSASSAVerifier((RSAPublicKey) rsaKey.toPublicKey());
                } catch (Exception e) {
                    throw new IllegalStateException("Impossible de parser la clé JWK du Kernel", e);
                }
            });
    }

    // ─── Validation de token JWT (locale) ─────────────────────────────────────

    /**
     * Valide un JWT émis par KSM_Kernel_Layer.
     *
     * Validation locale RS256 avec la clé publique du Kernel (chargée au démarrage,
     * rechargée à la volée si absente). Aucun token n'est accepté sans clé publique.
     *
     * Résultat mis en cache (clé = token) pour éviter un appel par requête.
     */
    @Cacheable(value = "jwt-validation", key = "#token")
    public Mono<AuthValidationResponse> validateToken(String token) {
        RSASSAVerifier verifier = kernelVerifier.get();
        if (verifier != null) {
            // Validation locale RS256 — aucun appel réseau
            return Mono.fromCallable(() -> validateJwtLocally(token, verifier));
        }

        // Clé publique non chargée : tenter de la récupérer maintenant
        return fetchKernelPublicKey()
            .doOnSuccess(v -> kernelVerifier.set(v))
            .map(v -> validateJwtLocally(token, v))
            .onErrorResume(e -> {
                log.warn("[AUTH] Clé publique Kernel indisponible — token rejeté : {}", e.getMessage());
                return Mono.just(AuthValidationResponse.invalid());
            });
    }

    /**
     * Valide le JWT localement avec la clé publique RSA du Kernel.
     * Claims attendus (cf. JwtTokenService du Kernel) :
     *   - sub   : userId (UUID)
     *   - tid   : tenantId (UUID)
     *   - oid   : organizationId (UUID, optionnel)
     *   - actor : actorId (UUID, optionnel)
     *   - permissions : List<String>
     *   - aud   : ["iwm-api"]
     */
    private AuthValidationResponse validateJwtLocally(String token, RSASSAVerifier verifier) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);

            // Vérification de la signature RS256
            if (!jwt.verify(verifier)) {
                log.debug("[AUTH] JWT : signature invalide");
                return AuthValidationResponse.invalid();
            }

            JWTClaimsSet claims = jwt.getJWTClaimsSet();

            // Vérification de l'expiration
            if (claims.getExpirationTime() == null ||
                claims.getExpirationTime().toInstant().isBefore(Instant.now())) {
                log.debug("[AUTH] JWT expiré");
                return AuthValidationResponse.invalid();
            }

            // Vérification de l'audience
            List<String> audience = claims.getAudience();
            if (audience == null || !audience.contains(KERNEL_AUDIENCE)) {
                log.debug("[AUTH] JWT : audience invalide {}", audience);
                return AuthValidationResponse.invalid();
            }

            String configuredIssuer = normalize(expectedIssuer);
            if (configuredIssuer != null && !Objects.equals(claims.getIssuer(), configuredIssuer)) {
                log.debug("[AUTH] JWT : issuer invalide {} (attendu {})", claims.getIssuer(), configuredIssuer);
                return AuthValidationResponse.invalid();
            }

            // Extraction des claims
            String userId         = claims.getSubject();
            String tenantId       = claims.getStringClaim("tid");
            String organizationId = claims.getStringClaim("oid");

            // Extraction des permissions → utilisées comme roles dans le BACKEND
            Object rawPerms = claims.getClaim("permissions");
            String[] roles = new String[0];
            if (rawPerms instanceof List<?> permList) {
                roles = permList.stream()
                    .filter(p -> p instanceof String)
                    .map(Object::toString)
                    .toArray(String[]::new);
            }

            log.debug("[AUTH] JWT valide — userId={} tenantId={} orgId={} permissions={}",
                userId, tenantId, organizationId, Arrays.toString(roles));

            return AuthValidationResponse.builder()
                .valid(true)
                .userId(userId)
                .tenantId(tenantId != null ? tenantId : "")
                .organizationId(organizationId != null ? organizationId : "")
                .roles(roles)
                .build();

        } catch (ParseException e) {
            log.debug("[AUTH] JWT non parseable : {}", e.getMessage());
            return AuthValidationResponse.invalid();
        } catch (Exception e) {
            log.warn("[AUTH] Erreur inattendue lors de la validation JWT : {}", e.getMessage());
            return AuthValidationResponse.invalid();
        }
    }

    // ─── Login (délégué au Kernel — POST /api/auth/login) ────────────────────

    /**
     * Délègue le login au Kernel via le vrai endpoint /api/auth/login.
     * Le Kernel retourne un token JWT RS256 + infos utilisateur.
     */
    public Mono<AuthController.LoginResponse> loginExternal(String email, String password, String tenantId) {
        String resolvedTenantId = resolveTenantId(tenantId);
        log.info("[AUTH] Calling external Kernel login: email={}, tenantId={}, resolvedTenantId={}, authApiUrl={}", email, tenantId, resolvedTenantId, authApiUrl);
        return webClient
            .post()
            .uri(authApiUrl + "/api/auth/login")
            .header("X-Tenant-Id", resolvedTenantId)
            .headers(this::addClientCredentials)
            .bodyValue(Map.of("principal", email, "password", password))
            .retrieve()
            .bodyToMono(JsonNode.class)
            .timeout(Duration.ofMillis(timeout))
            .map(this::parseKernelLoginResponse)
            .doOnError(e -> log.error("[AUTH] Erreur login Kernel : {}", e.getMessage()));
    }

    // ─── Récupération du profil utilisateur (GET /api/users/me) ──────────────

    /**
     * Récupère le profil de l'utilisateur courant depuis le Kernel.
     * Utilise le bearer token de la requête originale.
     */
    @Cacheable(value = "user-info", key = "#token")
    public Mono<UserInfo> getUserInfo(String userEmail, String token) {
        return webClient
            .get()
            .uri(authApiUrl + "/api/users/me")
            .header("Authorization", "Bearer " + token)
            .headers(this::addClientCredentials)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .timeout(Duration.ofMillis(timeout))
            .map(this::parseUserInfo)
            .doOnError(e -> log.error("[AUTH] Erreur récupération profil utilisateur : {}", e.getMessage()));
    }

    // ─── Vérification disponibilité du Kernel ─────────────────────────────────

    /**
     * Vérifie si le Kernel est disponible via son actuator health.
     */
    public Mono<Boolean> isExternalServiceAvailable() {
        return webClient
            .get()
            .uri(authApiHealthUrl)
            .retrieve()
            .toBodilessEntity()
            .timeout(Duration.ofMillis(connectivityTimeoutMs))
            .map(r -> {
                log.debug("[AUTH] Kernel disponible");
                return true;
            })
            .onErrorResume(e -> {
                log.warn("[AUTH] Kernel indisponible : {}", e.getMessage());
                return Mono.just(false);
            });
    }

    // ─── getOrganizationMembers — non disponible dans le Kernel ──────────────
    // Le Kernel n'expose pas d'endpoint /employees.
    // Cette méthode retourne une liste vide en attendant une implémentation métier.
    public Flux<OrganizationMember> getOrganizationMembers(UUID organizationId) {
        log.debug("[AUTH] getOrganizationMembers non implémenté côté Kernel — retourne liste vide pour org={}",
            organizationId);
        return Flux.empty();
    }

    // ─── Parsers de réponses Kernel ───────────────────────────────────────────

    /**
     * Parse la réponse du Kernel au format :
     * {
     *   "data": {
     *     "token": "eyJ...",
     *     "user": { "id": "...", "email": "...", ... }
     *   }
     * }
     */
    private AuthController.LoginResponse parseKernelLoginResponse(JsonNode json) {
        // Le Kernel enveloppe les réponses dans { "data": { ... } }
        JsonNode data = json.has("data") ? json.get("data") : json;
        String token = data.path("accessToken").asText(data.path("token").asText(""));

        JsonNode userNode = data.has("user") ? data.get("user") : data;

        var payload = new AuthController.LoginResponse.UserPayload();
        payload.setId(userNode.path("id").asText());
        payload.setEmail(userNode.path("email").asText());
        payload.setFirstName(userNode.path("firstName").asText());
        payload.setLastName(userNode.path("lastName").asText());
        payload.setOrganizationId(userNode.path("organizationId").asText(""));

        // Le Kernel renvoie les rôles dans "authorities" (variantes ROLE_/#SCOPE incluses)
        // et non dans un champ "permissions". On normalise chaque entrée en code de rôle brut
        // attendu par le frontend : "ROLE_COMPTABLE#TENANT" -> "COMPTABLE" (dédupliqué).
        JsonNode rolesNode = userNode.has("authorities") ? userNode.path("authorities") : userNode.path("permissions");
        java.util.Set<String> roleSet = new java.util.LinkedHashSet<>();
        if (rolesNode.isArray()) {
            for (JsonNode node : rolesNode) {
                String code = node.asText("");
                int hash = code.indexOf('#');
                if (hash >= 0) {
                    code = code.substring(0, hash);
                }
                if (code.startsWith("ROLE_")) {
                    code = code.substring("ROLE_".length());
                }
                if (!code.isBlank()) {
                    roleSet.add(code);
                }
            }
        }
        payload.setRoles(roleSet.toArray(new String[0]));

        var resp = new AuthController.LoginResponse();
        resp.setToken(token);
        resp.setUser(payload);
        return resp;
    }

    /**
     * Ajoute les en-têtes d'identification de l'application cliente backend
     * (X-Client-Id / X-Api-Key) requis par le Kernel sur /api/**.
     * N'ajoute rien si les credentials ne sont pas configurés.
     */
    private void addClientCredentials(HttpHeaders headers) {
        String clientId = normalize(kernelClientId);
        String apiKey = normalize(kernelClientSecret);
        if (clientId != null) {
            headers.set("X-Client-Id", clientId);
        }
        if (apiKey != null) {
            headers.set("X-Api-Key", apiKey);
        }
    }

    private String resolveTenantId(String tenantId) {
        String resolved = normalize(tenantId);
        if (resolved != null) {
            log.info("[AUTH] Resolved tenantId from parameter: {}", resolved);
            return resolved;
        }
        resolved = normalize(defaultTenantId);
        if (resolved != null) {
            log.info("[AUTH] Resolved tenantId from defaultTenantId: {}", resolved);
            return resolved;
        }
        log.warn("[AUTH] Failed to resolve tenantId: input tenantId={}, defaultTenantId={}", tenantId, defaultTenantId);
        throw new IllegalArgumentException("tenantId is required for Kernel login");
    }

    private UserInfo parseUserInfo(JsonNode json) {
        JsonNode data = json.has("data") ? json.get("data") : json;
        UserInfo info = new UserInfo();
        // Mapping des champs du Kernel → UserInfo du BACKEND
        // À adapter selon la structure exacte de UserInfo.java
        return info;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
