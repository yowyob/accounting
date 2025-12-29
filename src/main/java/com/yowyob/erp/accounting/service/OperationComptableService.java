package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.dto.ContrepartieDto;
import com.yowyob.erp.accounting.dto.OperationComptableDto;
import com.yowyob.erp.accounting.entity.Contrepartie;
import com.yowyob.erp.accounting.entity.JournalAudit;
import com.yowyob.erp.accounting.entity.JournalComptable;
import com.yowyob.erp.accounting.entity.OperationComptable;
import com.yowyob.erp.accounting.entity.PlanComptable;
import com.yowyob.erp.accounting.entity.Tenant;
import com.yowyob.erp.accounting.repository.ContrepartieRepository;
import com.yowyob.erp.accounting.repository.JournalAuditRepository;
import com.yowyob.erp.accounting.repository.JournalComptableRepository;
import com.yowyob.erp.accounting.repository.OperationComptableRepository;
import com.yowyob.erp.accounting.repository.PlanComptableRepository;
import com.yowyob.erp.common.exception.ResourceNotFoundException;
import com.yowyob.erp.config.kafka.KafkaMessageService;
import com.yowyob.erp.config.redis.RedisService;
import com.yowyob.erp.config.tenant.TenantContext;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing accounting operations.
 * Handles CRUD operations with caching, auditing, and multi-tenant support.
 * Follows snake_case naming and English Javadoc as per development charter.
 *
 * @author ALD
 * @date 30.09.25
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OperationComptableService {

    private final OperationComptableRepository operation_repository;
    private final ContrepartieRepository contrepartie_repository;
    private final JournalComptableRepository journal_repository;
    private final PlanComptableRepository plan_repository;
    private final JournalAuditRepository audit_repository;
    private final Validator validator;
    private final KafkaMessageService kafka_service;
    private final RedisService redis_service;

    private static final String CACHE_OPERATIONS_ALL = "operations:all:";
    private static final String CACHE_OPERATION = "operation:";

    /**
     * Creates a new accounting operation and its associated counterparties.
     * 
     * @param dto the operation data
     * @return the created operation DTO
     * @throws IllegalArgumentException if validation or uniqueness check fails
     */
    @Transactional
    public OperationComptableDto createOperation(OperationComptableDto dto) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        Tenant tenant = TenantContext.getCurrentTenantAsTenant();
        String user = Optional.ofNullable(TenantContext.getCurrentUser()).orElse("system");
        log.info("📝 Creating accounting operation [{} - {}] for tenant {}", dto.getType_operation(),
                dto.getMode_reglement(), tenant_id);

        validateOperationDto(dto);

        // Validate journal and account
        journal_repository.findByTenant_IdAndId(tenant_id, dto.getJournal_comptable_id())
                .filter(JournalComptable::getActif)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid or inactive journal: " + dto.getJournal_comptable_id()));

        plan_repository.findByTenant_IdAndNo_compte(tenant_id, dto.getCompte_principal())
                .filter(PlanComptable::getActif)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid or inactive principal account: " + dto.getCompte_principal()));

        // Check uniqueness of type + mode
        if (operation_repository.findByTenant_IdAndType_operationAndMode_reglement(tenant_id, dto.getType_operation(),
                dto.getMode_reglement()).isPresent()) {
            throw new IllegalArgumentException(
                    "Existing operation: " + dto.getType_operation() + " / " + dto.getMode_reglement());
        }

        OperationComptable entity = mapToEntity(dto, tenant);
        entity.setCreated_at(LocalDateTime.now());
        entity.setUpdated_at(LocalDateTime.now());
        entity.setCreated_by(user);
        entity.setUpdated_by(user);

        OperationComptable saved = operation_repository.save(entity);

        // Counterparties
        if (dto.getContreparties() != null && !dto.getContreparties().isEmpty()) {
            List<Contrepartie> contreparties = dto.getContreparties()
                    .stream()
                    .map(cp_dto -> mapContrepartie(cp_dto, tenant, saved.getId(), user))
                    .collect(Collectors.toList());
            contrepartie_repository.saveAll(contreparties);
        }

        kafka_service.sendAuditLog(saved, tenant_id.toString(), "OPERATION_CREATED");
        logAudit(tenant, user, "CREATE", "Creation of operation: " + dto.getType_operation());
        redis_service.delete(CACHE_OPERATIONS_ALL + tenant_id);

        return mapToDto(saved);
    }

    /**
     * Retrieves all accounting operations for the current tenant.
     * 
     * @return list of operation DTOs
     */
    @SuppressWarnings("unchecked")
    public List<OperationComptableDto> getAllOperations() {
        UUID tenant_id = TenantContext.getCurrentTenant();
        String cache_key = CACHE_OPERATIONS_ALL + tenant_id;

        List<OperationComptableDto> cached = redis_service.get(cache_key, List.class);
        if (cached != null)
            return cached;

        List<OperationComptableDto> operations = operation_repository.findByTenant_Id(tenant_id)
                .stream().map(this::mapToDto).collect(Collectors.toList());

        redis_service.save(cache_key, operations, Duration.ofMinutes(10));
        return operations;
    }

    /**
     * Retrieves a specific accounting operation by ID.
     * 
     * @param id the operation ID
     * @return the operation DTO
     * @throws ResourceNotFoundException if no operation exists with the given ID
     */
    public Optional<OperationComptableDto> getOperation(UUID id) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        String cache_key = CACHE_OPERATION + tenant_id + ":" + id;

        OperationComptableDto cached = redis_service.get(cache_key, OperationComptableDto.class);
        if (cached != null)
            return Optional.of(cached);

        OperationComptable operation = operation_repository.findByTenant_IdAndId(tenant_id, id)
                .orElseThrow(() -> new ResourceNotFoundException("Accounting operation", id.toString()));

        OperationComptableDto dto = mapToDto(operation);
        redis_service.save(cache_key, dto, Duration.ofMinutes(10));
        return Optional.of(dto);
    }

    /**
     * Retrieves operations associated with a specific principal account.
     * 
     * @param compte the account number
     * @return list of operation DTOs
     */
    public List<OperationComptableDto> getOperationsByCompte(String compte) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        return operation_repository.findByTenant_IdAndCompte_principal(tenant_id, compte)
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    /**
     * Retrieves an operation by its type and settlement mode.
     * 
     * @param type the operation type
     * @param mode the settlement mode
     * @return the matching operation DTO if found
     */
    public Optional<OperationComptableDto> getByTypeAndMode(String type, String mode) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        return operation_repository.findByTenant_IdAndType_operationAndMode_reglement(tenant_id, type, mode)
                .map(this::mapToDto);
    }

    /**
     * Updates an existing accounting operation.
     * 
     * @param id  the operation ID to update
     * @param dto the new operation data
     * @return the updated operation DTO
     */
    @Transactional
    public OperationComptableDto updateOperation(UUID id, OperationComptableDto dto) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        Tenant tenant = TenantContext.getCurrentTenantAsTenant();
        String user = Optional.ofNullable(TenantContext.getCurrentUser()).orElse("system");

        OperationComptable existing = operation_repository.findByTenant_IdAndId(tenant_id, id)
                .orElseThrow(() -> new ResourceNotFoundException("Accounting operation", id.toString()));

        validateOperationDto(dto);

        existing.setType_operation(dto.getType_operation());
        existing.setMode_reglement(dto.getMode_reglement());
        existing.setCompte_principal(dto.getCompte_principal());
        existing.setSens_principal(dto.getSens_principal());
        existing.setJournal_comptable(journal_repository.findById(dto.getJournal_comptable_id())
                .orElseThrow(() -> new ResourceNotFoundException("Journal", dto.getJournal_comptable_id().toString())));
        existing.setType_montant(dto.getType_montant());
        existing.setPlafond_client(dto.getPlafond_client());
        existing.setEst_compte_statique(dto.getEst_compte_statique());
        existing.setActif(dto.getActif());
        existing.setUpdated_by(user);
        existing.setUpdated_at(LocalDateTime.now());

        OperationComptable saved = operation_repository.save(existing);

        // Counterparties
        contrepartie_repository.deleteByTenantIdAndOperationComptableId(tenant_id, id);
        if (dto.getContreparties() != null && !dto.getContreparties().isEmpty()) {
            List<Contrepartie> contreparties = dto.getContreparties().stream()
                    .map(cp_dto -> mapContrepartie(cp_dto, tenant, id, user)).collect(Collectors.toList());
            contrepartie_repository.saveAll(contreparties);
        }

        kafka_service.sendAuditLog(saved, tenant_id.toString(), "OPERATION_UPDATED");
        logAudit(tenant, user, "UPDATE", "Update of operation: " + dto.getType_operation());
        redis_service.delete(CACHE_OPERATIONS_ALL + tenant_id);
        redis_service.delete(CACHE_OPERATION + tenant_id + ":" + id);

        return mapToDto(saved);
    }

    /**
     * Deletes an accounting operation and its counterparties.
     * 
     * @param id the operation ID to delete
     */
    @Transactional
    public void deleteOperation(UUID id) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        Tenant tenant = TenantContext.getCurrentTenantAsTenant();
        String user = Optional.ofNullable(TenantContext.getCurrentUser()).orElse("system");

        OperationComptable operation = operation_repository.findByTenant_IdAndId(tenant_id, id)
                .orElseThrow(() -> new ResourceNotFoundException("Accounting operation", id.toString()));

        contrepartie_repository.deleteByTenantIdAndOperationComptableId(tenant_id, id);
        operation_repository.delete(operation);

        kafka_service.sendAuditLog(operation, tenant_id.toString(), "OPERATION_DELETED");
        logAudit(tenant, user, "DELETE", "Deletion of operation: " + operation.getType_operation());
        redis_service.delete(CACHE_OPERATIONS_ALL + tenant_id);
        redis_service.delete(CACHE_OPERATION + tenant_id + ":" + id);
    }

    /**
     * Performs business validation on the operation DTO.
     * 
     * @param dto the DTO to validate
     * @throws ConstraintViolationException if validation rules are violated
     * @throws IllegalArgumentException     if enums are invalid
     */
    private void validateOperationDto(OperationComptableDto dto) {
        var violations = validator.validate(dto);
        if (!violations.isEmpty())
            throw new ConstraintViolationException(violations);
        if (!("DEBIT".equals(dto.getSens_principal()) || "CREDIT".equals(dto.getSens_principal()))) {
            throw new IllegalArgumentException("Sens principal must be DEBIT or CREDIT");
        }
        if (!("HT".equals(dto.getType_montant()) || "TTC".equals(dto.getType_montant())
                || "TVA".equals(dto.getType_montant()) || "PAU".equals(dto.getType_montant()))) {
            throw new IllegalArgumentException("Type montant must be HT, TTC, TVA, or PAU");
        }
    }

    /**
     * Maps a counterparty DTO to an entity.
     */
    private Contrepartie mapContrepartie(ContrepartieDto dto, Tenant tenant, UUID operation_id, String user) {
        Contrepartie cp = new Contrepartie();
        cp.setTenant(tenant);
        cp.setOperation_comptable(operation_repository.findByTenant_IdAndId(tenant.getId(), operation_id)
                .orElseThrow(() -> new ResourceNotFoundException("Accounting operation", operation_id.toString())));
        cp.setJournal_comptable(journal_repository.findById(dto.getJournal_comptable_id())
                .orElseThrow(() -> new ResourceNotFoundException("Journal", dto.getJournal_comptable_id().toString())));
        cp.setCompte(dto.getCompte());
        cp.setSens(dto.getSens());
        cp.setType_montant(dto.getType_montant());
        cp.setEst_compte_tiers(dto.getEst_compte_tiers());
        cp.setNotes(dto.getNotes());
        cp.setCreated_at(LocalDateTime.now());
        cp.setUpdated_at(LocalDateTime.now());
        cp.setCreated_by(user);
        cp.setUpdated_by(user);
        return cp;
    }

    /**
     * Logs an action in the audit journal and sends a Kafka message.
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
        kafka_service.sendAuditLog(audit, tenant.getId().toString(), action);
    }

    /**
     * Maps a DTO to an entity for creation.
     */
    private OperationComptable mapToEntity(OperationComptableDto dto, Tenant tenant) {
        OperationComptable op = new OperationComptable();
        op.setTenant(tenant);
        op.setType_operation(dto.getType_operation());
        op.setMode_reglement(dto.getMode_reglement());
        op.setCompte_principal(dto.getCompte_principal());
        op.setEst_compte_statique(dto.getEst_compte_statique());
        op.setSens_principal(dto.getSens_principal());
        op.setJournal_comptable(journal_repository.findById(dto.getJournal_comptable_id())
                .orElseThrow(() -> new ResourceNotFoundException("Journal", dto.getJournal_comptable_id().toString())));
        op.setType_montant(dto.getType_montant());
        op.setPlafond_client(dto.getPlafond_client() != null ? dto.getPlafond_client() : BigDecimal.ZERO);
        op.setActif(dto.getActif());
        op.setNotes(dto.getNotes());
        return op;
    }

    /**
     * Maps an OperationComptable entity to its DTO representation.
     *
     * @param op the OperationComptable entity
     * @return the corresponding OperationComptableDto
     */
    private OperationComptableDto mapToDto(OperationComptable op) {
        List<ContrepartieDto> contrepartie_dtos = contrepartie_repository
                .findByTenant_IdAndOperation_comptable_Id(op.getTenant().getId(), op.getId())
                .stream()
                .map((Contrepartie cp) -> ContrepartieDto.builder()
                        .id(cp.getId())
                        .operation_comptable_id(op.getId())
                        .compte(cp.getCompte())
                        .sens(cp.getSens())
                        .est_compte_tiers(cp.getEst_compte_tiers())
                        .type_montant(cp.getType_montant())
                        .notes(cp.getNotes())
                        .journal_comptable_id(cp.getJournal_comptable().getId())
                        .created_at(cp.getCreated_at())
                        .updated_at(cp.getUpdated_at())
                        .build())
                .collect(Collectors.toList());

        return OperationComptableDto.builder()
                .id(op.getId())
                .type_operation(op.getType_operation())
                .mode_reglement(op.getMode_reglement())
                .compte_principal(op.getCompte_principal())
                .est_compte_statique(op.getEst_compte_statique())
                .sens_principal(op.getSens_principal())
                .journal_comptable_id(op.getJournal_comptable() != null ? op.getJournal_comptable().getId() : null)
                .type_montant(op.getType_montant())
                .plafond_client(op.getPlafond_client() != null ? op.getPlafond_client() : BigDecimal.ZERO)
                .actif(op.getActif())
                .notes(op.getNotes())
                .created_at(op.getCreated_at())
                .updated_at(op.getUpdated_at())
                .created_by(op.getCreated_by())
                .updated_by(op.getUpdated_by())
                .contreparties(contrepartie_dtos)
                .build();
    }
}