package com.yowyob.erp.accounting.infrastructure.initialization;

import com.yowyob.erp.accounting.domain.model.ExerciceComptable;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.ExerciceComptableRepository;
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
@Order(3)
@Slf4j
public class ExerciceComptableInitializationService implements CommandLineRunner {

    private final ExerciceComptableRepository exercice_repository;
    private final UUID organization_id;

    public ExerciceComptableInitializationService(
            ExerciceComptableRepository exercice_repository,
            @Value("${app.organization.default-organization:4e177ff2-89b8-4d24-926a-5763dfa1b19a}") String organization_id_str) {
        this.exercice_repository = exercice_repository;
        this.organization_id = UUID.fromString(organization_id_str);
    }

    @Override
    public void run(String... args) {
        int currentYear = LocalDate.now().getYear();
        String yearStr = String.valueOf(currentYear);
        createExerciceIfNotExists(yearStr, "Exercice " + yearStr,
                LocalDate.of(currentYear, 1, 1), LocalDate.of(currentYear, 12, 31))
                .doOnSuccess(v -> log.info("✅ Initialization of fiscal year {} complete", yearStr))
                .doOnError(e -> log.error("❌ Error initializing fiscal year {}", yearStr, e))
                .block();
    }

    private Mono<Void> createExerciceIfNotExists(String code, String libelle, LocalDate start, LocalDate end) {
        return exercice_repository.findByOrganizationIdAndCode(organization_id, code)
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("Creating initial fiscal year '{}' for organization {}", code, organization_id);
                    ExerciceComptable exercice = ExerciceComptable.builder()
                            .organizationId(organization_id)
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
