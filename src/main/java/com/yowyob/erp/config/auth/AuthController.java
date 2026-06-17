package com.yowyob.erp.config.auth;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Contrôleur d'authentification exposé sur /api/auth/*.
 * Délègue le login au Kernel (KSM_Kernel_Layer).
 * Endpoints permis sans token (voir SecurityConfig).
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    // ─── DTOs ────────────────────────────────────────────────────────────────

    @Data
    public static class LoginRequest {
        private String email;
        private String password;
        private String tenantId;
    }

    @Data
    public static class LoginResponse {
        private String token;
        private UserPayload user;

        @Data
        public static class UserPayload {
            private String id;
            private String email;
            private String firstName;
            private String lastName;
            private String organizationId;
            private String[] roles;
        }
    }

    // ─── POST /api/auth/login ─────────────────────────────────────────────────

    /**
     * Authentifie un utilisateur en déléguant au Kernel.
     * - identifiants refusés par le Kernel (4xx, ex. 401) → on relaie le statut ;
     * - Kernel injoignable / timeout / erreur interne (5xx) → 502 Bad Gateway.
     */
    @PostMapping("/login")
    public Mono<ResponseEntity<?>> login(@RequestBody LoginRequest request) {
        return authService.loginExternal(request.getEmail(), request.getPassword(), request.getTenantId())
            .<ResponseEntity<?>>map(ResponseEntity::ok)
            .onErrorResume(e -> Mono.just(toLoginError(e)));
    }

    /**
     * Traduit une erreur de login Kernel en réponse HTTP :
     * un refus authentification (4xx) est relayé tel quel (le front peut afficher
     * « identifiants invalides »), tandis qu'une panne réseau/timeout/5xx devient 502.
     */
    private ResponseEntity<?> toLoginError(Throwable e) {
        if (e instanceof WebClientResponseException wcre && wcre.getStatusCode().is4xxClientError()) {
            HttpStatusCode status = wcre.getStatusCode();
            log.warn("[AUTH] Login refusé par le Kernel ({}) : {}", status.value(), wcre.getMessage());
            return ResponseEntity.status(status).body(Map.of(
                "error", "authentication_failed",
                "message", "Identifiants invalides ou requête refusée par le service d'authentification"));
        }

        log.error("[AUTH] Kernel injoignable lors du login : {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
            "error", "kernel_unavailable",
            "message", "Service d'authentification indisponible, réessayez plus tard"));
    }

    // ─── GET /api/auth/health ─────────────────────────────────────────────────

    /**
     * Indique si le service d'authentification externe (Kernel) est joignable.
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, Object>>> health() {
        return authService.isExternalServiceAvailable()
            .map(available -> ResponseEntity.ok(Map.of(
                "externalAuthAvailable", available,
                "mode", available ? "external" : "unavailable"
            )));
    }
}
