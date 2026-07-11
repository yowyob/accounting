package com.yowyob.erp.accounting.application.service;

import com.yowyob.erp.accounting.domain.model.ChargeAnalytique;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.AxeAnalytiqueRepository;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.ChargeAnalytiqueRepository;
import com.yowyob.erp.accounting.infrastructure.web.dto.ChargeAnalytiqueDto;
import com.yowyob.erp.config.organization.ReactiveOrganizationContext;
import com.yowyob.erp.shared.domain.exception.BusinessException;
import com.yowyob.erp.shared.domain.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChargeAnalytiqueService {

    private static final List<String> TYPES_VALIDES = List.of("DIRECTE", "INDIRECTE");

    private final ChargeAnalytiqueRepository repository;
    private final AxeAnalytiqueRepository axeRepo;

    @Transactional
    public Mono<ChargeAnalytiqueDto> create(ChargeAnalytiqueDto dto) {
        validateBusinessRules(dto);
        return ReactiveOrganizationContext.getOrganizationId()
            .zipWith(ReactiveOrganizationContext.getCurrentUser().defaultIfEmpty("system"))
            .flatMap(t -> {
                UUID orgId = t.getT1();
                String user = t.getT2();
                LocalDateTime now = LocalDateTime.now();
                ChargeAnalytique entity = ChargeAnalytique.builder()
                    .id(UUID.randomUUID())
                    .organizationId(orgId)
                    .nature(dto.getNature().trim())
                    .montant(dto.getMontant())
                    .type(dto.getType())
                    .incorporable(dto.getIncorporable() != null ? dto.getIncorporable() : true)
                    .centreId(dto.getCentreId())
                    .periodeId(dto.getPeriodeId())
                    .description(dto.getDescription())
                    .createdAt(now)
                    .updatedAt(now)
                    .createdBy(user)
                    .updatedBy(user)
                    .build();
                return repository.save(entity).flatMap(this::enrichDto);
            });
    }

    @Transactional
    public Mono<ChargeAnalytiqueDto> update(UUID id, ChargeAnalytiqueDto dto) {
        validateBusinessRules(dto);
        return ReactiveOrganizationContext.getOrganizationId()
            .zipWith(ReactiveOrganizationContext.getCurrentUser().defaultIfEmpty("system"))
            .flatMap(t -> {
                UUID orgId = t.getT1();
                String user = t.getT2();
                return repository.findById(id)
                    .filter(e -> orgId.equals(e.getOrganizationId()))
                    .switchIfEmpty(Mono.error(new ResourceNotFoundException("ChargeAnalytique", id.toString())))
                    .flatMap(existing -> {
                        existing.setNature(dto.getNature().trim());
                        existing.setMontant(dto.getMontant());
                        existing.setType(dto.getType());
                        existing.setIncorporable(dto.getIncorporable() != null ? dto.getIncorporable() : true);
                        existing.setCentreId(dto.getCentreId());
                        existing.setPeriodeId(dto.getPeriodeId());
                        existing.setDescription(dto.getDescription());
                        existing.setUpdatedAt(LocalDateTime.now());
                        existing.setUpdatedBy(user);
                        existing.setNotNew();
                        return repository.save(existing).flatMap(this::enrichDto);
                    });
            });
    }

    @Transactional
    public Mono<Void> delete(UUID id) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMap(orgId -> repository.findById(id)
                .filter(e -> orgId.equals(e.getOrganizationId()))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("ChargeAnalytique", id.toString())))
                .flatMap(repository::delete));
    }

    public Mono<ChargeAnalytiqueDto> findById(UUID id) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMap(orgId -> repository.findById(id)
                .filter(e -> orgId.equals(e.getOrganizationId()))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("ChargeAnalytique", id.toString())))
                .flatMap(this::enrichDto));
    }

    public Flux<ChargeAnalytiqueDto> getAll(UUID periodeId, String type) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMapMany(orgId -> {
                if (periodeId != null && type != null) {
                    return repository.findByOrganizationIdAndPeriodeId(orgId, periodeId)
                        .filter(c -> type.equals(c.getType()));
                }
                if (periodeId != null) {
                    return repository.findByOrganizationIdAndPeriodeId(orgId, periodeId);
                }
                if (type != null) {
                    return repository.findByOrganizationIdAndType(orgId, type);
                }
                return repository.findByOrganizationId(orgId);
            })
            .flatMap(this::enrichDto);
    }

    private void validateBusinessRules(ChargeAnalytiqueDto dto) {
        if (dto.getType() != null && !TYPES_VALIDES.contains(dto.getType())) {
            throw new BusinessException("Type invalide : " + dto.getType());
        }
        if (dto.getMontant() != null && dto.getMontant().signum() <= 0) {
            throw new BusinessException("Le montant doit être strictement positif.");
        }
    }

    private Mono<ChargeAnalytiqueDto> enrichDto(ChargeAnalytique entity) {
        return axeRepo.findById(entity.getCentreId())
            .map(a -> a.getLibelle())
            .defaultIfEmpty("")
            .map(lib -> ChargeAnalytiqueDto.builder()
                .id(entity.getId())
                .nature(entity.getNature())
                .montant(entity.getMontant())
                .type(entity.getType())
                .incorporable(entity.getIncorporable())
                .centreId(entity.getCentreId())
                .centreLibelle(lib)
                .periodeId(entity.getPeriodeId())
                .description(entity.getDescription())
                .updatedAt(entity.getUpdatedAt())
                .build());
    }
}
