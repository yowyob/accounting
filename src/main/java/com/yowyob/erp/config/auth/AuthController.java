package com.yowyob.erp.config.auth;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
     * En cas d'échec du Kernel, renvoie 502 Bad Gateway.
     */
    @PostMapping("/login")
    public Mono<ResponseEntity<LoginResponse>> login(@RequestBody LoginRequest request) {
        return authService.loginExternal(request.getEmail(), request.getPassword(), request.getTenantId())
            .map(ResponseEntity::ok)
            .onErrorResume(e -> {
                log.error("[AUTH] Échec du login Kernel : {}", e.getMessage());
                return Mono.just(ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(null));
            });
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
