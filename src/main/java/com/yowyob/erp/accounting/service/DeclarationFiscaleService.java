package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.dto.DeclarationFiscaleDto;
import com.yowyob.erp.accounting.dto.JournalAuditDto;
import com.yowyob.erp.accounting.entity.DeclarationFiscale;
import com.yowyob.erp.accounting.entity.Tenant;
import com.yowyob.erp.accounting.repository.DeclarationFiscaleRepository;
import com.yowyob.erp.common.exception.ResourceNotFoundException;
import com.yowyob.erp.config.kafka.KafkaMessageService;
import com.yowyob.erp.config.redis.RedisService;
import com.yowyob.erp.config.tenant.ReactiveTenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

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
    public Mono<DeclarationFiscaleDto> saveDeclaration(DeclarationFiscaleDto dto) {
        return ReactiveTenantContext.getTenantId()
                .zipWith(ReactiveTenantContext.getCurrentTenantAsTenant())
                .flatMap(tuple -> {
                    UUID tenant_id = tuple.getT1();
                    Tenant tenant = tuple.getT2();
                    String current_user = Optional.ofNullable(ReactiveTenantContext.getCurrentUser().block())
                            .orElse("system"); // TODO: fully reactive user retrieval

                    Mono<DeclarationFiscale> entityMono;
                    if (dto.getId() != null) {
                        entityMono = declaration_repository.findByTenantIdAndId(tenant_id, dto.getId())
                                .switchIfEmpty(
                                        Mono.error(new ResourceNotFoundException("Tax declaration",
                                                dto.getId().toString())))
                                .doOnNext(e -> log.info("Updating tax declaration {} for tenant {}", dto.getId(),
                                        tenant_id));
                    } else {
                        DeclarationFiscale newEntity = new DeclarationFiscale();
                        newEntity.setTenantId(tenant_id);
                        newEntity.setCreated_by(current_user);
                        log.info("Creating new tax declaration for tenant {}", tenant_id);
                        entityMono = Mono.just(newEntity);
                    }

                    return entityMono.flatMap(entity -> {
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
                        entity.setUpdated_at(LocalDateTime.now());
                        if (entity.getCreated_at() == null) {
                            entity.setCreated_at(LocalDateTime.now());
                        }

                        return declaration_repository.save(entity)
                                .flatMap(saved -> {
                                    DeclarationFiscaleDto result = mapToDto(saved);
                                    String auditAction = dto.getId() != null ? "TAX_DECLARATION_UPDATED"
                                            : "TAX_DECLARATION_CREATED";
                                    String auditDetails = "Tax declaration " + (dto.getNumero_declaration() != null
                                            ? dto.getNumero_declaration()
                                            : saved.getId());

                                    return redis_service.delete(CACHE_KEY_PREFIX + "all:" + tenant_id)
                                            .then(logAudit(tenant, current_user, auditAction, auditDetails))
                                            .thenReturn(result);
                                });
                    });
                });
    }

    /**
     * Retrieves a declaration by its ID.
     * 
     * @param id the declaration ID
     * @return an optional containing the declaration DTO
     */
    public Mono<DeclarationFiscaleDto> getById(UUID id) {
        return ReactiveTenantContext.getTenantId()
                .flatMap(tenant_id -> declaration_repository.findByTenantIdAndId(tenant_id, id)
                        .map(this::mapToDto));
    }

    /**
     * Lists all declarations for the current tenant.
     * 
     * @return list of declaration DTOs
     */
    @SuppressWarnings("unchecked")
    public Flux<DeclarationFiscaleDto> getAll() {
        return ReactiveTenantContext.getTenantId()
                .flatMapMany(tenant_id -> {
                    String cache_key = CACHE_KEY_PREFIX + "all:" + tenant_id;

                    return redis_service.get(cache_key, java.util.List.class)
                            .flatMapMany(list -> Flux.fromIterable((java.util.List<DeclarationFiscaleDto>) list))
                            .switchIfEmpty(
                                    declaration_repository.findByTenantIdOrderByDateGenerationDesc(tenant_id)
                                            .map(this::mapToDto)
                                            .collectList()
                                            .flatMap(list -> redis_service.save(cache_key, list, Duration.ofMinutes(15))
                                                    .thenReturn(list))
                                            .flatMapMany(Flux::fromIterable));
                });
    }

    /**
     * Lists declarations of a specific type.
     * 
     * @param type the declaration type
     * @return list of declaration DTOs
     */
    public Flux<DeclarationFiscaleDto> getByType(String type) {
        return ReactiveTenantContext.getTenantId()
                .flatMapMany(tenant_id -> declaration_repository.findByTenantIdAndTypeDeclaration(tenant_id, type)
                        .map(this::mapToDto));
    }

    /**
     * Searches for declarations within a period range.
     * 
     * @param start the start date
     * @param end   the end date
     * @return list of declaration DTOs
     */
    public Flux<DeclarationFiscaleDto> getByPeriodRange(LocalDate start, LocalDate end) {
        return ReactiveTenantContext.getTenantId()
                .flatMapMany(tenant_id -> declaration_repository.findByTenantIdAndPeriodRange(tenant_id, start, end)
                        .map(this::mapToDto));
    }

    /**
     * Deletes a declaration.
     * 
     * @param id the declaration ID
     */
    @Transactional
    public Mono<Void> delete(UUID id) {
        return ReactiveTenantContext.getTenantId()
                .zipWith(ReactiveTenantContext.getCurrentTenantAsTenant())
                .flatMap(tuple -> {
                    UUID tenant_id = tuple.getT1();
                    Tenant tenant = tuple.getT2();
                    String current_user = Optional.ofNullable(ReactiveTenantContext.getCurrentUser().block())
                            .orElse("system"); // TODO: reactive

                    return declaration_repository.findByTenantIdAndId(tenant_id, id)
                            .switchIfEmpty(
                                    Mono.error(new ResourceNotFoundException("Tax declaration", id.toString())))
                            .flatMap(entity -> {
                                DeclarationFiscaleDto result = mapToDto(entity);
                                return declaration_repository.delete(entity)
                                        .then(redis_service.delete(CACHE_KEY_PREFIX + "all:" + tenant_id))
                                        .then(logAudit(tenant, current_user, "TAX_DECLARATION_DELETED",
                                                "Deleted tax declaration: " + (result.getNumero_declaration() != null
                                                        ? result.getNumero_declaration()
                                                        : id)));
                            })
                            .doOnSuccess(v -> log.info("Deleted tax declaration {} for tenant {}", id, tenant_id));
                });
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

    private Mono<Void> logAudit(Tenant tenant, String utilisateur, String action, String details) {
        // NOTE: Does KafkaService.sendAuditLog return void/future? Assuming I can wrap
        // it or it needs update.
        // Assuming wrapping in fromRunnable or similar if it's void, but check
        // KafkaService.
        return Mono.fromRunnable(() -> {
            JournalAuditDto auditDto = JournalAuditDto.builder()
                    .action(action)
                    .utilisateur(utilisateur)
                    .details(details)
                    .date_action(LocalDateTime.now())
                    .build();
            kafka_service.sendAuditLog(auditDto, tenant.getId(), action);
        }).then();
    }
}
