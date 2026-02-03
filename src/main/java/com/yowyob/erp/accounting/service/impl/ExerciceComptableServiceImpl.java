package com.yowyob.erp.accounting.service.impl;

import com.yowyob.erp.accounting.dto.ExerciceComptableDto;
import com.yowyob.erp.accounting.entity.ExerciceComptable;
import com.yowyob.erp.accounting.repository.ExerciceComptableRepository;
import com.yowyob.erp.accounting.service.ClotureAnnuelleService;
import com.yowyob.erp.accounting.service.ExerciceComptableService;
import com.yowyob.erp.common.exception.BusinessException;
import com.yowyob.erp.common.exception.ResourceNotFoundException;
import com.yowyob.erp.accounting.entity.ExerciceStatut;
import com.yowyob.erp.accounting.repository.PeriodeComptableRepository;
import com.yowyob.erp.config.tenant.ReactiveTenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Reactive Implementation of ExerciceComptableService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExerciceComptableServiceImpl implements ExerciceComptableService {

        private final ExerciceComptableRepository exercice_repository;
        private final PeriodeComptableRepository periode_repository;
        private final ClotureAnnuelleService cloture_annuelle_service;

        @Override
        @Transactional
        public Mono<ExerciceComptableDto> createExercice(ExerciceComptableDto exercice_dto) {
                log.info("Starting createExercice for code: {}", exercice_dto.getCode());
                return ReactiveTenantContext.getTenantId()
                                .doOnNext(tid -> log.info("TenantId resolved: {}", tid))
                                .switchIfEmpty(Mono.defer(() -> {
                                        log.error("Tenant ID NOT RESOLVED in createExercice");
                                        return Mono.empty();
                                }))
                                .flatMap(tenant_id -> ReactiveTenantContext.getCurrentUser().defaultIfEmpty("system")
                                                .doOnNext(u -> log.info("User resolved: {}", u))
                                                .flatMap(user -> {
                                                        log.info("Creating new fiscal year '{}' for tenant {}",
                                                                        exercice_dto.getCode(), tenant_id);

                                                        return validateDates(exercice_dto)
                                                                        .doOnSuccess(v -> log
                                                                                        .info("Date validation passed"))
                                                                        .doOnError(e -> log.error(
                                                                                        "Date validation failed: {}",
                                                                                        e.getMessage()))
                                                                        .then(checkOverlap(exercice_dto, tenant_id,
                                                                                        null))
                                                                        .doOnSuccess(v -> log
                                                                                        .info("Overlap check passed"))
                                                                        .doOnError(e -> log.error(
                                                                                        "Overlap check failed: {}",
                                                                                        e.getMessage()))
                                                                        .then(Mono.defer(() -> {
                                                                                log.info("Building entity...");
                                                                                ExerciceComptable exercice = ExerciceComptable
                                                                                                .builder()
                                                                                                .tenantId(tenant_id)
                                                                                                .code(exercice_dto
                                                                                                                .getCode())
                                                                                                .libelle(exercice_dto
                                                                                                                .getLibelle())
                                                                                                .date_debut(exercice_dto
                                                                                                                .getDate_debut())
                                                                                                .date_fin(exercice_dto
                                                                                                                .getDate_fin())
                                                                                                .cloture(false)
                                                                                                .statut(ExerciceStatut.OUVERT)
                                                                                                .actif(true)
                                                                                                .created_at(LocalDateTime
                                                                                                                .now())
                                                                                                .updated_at(LocalDateTime
                                                                                                                .now())
                                                                                                .created_by(user)
                                                                                                .updated_by(user)
                                                                                                .build();

                                                                                return exercice_repository
                                                                                                .save(exercice)
                                                                                                .doOnSuccess(saved -> log
                                                                                                                .info("Entity saved: {}",
                                                                                                                                saved.getId()))
                                                                                                .map(this::mapToDto);
                                                                        }));
                                                }));
        }

        @Override
        public Mono<ExerciceComptableDto> getExercice(UUID id) {
                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenant_id -> exercice_repository.findById(id)
                                                .filter(e -> tenant_id.equals(e.getTenantId()))
                                                .map(this::mapToDto)
                                                .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                                                                "ExerciceComptable", id.toString()))));
        }

        @Override
        public Mono<List<ExerciceComptableDto>> getAllExercices() {
                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenant_id -> exercice_repository.findByTenantId(tenant_id)
                                                .map(this::mapToDto)
                                                .collectList());
        }

        @Override
        public Mono<ExerciceComptableDto> getActiveExerciceForDate(LocalDate date) {
                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenant_id -> exercice_repository.findActiveForDate(tenant_id, date)
                                                .map(this::mapToDto)
                                                .switchIfEmpty(
                                                                Mono.error(new BusinessException(
                                                                                "No active fiscal year found for date "
                                                                                                + date))));
        }

        @Override
        @Transactional
        public Mono<ExerciceComptableDto> updateExercice(UUID id, ExerciceComptableDto exercice_dto) {
                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenant_id -> ReactiveTenantContext.getCurrentUser().defaultIfEmpty("system")
                                                .flatMap(user -> exercice_repository.findById(id)
                                                                .filter(e -> tenant_id.equals(e.getTenantId()))
                                                                .switchIfEmpty(
                                                                                Mono.error(new ResourceNotFoundException(
                                                                                                "ExerciceComptable",
                                                                                                id.toString())))
                                                                .flatMap(exercice -> {
                                                                        if (Boolean.TRUE.equals(
                                                                                        exercice.getCloture())) {
                                                                                return Mono.error(new BusinessException(
                                                                                                "Cannot update a closed fiscal year."));
                                                                        }

                                                                        return validateDates(exercice_dto)
                                                                                        .then(checkOverlap(exercice_dto,
                                                                                                        tenant_id, id))
                                                                                        .then(Mono.defer(() -> {
                                                                                                exercice.setCode(
                                                                                                                exercice_dto.getCode());
                                                                                                exercice.setLibelle(
                                                                                                                exercice_dto.getLibelle());
                                                                                                exercice.setDate_debut(
                                                                                                                exercice_dto.getDate_debut());
                                                                                                exercice.setDate_fin(
                                                                                                                exercice_dto.getDate_fin());
                                                                                                exercice.setUpdated_at(
                                                                                                                LocalDateTime.now());
                                                                                                exercice.setUpdated_by(
                                                                                                                user);
                                                                                                exercice.setNotNew();

                                                                                                return exercice_repository
                                                                                                                .save(exercice)
                                                                                                                .map(this::mapToDto);
                                                                                        }));
                                                                })));
        }

        @Override
        @Transactional
        public Mono<Void> closeExercice(UUID id) {
                return ReactiveTenantContext.getTenantId()
                                .doOnSubscribe(s -> log.info("Service: closeExercice called for ID: {}", id))
                                .doOnNext(uuid -> log.info("Service: Content Tenant ID found: {}", uuid))
                                .switchIfEmpty(Mono.defer(() -> {
                                        log.error("Service: Tenant ID is EMPTY in Reactive Context!");
                                        return Mono.error(new BusinessException("Tenant ID missing in context"));
                                }))
                                .flatMap(tenant_id -> ReactiveTenantContext.getCurrentUser().defaultIfEmpty("system")
                                                .flatMap(user -> exercice_repository.findById(id)
                                                                .filter(e -> tenant_id.equals(e.getTenantId()))
                                                                .switchIfEmpty(
                                                                                Mono.error(new ResourceNotFoundException(
                                                                                                "ExerciceComptable",
                                                                                                id.toString())))
                                                                .flatMap(exercice -> {
                                                                        log.info("Closing fiscal year: {} / Tenant: {}",
                                                                                        id, tenant_id);
                                                                        log.info("Before update - Cloture: {}",
                                                                                        exercice.getCloture());

                                                                        exercice.setCloture(true);
                                                                        exercice.setUpdated_at(LocalDateTime.now());
                                                                        exercice.setUpdated_by(user);
                                                                        exercice.setNotNew();

                                                                        return cloture_annuelle_service
                                                                                        .executerCloture(id)
                                                                                        .then(exercice_repository
                                                                                                        .save(exercice))
                                                                                        .doOnSuccess(saved -> log.info(
                                                                                                        "After save - Cloture: {}",
                                                                                                        saved.getCloture()))
                                                                                        .doOnError(e -> log.error(
                                                                                                        "Error saving fiscal year",
                                                                                                        e))
                                                                                        .then();
                                                                })));
        }

        @Override
        @Transactional
        public Mono<Void> deleteExercice(UUID id) {
                return ReactiveTenantContext.getTenantId()
                                .doOnSubscribe(s -> log.info("Attempting to delete fiscal year {}", id))
                                .switchIfEmpty(Mono.defer(() -> {
                                        log.error("Tenant ID not found in context for delete operation");
                                        return Mono.error(new BusinessException("Tenant context missing"));
                                }))
                                .flatMap(tenant_id -> exercice_repository.findById(id)
                                                .filter(e -> tenant_id.equals(e.getTenantId()))
                                                .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                                                                "ExerciceComptable", id.toString())))
                                                .flatMap(exercice -> {
                                                        if (Boolean.TRUE.equals(exercice.getCloture())) {
                                                                return Mono.error(new BusinessException(
                                                                                "Cannot delete a closed fiscal year."));
                                                        }
                                                        return exercice_repository.delete(exercice);
                                                }));
        }

        @Override
        @Transactional
        public Mono<Void> deactivateExercice(UUID id) {
                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenant_id -> exercice_repository.findById(id)
                                                .filter(e -> tenant_id.equals(e.getTenantId()))
                                                .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                                                                "ExerciceComptable", id.toString())))
                                                .flatMap(exercice -> {
                                                        exercice.setActif(false);
                                                        exercice.setUpdated_at(LocalDateTime.now());
                                                        return ReactiveTenantContext.getCurrentUser()
                                                                        .defaultIfEmpty("system")
                                                                        .flatMap(user -> {
                                                                                exercice.setUpdated_by(user);
                                                                                exercice.setNotNew();
                                                                                return exercice_repository
                                                                                                .save(exercice);
                                                                        });
                                                }))
                                .then();
        }

        @Override
        public Mono<List<com.yowyob.erp.accounting.dto.PeriodeComptableDto>> getPeriodesByExercice(UUID exerciceId) {
                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenant_id -> exercice_repository.findById(exerciceId)
                                                .filter(e -> tenant_id.equals(e.getTenantId()))
                                                .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                                                                "ExerciceComptable", exerciceId.toString())))
                                                .thenMany(periode_repository.findByExerciceId(exerciceId))
                                                .map(p -> com.yowyob.erp.accounting.dto.PeriodeComptableDto.builder()
                                                                .id(p.getId())
                                                                .code(p.getCode())
                                                                .date_debut(p.getDate_debut())
                                                                .date_fin(p.getDate_fin())
                                                                .cloturee(p.getCloturee())
                                                                .date_cloture(p.getDate_cloture())
                                                                .notes(p.getNotes())
                                                                .created_at(p.getCreated_at())
                                                                .updated_at(p.getUpdated_at())
                                                                .updated_by(p.getUpdated_by())
                                                                .exercice_id(p.getExerciceId())
                                                                .build())
                                                .collectList());
        }

        private Mono<Void> validateDates(ExerciceComptableDto dto) {
                if (dto.getDate_debut().isAfter(dto.getDate_fin())) {
                        return Mono.error(new BusinessException("Start date cannot be after end date."));
                }
                return Mono.empty();
        }

        private Mono<Void> checkOverlap(ExerciceComptableDto dto, UUID tenant_id, UUID current_id) {
                return exercice_repository.findByTenantId(tenant_id)
                                .filter(e -> current_id == null || !e.getId().equals(current_id))
                                .filter(e -> (dto.getDate_debut().isBefore(e.getDate_fin())
                                                && dto.getDate_fin().isAfter(e.getDate_debut()))
                                                || dto.getDate_debut().equals(e.getDate_debut())
                                                || dto.getDate_fin().equals(e.getDate_fin()))
                                .next()
                                .flatMap(e -> Mono.error(new BusinessException(
                                                "The fiscal year dates overlap with an existing fiscal year: "
                                                                + e.getCode())))
                                .then(Mono.empty());
        }

        private ExerciceComptableDto mapToDto(ExerciceComptable entity) {
                return ExerciceComptableDto.builder()
                                .id(entity.getId())
                                .tenant_id(entity.getTenantId())
                                .code(entity.getCode())
                                .libelle(entity.getLibelle())
                                .date_debut(entity.getDate_debut())
                                .date_fin(entity.getDate_fin())
                                .cloture(entity.getCloture())
                                .statut(entity.getStatut() != null ? entity.getStatut().name() : null)
                                .actif(entity.getActif())
                                .created_at(entity.getCreated_at())
                                .updated_at(entity.getUpdated_at())
                                .created_by(entity.getCreated_by())
                                .build();
        }
}
