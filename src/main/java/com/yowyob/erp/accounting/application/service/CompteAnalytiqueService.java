package com.yowyob.erp.accounting.application.service;

import com.yowyob.erp.accounting.domain.port.in.CompteAnalytiqueUseCase;
import com.yowyob.erp.accounting.infrastructure.web.dto.CompteAnalytiqueDto;
import com.yowyob.erp.accounting.domain.model.CompteAnalytique;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.CompteAnalytiqueRepository;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.CompteRepository;
import com.yowyob.erp.shared.domain.exception.ResourceNotFoundException;
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
public class CompteAnalytiqueService implements CompteAnalytiqueUseCase {

    private final CompteAnalytiqueRepository repository;
    private final CompteRepository compteRepository;

    @Override
    @Transactional
    public Mono<CompteAnalytiqueDto> create(CompteAnalytiqueDto dto) {
        return ReactiveOrganizationContext.getOrganizationId()
            .zipWith(ReactiveOrganizationContext.getCurrentUser().defaultIfEmpty("system"))
            .flatMap(tuple -> {
                UUID orgId = tuple.getT1();
                String user = tuple.getT2();

                CompteAnalytique entity = CompteAnalytique.builder()
                    .id(UUID.randomUUID())
                    .organizationId(orgId)
                    .code(dto.getCode())
                    .libelle(dto.getLibelle())
                    .classe(dto.getClasse())
                    .nature(dto.getNature())
                    .compteGeneralId(dto.getCompteGeneralId())
                    .actif(dto.getActif() != null ? dto.getActif() : true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .createdBy(user)
                    .updatedBy(user)
                    .build();

                return repository.save(entity)
                    .flatMap(this::enrichDto);
            });
    }

    @Override
    @Transactional
    public Mono<CompteAnalytiqueDto> update(UUID id, CompteAnalytiqueDto dto) {
        return ReactiveOrganizationContext.getOrganizationId()
            .zipWith(ReactiveOrganizationContext.getCurrentUser().defaultIfEmpty("system"))
            .flatMap(tuple -> {
                UUID orgId = tuple.getT1();
                String user = tuple.getT2();

                return repository.findById(id)
                    .filter(c -> orgId.equals(c.getOrganizationId()))
                    .switchIfEmpty(Mono.error(new ResourceNotFoundException("CompteAnalytique", id.toString())))
                    .flatMap(existing -> {
                        existing.setCode(dto.getCode());
                        existing.setLibelle(dto.getLibelle());
                        existing.setClasse(dto.getClasse());
                        existing.setNature(dto.getNature());
                        existing.setCompteGeneralId(dto.getCompteGeneralId());
                        existing.setActif(dto.getActif() != null ? dto.getActif() : existing.getActif());
                        existing.setUpdatedAt(LocalDateTime.now());
                        existing.setUpdatedBy(user);
                        existing.setNotNew();

                        return repository.save(existing)
                            .flatMap(this::enrichDto);
                    });
            });
    }

    @Override
    @Transactional
    public Mono<Void> delete(UUID id) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMap(orgId -> repository.findById(id)
                .filter(c -> orgId.equals(c.getOrganizationId()))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("CompteAnalytique", id.toString())))
                .flatMap(repository::delete));
    }

    @Override
    public Mono<CompteAnalytiqueDto> findById(UUID id) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMap(orgId -> repository.findById(id)
                .filter(c -> orgId.equals(c.getOrganizationId()))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("CompteAnalytique", id.toString())))
                .flatMap(this::enrichDto));
    }

    @Override
    public Flux<CompteAnalytiqueDto> getAll() {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMapMany(orgId -> repository.findByOrganizationId(orgId)
                .flatMap(this::enrichDto));
    }

    @Override
    public Flux<CompteAnalytiqueDto> getActive() {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMapMany(orgId -> repository.findByOrganizationIdAndActif(orgId, true)
                .flatMap(this::enrichDto));
    }

    private Mono<CompteAnalytiqueDto> enrichDto(CompteAnalytique compte) {
        CompteAnalytiqueDto dto = CompteAnalytiqueDto.builder()
            .id(compte.getId())
            .code(compte.getCode())
            .libelle(compte.getLibelle())
            .classe(compte.getClasse())
            .nature(compte.getNature())
            .compteGeneralId(compte.getCompteGeneralId())
            .actif(compte.getActif())
            .build();

        if (compte.getCompteGeneralId() == null) {
            return Mono.just(dto);
        }

        return compteRepository.findById(compte.getCompteGeneralId())
            .map(cg -> {
                dto.setCompteGeneralNo(cg.getNo_compte());
                dto.setCompteGeneralLibelle(cg.getLibelle());
                return dto;
            })
            .defaultIfEmpty(dto);
    }
}
