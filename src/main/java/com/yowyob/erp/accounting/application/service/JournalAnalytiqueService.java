package com.yowyob.erp.accounting.application.service;

import com.yowyob.erp.accounting.domain.model.JournalAnalytique;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.JournalAnalytiqueRepository;
import com.yowyob.erp.accounting.infrastructure.web.dto.JournalAnalytiqueDto;
import com.yowyob.erp.config.organization.ReactiveOrganizationContext;
import com.yowyob.erp.shared.domain.exception.ResourceNotFoundException;
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
public class JournalAnalytiqueService {

    private final JournalAnalytiqueRepository repository;

    @Transactional
    public Mono<JournalAnalytiqueDto> create(JournalAnalytiqueDto dto) {
        return ReactiveOrganizationContext.getOrganizationId()
            .zipWith(ReactiveOrganizationContext.getCurrentUser().defaultIfEmpty("system"))
            .flatMap(t -> {
                UUID orgId = t.getT1();
                String user = t.getT2();

                JournalAnalytique entity = JournalAnalytique.builder()
                    .id(UUID.randomUUID())
                    .organizationId(orgId)
                    .code(dto.getCode())
                    .libelle(dto.getLibelle())
                    .type(dto.getType())
                    .actif(dto.getActif() != null ? dto.getActif() : true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .createdBy(user)
                    .updatedBy(user)
                    .build();

                return repository.save(entity).map(this::mapToDto);
            });
    }

    @Transactional
    public Mono<JournalAnalytiqueDto> update(UUID id, JournalAnalytiqueDto dto) {
        return ReactiveOrganizationContext.getOrganizationId()
            .zipWith(ReactiveOrganizationContext.getCurrentUser().defaultIfEmpty("system"))
            .flatMap(t -> {
                UUID orgId = t.getT1();
                String user = t.getT2();

                return repository.findById(id)
                    .filter(j -> orgId.equals(j.getOrganizationId()))
                    .switchIfEmpty(Mono.error(new ResourceNotFoundException("JournalAnalytique", id.toString())))
                    .flatMap(existing -> {
                        existing.setCode(dto.getCode());
                        existing.setLibelle(dto.getLibelle());
                        existing.setType(dto.getType());
                        existing.setActif(dto.getActif() != null ? dto.getActif() : existing.getActif());
                        existing.setUpdatedAt(LocalDateTime.now());
                        existing.setUpdatedBy(user);
                        existing.setNotNew();

                        return repository.save(existing).map(this::mapToDto);
                    });
            });
    }

    @Transactional
    public Mono<Void> delete(UUID id) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMap(orgId -> repository.findById(id)
                .filter(j -> orgId.equals(j.getOrganizationId()))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("JournalAnalytique", id.toString())))
                .flatMap(repository::delete));
    }

    public Mono<JournalAnalytiqueDto> findById(UUID id) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMap(orgId -> repository.findById(id)
                .filter(j -> orgId.equals(j.getOrganizationId()))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("JournalAnalytique", id.toString())))
                .map(this::mapToDto));
    }

    public Flux<JournalAnalytiqueDto> getAll() {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMapMany(orgId -> repository.findByOrganizationId(orgId).map(this::mapToDto));
    }

    public Flux<JournalAnalytiqueDto> getActive() {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMapMany(orgId -> repository.findByOrganizationIdAndActif(orgId, true).map(this::mapToDto));
    }

    private JournalAnalytiqueDto mapToDto(JournalAnalytique entity) {
        return JournalAnalytiqueDto.builder()
            .id(entity.getId())
            .code(entity.getCode())
            .libelle(entity.getLibelle())
            .type(entity.getType())
            .actif(entity.getActif())
            .build();
    }
}
