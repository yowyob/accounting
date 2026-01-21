package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.dto.JournalAuditDto;
import com.yowyob.erp.accounting.dto.TaxeDto;
import com.yowyob.erp.accounting.entity.JournalAudit;
import com.yowyob.erp.accounting.entity.Taxe;
import com.yowyob.erp.accounting.entity.Tenant;
import com.yowyob.erp.accounting.repository.TaxeRepository;
import com.yowyob.erp.common.exception.ResourceNotFoundException;
import com.yowyob.erp.config.kafka.KafkaMessageService;
import com.yowyob.erp.config.redis.RedisService;
import com.yowyob.erp.config.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing taxes.
 * 
 * @author ALD
 * @date 30.09.25
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TaxeService {

    private final TaxeRepository taxe_repository;
    private final RedisService redis_service;
    private final KafkaMessageService kafka_service;

    private static final String CACHE_TAXE_ALL = "taxe:all:";
    private static final String CACHE_TAXE_ACTIVE = "taxe:active:";
    private static final String CACHE_TAXE_SINGLE = "taxe:single:";

    @Transactional
    public TaxeDto createTaxe(TaxeDto dto) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        Tenant tenant = TenantContext.getCurrentTenantAsTenant();
        String user = Optional.ofNullable(TenantContext.getCurrentUser()).orElse("system");

        if (taxe_repository.existsByTenant_IdAndCode(tenant_id, dto.getCode())) {
            throw new IllegalArgumentException("Tax code already in use: " + dto.getCode());
        }

        Taxe entity = Taxe.builder()
                .tenant(tenant)
                .code(dto.getCode())
                .libelle(dto.getLibelle())
                .taux(dto.getTaux())
                .compte_collecte(dto.getCompte_collecte())
                .compte_deductible(dto.getCompte_deductible())
                .pays(dto.getPays())
                .date_debut_validite(dto.getDate_debut_validite())
                .date_fin_validite(dto.getDate_fin_validite())
                .actif(true)
                .created_at(LocalDateTime.now())
                .build();

        Taxe saved = taxe_repository.save(entity);
        logAudit(tenant, user, "TAXE_CREATED", "Creation of tax " + dto.getCode());

        invalidateCache(tenant_id);
        return mapToDto(saved);
    }

    @Transactional
    public TaxeDto updateTaxe(UUID id, TaxeDto dto) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        String user = Optional.ofNullable(TenantContext.getCurrentUser()).orElse("system");

        Taxe existing = taxe_repository.findByTenant_IdAndId(tenant_id, id)
                .orElseThrow(() -> new ResourceNotFoundException("Taxe", id.toString()));

        if (!existing.getCode().equals(dto.getCode())
                && taxe_repository.existsByTenant_IdAndCode(tenant_id, dto.getCode())) {
            throw new IllegalArgumentException("Tax code already in use: " + dto.getCode());
        }

        existing.setCode(dto.getCode());
        existing.setLibelle(dto.getLibelle());
        existing.setTaux(dto.getTaux());
        existing.setCompte_collecte(dto.getCompte_collecte());
        existing.setCompte_deductible(dto.getCompte_deductible());
        existing.setPays(dto.getPays());
        existing.setDate_debut_validite(dto.getDate_debut_validite());
        existing.setDate_fin_validite(dto.getDate_fin_validite());
        existing.setActif(dto.isActif());

        Taxe saved = taxe_repository.save(existing);
        logAudit(TenantContext.getCurrentTenantAsTenant(), user, "TAXE_UPDATED", "Update of tax " + dto.getCode());

        invalidateCache(tenant_id);
        redis_service.delete(CACHE_TAXE_SINGLE + tenant_id + ":" + id);

        return mapToDto(saved);
    }

    public Optional<TaxeDto> getTaxe(UUID id) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        String cache_key = CACHE_TAXE_SINGLE + tenant_id + ":" + id;

        TaxeDto cached = redis_service.get(cache_key, TaxeDto.class);
        if (cached != null)
            return Optional.of(cached);

        return taxe_repository.findByTenant_IdAndId(tenant_id, id)
                .map(taxe -> {
                    TaxeDto dto = mapToDto(taxe);
                    redis_service.save(cache_key, dto, Duration.ofMinutes(15));
                    return dto;
                });
    }

    @SuppressWarnings("unchecked")
    public List<TaxeDto> getAllTaxes() {
        UUID tenant_id = TenantContext.getCurrentTenant();
        String cache_key = CACHE_TAXE_ALL + tenant_id;

        List<TaxeDto> cached = redis_service.get(cache_key, List.class);
        if (cached != null)
            return cached;

        List<TaxeDto> taxes = taxe_repository.findByTenant_Id(tenant_id)
                .stream().map(this::mapToDto).collect(Collectors.toList());

        redis_service.save(cache_key, taxes, Duration.ofMinutes(10));
        return taxes;
    }

    @SuppressWarnings("unchecked")
    public List<TaxeDto> getActiveTaxes() {
        UUID tenant_id = TenantContext.getCurrentTenant();
        String cache_key = CACHE_TAXE_ACTIVE + tenant_id;

        List<TaxeDto> cached = redis_service.get(cache_key, List.class);
        if (cached != null)
            return cached;

        List<TaxeDto> taxes = taxe_repository.findByTenant_IdAndActifTrue(tenant_id)
                .stream().map(this::mapToDto).collect(Collectors.toList());

        redis_service.save(cache_key, taxes, Duration.ofMinutes(10));
        return taxes;
    }

    @Transactional
    public void deleteTaxe(UUID id) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        String user = Optional.ofNullable(TenantContext.getCurrentUser()).orElse("system");

        Taxe taxe = taxe_repository.findByTenant_IdAndId(tenant_id, id)
                .orElseThrow(() -> new ResourceNotFoundException("Taxe", id.toString()));

        taxe_repository.delete(taxe);
        logAudit(TenantContext.getCurrentTenantAsTenant(), user, "TAXE_DELETED", "Deletion of tax " + taxe.getCode());

        invalidateCache(tenant_id);
        redis_service.delete(CACHE_TAXE_SINGLE + tenant_id + ":" + id);
    }

    private void invalidateCache(UUID tenant_id) {
        redis_service.delete(CACHE_TAXE_ALL + tenant_id);
        redis_service.delete(CACHE_TAXE_ACTIVE + tenant_id);
    }

    private TaxeDto mapToDto(Taxe entity) {
        return TaxeDto.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .libelle(entity.getLibelle())
                .taux(entity.getTaux())
                .compte_collecte(entity.getCompte_collecte())
                .compte_deductible(entity.getCompte_deductible())
                .pays(entity.getPays())
                .date_debut_validite(entity.getDate_debut_validite())
                .date_fin_validite(entity.getDate_fin_validite())
                .actif(entity.isActif())
                .created_at(entity.getCreated_at())
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
