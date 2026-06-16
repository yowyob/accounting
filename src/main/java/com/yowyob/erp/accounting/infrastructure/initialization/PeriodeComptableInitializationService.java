package com.yowyob.erp.accounting.infrastructure.initialization;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import com.yowyob.erp.accounting.domain.port.in.PeriodeComptableUseCase;
import com.yowyob.erp.config.organization.ReactiveOrganizationContext;
import reactor.core.publisher.Mono;

/**
 * Reactive Service for initializing default accounting periods for the default
 * organization.
 * Runs on application startup.
 */
@Service
@Order(4)
@Slf4j
@ConditionalOnProperty(name = "app.organization.seed-default", havingValue = "true", matchIfMissing = false)
public class PeriodeComptableInitializationService implements CommandLineRunner {

    private final PeriodeComptableUseCase periode_service;
    private final com.yowyob.erp.accounting.infrastructure.persistence.repository.PeriodeComptableRepository periode_repository;
    private final com.yowyob.erp.accounting.infrastructure.persistence.repository.ExerciceComptableRepository exercice_repository;
    private final com.yowyob.erp.accounting.infrastructure.persistence.repository.EcritureComptableRepository ecriture_repository;
    private final com.yowyob.erp.accounting.infrastructure.persistence.repository.DetailEcritureRepository detail_ecriture_repository;
    private final UUID organization_id;

    public PeriodeComptableInitializationService(PeriodeComptableUseCase periode_service,
            com.yowyob.erp.accounting.infrastructure.persistence.repository.PeriodeComptableRepository periode_repository,
            com.yowyob.erp.accounting.infrastructure.persistence.repository.ExerciceComptableRepository exercice_repository,
            com.yowyob.erp.accounting.infrastructure.persistence.repository.EcritureComptableRepository ecriture_repository,
            com.yowyob.erp.accounting.infrastructure.persistence.repository.DetailEcritureRepository detail_ecriture_repository,
            @Value("${app.organization.default-organization:4e177ff2-89b8-4d24-926a-5763dfa1b19a}") String organization_id_str) {
        this.periode_service = periode_service;
        this.periode_repository = periode_repository;
        this.exercice_repository = exercice_repository;
        this.ecriture_repository = ecriture_repository;
        this.detail_ecriture_repository = detail_ecriture_repository;
        this.organization_id = UUID.fromString(organization_id_str);
    }

    @Override
    public void run(String... args) {
        LocalDate today = LocalDate.now();
        String currentYearStr = String.valueOf(today.getYear());

        detail_ecriture_repository.deleteAllByOrganizationId(organization_id)
                .then(ecriture_repository.deleteAllByOrganizationId(organization_id))
                .then(periode_repository.deleteAllByOrganizationId(organization_id))
                .then(exercice_repository.findByOrganizationIdAndCode(organization_id, currentYearStr))
                .flatMapMany(exercice -> {
                    UUID exercice_id = exercice.getId();

                    // Initializing only one period starting today until the end of the current
                    // month
                    String code = String.format("%d-%02d", today.getYear(), today.getMonthValue());
                    LocalDate start_date = today;
                    LocalDate end_date = today.withDayOfMonth(today.lengthOfMonth());

                    com.yowyob.erp.accounting.domain.model.PeriodeComptable entity = com.yowyob.erp.accounting.domain.model.PeriodeComptable
                            .builder()
                            .id(UUID.randomUUID())
                            .organizationId(organization_id)
                            .exerciceId(exercice_id)
                            .code(code)
                            .date_debut(start_date)
                            .date_fin(end_date)
                            .cloturee(false)
                            .created_at(LocalDateTime.now())
                            .updated_at(LocalDateTime.now())
                            .created_by("system")
                            .updated_by("system")
                            .build();

                    return periode_repository.save(entity)
                            .onErrorResume(e -> {
                                log.warn("Error creating period {}: {}", code, e.getMessage());
                                return Mono.empty();
                            });
                })
                .contextWrite(ReactiveOrganizationContext.withOrganizationId(organization_id))
                .collectList()
                .doOnSuccess(v -> log.info("✅ Initialization of accounting periods complete for {}", currentYearStr))
                .doOnError(e -> log.error("❌ Error initializing periods", e))
                .block();
    }
}