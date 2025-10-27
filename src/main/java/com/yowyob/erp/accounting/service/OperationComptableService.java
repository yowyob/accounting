package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.dto.ContrepartieDto;
import com.yowyob.erp.accounting.dto.OperationComptableDto;
import com.yowyob.erp.accounting.entity.*;
import com.yowyob.erp.accounting.repository.*;
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
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing accounting operations.
 * Handles CRUD operations with caching, auditing, and multi-tenant support.
 *
 * @author ALD
 * @date 12/10/2025 02:27 PM WAT
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OperationComptableService {

    private final OperationComptableRepository operationRepository;
    private final ContrepartieRepository contrepartieRepository;
    private final JournalComptableRepository journalRepository;
    private final PlanComptableRepository planRepository;
    private final JournalAuditRepository auditRepository;
    private final Validator validator;
    private final KafkaMessageService kafkaMessageService;
    private final RedisService redisService;

    private static final String CACHE_OPERATIONS_ALL = "operations:all:";
    private static final String CACHE_OPERATION = "operation:";

    /* -------------------------------------------------------------------------
     *  CREATE
     * ---------------------------------------------------------------------- */
    @Transactional
    public OperationComptableDto createOperation(OperationComptableDto dto) {
        UUID tenantId = TenantContext.getCurrentTenant();
        Tenant tenant = TenantContext.getCurrentTenantAsTenant();
        String user = Optional.ofNullable(TenantContext.getCurrentUser()).orElse("system");
        log.info("📝 Creating accounting operation [{} - {}] for tenant {}", dto.getTypeOperation(), dto.getModeReglement(), tenantId);

        validateOperationDto(dto);

        // Validate journal and account
        journalRepository.findByTenant_IdAndId(tenantId, dto.getJournalComptableId())
                .filter(JournalComptable::getActif)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or inactive journal: " + dto.getJournalComptableId()));

        planRepository.findByTenant_IdAndNoCompte(tenantId, dto.getComptePrincipal())
                .filter(PlanComptable::getActif)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or inactive principal account: " + dto.getComptePrincipal()));

        // Check uniqueness of type + mode
        if (operationRepository.findByTenant_IdAndTypeOperationAndModeReglement(tenantId, dto.getTypeOperation(), dto.getModeReglement()).isPresent()) {
            throw new IllegalArgumentException("Existing operation: " + dto.getTypeOperation() + " / " + dto.getModeReglement());
        }

        OperationComptable entity = mapToEntity(dto, tenant);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setCreatedBy(user);
        entity.setUpdatedBy(user);

        OperationComptable saved = operationRepository.save(entity);

        // Counterparties
        if (dto.getContreparties() != null && !dto.getContreparties().isEmpty()) {
            List<Contrepartie> contreparties = dto.getContreparties()
                                                                                .stream()
                                                                                .map(
                                                                                    cpDto -> mapContrepartie(cpDto, tenant, saved.getId(), user))
                                                                                    .toList();
            contrepartieRepository.saveAll(contreparties);
        }

        kafkaMessageService.sendAuditLog(saved, tenantId.toString(), "OPERATION_CREATED");
        logAudit(tenant, user, "CREATE", "Creation of operation: " + dto.getTypeOperation());
        redisService.delete(CACHE_OPERATIONS_ALL + tenantId);

        return mapToDto(saved);
    }

    /* -------------------------------------------------------------------------
     *  READ
     * ---------------------------------------------------------------------- */
    public List<OperationComptableDto> getAllOperations() {
        UUID tenantId = TenantContext.getCurrentTenant();
        String cacheKey = CACHE_OPERATIONS_ALL + tenantId;

        List<OperationComptableDto> cached = redisService.get(cacheKey, List.class);
        if (cached != null) return cached;

        List<OperationComptableDto> operations = operationRepository.findByTenant_Id(tenantId)
                .stream().map(this::mapToDto).toList();

        redisService.save(cacheKey, operations, Duration.ofMinutes(10));
        return operations;
    }

    public Optional<OperationComptableDto> getOperation(UUID id) {
        UUID tenantId = TenantContext.getCurrentTenant();
        String cacheKey = CACHE_OPERATION + tenantId + ":" + id;

        OperationComptableDto cached = redisService.get(cacheKey, OperationComptableDto.class);
        if (cached != null) return Optional.of(cached);

        OperationComptable operation = operationRepository.findByTenant_IdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("OperationComptable", id.toString()));

        OperationComptableDto dto = mapToDto(operation);
        redisService.save(cacheKey, dto, Duration.ofMinutes(10));
        return Optional.of(dto);
    }

    public List<OperationComptableDto> getOperationsByCompte(String compte) {
        UUID tenantId = TenantContext.getCurrentTenant();
        return operationRepository.findByTenant_IdAndComptePrincipal(tenantId, compte)
                .stream().map(this::mapToDto).toList();
    }

    public Optional<OperationComptableDto> getByTypeAndMode(String type, String mode) {
        UUID tenantId = TenantContext.getCurrentTenant();
        return operationRepository.findByTenant_IdAndTypeOperationAndModeReglement(tenantId, type, mode)
                .map(this::mapToDto);
    }

    /* -------------------------------------------------------------------------
     *  UPDATE
     * ---------------------------------------------------------------------- */
    @Transactional
    public OperationComptableDto updateOperation(UUID id, OperationComptableDto dto) {
        UUID tenantId = TenantContext.getCurrentTenant();
        Tenant tenant = TenantContext.getCurrentTenantAsTenant();
        String user = Optional.ofNullable(TenantContext.getCurrentUser()).orElse("system");

        OperationComptable existing = operationRepository.findByTenant_IdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("OperationComptable", id.toString()));

        validateOperationDto(dto);

        existing.setTypeOperation(dto.getTypeOperation());
        existing.setModeReglement(dto.getModeReglement());
        existing.setComptePrincipal(dto.getComptePrincipal());
        existing.setSensPrincipal(dto.getSensPrincipal());
        existing.setJournalComptable(journalRepository.findById(dto.getJournalComptableId())
                        .orElseThrow(() -> new ResourceNotFoundException("Journal", dto.getJournalComptableId().toString())));
        existing.setTypeMontant(dto.getTypeMontant());
        existing.setPlafondClient(dto.getPlafondClient());
        existing.setEstCompteStatique(dto.getEstCompteStatique());
        existing.setActif(dto.getActif());
        existing.setUpdatedBy(user);
        existing.setUpdatedAt(LocalDateTime.now());

        OperationComptable saved = operationRepository.save(existing);

        // Counterparties
        contrepartieRepository.deleteByTenantIdAndOperationComptableId(tenantId, id);
        if (dto.getContreparties() != null && !dto.getContreparties().isEmpty()) {
            List<Contrepartie> contreparties = dto.getContreparties().stream().map(cpDto -> mapContrepartie(cpDto, tenant, id, user)).toList();
            contrepartieRepository.saveAll(contreparties);
        }

        kafkaMessageService.sendAuditLog(saved, tenantId.toString(), "OPERATION_UPDATED");
        logAudit(tenant, user, "UPDATE", "Update of operation: " + dto.getTypeOperation());
        redisService.delete(CACHE_OPERATIONS_ALL + tenantId);
        redisService.delete(CACHE_OPERATION + tenantId + ":" + id);

        return mapToDto(saved);
    }

    /* -------------------------------------------------------------------------
     *  DELETE
     * ---------------------------------------------------------------------- */
    @Transactional
    public void deleteOperation(UUID id) {
        UUID tenantId = TenantContext.getCurrentTenant();
        Tenant tenant = TenantContext.getCurrentTenantAsTenant();
        String user = Optional.ofNullable(TenantContext.getCurrentUser()).orElse("system");

        OperationComptable operation = operationRepository.findByTenant_IdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("OperationComptable", id.toString()));

        contrepartieRepository.deleteByTenantIdAndOperationComptableId(tenantId, id);
        operationRepository.delete(operation);

        kafkaMessageService.sendAuditLog(operation, tenantId.toString(), "OPERATION_DELETED");
        logAudit(tenant, user, "DELETE", "Deletion of operation: " + operation.getTypeOperation());
        redisService.delete(CACHE_OPERATIONS_ALL + tenantId);
        redisService.delete(CACHE_OPERATION + tenantId + ":" + id);
    }

    /* -------------------------------------------------------------------------
     *  VALIDATION & HELPERS
     * ---------------------------------------------------------------------- */
    private void validateOperationDto(OperationComptableDto dto) {
        var violations = validator.validate(dto);
        if (!violations.isEmpty()) throw new ConstraintViolationException(violations);
        if (!("DEBIT".equals(dto.getSensPrincipal()) || "CREDIT".equals(dto.getSensPrincipal()))) {
            throw new IllegalArgumentException("Sens principal must be DEBIT or CREDIT");
        }
        if (!("HT".equals(dto.getTypeMontant()) || "TTC".equals(dto.getTypeMontant()) || "TVA".equals(dto.getTypeMontant()) || "PAU".equals(dto.getTypeMontant()))) {
            throw new IllegalArgumentException("Type montant must be HT, TTC, TVA, or PAU");
        }
    }

    private Contrepartie mapContrepartie(ContrepartieDto dto, Tenant tenant, UUID operationId, String user) {
        Contrepartie cp = new Contrepartie();
        cp.setTenant(tenant);
        cp.setOperationComptable(operationRepository.findByTenant_IdAndId(tenant.getId(),operationId)
                        .orElseThrow(() -> new ResourceNotFoundException("Operation Comptable", operationId.toString())));
        cp.setJournalComptable(journalRepository.findById(dto.getJournalComptableId())
                        .orElseThrow(() -> new ResourceNotFoundException("Journal", dto.getJournalComptableId().toString())));
        cp.setCompte(dto.getCompte());
        cp.setSens(dto.getSens());
        cp.setTypeMontant(dto.getTypeMontant());
        cp.setEstCompteTiers(dto.getEstCompteTiers());
        cp.setNotes(dto.getNotes());
        cp.setCreatedAt(LocalDateTime.now());
        cp.setCreatedBy(user);
        return cp;
    }

    
    private void logAudit(Tenant tenant, String utilisateur, String action, String details) {
        JournalAudit audit = new JournalAudit();
        audit.setTenant(tenant);
        audit.setAction(action);
        audit.setUtilisateur(utilisateur);
        audit.setDetails(details);
        audit.setDateAction(LocalDateTime.now());
        auditRepository.save(audit);
        kafkaMessageService.sendAuditLog(audit, tenant.getId().toString(), action);
    }

    private OperationComptable mapToEntity(OperationComptableDto dto, Tenant tenant) {
        OperationComptable op = new OperationComptable();
        op.setTenant(tenant);
        op.setTypeOperation(dto.getTypeOperation());
        op.setModeReglement(dto.getModeReglement());
        op.setComptePrincipal(dto.getComptePrincipal());
        op.setEstCompteStatique(dto.getEstCompteStatique());
        op.setSensPrincipal(dto.getSensPrincipal());
        op.setJournalComptable(journalRepository.findById(dto.getJournalComptableId())
                        .orElseThrow(() -> new ResourceNotFoundException("Journal", dto.getJournalComptableId().toString())));
        op.setTypeMontant(dto.getTypeMontant());
        op.setPlafondClient(dto.getPlafondClient() != null ? dto.getPlafondClient() : BigDecimal.ZERO);
        op.setActif(dto.getActif());
        op.setNotes(dto.getNotes());
        return op;
    }

