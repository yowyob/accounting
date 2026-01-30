package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.dto.JournalAuditDto;
import com.yowyob.erp.accounting.dto.PeriodeComptableDto;
import com.yowyob.erp.accounting.entity.JournalAudit;
import com.yowyob.erp.accounting.entity.PeriodeComptable;
import com.yowyob.erp.accounting.entity.Tenant;
import com.yowyob.erp.accounting.repository.JournalAuditRepository;
import com.yowyob.erp.accounting.repository.PeriodeComptableRepository;
import com.yowyob.erp.common.exception.ResourceNotFoundException;
import com.yowyob.erp.config.kafka.KafkaMessageService;
import com.yowyob.erp.config.redis.RedisService;
import com.yowyob.erp.config.tenant.ReactiveTenantContext;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Reactive Service for managing accounting periods.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PeriodeComptableService {

        private final PeriodeComptableRepository periode_repository;
        private final JournalAuditRepository audit_repository;
        private final com.yowyob.erp.accounting.repository.ExerciceComptableRepository exercice_repository;
        private final Validator validator;
        private final KafkaMessageService kafka_service;
        private final RedisService redis_service;

        private static final String CACHE_ALL = "periodes:all:";
        private static final String CACHE_ACTIVE = "periodes:active:";
        private static final String CACHE_SINGLE = "periode:";
        private static final String CACHE_CURRENT = "periode:current:";

        /**
         * Creates a new accounting period.
         */
        @Transactional
        public Mono<PeriodeComptableDto> createPeriode(PeriodeComptableDto dto) {
                return ReactiveTenantContext.getTenantId()
                                .doOnSubscribe(s -> log.info("Attempting to create period: {}", dto.getCode()))
                                .switchIfEmpty(Mono.defer(() -> {
                                        log.error("Tenant ID missing in reactor context for createPeriode");
                                        return Mono.error(new IllegalStateException("Tenant context missing"));
                                }))
                                .flatMap(tenant_id -> ReactiveTenantContext.getCurrentUser().defaultIfEmpty("system")
                                                .flatMap(user -> {
                                                        log.info("🧾 Creating accounting period [{} - {}] for tenant {}",
                                                                        dto.getCode(),
                                                                        dto.getDate_debut(), tenant_id);

                                                        return validateDto(dto)
                                                                        .then(periode_repository.findByTenant_IdAndCode(
                                                                                        tenant_id, dto.getCode())
                                                                                        .flatMap(p -> Mono.error(
                                                                                                        new IllegalArgumentException(
                                                                                                                        "Period code already exists: "
                                                                                                                                        + dto.getCode()))))
                                                                        .then(validateNoOverlap(tenant_id,
                                                                                        dto.getDate_debut(),
                                                                                        dto.getDate_fin(), null))
                                                                        .then(ReactiveTenantContext
                                                                                        .getCurrentTenantAsTenant())
                                                                        .flatMap(tenant -> {
                                                                                PeriodeComptable entity = mapToEntity(
                                                                                                dto, tenant);
                                                                                entity.setTenantId(tenant.getId());
                                                                                entity.setCreated_at(
                                                                                                LocalDateTime.now());
                                                                                entity.setUpdated_at(
                                                                                                LocalDateTime.now());
                                                                                entity.setCreated_by(user);
                                                                                entity.setUpdated_by(user);

                                                                                return periode_repository.save(entity)
                                                                                                .flatMap(saved -> logAudit(
                                                                                                                tenant,
                                                                                                                user,
                                                                                                                "PERIODE_CREATED",
                                                                                                                "Created period: "
                                                                                                                                + dto.getCode())
                                                                                                                .then(redis_service
                                                                                                                                .delete(CACHE_ALL
                                                                                                                                                + tenant_id))
                                                                                                                .then(redis_service
                                                                                                                                .delete(CACHE_ACTIVE
                                                                                                                                                + tenant_id))
                                                                                                                .thenReturn(mapToDto(
                                                                                                                                saved)));
                                                                        });
                                                }));
        }

        /**
         * Retrieves a period by its ID.
         */
        public Mono<PeriodeComptableDto> getPeriode(UUID id) {
                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenant_id -> {
                                        String cache_key = CACHE_SINGLE + tenant_id + ":" + id;
                                        return redis_service.get(cache_key, PeriodeComptableDto.class)
                                                        .switchIfEmpty(periode_repository
                                                                        .findByTenant_IdAndId(tenant_id, id)
                                                                        .switchIfEmpty(Mono
                                                                                        .error(new ResourceNotFoundException(
                                                                                                        "Accounting period",
                                                                                                        id.toString())))
                                                                        .map(this::mapToDto)
                                                                        .flatMap(dto -> redis_service
                                                                                        .save(cache_key, dto, Duration
                                                                                                        .ofMinutes(15))
                                                                                        .thenReturn(dto)));
                                });
        }

        /**
         * Retrieves all periods for the current tenant.
         */
        @SuppressWarnings("unchecked")
        public Mono<List<PeriodeComptableDto>> getAllPeriodes() {
                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenant_id -> {
                                        String cache_key = CACHE_ALL + tenant_id;
                                        return redis_service.get(cache_key, List.class)
                                                        .map(list -> (List<PeriodeComptableDto>) list)
                                                        .switchIfEmpty(periode_repository
                                                                        .findByTenant_IdOrderByDate_debutDesc(tenant_id)
                                                                        .map(this::mapToDto)
                                                                        .collectList()
                                                                        .flatMap(periodes -> redis_service.save(
                                                                                        cache_key, periodes,
                                                                                        Duration.ofMinutes(10))
                                                                                        .thenReturn(periodes)));
                                });
        }

        /**
         * Retrieves a period by its code.
         */
        public Mono<PeriodeComptableDto> getByCode(String code) {
                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenant_id -> periode_repository.findByTenant_IdAndCode(tenant_id, code)
                                                .map(this::mapToDto));
        }

        /**
         * Retrieves a period by a date within its range.
         */
        public Mono<PeriodeComptableDto> getByDate(LocalDate date) {
                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenant_id -> periode_repository.findByTenant_IdAndDateInRange(tenant_id, date)
                                                .map(this::mapToDto));
        }

        /**
         * Retrieves all non-closed periods for the current tenant.
         */
        @SuppressWarnings("unchecked")
        public Mono<List<PeriodeComptableDto>> getNonClosedPeriodes() {
                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenant_id -> {
                                        String cache_key = CACHE_ACTIVE + tenant_id;
                                        return redis_service.get(cache_key, List.class)
                                                        .map(list -> (List<PeriodeComptableDto>) list)
                                                        .switchIfEmpty(periode_repository
                                                                        .findByTenant_IdAndClotureeFalse(tenant_id)
                                                                        .map(this::mapToDto)
                                                                        .collectList()
                                                                        .flatMap(periodes -> redis_service.save(
                                                                                        cache_key, periodes,
                                                                                        Duration.ofMinutes(10))
                                                                                        .thenReturn(periodes)));
                                });
        }

        /**
         * Retrieves periods within a specific date range.
         */
        public Mono<List<PeriodeComptableDto>> getByRange(LocalDate start, LocalDate end) {
                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenant_id -> periode_repository
                                                .findByTenant_IdAndPeriodRange(tenant_id, start, end)
                                                .map(this::mapToDto)
                                                .collectList());
        }

        /**
         * Retrieves the current open period for a given tenant.
         */
        public Mono<PeriodeComptableDto> getCurrentPeriode(UUID tenant_id) {
                String cache_key = CACHE_CURRENT + tenant_id;
                return redis_service.get(cache_key, PeriodeComptableDto.class)
                                .switchIfEmpty(Mono.defer(() -> {
                                        LocalDate today = LocalDate.now();
                                        return periode_repository.findByTenant_IdAndDateInRange(tenant_id, today)
                                                        .filter(p -> !Boolean.TRUE.equals(p.getCloturee()))
                                                        .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                                                                        "No open accounting period found for the current date")))
                                                        .map(this::mapToDto)
                                                        .flatMap(dto -> {
                                                                log.info("📅 Current accounting period for tenant {} : {}",
                                                                                tenant_id, dto.getCode());
                                                                return redis_service
                                                                                .save(cache_key, dto,
                                                                                                Duration.ofMinutes(30))
                                                                                .thenReturn(dto);
                                                        });
                                }));
        }

        /**
         * Updates an existing accounting period.
         */
        @Transactional
        public Mono<PeriodeComptableDto> updatePeriode(UUID id, PeriodeComptableDto dto) {
                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenant_id -> ReactiveTenantContext.getCurrentUser().defaultIfEmpty("system")
                                                .flatMap(user -> {
                                                        return periode_repository.findByTenant_IdAndId(tenant_id, id)
                                                                        .switchIfEmpty(Mono
                                                                                        .error(new ResourceNotFoundException(
                                                                                                        "Accounting period",
                                                                                                        id.toString())))
                                                                        .flatMap(existing -> {
                                                                                if (Boolean.TRUE.equals(existing
                                                                                                .getCloturee())) {
                                                                                        return Mono
                                                                                                        .error(new IllegalStateException(
                                                                                                                        "Cannot modify a closed period"));
                                                                                }

                                                                                return validateDto(dto)
                                                                                                .then(Mono.defer(() -> {
                                                                                                        if (!existing.getCode()
                                                                                                                        .equals(dto.getCode())) {
                                                                                                                return periode_repository
                                                                                                                                .findByTenant_IdAndCode(
                                                                                                                                                tenant_id,
                                                                                                                                                dto.getCode())
                                                                                                                                .flatMap(p -> Mono
                                                                                                                                                .error(new IllegalArgumentException(
                                                                                                                                                                "Period code already exists: "
                                                                                                                                                                                + dto.getCode())));
                                                                                                        }
                                                                                                        return Mono.empty();
                                                                                                }))
                                                                                                .then(validateNoOverlap(
                                                                                                                tenant_id,
                                                                                                                dto.getDate_debut(),
                                                                                                                dto.getDate_fin(),
                                                                                                                id))
                                                                                                .then(Mono.defer(() -> {
                                                                                                        existing.setCode(
                                                                                                                        dto.getCode());
                                                                                                        existing.setDate_debut(
                                                                                                                        dto.getDate_debut());
                                                                                                        existing.setDate_fin(
                                                                                                                        dto.getDate_fin());
                                                                                                        existing.setNotes(
                                                                                                                        dto.getNotes());
                                                                                                        existing.setUpdated_at(
                                                                                                                        LocalDateTime.now());
                                                                                                        existing.setUpdated_by(
                                                                                                                        user);
                                                                                                        existing.setNotNew();
                                                                                                        return periode_repository
                                                                                                                        .save(existing)
                                                                                                                        .flatMap(saved -> ReactiveTenantContext
                                                                                                                                        .getCurrentTenantAsTenant()
                                                                                                                                        .flatMap(tenant -> logAudit(
                                                                                                                                                        tenant,
                                                                                                                                                        user,
                                                                                                                                                        "PERIODE_UPDATED",
                                                                                                                                                        "Updated period: "
                                                                                                                                                                        + dto.getCode()))
                                                                                                                                        .then(redis_service
                                                                                                                                                        .delete(CACHE_ALL
                                                                                                                                                                        + tenant_id))
                                                                                                                                        .then(redis_service
                                                                                                                                                        .delete(CACHE_ACTIVE
                                                                                                                                                                        + tenant_id))
                                                                                                                                        .then(redis_service
                                                                                                                                                        .delete(
                                                                                                                                                                        CACHE_SINGLE + tenant_id
                                                                                                                                                                                        + ":"
                                                                                                                                                                                        + id))
                                                                                                                                        .then(redis_service
                                                                                                                                                        .delete(CACHE_CURRENT
                                                                                                                                                                        + tenant_id))
                                                                                                                                        .thenReturn(mapToDto(
                                                                                                                                                        saved)));
                                                                                                }));
                                                                        });
                                                }));
        }

        /**
         * Closes an accounting period.
         */
        @Transactional
        public Mono<PeriodeComptableDto> closePeriode(UUID id) {
                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenant_id -> ReactiveTenantContext.getCurrentUser().defaultIfEmpty("system")
                                                .flatMap(user -> {
                                                        return periode_repository.findByTenant_IdAndId(tenant_id, id)
                                                                        .switchIfEmpty(Mono
                                                                                        .error(new ResourceNotFoundException(
                                                                                                        "Accounting period",
                                                                                                        id.toString())))
                                                                        .flatMap(periode -> {
                                                                                if (Boolean.TRUE.equals(periode
                                                                                                .getCloturee())) {
                                                                                        return Mono.error(
                                                                                                        new IllegalStateException(
                                                                                                                        "Period is already closed"));
                                                                                }

                                                                                periode.setCloturee(true);
                                                                                periode.setDate_cloture(
                                                                                                LocalDate.now());
                                                                                periode.setUpdated_by(user);
                                                                                periode.setUpdated_at(
                                                                                                LocalDateTime.now());
                                                                                periode.setNotNew();
                                                                                return periode_repository.save(periode)
                                                                                                .flatMap(saved -> ReactiveTenantContext
                                                                                                                .getCurrentTenantAsTenant()
                                                                                                                .flatMap(tenant -> logAudit(
                                                                                                                                tenant,
                                                                                                                                user,
                                                                                                                                "PERIODE_CLOSED",
                                                                                                                                "Closed period: "
                                                                                                                                                + periode.getCode()))
                                                                                                                .then(redis_service
                                                                                                                                .delete(CACHE_ALL
                                                                                                                                                + tenant_id))
                                                                                                                .then(redis_service
                                                                                                                                .delete(CACHE_ACTIVE
                                                                                                                                                + tenant_id))
                                                                                                                .then(redis_service
                                                                                                                                .delete(CACHE_SINGLE
                                                                                                                                                + tenant_id
                                                                                                                                                + ":"
                                                                                                                                                + id))
                                                                                                                .then(redis_service
                                                                                                                                .delete(CACHE_CURRENT
                                                                                                                                                + tenant_id))
                                                                                                                .thenReturn(mapToDto(
                                                                                                                                saved)));
                                                                        });
                                                }));
        }

        /**
         * Deletes an accounting period.
         */
        @Transactional
        public Mono<Void> deletePeriode(UUID id) {
                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenant_id -> ReactiveTenantContext.getCurrentUser().defaultIfEmpty("system")
                                                .flatMap(user -> {
                                                        return periode_repository.findByTenant_IdAndId(tenant_id, id)
                                                                        .switchIfEmpty(Mono
                                                                                        .error(new ResourceNotFoundException(
                                                                                                        "Accounting period",
                                                                                                        id.toString())))
                                                                        .flatMap(periode -> {
                                                                                if (Boolean.TRUE.equals(periode
                                                                                                .getCloturee())) {
                                                                                        return Mono
                                                                                                        .error(new IllegalStateException(
                                                                                                                        "Cannot delete a closed period"));
                                                                                }

                                                                                return periode_repository
                                                                                                .delete(periode)
                                                                                                .then(ReactiveTenantContext
                                                                                                                .getCurrentTenantAsTenant()
                                                                                                                .flatMap(tenant -> logAudit(
                                                                                                                                tenant,
                                                                                                                                user,
                                                                                                                                "PERIODE_DELETED",
                                                                                                                                "Deleted period: "
                                                                                                                                                + periode.getCode())))
                                                                                                .then(redis_service
                                                                                                                .delete(CACHE_ALL
                                                                                                                                + tenant_id))
                                                                                                .then(redis_service
                                                                                                                .delete(CACHE_ACTIVE
                                                                                                                                + tenant_id))
                                                                                                .then(redis_service
                                                                                                                .delete(CACHE_SINGLE
                                                                                                                                + tenant_id
                                                                                                                                + ":"
                                                                                                                                + id))
                                                                                                .then(redis_service
                                                                                                                .delete(CACHE_CURRENT
                                                                                                                                + tenant_id))
                                                                                                .then();
                                                                        });
                                                }));
        }

        /**
         * Validates a period DTO.
         */
        private Mono<Void> validateDto(PeriodeComptableDto dto) {
                return Mono.defer(() -> {
                        var violations = validator.validate(dto);
                        if (!violations.isEmpty())
                                return Mono.error(new ConstraintViolationException(violations));
                        if (dto.getDate_fin().isBefore(dto.getDate_debut())) {
                                return Mono.error(new IllegalArgumentException("End date must be after start date"));
                        }
                        if (dto.getExercice_id() != null) {
                                return exercice_repository.findById(dto.getExercice_id())
                                                .switchIfEmpty(Mono.error(
                                                                new ResourceNotFoundException("ExerciceComptable",
                                                                                dto.getExercice_id().toString())))
                                                .flatMap(exercice -> {
                                                        if (Boolean.TRUE.equals(exercice.getCloture())) {
                                                                return Mono.error(new IllegalStateException(
                                                                                "Cannot add a period to a closed fiscal year."));
                                                        }
                                                        if (dto.getDate_debut().isBefore(exercice.getDate_debut())
                                                                        || dto.getDate_fin().isAfter(
                                                                                        exercice.getDate_fin())) {
                                                                return Mono.error(new IllegalArgumentException(
                                                                                "Period dates must be within the fiscal year range."));
                                                        }
                                                        return Mono.empty();
                                                });
                        }
                        return Mono.empty();
                });
        }

        /**
         * Validates that a period does not overlap with existing periods.
         */
        private Mono<Void> validateNoOverlap(UUID tenant_id, LocalDate debut, LocalDate fin, UUID exclude_id) {
                return periode_repository.findByTenant_IdOrderByDate_debutDesc(tenant_id)
                                .filter(p -> exclude_id == null || !p.getId().equals(exclude_id))
                                .filter(p -> !(fin.isBefore(p.getDate_debut()) || debut.isAfter(p.getDate_fin())))
                                .next()
                                .flatMap(p -> Mono
                                                .error(new IllegalArgumentException(
                                                                "Overlapping period detected with: " + p.getCode())));
        }

        /**
         * Maps a DTO to a PeriodeComptable entity.
         */
        private PeriodeComptable mapToEntity(PeriodeComptableDto dto, Tenant tenant) {
                PeriodeComptable p = new PeriodeComptable();
                p.setId(dto.getId() != null ? dto.getId() : UUID.randomUUID());
                p.setTenantId(tenant.getId());
                p.setNew(dto.getId() == null);
                p.setCode(dto.getCode());
                p.setDate_debut(dto.getDate_debut());
                p.setDate_fin(dto.getDate_fin());
                p.setCloturee(dto.getCloturee() != null ? dto.getCloturee() : false);
                p.setDate_cloture(dto.getDate_cloture());
                p.setNotes(dto.getNotes());
                p.setExerciceId(dto.getExercice_id());
                return p;
        }

        /**
         * Maps a PeriodeComptable entity to its DTO.
         */
        private PeriodeComptableDto mapToDto(PeriodeComptable p) {
                return PeriodeComptableDto.builder()
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
                                .build();
        }

        /**
         * Logs an audit entry.
         */
        private Mono<Void> logAudit(Tenant tenant, String user, String action, String details) {
                JournalAudit audit = JournalAudit.builder()
                                .id(UUID.randomUUID())
                                .tenantId(tenant.getId())
                                .utilisateur(user)
                                .action(action)
                                .details(details)
                                .date_action(LocalDateTime.now())
                                .created_at(LocalDateTime.now())
                                .updated_at(LocalDateTime.now())
                                .created_by(user)
                                .updated_by(user)
                                .build();

                return audit_repository.save(audit)
                                .flatMap(savedAudit -> {
                                        JournalAuditDto auditDto = JournalAuditDto.builder()
                                                        .action(action)
                                                        .utilisateur(user)
                                                        .details(details)
                                                        .date_action(savedAudit.getDate_action())
                                                        .build();

                                        return kafka_service.sendAuditLog(auditDto, tenant.getId(), action);
                                });
        }
}
