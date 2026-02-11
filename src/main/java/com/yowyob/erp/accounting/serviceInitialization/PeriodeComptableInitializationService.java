package com.yowyob.erp.accounting.serviceInitialization;

import java.time.LocalDate;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import com.yowyob.erp.accounting.dto.PeriodeComptableDto;
import com.yowyob.erp.accounting.service.PeriodeComptableService;
import com.yowyob.erp.config.organization.ReactiveOrganizationContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive Service for initializing default accounting periods for the default
 * organization.
 * Runs on application startup.
 */
@Service
@Order(2)
@Slf4j
public class PeriodeComptableInitializationService implements CommandLineRunner {

    private final PeriodeComptableService periode_service;
    private final com.yowyob.erp.accounting.repository.ExerciceComptableRepository exercice_repository;
    private final UUID organization_id;

    public PeriodeComptableInitializationService(PeriodeComptableService periode_service,
            com.yowyob.erp.accounting.repository.ExerciceComptableRepository exercice_repository,
            @Value("${app.organization.default-organization:550e8400-e29b-41d4-a716-446655440000}") String organization_id_str) {
        this.periode_service = periode_service;
        this.exercice_repository = exercice_repository;
        this.organization_id = UUID.fromString(organization_id_str);
    }

    @Override
    public void run(String... args) {
        exercice_repository.findByOrganizationIdAndCode(organization_id, "2026")
                .flatMapMany(exercice -> {
                    UUID exercice_id = exercice.getId();
                    return Flux.range(1, 12)
                            .flatMap(month -> {
                                String code = String.format("2026-%02d", month);
                                LocalDate start_date = LocalDate.of(2026, month, 1);
                                LocalDate end_date = start_date.withDayOfMonth(start_date.lengthOfMonth());

                                PeriodeComptableDto dto = PeriodeComptableDto.builder()
                                        .exercice_id(exercice_id)
                                        .code(code)
                                        .date_debut(start_date)
                                        .date_fin(end_date)
                                        .cloturee(false)
                                        .build();

                                return periode_service.createPeriode(dto)
                                        .onErrorResume(e -> {
                                            log.warn("Error creating period {}: {}", code, e.getMessage());
                                            return Mono.empty();
                                        });
                            });
                })
                .contextWrite(ReactiveOrganizationContext.withOrganizationId(organization_id))
                .subscribe(
                        v -> log.debug("Period iteration check: {}", v.getCode()),
                        e -> log.error("❌ Error initializing periods", e),
                        () -> log.info("✅ Initialization of accounting periods complete"));
    }
}