/**
 * Maps an OperationComptable entity to its DTO representation.
 *
 * @param op the OperationComptable entity
 * @return the corresponding OperationComptableDto
 * @author ALD
 * @date 12/10/2025 02:51 PM WAT
 */
private OperationComptableDto mapToDto(OperationComptable op) {
    List<ContrepartieDto> contrepartieDtos = contrepartieRepository.findByTenant_IdAndOperationComptable_Id(op.getTenant().getId(), op.getId())
            .stream()
            .map((Contrepartie cp) -> ContrepartieDto.builder() 
                    .compte(cp.getCompte())
                    .sens(cp.getSens())
                    .estCompteTiers(cp.getEstCompteTiers())
                    .notes(cp.getNotes())
                    .journalComptableId(cp.getJournalComptable().getId())
                    .createdAt(cp.getCreatedAt())
                    .build())
            .collect(Collectors.toList());

    return OperationComptableDto.builder()
            .id(op.getId())
            .typeOperation(op.getTypeOperation())
            .modeReglement(op.getModeReglement())
            .comptePrincipal(op.getComptePrincipal())
            .estCompteStatique(op.getEstCompteStatique())
            .sensPrincipal(op.getSensPrincipal())
            .journalComptableId(op.getJournalComptable() != null ? op.getJournalComptable().getId() : null)
            .typeMontant(op.getTypeMontant())
            .plafondClient(op.getPlafondClient() != null ? op.getPlafondClient() : BigDecimal.ZERO)
            .actif(op.getActif())
            .notes(op.getNotes())
            .createdAt(op.getCreatedAt())
            .updatedAt(op.getUpdatedAt())
            .createdBy(op.getCreatedBy())
            .updatedBy(op.getUpdatedBy())
            .contreparties(contrepartieDtos)
            .build();
    }
}