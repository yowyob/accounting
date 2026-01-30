package com.yowyob.erp.accounting.serviceInitialization;

import com.yowyob.erp.accounting.entity.ExerciceComptable;
import com.yowyob.erp.accounting.repository.ExerciceComptableRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Reactive Service to initialize default Fiscal Years (Exercice Comptable).
 */
@Service
@Order(1)
@Slf4j
public class ExerciceComptableInitializationService implements CommandLineRunner {

    private final ExerciceComptableRepository exercice_repository;
    private final UUID tenant_id;

    public ExerciceComptableInitializationService(
            ExerciceComptableRepository exercice_repository,
            @Value("${app.tenant.default-tenant:550e8400-e29b-41d4-a716-446655440000}") String tenant_id_str) {
        this.exercice_repository = exercice_repository;
        this.tenant_id = UUID.fromString(tenant_id_str);
    }

    @Override
    public void run(String... args) {
        createExerciceIfNotExists("2026", "Exercice 2026",
                LocalDate.of(2026, 1, 4), LocalDate.of(2026, 12, 31))
                .subscribe(
                        v -> log.info("✅ Initialization of fiscal year complete"),
                        e -> log.error("❌ Error initializing fiscal year", e));
    }

    private Mono<Void> createExerciceIfNotExists(String code, String libelle, LocalDate start, LocalDate end) {
        return exercice_repository.findByTenantIdAndCode(tenant_id, code)
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("Creating initial fiscal year '{}' for tenant {}", code, tenant_id);
                    ExerciceComptable exercice = ExerciceComptable.builder()
                            .tenantId(tenant_id)
                            .code(code)
                            .libelle(libelle)
                            .date_debut(start)
                            .date_fin(end)
                            .cloture(false)
                            .created_at(LocalDateTime.now())
                            .updated_at(LocalDateTime.now())
                            .created_by("system")
                            .updated_by("system")
                            .build();
                    return exercice_repository.save(exercice);
                }))
                .then();
    }
}
