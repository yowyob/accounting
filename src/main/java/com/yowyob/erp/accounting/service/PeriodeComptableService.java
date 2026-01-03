package com.yowyob.erp.accounting.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.yowyob.erp.config.tenant.TenantContext;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing accounting periods.
 * Handles creation, validation, closing, and retrieval of periods.
 * Multi-tenant aware and uses Redis for caching.
 * 
 * @author ALD
 * @date 30.09.25
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
     * 
     * @param dto the period data
     * @return the created period DTO
     */
    @Transactional
    public PeriodeComptableDto createPeriode(PeriodeComptableDto dto) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        String user = Optional.ofNullable(TenantContext.getCurrentUser()).orElse("system");

        log.info("🧾 Creating accounting period [{} - {}] for tenant {}", dto.getCode(), dto.getDate_debut(),
                tenant_id);
        validateDto(dto);

        if (periode_repository.findByTenant_IdAndCode(tenant_id, dto.getCode()).isPresent()) {
            throw new IllegalArgumentException("Period code already exists: " + dto.getCode());
        }

        validateNoOverlap(tenant_id, dto.getDate_debut(), dto.getDate_fin(), null);

        PeriodeComptable entity = mapToEntity(dto);
        entity.setCreated_at(LocalDateTime.now());
        entity.setUpdated_at(LocalDateTime.now());
        entity.setCreated_by(user);
        entity.setUpdated_by(user);

        PeriodeComptable saved = periode_repository.save(entity);
        PeriodeComptableDto result = mapToDto(saved);

        logAudit(tenant_id, user, "PERIODE_CREATED", "Created period: " + dto.getCode());

        redis_service.delete(CACHE_ALL + tenant_id);
        redis_service.delete(CACHE_ACTIVE + tenant_id);

        return result;
    }

    /**
     * Retrieves a period by its ID.
     * 
     * @param id the period ID
     * @return an Optional containing the period DTO if found
     */
    public Optional<PeriodeComptableDto> getPeriode(UUID id) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        String cache_key = CACHE_SINGLE + tenant_id + ":" + id;

        PeriodeComptableDto cached = redis_service.get(cache_key, PeriodeComptableDto.class);
        if (cached != null)
            return Optional.of(cached);

        PeriodeComptable periode = periode_repository.findByTenant_IdAndId(tenant_id, id)
                .orElseThrow(() -> new ResourceNotFoundException("Accounting period", id.toString()));

        PeriodeComptableDto dto = mapToDto(periode);
        redis_service.save(cache_key, dto, Duration.ofMinutes(15));
        return Optional.of(dto);
    }

    /**
     * Retrieves all periods for the current tenant.
     * 
     * @return list of period DTOs
     */
    @SuppressWarnings("unchecked")
    public List<PeriodeComptableDto> getAllPeriodes() {
        UUID tenant_id = TenantContext.getCurrentTenant();
        String cache_key = CACHE_ALL + tenant_id;

        List<PeriodeComptableDto> cached = redis_service.get(cache_key, List.class);
        if (cached != null)
            return cached;

        List<PeriodeComptableDto> periodes = periode_repository.findByTenant_IdOrderByDate_debutDesc(tenant_id)
                .stream().map(this::mapToDto).collect(Collectors.toList());

        redis_service.save(cache_key, periodes, Duration.ofMinutes(10));
        return periodes;
    }

    /**
     * Retrieves a period by its code.
     * 
     * @param code the period code
     * @return an Optional containing the period DTO if found
     */
    public Optional<PeriodeComptableDto> getByCode(String code) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        return periode_repository.findByTenant_IdAndCode(tenant_id, code).map(this::mapToDto);
    }

    /**
     * Retrieves a period by a date within its range.
     * 
     * @param date the date to check
     * @return an Optional containing the period DTO if found
     */
    public Optional<PeriodeComptableDto> getByDate(LocalDate date) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        return periode_repository.findByTenant_IdAndDateInRange(tenant_id, date).map(this::mapToDto);
    }

    /**
     * Retrieves all non-closed periods for the current tenant.
     * 
     * @return list of non-closed period DTOs
     */
    @SuppressWarnings("unchecked")
    public List<PeriodeComptableDto> getNonClosedPeriodes() {
        UUID tenant_id = TenantContext.getCurrentTenant();
        String cache_key = CACHE_ACTIVE + tenant_id;

        List<PeriodeComptableDto> cached = redis_service.get(cache_key, List.class);
        if (cached != null)
            return cached;

        List<PeriodeComptableDto> periodes = periode_repository.findByTenant_IdAndClotureeFalse(tenant_id)
                .stream().map(this::mapToDto).collect(Collectors.toList());

        redis_service.save(cache_key, periodes, Duration.ofMinutes(10));
        return periodes;
    }

    /**
     * Retrieves periods within a specific date range.
     * JournalAudit.builder
     * 
     * @param start the start date
     * @param end   the end date
     * @return list of period DTOs
     */
    public List<PeriodeComptableDto> getByRange(LocalDate start, LocalDate end) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        return periode_repository.findByTenant_IdAndPeriodRange(tenant_id, start, end)
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    /**
     * Retrieves the current open period for a given tenant.
     * 
     * @param tenant_id the tenant ID
     * @return the current period DTO
     */
    public PeriodeComptableDto getCurrentPeriode(UUID tenant_id) {
        String cache_key = CACHE_CURRENT + tenant_id;

        PeriodeComptableDto cached = redis_service.get(cache_key, PeriodeComptableDto.class);
        if (cached != null)
            return cached;

        LocalDate today = LocalDate.now();
        PeriodeComptable periode = periode_repository.findByTenant_IdAndDateInRange(tenant_id, today)
                .filter(p -> !Boolean.TRUE.equals(p.getCloturee()))
                .orElseThrow(
                        () -> new ResourceNotFoundException("No open accounting period found for the current date"));

        PeriodeComptableDto dto = mapToDto(periode);
        redis_service.save(cache_key, dto, Duration.ofMinutes(30));

        log.info("📅 Current accounting period for tenant {} : {}", tenant_id, dto.getCode());
        return dto;
    }

    /**
     * Updates an existing accounting period.
     * 
     * @param id  the period ID
     * @param dto the new period data
     * @return the updated period DTO
     */
    @Transactional
    public PeriodeComptableDto updatePeriode(UUID id, PeriodeComptableDto dto) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        String user = Optional.ofNullable(TenantContext.getCurrentUser()).orElse("system");

        PeriodeComptable existing = periode_repository.findByTenant_IdAndId(tenant_id, id)
                .orElseThrow(() -> new ResourceNotFoundException("Accounting period", id.toString()));

        if (Boolean.TRUE.equals(existing.getCloturee())) {
            throw new IllegalStateException("Cannot modify a closed period");
        }

        if (!existing.getCode().equals(dto.getCode()) &&
                periode_repository.findByTenant_IdAndCode(tenant_id, dto.getCode()).isPresent()) {
            throw new IllegalArgumentException("Period code already exists: " + dto.getCode());
        }

        validateNoOverlap(tenant_id, dto.getDate_debut(), dto.getDate_fin(), id);
        validateDto(dto);

        existing.setCode(dto.getCode());
        existing.setDate_debut(dto.getDate_debut());
        existing.setDate_fin(dto.getDate_fin());
        existing.setNotes(dto.getNotes());
        existing.setUpdated_at(LocalDateTime.now());
        existing.setUpdated_by(user);

        PeriodeComptable saved = periode_repository.save(existing);
        PeriodeComptableDto result = mapToDto(saved);

        kafka_service.sendAuditLog(result, tenant_id, "PERIODE_UPDATED");
        logAudit(tenant_id, user, "UPDATE", "Updated period: " + dto.getCode());

        redis_service.delete(CACHE_ALL + tenant_id);
        redis_service.delete(CACHE_ACTIVE + tenant_id);
        redis_service.delete(CACHE_SINGLE + tenant_id + ":" + id);
        redis_service.delete(CACHE_CURRENT + tenant_id);

        return result;
    }

    /**
     * Closes an accounting period.
     * 
     * @param id the period ID to close
     * @return the closed period DTO
     */
    @Transactional
    public PeriodeComptableDto closePeriode(UUID id) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        String user = Optional.ofNullable(TenantContext.getCurrentUser()).orElse("system");

        PeriodeComptable periode = periode_repository.findByTenant_IdAndId(tenant_id, id)
                .orElseThrow(() -> new ResourceNotFoundException("Accounting period", id.toString()));

        if (Boolean.TRUE.equals(periode.getCloturee())) {
            throw new IllegalStateException("Period is already closed");
        }

        periode.setCloturee(true);
        periode.setDate_cloture(LocalDate.now());
        periode.setUpdated_by(user);
        periode.setUpdated_at(LocalDateTime.now());

        PeriodeComptable saved = periode_repository.save(periode);
        PeriodeComptableDto result = mapToDto(saved);

        kafka_service.sendAuditLog(result, tenant_id, "PERIODE_CLOSED");
        logAudit(tenant_id, user, "CLOSE", "Closed period: " + periode.getCode());

        redis_service.delete(CACHE_ALL + tenant_id);
        redis_service.delete(CACHE_ACTIVE + tenant_id);
        redis_service.delete(CACHE_SINGLE + tenant_id + ":" + id);
        redis_service.delete(CACHE_CURRENT + tenant_id);

        return result;
    }

    /**
     * Deletes an accounting period.
     * 
     * @param id the period ID to delete
     */
    @Transactional
    public void deletePeriode(UUID id) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        String user = Optional.ofNullable(TenantContext.getCurrentUser()).orElse("system");

        PeriodeComptable periode = periode_repository.findByTenant_IdAndId(tenant_id, id)
                .orElseThrow(() -> new ResourceNotFoundException("Accounting period", id.toString()));

        if (Boolean.TRUE.equals(periode.getCloturee())) {
            throw new IllegalStateException("Cannot delete a closed period");
        }

        periode_repository.delete(periode);
        kafka_service.sendAuditLog(periode, tenant_id, "PERIODE_DELETED");
        logAudit(tenant_id, user, "DELETE", "Deleted period: " + periode.getCode());

        redis_service.delete(CACHE_ALL + tenant_id);
        redis_service.delete(CACHE_ACTIVE + tenant_id);
        redis_service.delete(CACHE_SINGLE + tenant_id + ":" + id);
        redis_service.delete(CACHE_CURRENT + tenant_id);
    }

    /**
     * Validates a period DTO.
     * 
     * @param dto the DTO to validate
     */
    private void validateDto(PeriodeComptableDto dto) {
        var violations = validator.validate(dto);
        if (!violations.isEmpty())
            throw new ConstraintViolationException(violations);
        if (dto.getDate_fin().isBefore(dto.getDate_debut())) {
            throw new IllegalArgumentException("End date must be after start date");
        }
        if (dto.getExercice_id() != null) {
            com.yowyob.erp.accounting.entity.ExerciceComptable exercice = exercice_repository
                    .findById(dto.getExercice_id())
                    .orElseThrow(
                            () -> new ResourceNotFoundException("ExerciceComptable", dto.getExercice_id().toString()));
            if (dto.getDate_debut().isBefore(exercice.getDate_debut())
                    || dto.getDate_fin().isAfter(exercice.getDate_fin())) {
                throw new IllegalArgumentException("Period dates must be within the fiscal year range.");
            }
        }
    }

    /**
     * Validates that a period does not overlap with existing periods for the same
     * tenant.
     * 
     * @param tenant_id  the tenant ID
     * @param debut      the start date
     * @param fin        the end date
     * @param exclude_id the ID to exclude from validation (for updates)
     */
    private void validateNoOverlap(UUID tenant_id, LocalDate debut, LocalDate fin, UUID exclude_id) {
        List<PeriodeComptable> existing = periode_repository.findByTenant_IdOrderByDate_debutDesc(tenant_id);
        for (PeriodeComptable p : existing) {
            if (exclude_id != null && p.getId().equals(exclude_id))
                continue;
            if (!(fin.isBefore(p.getDate_debut()) || debut.isAfter(p.getDate_fin()))) {
                throw new IllegalArgumentException("Overlapping period detected with: " + p.getCode());
            }
        }
    }

    /**
     * Maps a DTO to a PeriodeComptable entity.
     * 
     * @param dto the DTO to map
     * @return the mapped entity
     */
    private PeriodeComptable mapToEntity(PeriodeComptableDto dto) {
        Tenant tenant = TenantContext.getCurrentTenantAsTenant();
        if (tenant == null) {
            throw new IllegalStateException("TenantContext could not provide a valid Tenant entity.");
        }

        PeriodeComptable p = new PeriodeComptable();
        p.setTenant(tenant);
        p.setCode(dto.getCode());
        p.setDate_debut(dto.getDate_debut());
        p.setDate_fin(dto.getDate_fin());
        p.setCloturee(Optional.ofNullable(dto.getCloturee()).orElse(false));
        p.setDate_cloture(dto.getDate_cloture());
        p.setNotes(dto.getNotes());
        if (dto.getExercice_id() != null) {
            p.setExercice(exercice_repository.getReferenceById(dto.getExercice_id()));
        }
        return p;
    }

    /**
     * Maps a PeriodeComptable entity to its DTO.
     * 
     * @param p the entity to map
     * @return the mapped DTO
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
                .exercice_id(p.getExercice() != null ? p.getExercice().getId() : null)
                .build();
    }

    /**
     * Logs an audit entry.
     * 
     * @param tenant_id the tenant ID
     * @param user      the user taking action
     * @param action    the action taken
     * @param details   the action details
     */
    private void logAudit(UUID tenant_id, String user, String action, String details) {
        // 1. Sauvegarde SQL Locale (On garde l'entité car Hibernate est fait pour ça)
        JournalAudit audit = JournalAudit.builder()
                .tenant(TenantContext.getCurrentTenantAsTenant())
                .utilisateur(user)
                .action(action)
                .details(details)
                .date_action(LocalDateTime.now())
                .build();
        JournalAudit savedAudit = audit_repository.save(audit);

        // 2. Conversion en DTO pour Kafka (On extrait les données "pures")
        JournalAuditDto auditDto = JournalAuditDto.builder()
                .id(savedAudit.getId()) // Optionnel car on veut forcer l'insert côté listener
                .action(action)
                .utilisateur(user)
                .details(details)
                .date_action(savedAudit.getDate_action())
                .build();

        // 3. Appel du service Kafka avec le DTO
        kafka_service.sendAuditLog(auditDto, tenant_id, action);
    }
}
