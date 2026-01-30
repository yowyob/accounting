package com.yowyob.erp.accounting.serviceInitialization;

import com.yowyob.erp.accounting.entity.Agence;
import com.yowyob.erp.accounting.repository.AgenceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service to initialize default Agencies (Agences).
 * 
 * @author ALD
 * @date 03.01.2026
 */
@Service
@Order(1)
@Slf4j
public class AgenceInitializationService implements CommandLineRunner {

    private final AgenceRepository agence_repository;
    private final UUID tenant_id;

    public AgenceInitializationService(
            AgenceRepository agence_repository,
            @Value("${app.tenant.default-tenant:550e8400-e29b-41d4-a716-446655440000}") String tenant_id_str) {
        this.agence_repository = agence_repository;
        this.tenant_id = UUID.fromString(tenant_id_str);
    }

    @Override
    public void run(String... args) {
        createAgenceIfNotExists("HQ", "Headquarters", "Yaounde", "Cameroon")
                .doOnSuccess(agence -> log.info("Default Agency initialized: {}", agence.getName()))
                .doOnError(error -> log.error("Failed to initialize default agency: {}", error.getMessage()))
                .block();
    }

    private Mono<Agence> createAgenceIfNotExists(String code, String name, String city, String country) {
        return agence_repository.findByTenantIdAndCode(tenant_id, code)
                .switchIfEmpty(Mono.defer(() -> {
                    Agence agence = Agence.builder()
                            .tenantId(tenant_id)
                            .code(code)
                            .name(name)
                            .city(city)
                            .country(country)
                            .address("Default Street")
                            .created_at(LocalDateTime.now())
                            .updated_at(LocalDateTime.now())
                            .build();
                    return agence_repository.save(agence);
                }));
    }
}
