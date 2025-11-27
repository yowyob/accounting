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
import com.yowyob.erp.accounting.entity.PlanComptable;
import com.yowyob.erp.accounting.entity.PlanComptableTemplate;
import com.yowyob.erp.accounting.entity.Tenant;
import com.yowyob.erp.accounting.repository.PlanComptableRepository;
import com.yowyob.erp.accounting.repository.PlanComptableTemplateRepository;
import com.yowyob.erp.common.exception.BusinessException;
import com.yowyob.erp.common.exception.ResourceNotFoundException;
import com.yowyob.erp.common.service.ValidationService;
import com.yowyob.erp.config.kafka.KafkaMessageService;
import com.yowyob.erp.config.redis.RedisService;
import com.yowyob.erp.config.tenant.TenantContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlanComptableService {

    private final PlanComptableRepository repository;
    private final PlanComptableTemplateRepository templateRepository;
    private final ValidationService validationService;
    private final KafkaMessageService kafkaMessageService;
    private final RedisService redisService;

    // --- Constantes cache Redis ---
    private static final String CACHE_ALL = "plancomptable:all:";
    private static final String CACHE_ACTIVE = "plancomptable:active:";
    private static final String CACHE_SINGLE = "plancomptable:id:";
    private static final String CACHE_PREFIX = "plancomptable:prefix:";
    private static final String CACHE_CLASS = "plancomptable:class:";

    /* ============================================================================
     * CREATE ACCOUNT
     * ========================================================================== */



    @Transactional
    public void initialiserPlanComptablePourTenant(UUID tenantId) {
        // 1. On charge le modèle OHADA officiel (les 713 comptes)
        List<PlanComptableTemplate> template = templateRepository.findAll();

        //On doit recuperer le tenant par I
        Tenant tenant = TenantContext.getCurrentTenantAsTenant();

        String currentUser = Optional.ofNullable(TenantContext.getCurrentUser()).orElse("system");


        // 2. On copie TOUT dans la table compte avec le tenant_id
        template.forEach(dto -> {
            PlanComptable account = new PlanComptable();

                account.setTenant(tenant);
                account.setNoCompte(dto.getNumero());
                account.setClasse(dto.getClasse());
                account.setLibelle(dto.getLibelle());
                account.setNotes(dto.getNotes());
                account.setActif(true);
                account.setCreatedAt(LocalDateTime.now());
                account.setUpdatedAt(LocalDateTime.now());
                account.setCreatedBy(currentUser);
                account.setUpdatedBy(currentUser);

            repository.save(account);

        });

        // 3. Optionnel : on crée les comptes de base du tenant
        // ex: 41110001 - Client par défaut, 40110001 - Fournisseur par défaut, etc.
    }


    @Transactional
    public PlanComptableDto createAccount(PlanComptableDto dto) {
        UUID tenantId = TenantContext.getCurrentTenant();
        Tenant tenant = TenantContext.getCurrentTenantAsTenant();

        String currentUser = Optional.ofNullable(TenantContext.getCurrentUser()).orElse("system");
        log.info("🧾 Création du compte comptable {} pour le tenant {}", dto.getNoCompte(), tenantId);

        // ✅ Validation du numéro de compte
        validationService.validateAccountNumber(dto.getNoCompte());

        // Vérification de l’unicité
        if (repository.existsByTenantIdAndNoCompte(tenantId, dto.getNoCompte())) {
            throw new BusinessException("Un compte avec ce numéro existe déjà : " + dto.getNoCompte());
        }

        // Création de l'entité
        PlanComptable account = new PlanComptable();
        account.setTenant(tenant);
        account.setNoCompte(dto.getNoCompte());
        account.setClasse(Character.getNumericValue(dto.getNoCompte().charAt(0)));
        account.setLibelle(dto.getLibelle());
        account.setNotes(dto.getNotes());
        account.setActif(true);
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());
        account.setCreatedBy(currentUser);
        account.setUpdatedBy(currentUser);

        PlanComptable saved = repository.save(account);
        PlanComptableDto result = mapToDto(saved);

        kafkaMessageService.sendAuditLog(result, tenantId.toString(), "CREATION");
        redisService.delete(CACHE_ALL + tenantId);
        log.info("✅ Compte comptable créé : {} - {}", saved.getNoCompte(), saved.getLibelle());

        return result;
    }

    /* ============================================================================
     * READ
     * ========================================================================== */
    public List<PlanComptableDto> getAllAccounts() {
        UUID tenantId = TenantContext.getCurrentTenant();
        String cacheKey = CACHE_ALL + tenantId;

        List<PlanComptableDto> cached = redisService.get(cacheKey, List.class);
        if (cached != null) return cached;

        List<PlanComptableDto> comptes = repository.findByTenant_Id(tenantId)
                .stream().map(this::mapToDto).collect(Collectors.toList());
        redisService.save(cacheKey, comptes, Duration.ofMinutes(15));
        return comptes;
    }

    public List<PlanComptableDto> getAllActiveAccounts() {
        UUID tenantId = TenantContext.getCurrentTenant();
        String cacheKey = CACHE_ACTIVE + tenantId;

        List<PlanComptableDto> cached = redisService.get(cacheKey, List.class);
        if (cached != null) return cached;

        List<PlanComptableDto> comptes = repository.findByTenant_IdAndActifTrue(tenantId)
                .stream().map(this::mapToDto).collect(Collectors.toList());
        redisService.save(cacheKey, comptes, Duration.ofMinutes(15));
        return comptes;
    }

    public PlanComptableDto getAccountById(UUID id) {
        UUID tenantId = TenantContext.getCurrentTenant();
        String cacheKey = CACHE_SINGLE + tenantId + ":" + id;

        PlanComptableDto cached = redisService.get(cacheKey, PlanComptableDto.class);
        if (cached != null) return cached;

        PlanComptable account = repository.findByTenant_IdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Compte Comptable", id.toString()));

        PlanComptableDto dto = mapToDto(account);
        redisService.save(cacheKey, dto, Duration.ofMinutes(15));
        return dto;
    }

    public List<PlanComptableDto> getAccountsByClass(Integer classe) {
        UUID tenantId = TenantContext.getCurrentTenant();
        String cacheKey = CACHE_CLASS + tenantId + ":" + classe;

        List<PlanComptableDto> cached = redisService.get(cacheKey, List.class);
        if (cached != null) return cached;

        List<PlanComptableDto> comptes = repository.findByTenant_IdAndClasse(tenantId, classe)
                .stream().map(this::mapToDto).collect(Collectors.toList());
        redisService.save(cacheKey, comptes, Duration.ofMinutes(20));
        return comptes;
    }

    public List<PlanComptableDto> getAccountsByPrefix(String prefix) {
        UUID tenantId = TenantContext.getCurrentTenant();
        String cacheKey = CACHE_PREFIX + tenantId + ":" + prefix;

        List<PlanComptableDto> cached = redisService.get(cacheKey, List.class);
        if (cached != null) return cached;

        List<PlanComptableDto> comptes = repository.findByTenant_IdAndNoCompteStartingWith(tenantId, prefix)
                .stream().map(this::mapToDto).collect(Collectors.toList());
        redisService.save(cacheKey, comptes, Duration.ofMinutes(20));
        return comptes;
    }

    /* ============================================================================
     * UPDATE
     * ========================================================================== */
    @Transactional
    public PlanComptableDto updateAccount(UUID id, PlanComptableDto dto) {
        UUID tenantId = TenantContext.getCurrentTenant();
        String currentUser = Optional.ofNullable(TenantContext.getCurrentUser()).orElse("system");

        PlanComptable account = repository.findByTenant_IdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Compte Comptable", id.toString()));

        account.setLibelle(dto.getLibelle());
        account.setNotes(dto.getNotes());
        account.setUpdatedAt(LocalDateTime.now());
        account.setUpdatedBy(currentUser);

        PlanComptable saved = repository.save(account);
        PlanComptableDto result = mapToDto(saved);

        kafkaMessageService.sendAuditLog(result, tenantId.toString(), "PLAN_COMPTABLE_UPDATED");
        redisService.delete(CACHE_SINGLE + tenantId + ":" + id);
        redisService.delete(CACHE_ALL + tenantId);
        log.info("✏️ Compte mis à jour : {}", saved.getNoCompte());

        return result;
    }

    /* ============================================================================
     * DEACTIVATE
     * ========================================================================== */
    @Transactional
    public void deactivateAccount(UUID id) {
        UUID tenantId = TenantContext.getCurrentTenant();
        String currentUser = Optional.ofNullable(TenantContext.getCurrentUser()).orElse("system");

        PlanComptable account = repository.findByTenant_IdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Compte Comptable", id.toString()));

        account.setActif(false);
        account.setUpdatedAt(LocalDateTime.now());
        account.setUpdatedBy(currentUser);
        repository.save(account);

        kafkaMessageService.sendAuditLog(account, tenantId.toString(), "PLAN_COMPTABLE_DEACTIVATED");
        redisService.delete(CACHE_SINGLE + tenantId + ":" + id);
        redisService.delete(CACHE_ACTIVE + tenantId);
        log.info("🛑 Compte désactivé : {}", account.getNoCompte());
    }

    /* ============================================================================
     * MAPPING
     * ========================================================================== */
    private PlanComptableDto mapToDto(PlanComptable entity) {
        return PlanComptableDto.builder()
                .id(entity.getId())
                .noCompte(entity.getNoCompte())
                .libelle(entity.getLibelle())
                .classe(entity.getClasse())
                .notes(entity.getNotes())
                .actif(entity.getActif())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }
}
