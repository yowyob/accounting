package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.dto.DeclarationFiscaleDto;
import com.yowyob.erp.accounting.entity.DeclarationFiscale;
import com.yowyob.erp.accounting.entity.Tenant;
import com.yowyob.erp.accounting.repository.DeclarationFiscaleRepository;
import com.yowyob.erp.common.exception.ResourceNotFoundException;
import com.yowyob.erp.config.kafka.KafkaMessageService;
import com.yowyob.erp.config.redis.RedisService;
import com.yowyob.erp.config.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing tax declarations (DeclarationFiscale).
 * Handles CRUD operations, retrieval by tenant, type, and period range.
 * Integrates with Redis for caching and Kafka for audit logging.
 * 
 * @author Leonel Delmat AZANGUE
 * @date 30.09.25
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeclarationFiscaleService {

    private final DeclarationFiscaleRepository declaration_repository;
    private final KafkaMessageService kafka_service;
    private final RedisService redis_service;

    private static final String CACHE_KEY_PREFIX = "declaration_fiscale:";

    /**
     * Creates or updates a tax declaration.
     * 
     * @param dto the declaration data
     * @return the saved declaration DTO
     */
    @Transactional
    public DeclarationFiscaleDto saveDeclaration(DeclarationFiscaleDto dto) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        Tenant tenant = TenantContext.getCurrentTenantAsTenant();
        String current_user = Optional.ofNullable(TenantContext.getCurrentUser()).orElse("system");

        DeclarationFiscale entity;
        if (dto.getId() != null) {
            entity = declaration_repository.findByTenant_IdAndId(tenant_id, dto.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Tax declaration", dto.getId().toString()));
            log.info("Updating tax declaration {} for tenant {}", dto.getId(), tenant_id);
        } else {
            entity = new DeclarationFiscale();
            entity.setTenant(tenant);
            entity.setCreated_by(current_user);
            log.info("Creating new tax declaration for tenant {}", tenant_id);
        }

        entity.setType_declaration(dto.getType_declaration());
        entity.setPeriode_debut(dto.getPeriode_debut());
        entity.setPeriode_fin(dto.getPeriode_fin());
        entity.setMontant_total(dto.getMontant_total());
        entity.setDate_generation(dto.getDate_generation());
        entity.setStatut(dto.getStatut());
        entity.setNumero_declaration(dto.getNumero_declaration());
        entity.setDonnees_declaration(dto.getDonnees_declaration());
        entity.setNotes(dto.getNotes());
        entity.setUpdated_by(current_user);

        DeclarationFiscale saved = declaration_repository.save(entity);
        DeclarationFiscaleDto result = mapToDto(saved);

        // Invalidate cache
        redis_service.delete(CACHE_KEY_PREFIX + "all:" + tenant_id);

        // Audit log
        kafka_service.sendAuditLog(result, tenant_id.toString(),
                dto.getId() != null ? "TAX_DECLARATION_UPDATED" : "TAX_DECLARATION_CREATED");

        return result;
    }

    /**
     * Retrieves a declaration by its ID.
     * 
     * @param id the declaration ID
     * @return an optional containing the declaration DTO
     */
    public Optional<DeclarationFiscaleDto> getById(UUID id) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        return declaration_repository.findByTenant_IdAndId(tenant_id, id).map(this::mapToDto);
    }

    /**
     * Lists all declarations for the current tenant.
     * 
     * @return list of declaration DTOs
     */
    @SuppressWarnings("unchecked")
    public List<DeclarationFiscaleDto> getAll() {
        UUID tenant_id = TenantContext.getCurrentTenant();
        String cache_key = CACHE_KEY_PREFIX + "all:" + tenant_id;

        List<DeclarationFiscaleDto> cached = (List<DeclarationFiscaleDto>) redis_service.get(cache_key, List.class);
        if (cached != null) {
            return cached;
        }

        List<DeclarationFiscaleDto> list = declaration_repository.findByTenant_IdOrderByDate_generationDesc(tenant_id)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        redis_service.save(cache_key, list, Duration.ofMinutes(15));
        return list;
    }

    /**
     * Lists declarations of a specific type.
     * 
     * @param type the declaration type
     * @return list of declaration DTOs
     */
    public List<DeclarationFiscaleDto> getByType(String type) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        return declaration_repository.findByTenant_IdAndType_declaration(tenant_id, type)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Searches for declarations within a period range.
     * 
     * @param start the start date
     * @param end   the end date
     * @return list of declaration DTOs
     */
    public List<DeclarationFiscaleDto> getByPeriodRange(LocalDate start, LocalDate end) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        return declaration_repository.findByTenant_IdAndPeriodRange(tenant_id, start, end)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Deletes a declaration.
     * 
     * @param id the declaration ID
     */
    @Transactional
    public void delete(UUID id) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        DeclarationFiscale entity = declaration_repository.findByTenant_IdAndId(tenant_id, id)
                .orElseThrow(() -> new ResourceNotFoundException("Tax declaration", id.toString()));

        declaration_repository.delete(entity);
        redis_service.delete(CACHE_KEY_PREFIX + "all:" + tenant_id);

        kafka_service.sendAuditLog(entity, tenant_id.toString(), "TAX_DECLARATION_DELETED");
        log.info("Deleted tax declaration {} for tenant {}", id, tenant_id);
    }

    /**
     * Maps an entity to a DTO.
     * 
     * @param entity the entity to map
     * @return the mapped DTO
     */
    private DeclarationFiscaleDto mapToDto(DeclarationFiscale entity) {
        return DeclarationFiscaleDto.builder()
                .id(entity.getId())
                .type_declaration(entity.getType_declaration())
                .periode_debut(entity.getPeriode_debut())
                .periode_fin(entity.getPeriode_fin())
                .montant_total(entity.getMontant_total())
                .date_generation(entity.getDate_generation())
                .statut(entity.getStatut())
                .numero_declaration(entity.getNumero_declaration())
                .donnees_declaration(entity.getDonnees_declaration())
                .notes(entity.getNotes())
                .build();
    }
}
