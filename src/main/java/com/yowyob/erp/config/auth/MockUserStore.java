package com.yowyob.erp.config.auth;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stockage en mémoire des utilisateurs et tokens mock.
 * Actif uniquement lorsque le service d'authentification externe est indisponible.
 */
@Component
@Slf4j
public class MockUserStore {

    @Getter
    public static class MockUser {
        private final String userId;
        private final String email;
        private final String password;
        private final String firstName;
        private final String lastName;
        private final String organizationId;
        private final String[] roles;

        public MockUser(String userId, String email, String password,
                        String firstName, String lastName,
                        String organizationId, String... roles) {
            this.userId = userId;
            this.email = email;
            this.password = password;
            this.firstName = firstName;
            this.lastName = lastName;
            this.organizationId = organizationId;
            this.roles = roles;
        }
    }

    // Utilisateurs de test prédéfinis
    private final List<MockUser> mockUsers = List.of(
        new MockUser(
            "mock-user-admin-001",
            "admin@ksm.dev", "admin123",
            "Admin", "KSM",
            "mock-org-001",
            "ADMIN", "RESPONSABLE_COMPTABLE"
        ),
        new MockUser(
            "mock-user-comptable-001",
            "comptable@ksm.dev", "comptable123",
            "Jean", "Dupont",
            "mock-org-001",
            "COMPTABLE"
        ),
        new MockUser(
            "mock-user-aide-001",
            "aide@ksm.dev", "aide123",
            "Marie", "Martin",
            "mock-org-001",
            "AIDE_COMPTABLE"
        ),
        new MockUser(
            "mock-user-daf-001",
            "daf@ksm.dev", "daf123",
            "Paul", "Bernard",
            "mock-org-001",
            "RESPONSABLE_COMPTABLE"
        )
    );

    // Map token → utilisateur (tokens émis lors des logins mock)
    private final Map<String, MockUser> issuedTokens = new ConcurrentHashMap<>();

    /**
     * Authentifie un utilisateur mock par email/password.
     * Retourne le token généré si les credentials sont valides.
     */
    public Optional<String> authenticate(String email, String password) {
        return mockUsers.stream()
            .filter(u -> u.getEmail().equalsIgnoreCase(email) && u.getPassword().equals(password))
            .findFirst()
            .map(user -> {
                String token = "mock-" + UUID.randomUUID().toString().replace("-", "");
                issuedTokens.put(token, user);
                log.warn("[MOCK AUTH] Login mock pour {} ({})", email, String.join(", ", user.getRoles()));
                return token;
            });
    }

    /**
     * Valide un token mock et retourne l'AuthValidationResponse correspondant.
     */
    public AuthValidationResponse validateToken(String token) {
        MockUser user = issuedTokens.get(token);
        if (user == null) {
            return AuthValidationResponse.invalid();
        }
        return AuthValidationResponse.builder()
            .valid(true)
            .userId(user.getUserId())
            .organizationId(user.getOrganizationId())
            .roles(user.getRoles())
            .build();
    }

    /**
     * Retourne un utilisateur mock par son userId (pour /api/auth/me).
     */
    public Optional<MockUser> findByToken(String token) {
        return Optional.ofNullable(issuedTokens.get(token));
    }

    public List<MockUser> getAllUsers() {
        return mockUsers;
    }
}
