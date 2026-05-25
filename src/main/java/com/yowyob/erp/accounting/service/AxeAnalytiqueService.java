package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.dto.AxeAnalytiqueDto;
import com.yowyob.erp.accounting.entity.AxeAnalytique;
import com.yowyob.erp.accounting.repository.AxeAnalytiqueRepository;
import com.yowyob.erp.accounting.repository.CompteRepository;
import com.yowyob.erp.common.exception.ResourceNotFoundException;
import com.yowyob.erp.config.organization.ReactiveOrganizationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AxeAnalytiqueService {

    private final AxeAnalytiqueRepository axeRepository;
    private final CompteRepository compteRepository;

    @Transactional
    public Mono<AxeAnalytiqueDto> create(AxeAnalytiqueDto dto) {
        return ReactiveOrganizationContext.getOrganizationId()
            .zipWith(ReactiveOrganizationContext.getCurrentUser().defaultIfEmpty("system"))
            .flatMap(tuple -> {
                UUID orgId = tuple.getT1();
                String user = tuple.getT2();
                
                AxeAnalytique entity = AxeAnalytique.builder()
                    .id(UUID.randomUUID())
                    .organizationId(orgId)
                    .code(dto.getCode())
                    .libelle(dto.getLibelle())
                    .type(dto.getType())
                    .responsable(dto.getResponsable())
                    .actif(dto.getActif() != null ? dto.getActif() : true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .createdBy(user)
                    .updatedBy(user)
                    .build();
                
                return axeRepository.save(entity)
                    .flatMap(saved -> {
                        if (dto.getCompteIds() == null || dto.getCompteIds().isEmpty()) {
                            return enrichDto(saved);
                        }
                        return Flux.fromIterable(dto.getCompteIds())
                            .flatMap(compteId -> axeRepository.linkCompte(saved.getId(), compteId))
                            .then(enrichDto(saved));
                    });
            });
    }

    @Transactional
    public Mono<AxeAnalytiqueDto> update(UUID id, AxeAnalytiqueDto dto) {
        return ReactiveOrganizationContext.getOrganizationId()
            .zipWith(ReactiveOrganizationContext.getCurrentUser().defaultIfEmpty("system"))
            .flatMap(tuple -> {
                UUID orgId = tuple.getT1();
                String user = tuple.getT2();
                
                return axeRepository.findById(id)
                    .filter(a -> orgId.equals(a.getOrganizationId()))
                    .switchIfEmpty(Mono.error(new ResourceNotFoundException("AxeAnalytique", id.toString())))
                    .flatMap(existing -> {
                        existing.setCode(dto.getCode());
                        existing.setLibelle(dto.getLibelle());
                        existing.setType(dto.getType());
                        existing.setResponsable(dto.getResponsable());
                        existing.setActif(dto.getActif() != null ? dto.getActif() : existing.getActif());
                        existing.setUpdatedAt(LocalDateTime.now());
                        existing.setUpdatedBy(user);
                        existing.setNotNew();
                        
                        return axeRepository.save(existing)
                            .flatMap(saved -> axeRepository.unlinkAllComptes(saved.getId())
                                .then(Mono.defer(() -> {
                                    if (dto.getCompteIds() == null || dto.getCompteIds().isEmpty()) {
                                        return enrichDto(saved);
                                    }
                                    return Flux.fromIterable(dto.getCompteIds())
                                        .flatMap(compteId -> axeRepository.linkCompte(saved.getId(), compteId))
                                        .then(enrichDto(saved));
                                })));
                    });
            });
    }

    @Transactional
    public Mono<Void> delete(UUID id) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMap(orgId -> axeRepository.findById(id)
                .filter(a -> orgId.equals(a.getOrganizationId()))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("AxeAnalytique", id.toString())))
                .flatMap(existing -> axeRepository.unlinkAllComptes(existing.getId())
                    .then(axeRepository.delete(existing))));
    }

    public Mono<AxeAnalytiqueDto> findById(UUID id) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMap(orgId -> axeRepository.findById(id)
                .filter(a -> orgId.equals(a.getOrganizationId()))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("AxeAnalytique", id.toString())))
                .flatMap(this::enrichDto));
    }

    public Flux<AxeAnalytiqueDto> getAll() {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMapMany(orgId -> axeRepository.findByOrganizationId(orgId)
                .flatMap(this::enrichDto));
    }

    public Flux<AxeAnalytiqueDto> getActive() {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMapMany(orgId -> axeRepository.findByOrganizationIdAndActif(orgId, true)
                .flatMap(this::enrichDto));
    }

    private Mono<AxeAnalytiqueDto> enrichDto(AxeAnalytique axe) {
        return axeRepository.findLinkedCompteIds(axe.getId())
            .collectList()
            .flatMap(compteIds -> {
                if (compteIds.isEmpty()) {
                    return Mono.just(AxeAnalytiqueDto.builder()
                        .id(axe.getId())
                        .code(axe.getCode())
                        .libelle(axe.getLibelle())
                        .type(axe.getType())
                        .responsable(axe.getResponsable())
                        .actif(axe.getActif())
                        .compteIds(compteIds)
                        .build());
                }
                return compteRepository.findAllById(compteIds)
                    .map(compte -> compte.getNo_compte() + " - " + compte.getLibelle())
                    .collectList()
                    .map(compteLibelles -> AxeAnalytiqueDto.builder()
                        .id(axe.getId())
                        .code(axe.getCode())
                        .libelle(axe.getLibelle())
                        .type(axe.getType())
                        .responsable(axe.getResponsable())
                        .actif(axe.getActif())
                        .compteIds(compteIds)
                        .compteLibelles(compteLibelles)
                        .build());
            });
    }
}
