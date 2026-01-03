package com.yowyob.erp.accounting.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yowyob.erp.accounting.dto.EcritureComptableDto;
import com.yowyob.erp.accounting.dto.JournalComptableDto;
import com.yowyob.erp.accounting.entity.EcritureComptable;
import com.yowyob.erp.accounting.entity.JournalAudit;
import com.yowyob.erp.accounting.entity.JournalComptable;
import com.yowyob.erp.accounting.entity.Tenant;
import com.yowyob.erp.accounting.repository.EcritureComptableRepository;
import com.yowyob.erp.accounting.repository.JournalAuditRepository;
import com.yowyob.erp.accounting.repository.JournalComptableRepository;
import com.yowyob.erp.common.constants.AppConstants;
import com.yowyob.erp.common.exception.ResourceNotFoundException;
import com.yowyob.erp.config.kafka.KafkaMessageService;
import com.yowyob.erp.config.redis.RedisService;
import com.yowyob.erp.config.tenant.TenantContext;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing accounting journals.
 * Compatible with PostgreSQL + Redis + Kafka + Multi-tenant.
 *
 * @author ALD
 * @date 30.09.25
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JournalComptableService {

    private final JournalComptableRepository journal_repository;
    private final EcritureComptableRepository ecriture_repository;
    private final JournalAuditRepository audit_repository;
    private final Validator validator;
    private final KafkaMessageService kafka_service;
    private final RedisService redis_service;

    private static final String CACHE_JOURNAL_ALL = "journal:all:";
    private static final String CACHE_JOURNAL_ACTIVE = "journal:active:";

    /**
     * Creates a new accounting journal.
     * 
     * @param dto the journal data
     * @return the created journal DTO
     */
    @Transactional
    public JournalComptableDto createJournalComptable(JournalComptableDto dto) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        String user = Optional.ofNullable(TenantContext.getCurrentUser()).orElse("system");
        log.info("📓 Creating accounting journal [{}] for tenant {}", dto.getCode_journal(), tenant_id);

        validateJournalComptableDto(dto);

        if (journal_repository.existsByTenant_IdAndCode_journal(tenant_id, dto.getCode_journal())) {
            throw new IllegalArgumentException("Journal code already in use: " + dto.getCode_journal());
        }

        Tenant tenant = TenantContext.getCurrentTenantAsTenant();
        JournalComptable entity = mapToEntity(dto, tenant);
        entity.setCreated_at(LocalDateTime.now());
        entity.setUpdated_at(LocalDateTime.now());
        entity.setCreated_by(user);
        entity.setUpdated_by(user);

        JournalComptable saved = journal_repository.save(entity);
        logAudit(tenant, user, "CREATE", "Creation of journal " + dto.getCode_journal());
        kafka_service.sendAuditLog(saved, tenant_id, "JOURNAL_CREATED");

        redis_service.delete(CACHE_JOURNAL_ALL + tenant_id);
        redis_service.delete(CACHE_JOURNAL_ACTIVE + tenant_id);

        return mapToDto(saved);
    }

    /**
     * Retrieves a journal by its ID.
     * 
     * @param journal_id the journal ID
     * @return an Optional containing the journal DTO if found
     */
    public Optional<JournalComptableDto> getJournalComptable(UUID journal_id) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        log.info("🔍 Retrieving accounting journal [{}] for tenant {}", journal_id, tenant_id);

        return journal_repository.findByTenant_IdAndId(tenant_id, journal_id)
                .map(journal -> {
                    JournalComptableDto dto = mapToDto(journal);
                    List<EcritureComptableDto> ecritures = ecriture_repository
                            .findByTenant_IdAndJournal_Id(tenant_id, journal_id)
                            .stream()
                            .map(this::mapEcritureToDto)
                            .collect(Collectors.toList());
                    dto.setEcriture_comptable(ecritures);
                    return dto;
                });
    }

    /**
     * Retrieves all journals for the current tenant.
     * 
     * @return list of journal DTOs
     */
    @SuppressWarnings("unchecked")
    public List<JournalComptableDto> getAllJournalComptables() {
        UUID tenant_id = TenantContext.getCurrentTenant();
        log.info("📚 Retrieving all journals for tenant {}", tenant_id);

        String cache_key = CACHE_JOURNAL_ALL + tenant_id;
        List<JournalComptableDto> cached = redis_service.get(cache_key, List.class);
        if (cached != null)
            return cached;

        List<JournalComptableDto> journals = journal_repository.findByTenant_Id(tenant_id)
                .stream().map(this::mapToDto).collect(Collectors.toList());

        redis_service.save(cache_key, journals, Duration.ofMinutes(10));
        return journals;
    }

    /**
     * Retrieves all active journals for the current tenant.
     * 
     * @return list of active journal DTOs
     */
    @SuppressWarnings("unchecked")
    public List<JournalComptableDto> getActiveJournalComptables() {
        UUID tenant_id = TenantContext.getCurrentTenant();
        log.info("📘 Retrieving active journals for tenant {}", tenant_id);

        String cache_key = CACHE_JOURNAL_ACTIVE + tenant_id;
        List<JournalComptableDto> cached = redis_service.get(cache_key, List.class);
        if (cached != null)
            return cached;

        List<JournalComptableDto> journals = journal_repository.findByTenant_IdAndActifTrue(tenant_id)
                .stream().map(this::mapToDto).collect(Collectors.toList());

        redis_service.save(cache_key, journals, Duration.ofMinutes(10));
        return journals;
    }

    /**
     * Updates an existing journal.
     * 
     * @param id  the journal ID
     * @param dto the new journal data
     * @return the updated journal DTO
     */
    @Transactional
    public JournalComptableDto updateJournalComptable(UUID id, JournalComptableDto dto) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        String user = Optional.ofNullable(TenantContext.getCurrentUser()).orElse("system");

        JournalComptable existing = journal_repository.findByTenant_IdAndId(tenant_id, id)
                .orElseThrow(() -> new ResourceNotFoundException("JournalComptable", id.toString()));

        validateJournalComptableDto(dto);

        existing.setCode_journal(dto.getCode_journal());
        existing.setLibelle(dto.getLibelle());
        existing.setType_journal(dto.getType_journal());
        existing.setNotes(dto.getNotes());
        existing.setActif(dto.getActif());
        existing.setUpdated_by(user);
        existing.setUpdated_at(LocalDateTime.now());

        JournalComptable saved = journal_repository.save(existing);
        logAudit(TenantContext.getCurrentTenantAsTenant(), user, "UPDATE",
                "Update of journal " + dto.getCode_journal());
        kafka_service.sendAuditLog(saved, tenant_id, "JOURNAL_UPDATED");

        redis_service.delete(CACHE_JOURNAL_ALL + tenant_id);
        redis_service.delete(CACHE_JOURNAL_ACTIVE + tenant_id);

        return mapToDto(saved);
    }

    /**
     * Deletes a journal by its ID.
     * 
     * @param id the journal ID
     */
    @Transactional
    public void deleteJournalComptable(UUID id) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        String user = Optional.ofNullable(TenantContext.getCurrentUser()).orElse("system");

        JournalComptable journal = journal_repository.findByTenant_IdAndId(tenant_id, id)
                .orElseThrow(() -> new ResourceNotFoundException("JournalComptable", id.toString()));

        journal_repository.delete(journal);
        logAudit(TenantContext.getCurrentTenantAsTenant(), user, "DELETE",
                "Deletion of journal " + journal.getCode_journal());
        kafka_service.sendAuditLog(journal, tenant_id, "JOURNAL_DELETED");

        redis_service.delete(CACHE_JOURNAL_ALL + tenant_id);
        redis_service.delete(CACHE_JOURNAL_ACTIVE + tenant_id);
    }

    /**
     * Validates a journal DTO.
     * 
     * @param dto the DTO to validate
     */
    private void validateJournalComptableDto(JournalComptableDto dto) {
        var violations = validator.validate(dto);
        if (!violations.isEmpty())
            throw new ConstraintViolationException(violations);

        if (!dto.getCode_journal().matches("^[A-Z]{1,5}$")) {
            throw new IllegalArgumentException("Invalid journal code: must contain 1 to 5 uppercase letters.");
        }

        List<String> valid_types = List.of(
                AppConstants.JournalTypes.SALES,
                AppConstants.JournalTypes.PURCHASES,
                AppConstants.JournalTypes.CASH,
                AppConstants.JournalTypes.BANK,
                AppConstants.JournalTypes.GENERAL);

        if (!valid_types.contains(dto.getType_journal())) {
            throw new IllegalArgumentException("Invalid journal type: " + dto.getType_journal());
        }
    }

    /**
     * Logs an audit entry.
     * 
     * @param tenant      the tenant
     * @param utilisateur the user
     * @param action      the action taken
     * @param details     the action details
     */
    private void logAudit(Tenant tenant, String utilisateur, String action, String details) {
        JournalAudit audit = JournalAudit.builder()
                .tenant(tenant)
                .action(action)
                .utilisateur(utilisateur)
                .details(details)
                .date_action(LocalDateTime.now())
                .created_at(LocalDateTime.now())
                .updated_at(LocalDateTime.now())
                .created_by(utilisateur)
                .updated_by(utilisateur)
                .build();
        audit_repository.save(audit);
        kafka_service.sendAuditLog(audit, tenant.getId(), action);
    }

    /**
     * Maps a DTO and tenant to a JournalComptable entity.
     * 
     * @param dto    the DTO to map
     * @param tenant the tenant
     * @return the mapped entity
     */
    private JournalComptable mapToEntity(JournalComptableDto dto, Tenant tenant) {
        JournalComptable j = new JournalComptable();
        j.setTenant(tenant);
        j.setCode_journal(dto.getCode_journal());
        j.setLibelle(dto.getLibelle());
        j.setType_journal(dto.getType_journal());
        j.setNotes(dto.getNotes());
        j.setActif(dto.getActif() != null ? dto.getActif() : true);
        j.setCreated_at(dto.getCreated_at() != null ? dto.getCreated_at() : LocalDateTime.now());
        j.setUpdated_at(dto.getUpdated_at() != null ? dto.getUpdated_at() : LocalDateTime.now());
        return j;
    }

    /**
     * Maps a JournalComptable entity to its DTO.
     * 
     * @param entity the entity to map
     * @return the mapped DTO
     */
    private JournalComptableDto mapToDto(JournalComptable entity) {
        return JournalComptableDto.builder()
                .id(entity.getId())
                .code_journal(entity.getCode_journal())
                .libelle(entity.getLibelle())
                .type_journal(entity.getType_journal())
                .notes(entity.getNotes())
                .actif(entity.getActif())
                .created_at(entity.getCreated_at())
                .updated_at(entity.getUpdated_at())
                .build();
    }

    /**
     * Maps an EcritureComptable entity to its DTO.
     * 
     * @param e the entity to map
     * @return the mapped DTO
     */
    private EcritureComptableDto mapEcritureToDto(EcritureComptable e) {
        return EcritureComptableDto.builder()
                .id(e.getId())
                .numero_ecriture(e.getNumero_ecriture())
                .libelle(e.getLibelle())
                .date_ecriture(e.getDate_ecriture())
                .journal_comptable_id(e.getJournal() != null ? e.getJournal().getId() : null)
                .periode_comptable_id(e.getPeriode() != null ? e.getPeriode().getId() : null)
                .montant_total_debit(e.getMontant_total_debit())
                .montant_total_credit(e.getMontant_total_credit())
                .validee(e.getValidee())
                .date_validation(e.getDate_validation())
                .validated_by(e.getValidated_by())
                .reference_externe(e.getReference_externe())
                .notes(e.getNotes())
                .created_at(e.getCreated_at())
                .updated_at(e.getUpdated_at())
                .build();
    }
}
