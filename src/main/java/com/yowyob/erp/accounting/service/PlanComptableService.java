package com.yowyob.erp.accounting.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yowyob.erp.accounting.dto.PlanComptableDto;
import com.yowyob.erp.accounting.entity.JournalAudit;
import com.yowyob.erp.accounting.entity.PlanComptable;
import com.yowyob.erp.accounting.entity.PlanComptableTemplate;
import com.yowyob.erp.accounting.entity.Tenant;
import com.yowyob.erp.accounting.repository.PlanComptableRepository;
import com.yowyob.erp.accounting.repository.PlanComptableTemplateRepository;
import com.yowyob.erp.accounting.dto.JournalAuditDto;
import com.yowyob.erp.common.exception.BusinessException;
import com.yowyob.erp.common.exception.ResourceNotFoundException;
import com.yowyob.erp.common.service.ValidationService;
import com.yowyob.erp.config.kafka.KafkaMessageService;
import com.yowyob.erp.config.redis.RedisService;
import com.yowyob.erp.config.tenant.TenantContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing the accounting plan (Plan Comptable).
 * Handles account creation, initialization from template, and searches.
 * 
 * @author ALD
 * @date 30.09.25
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlanComptableService {

    private final PlanComptableRepository account_repository;
    private final PlanComptableTemplateRepository template_repository;
    private final ValidationService validation_service;
    private final KafkaMessageService kafka_service;
    private final RedisService redis_service;

    // --- Redis Cache Constants ---
    private static final String CACHE_ALL = "plancomptable:all:";
    private static final String CACHE_ACTIVE = "plancomptable:active:";
    private static final String CACHE_SINGLE = "plancomptable:id:";
    private static final String CACHE_PREFIX = "plancomptable:prefix:";
    private static final String CACHE_CLASS = "plancomptable:class:";

    /**
     * Initializes the accounting plan for a specific tenant using the official
     * template.
     * 
     * @param tenant_id the ID of the tenant to initialize
     */
    @Transactional
    public void initializePlanComptableForTenant(UUID tenant_id) {
        log.info("Initializing accounting plan for tenant: {}", tenant_id);
        // Load the official OHADA template (713 accounts)
        List<PlanComptableTemplate> templates = template_repository.findAll();

        Tenant tenant = TenantContext.getCurrentTenantAsTenant();
        String current_user = Optional.ofNullable(TenantContext.getCurrentUser()).orElse("system");

        // Copy everything to the account table for this tenant
        for (PlanComptableTemplate template : templates) {
            PlanComptable account = PlanComptable.builder()
                    .tenant(tenant)
                    .no_compte(template.getNumero())
                    .classe(template.getClasse())
                    .libelle(template.getLibelle())
                    .notes(template.getNotes())
                    .actif(true)
                    .created_at(LocalDateTime.now())
                    .updated_at(LocalDateTime.now())
                    .created_by(current_user)
                    .updated_by(current_user)
                    .build();
            account_repository.save(account);
        }

        log.info("Successfully initialized plan for tenant {}", tenant_id);
    }

    /**
     * Creates a new accounting account.
     * 
     * @param dto the account data
     * @return the created account DTO
     */
    @Transactional
    public PlanComptableDto createAccount(PlanComptableDto dto) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        Tenant tenant = TenantContext.getCurrentTenantAsTenant();

        String current_user = Optional.ofNullable(TenantContext.getCurrentUser()).orElse("system");
        log.info("Creating account {} for tenant {}", dto.getNo_compte(), tenant_id);

        // Account number validation
        validation_service.validateAccountNumber(dto.getNo_compte());

        // Uniqueness check
        if (account_repository.existsByTenant_IdAndNo_compte(tenant_id, dto.getNo_compte())) {
            throw new BusinessException("Account already exists: " + dto.getNo_compte());
        }

        // Entity creation
        PlanComptable account = PlanComptable.builder()
                .tenant(tenant)
                .no_compte(dto.getNo_compte())
                .classe(Character.getNumericValue(dto.getNo_compte().charAt(0)))
                .libelle(dto.getLibelle())
                .notes(dto.getNotes())
                .actif(true)
                .created_at(LocalDateTime.now())
                .updated_at(LocalDateTime.now())
                .created_by(current_user)
                .updated_by(current_user)
                .build();

        PlanComptable saved = account_repository.save(account);
        PlanComptableDto result = mapToDto(saved);

        logAudit(tenant, current_user, "ACCOUNT_CREATED", "Creation of account: " + saved.getNo_compte());
        redis_service.delete(CACHE_ALL + tenant_id);
        log.info("✅ Account created: {} - {}", saved.getNo_compte(), saved.getLibelle());

        return result;
    }

    /**
     * Retrieves all accounts for the current tenant.
     * 
     * @return list of account DTOs
     */
    @SuppressWarnings("unchecked")
    public List<PlanComptableDto> getAllAccounts() {
        UUID tenant_id = TenantContext.getCurrentTenant();
        String key = CACHE_ALL + tenant_id;

        List<PlanComptableDto> cached = redis_service.get(key, List.class);
        if (cached != null)
            return cached;

        List<PlanComptableDto> list = account_repository.findByTenant_Id(tenant_id)
                .stream().map(this::mapToDto).collect(Collectors.toList());
        redis_service.save(key, list, Duration.ofMinutes(15));
        return list;
    }

    /**
     * Retrieves all active accounts for the current tenant.
     * 
     * @return list of active account DTOs
     */
    @SuppressWarnings("unchecked")
    public List<PlanComptableDto> getAllActiveAccounts() {
        UUID tenant_id = TenantContext.getCurrentTenant();
        String key = CACHE_ACTIVE + tenant_id;

        List<PlanComptableDto> cached = redis_service.get(key, List.class);
        if (cached != null)
            return cached;

        List<PlanComptableDto> list = account_repository.findByTenant_IdAndActifTrue(tenant_id)
                .stream().map(this::mapToDto).collect(Collectors.toList());
        redis_service.save(key, list, Duration.ofMinutes(15));
        return list;
    }

    /**
     * Retrieves an account by its ID.
     * 
     * @param id account ID
     * @return account DTO
     */
    public PlanComptableDto getAccountById(UUID id) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        String key = CACHE_SINGLE + tenant_id + ":" + id;

        PlanComptableDto cached = redis_service.get(key, PlanComptableDto.class);
        if (cached != null)
            return cached;

        PlanComptable account = account_repository.findByTenant_IdAndId(tenant_id, id)
                .orElseThrow(() -> new ResourceNotFoundException("Accounting account", id.toString()));

        PlanComptableDto dto = mapToDto(account);
        redis_service.save(key, dto, Duration.ofMinutes(15));
        return dto;
    }

    /**
     * Retrieves accounts by their class (1-7).
     * 
     * @param classe account class
     * @return list of account DTOs
     */
    @SuppressWarnings("unchecked")
    public List<PlanComptableDto> getAccountsByClass(Integer classe) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        String key = CACHE_CLASS + tenant_id + ":" + classe;

        List<PlanComptableDto> cached = redis_service.get(key, List.class);
        if (cached != null)
            return cached;

        List<PlanComptableDto> list = account_repository.findByTenant_IdAndClasse(tenant_id, classe)
                .stream().map(this::mapToDto).collect(Collectors.toList());
        redis_service.save(key, list, Duration.ofMinutes(20));
        return list;
    }

    /**
     * Retrieves accounts starting with a specific prefix.
     * 
     * @param prefix account prefix
     * @return list of account DTOs
     */
    @SuppressWarnings("unchecked")
    public List<PlanComptableDto> getAccountsByPrefix(String prefix) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        String key = CACHE_PREFIX + tenant_id + ":" + prefix;

        List<PlanComptableDto> cached = redis_service.get(key, List.class);
        if (cached != null)
            return cached;

        List<PlanComptableDto> list = account_repository.findByTenant_IdAndNo_compteStartingWith(tenant_id, prefix)
                .stream().map(this::mapToDto).collect(Collectors.toList());
        redis_service.save(key, list, Duration.ofMinutes(20));
        return list;
    }

    /**
     * Updates an existing account.
     * 
     * @param id  account ID
     * @param dto new account data
     * @return updated account DTO
     */
    @Transactional
    public PlanComptableDto updateAccount(UUID id, PlanComptableDto dto) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        String current_user = Optional.ofNullable(TenantContext.getCurrentUser()).orElse("system");

        PlanComptable account = account_repository.findByTenant_IdAndId(tenant_id, id)
                .orElseThrow(() -> new ResourceNotFoundException("Accounting account", id.toString()));

        account.setLibelle(dto.getLibelle());
        account.setNotes(dto.getNotes());
        account.setUpdated_at(LocalDateTime.now());
        account.setUpdated_by(current_user);

        PlanComptable saved = account_repository.save(account);
        PlanComptableDto result = mapToDto(saved);

        logAudit(TenantContext.getCurrentTenantAsTenant(), current_user, "ACCOUNT_UPDATED",
                "Update of account: " + saved.getNo_compte());
        redis_service.delete(CACHE_SINGLE + tenant_id + ":" + id);
        redis_service.delete(CACHE_ALL + tenant_id);
        log.info("✏️ Account updated: {}", saved.getNo_compte());

        return result;
    }

    /**
     * Deactivates an account by its ID.
     * 
     * @param id account ID
     */
    @Transactional
    public void deactivateAccount(UUID id) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        String current_user = Optional.ofNullable(TenantContext.getCurrentUser()).orElse("system");

        PlanComptable account = account_repository.findByTenant_IdAndId(tenant_id, id)
                .orElseThrow(() -> new ResourceNotFoundException("Accounting account", id.toString()));

        account.setActif(false);
        account.setUpdated_at(LocalDateTime.now());
        account.setUpdated_by(current_user);
        PlanComptable saved = account_repository.save(account);

        PlanComptableDto dto_out = mapToDto(saved);
        logAudit(TenantContext.getCurrentTenantAsTenant(), current_user, "ACCOUNT_DEACTIVATED",
                "Deactivation of account: " + saved.getNo_compte());
        redis_service.delete(CACHE_SINGLE + tenant_id + ":" + id);
        redis_service.delete(CACHE_ACTIVE + tenant_id);
        log.info("🛑 Account deactivated: {}", account.getNo_compte());
    }

    private PlanComptableDto mapToDto(PlanComptable entity) {
        return PlanComptableDto.builder()
                .id(entity.getId())
                .no_compte(entity.getNo_compte())
                .libelle(entity.getLibelle())
                .classe(entity.getClasse())
                .notes(entity.getNotes())
                .actif(entity.getActif())
                .created_at(entity.getCreated_at())
                .updated_at(entity.getUpdated_at())
                .created_by(entity.getCreated_by())
                .updated_by(entity.getUpdated_by())
                .build();
    }

    private void logAudit(Tenant tenant, String utilisateur, String action, String details) {
        JournalAudit audit = JournalAudit.builder()
                .tenant(tenant)
                .action(action)
                .utilisateur(utilisateur)
                .details(details)
                .date_action(LocalDateTime.now())
                .created_at(LocalDateTime.now())
                .updated_at(LocalDateTime.now())
                .created_by("system")
                .updated_by("system")
                .build();

        JournalAuditDto auditDto = JournalAuditDto.builder()
                .action(audit.getAction())
                .utilisateur(audit.getUtilisateur())
                .details(audit.getDetails())
                .date_action(audit.getDate_action())
                .build();

        kafka_service.sendAuditLog(auditDto, tenant.getId(), action);
    }
}
