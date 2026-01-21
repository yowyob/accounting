package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.dto.DeviseDto;
import com.yowyob.erp.accounting.dto.JournalAuditDto;
import com.yowyob.erp.accounting.entity.Devise;
import com.yowyob.erp.accounting.entity.JournalAudit;
import com.yowyob.erp.accounting.entity.Tenant;
import com.yowyob.erp.accounting.repository.DeviseRepository;
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
 * Service for managing currencies.
 * 
 * @author ALD
 * @date 30.09.25
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeviseService {

    private final DeviseRepository devise_repository;
    private final RedisService redis_service;
    private final KafkaMessageService kafka_service;

    private static final String CACHE_DEVISE_ALL = "devise:all";
    private static final String CACHE_DEVISE_ACTIVE = "devise:active";
    private static final String CACHE_DEVISE_SINGLE = "devise:single:";

    @Transactional
    public DeviseDto createDevise(DeviseDto dto) {
        String user = Optional.ofNullable(TenantContext.getCurrentUser()).orElse("system");

        if (devise_repository.existsByCode(dto.getCode())) {
            throw new IllegalArgumentException("Currency code already in use: " + dto.getCode());
        }

        Devise entity = Devise.builder()
                .code(dto.getCode())
                .nom(dto.getNom())
                .symbole(dto.getSymbole())
                .est_nationale(dto.isEst_nationale())
                .actif(true)
                .created_at(LocalDateTime.now())
                .build();

        Devise saved = devise_repository.save(entity);
        logAudit(TenantContext.getCurrentTenantAsTenant(), user, "DEVISE_CREATED",
                "Creation of currency " + dto.getCode());

        invalidateCache();
        return mapToDto(saved);
    }

    @Transactional
    public DeviseDto updateDevise(UUID id, DeviseDto dto) {
        String user = Optional.ofNullable(TenantContext.getCurrentUser()).orElse("system");

        Devise existing = devise_repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Devise", id.toString()));

        if (!existing.getCode().equals(dto.getCode()) && devise_repository.existsByCode(dto.getCode())) {
            throw new IllegalArgumentException("Currency code already in use: " + dto.getCode());
        }

        existing.setCode(dto.getCode());
        existing.setNom(dto.getNom());
        existing.setSymbole(dto.getSymbole());
        existing.setEst_nationale(dto.isEst_nationale());
        existing.setActif(dto.isActif());

        Devise saved = devise_repository.save(existing);
        logAudit(TenantContext.getCurrentTenantAsTenant(), user, "DEVISE_UPDATED",
                "Update of currency " + dto.getCode());

        invalidateCache();
        redis_service.delete(CACHE_DEVISE_SINGLE + id);

        return mapToDto(saved);
    }

    public Optional<DeviseDto> getDevise(UUID id) {
        String cache_key = CACHE_DEVISE_SINGLE + id;

        DeviseDto cached = redis_service.get(cache_key, DeviseDto.class);
        if (cached != null)
            return Optional.of(cached);

        return devise_repository.findById(id)
                .map(devise -> {
                    DeviseDto dto = mapToDto(devise);
                    redis_service.save(cache_key, dto, Duration.ofMinutes(30));
                    return dto;
                });
    }

    @SuppressWarnings("unchecked")
    public List<DeviseDto> getAllDevises() {
        List<DeviseDto> cached = redis_service.get(CACHE_DEVISE_ALL, List.class);
        if (cached != null)
            return cached;

        List<DeviseDto> devises = devise_repository.findAll()
                .stream().map(this::mapToDto).collect(Collectors.toList());

        redis_service.save(CACHE_DEVISE_ALL, devises, Duration.ofMinutes(15));
        return devises;
    }

    @SuppressWarnings("unchecked")
    public List<DeviseDto> getActiveDevises() {
        List<DeviseDto> cached = redis_service.get(CACHE_DEVISE_ACTIVE, List.class);
        if (cached != null)
            return cached;

        List<DeviseDto> devises = devise_repository.findByActifTrue()
                .stream().map(this::mapToDto).collect(Collectors.toList());

        redis_service.save(CACHE_DEVISE_ACTIVE, devises, Duration.ofMinutes(15));
        return devises;
    }

    @Transactional
    public void deleteDevise(UUID id) {
        String user = Optional.ofNullable(TenantContext.getCurrentUser()).orElse("system");

        Devise devise = devise_repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Devise", id.toString()));

        devise_repository.delete(devise);
        logAudit(TenantContext.getCurrentTenantAsTenant(), user, "DEVISE_DELETED",
                "Deletion of currency " + devise.getCode());

        invalidateCache();
        redis_service.delete(CACHE_DEVISE_SINGLE + id);
    }

    private void invalidateCache() {
        redis_service.delete(CACHE_DEVISE_ALL);
        redis_service.delete(CACHE_DEVISE_ACTIVE);
    }

    private DeviseDto mapToDto(Devise entity) {
        return DeviseDto.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .nom(entity.getNom())
                .symbole(entity.getSymbole())
                .est_nationale(entity.isEst_nationale())
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
