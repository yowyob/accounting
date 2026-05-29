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

import java.util.Arrays;
import java.util.Map;

/**
 * Contrôleur d'authentification exposé sur /api/auth/*.
 * Tente d'abord le service externe ; bascule sur les mock data si indisponible.
 * Endpoints permis sans token (voir SecurityConfig).
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final MockUserStore mockUserStore;

    // ─── DTOs ────────────────────────────────────────────────────────────────

    @Data
    public static class LoginRequest {
        private String email;
        private String password;
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
     * Authentifie un utilisateur.
     * 1. Vérifie la disponibilité du service externe.
     * 2. Si disponible  → délègue au service externe.
     * 3. Si indisponible → authentifie via MockUserStore.
     */
    @PostMapping("/login")
    public Mono<ResponseEntity<LoginResponse>> login(@RequestBody LoginRequest request) {
        return authService.isExternalServiceAvailable()
            .flatMap(available -> {
                if (available) {
                    return loginViaExternalService(request);
                } else {
                    log.warn("[MOCK AUTH] Service externe indisponible — utilisation des mock data");
                    return Mono.just(loginViaMock(request));
                }
            });
    }

    // ─── GET /api/auth/health ─────────────────────────────────────────────────

    /**
     * Indique si le service d'authentification externe est joignable.
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, Object>>> health() {
        return authService.isExternalServiceAvailable()
            .map(available -> ResponseEntity.ok(Map.of(
                "externalAuthAvailable", available,
                "mode", available ? "external" : "mock",
                "mockUsersCount", mockUserStore.getAllUsers().size()
            )));
    }

    // ─── GET /api/auth/mock-users ─────────────────────────────────────────────

    /**
     * Liste les comptes mock disponibles (utile en développement).
     * Ne renvoie jamais les mots de passe.
     */
    @GetMapping("/mock-users")
    public ResponseEntity<?> listMockUsers() {
        var users = mockUserStore.getAllUsers().stream().map(u -> Map.of(
            "email", u.getEmail(),
            "roles", Arrays.asList(u.getRoles()),
            "firstName", u.getFirstName(),
            "lastName", u.getLastName(),
            "hint", "password = " + u.getPassword().charAt(0) + "***"
        )).toList();
        return ResponseEntity.ok(Map.of("mockUsers", users));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Mono<ResponseEntity<LoginResponse>> loginViaExternalService(LoginRequest request) {
        return authService.loginExternal(request.getEmail(), request.getPassword())
            .map(ResponseEntity::ok)
            .onErrorResume(e -> {
                log.warn("[AUTH] Échec service externe ({}), bascule mock", e.getMessage());
                return Mono.just(loginViaMock(request));
            });
    }

    private ResponseEntity<LoginResponse> loginViaMock(LoginRequest request) {
        var tokenOpt = mockUserStore.authenticate(request.getEmail(), request.getPassword());

        if (tokenOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = tokenOpt.get();
        MockUserStore.MockUser user = mockUserStore.findByToken(token).orElseThrow();

        var payload = new LoginResponse.UserPayload();
        payload.setId(user.getUserId());
        payload.setEmail(user.getEmail());
        payload.setFirstName(user.getFirstName());
        payload.setLastName(user.getLastName());
        payload.setOrganizationId(user.getOrganizationId());
        payload.setRoles(user.getRoles());

        var response = new LoginResponse();
        response.setToken(token);
        response.setUser(payload);

        return ResponseEntity.ok(response);
    }
}
