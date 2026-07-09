package com.yowyob.erp.accounting.application.service;

import com.yowyob.erp.accounting.domain.model.PeriodeAnalytique;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.PeriodeAnalytiqueRepository;
import com.yowyob.erp.accounting.infrastructure.web.dto.PeriodeAnalytiqueDto;
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
public class PeriodeAnalytiqueService {

    private final PeriodeAnalytiqueRepository repository;

    @Transactional
    public Mono<PeriodeAnalytiqueDto> create(PeriodeAnalytiqueDto dto) {
        return ReactiveOrganizationContext.getOrganizationId()
            .zipWith(ReactiveOrganizationContext.getCurrentUser().defaultIfEmpty("system"))
            .flatMap(t -> {
                UUID orgId = t.getT1();
                String user = t.getT2();

                PeriodeAnalytique entity = PeriodeAnalytique.builder()
                    .id(UUID.randomUUID())
                    .organizationId(orgId)
                    .exerciceId(dto.getExerciceId())
                    .code(dto.getCode())
                    .libelle(dto.getLibelle())
                    .dateDebut(dto.getDateDebut())
                    .dateFin(dto.getDateFin())
                    .statut(dto.getStatut() != null ? dto.getStatut() : "OUVERTE")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .createdBy(user)
                    .build();

                return repository.save(entity).map(this::mapToDto);
            });
    }

    @Transactional
    public Mono<PeriodeAnalytiqueDto> update(UUID id, PeriodeAnalytiqueDto dto) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMap(orgId -> repository.findById(id)
                .filter(p -> orgId.equals(p.getOrganizationId()))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("PeriodeAnalytique", id.toString())))
                .flatMap(existing -> {
                    existing.setExerciceId(dto.getExerciceId());
                    existing.setCode(dto.getCode());
                    existing.setLibelle(dto.getLibelle());
                    existing.setDateDebut(dto.getDateDebut());
                    existing.setDateFin(dto.getDateFin());
                    existing.setStatut(dto.getStatut() != null ? dto.getStatut() : existing.getStatut());
                    existing.setUpdatedAt(LocalDateTime.now());
                    existing.setNotNew();

                    return repository.save(existing).map(this::mapToDto);
                }));
    }

    @Transactional
    public Mono<Void> delete(UUID id) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMap(orgId -> repository.findById(id)
                .filter(p -> orgId.equals(p.getOrganizationId()))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("PeriodeAnalytique", id.toString())))
                .flatMap(repository::delete));
    }

    public Mono<PeriodeAnalytiqueDto> findById(UUID id) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMap(orgId -> repository.findById(id)
                .filter(p -> orgId.equals(p.getOrganizationId()))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("PeriodeAnalytique", id.toString())))
                .map(this::mapToDto));
    }

    public Flux<PeriodeAnalytiqueDto> getAll() {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMapMany(orgId -> repository.findByOrganizationId(orgId).map(this::mapToDto));
    }

    public Flux<PeriodeAnalytiqueDto> getByStatut(String statut) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMapMany(orgId -> repository.findByOrganizationIdAndStatut(orgId, statut).map(this::mapToDto));
    }

    public Flux<PeriodeAnalytiqueDto> getByExercice(UUID exerciceId) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMapMany(orgId -> repository.findByOrganizationIdAndExerciceId(orgId, exerciceId).map(this::mapToDto));
    }

    private PeriodeAnalytiqueDto mapToDto(PeriodeAnalytique entity) {
        return PeriodeAnalytiqueDto.builder()
            .id(entity.getId())
            .exerciceId(entity.getExerciceId())
            .code(entity.getCode())
            .libelle(entity.getLibelle())
            .dateDebut(entity.getDateDebut())
            .dateFin(entity.getDateFin())
            .statut(entity.getStatut())
            .build();
    }
}
