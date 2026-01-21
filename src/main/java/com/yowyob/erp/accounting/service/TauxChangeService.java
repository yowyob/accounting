package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.dto.JournalAuditDto;
import com.yowyob.erp.accounting.dto.TauxChangeDto;
import com.yowyob.erp.accounting.entity.Devise;
import com.yowyob.erp.accounting.entity.JournalAudit;
import com.yowyob.erp.accounting.entity.TauxChange;
import com.yowyob.erp.accounting.entity.Tenant;
import com.yowyob.erp.accounting.repository.DeviseRepository;
import com.yowyob.erp.accounting.repository.TauxChangeRepository;
import com.yowyob.erp.common.exception.ResourceNotFoundException;
import com.yowyob.erp.config.kafka.KafkaMessageService;
import com.yowyob.erp.config.redis.RedisService;
import com.yowyob.erp.config.tenant.TenantContext;
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
 * Service for managing exchange rates.
 * 
 * @author ALD
 * @date 30.09.25
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TauxChangeService {

    private final TauxChangeRepository taux_repository;
    private final DeviseRepository devise_repository;
    private final RedisService redis_service;
    private final KafkaMessageService kafka_service;

    private static final String CACHE_TAUX_TENANT = "taux:tenant:";
    private static final String CACHE_TAUX_PAIR = "taux:pair:";

    @Transactional
    public TauxChangeDto createTauxChange(TauxChangeDto dto) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        Tenant tenant = TenantContext.getCurrentTenantAsTenant();
        String user = Optional.ofNullable(TenantContext.getCurrentUser()).orElse("system");

        Devise source = devise_repository.findById(dto.getDevise_source_id())
                .orElseThrow(() -> new ResourceNotFoundException("Devise", dto.getDevise_source_id().toString()));
        Devise target = devise_repository.findById(dto.getDevise_cible_id())
                .orElseThrow(() -> new ResourceNotFoundException("Devise", dto.getDevise_cible_id().toString()));

        TauxChange entity = TauxChange.builder()
                .tenant(tenant)
                .devise_source(source)
                .devise_cible(target)
                .taux(dto.getTaux())
                .date_effet(dto.getDate_effet())
                .notes(dto.getNotes())
                .created_at(LocalDateTime.now())
                .build();

        TauxChange saved = taux_repository.save(entity);
        logAudit(tenant, user, "TAUX_CHANGE_CREATED",
                String.format("New rate for %s -> %s: %s", source.getCode(), target.getCode(), dto.getTaux()));

        invalidateCache(tenant_id, source.getId(), target.getId());
        return mapToDto(saved);
    }

    public List<TauxChangeDto> getTenantRates() {
        UUID tenant_id = TenantContext.getCurrentTenant();
        String cache_key = CACHE_TAUX_TENANT + tenant_id;

        List<TauxChangeDto> cached = redis_service.get(cache_key, List.class);
        if (cached != null)
            return cached;

        List<TauxChangeDto> rates = taux_repository.findByTenant_Id(tenant_id)
                .stream().map(this::mapToDto).collect(Collectors.toList());

        redis_service.save(cache_key, rates, Duration.ofMinutes(15));
        return rates;
    }

    /**
     * Gets the most recent rate for a currency pair at a specific date.
     */
    public Optional<TauxChangeDto> getLatestRate(UUID sourceId, UUID targetId, LocalDateTime date) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        // Caching pair rates is harder due to the date parameter, we keep it simple for
        // now without cache or with very short TTL

        return taux_repository.findMostRecentRate(tenant_id, sourceId, targetId, date)
                .map(this::mapToDto);
    }

    @Transactional
    public void deleteTauxChange(UUID id) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        String user = Optional.ofNullable(TenantContext.getCurrentUser()).orElse("system");

        TauxChange rate = taux_repository.findById(id)
                .filter(r -> r.getTenant().getId().equals(tenant_id))
                .orElseThrow(() -> new ResourceNotFoundException("TauxChange", id.toString()));

        taux_repository.delete(rate);
        logAudit(TenantContext.getCurrentTenantAsTenant(), user, "TAUX_CHANGE_DELETED",
                String.format("Deletion of rate %s -> %s", rate.getDevise_source().getCode(),
                        rate.getDevise_cible().getCode()));

        invalidateCache(tenant_id, rate.getDevise_source().getId(), rate.getDevise_cible().getId());
    }

    private void invalidateCache(UUID tenant_id, UUID sourceId, UUID targetId) {
        redis_service.delete(CACHE_TAUX_TENANT + tenant_id);
    }

    private TauxChangeDto mapToDto(TauxChange entity) {
        return TauxChangeDto.builder()
                .id(entity.getId())
                .devise_source_id(entity.getDevise_source().getId())
                .devise_source_code(entity.getDevise_source().getCode())
                .devise_cible_id(entity.getDevise_cible().getId())
                .devise_cible_code(entity.getDevise_cible().getCode())
                .taux(entity.getTaux())
                .date_effet(entity.getDate_effet())
                .notes(entity.getNotes())
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
