package com.yowyob.erp.accounting.application.service;

import com.yowyob.erp.accounting.domain.model.UniteOeuvre;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.AxeAnalytiqueRepository;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.UniteOeuvreRepository;
import com.yowyob.erp.accounting.infrastructure.web.dto.UniteOeuvreDto;
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
public class UniteOeuvreService {

    private final UniteOeuvreRepository repository;
    private final AxeAnalytiqueRepository axeRepository;

    @Transactional
    public Mono<UniteOeuvreDto> create(UniteOeuvreDto dto) {
        return ReactiveOrganizationContext.getOrganizationId()
            .zipWith(ReactiveOrganizationContext.getCurrentUser().defaultIfEmpty("system"))
            .flatMap(t -> {
                UUID orgId = t.getT1(); String user = t.getT2();
                UniteOeuvre entity = UniteOeuvre.builder()
                    .id(UUID.randomUUID()).organizationId(orgId)
                    .code(dto.getCode()).libelle(dto.getLibelle())
                    .unite(dto.getUnite()).centreId(dto.getCentreId())
                    .coutUnitairePrevisionnel(dto.getCoutUnitairePrevisionnel())
                    .actif(dto.getActif() != null ? dto.getActif() : true)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .createdBy(user).updatedBy(user).build();
                return repository.save(entity).flatMap(this::enrichDto);
            });
    }

    @Transactional
    public Mono<UniteOeuvreDto> update(UUID id, UniteOeuvreDto dto) {
        return ReactiveOrganizationContext.getOrganizationId()
            .zipWith(ReactiveOrganizationContext.getCurrentUser().defaultIfEmpty("system"))
            .flatMap(t -> {
                UUID orgId = t.getT1(); String user = t.getT2();
                return repository.findById(id)
                    .filter(u -> orgId.equals(u.getOrganizationId()))
                    .switchIfEmpty(Mono.error(new ResourceNotFoundException("UniteOeuvre", id.toString())))
                    .flatMap(e -> {
                        e.setCode(dto.getCode()); e.setLibelle(dto.getLibelle());
                        e.setUnite(dto.getUnite()); e.setCentreId(dto.getCentreId());
                        e.setCoutUnitairePrevisionnel(dto.getCoutUnitairePrevisionnel());
                        e.setActif(dto.getActif() != null ? dto.getActif() : e.getActif());
                        e.setUpdatedAt(LocalDateTime.now()); e.setUpdatedBy(user); e.setNotNew();
                        return repository.save(e).flatMap(this::enrichDto);
                    });
            });
    }

    @Transactional
    public Mono<Void> delete(UUID id) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMap(orgId -> repository.findById(id)
                .filter(u -> orgId.equals(u.getOrganizationId()))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("UniteOeuvre", id.toString())))
                .flatMap(repository::delete));
    }

    public Mono<UniteOeuvreDto> findById(UUID id) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMap(orgId -> repository.findById(id)
                .filter(u -> orgId.equals(u.getOrganizationId()))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("UniteOeuvre", id.toString())))
                .flatMap(this::enrichDto));
    }

    public Flux<UniteOeuvreDto> getAll() {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMapMany(orgId -> repository.findByOrganizationId(orgId).flatMap(this::enrichDto));
    }

    public Flux<UniteOeuvreDto> getActive() {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMapMany(orgId -> repository.findByOrganizationIdAndActif(orgId, true).flatMap(this::enrichDto));
    }

    public Flux<UniteOeuvreDto> getByCentre(UUID centreId) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMapMany(orgId -> repository.findByOrganizationIdAndCentreId(orgId, centreId).flatMap(this::enrichDto));
    }

    private Mono<UniteOeuvreDto> enrichDto(UniteOeuvre u) {
        UniteOeuvreDto dto = UniteOeuvreDto.builder()
            .id(u.getId()).code(u.getCode()).libelle(u.getLibelle())
            .unite(u.getUnite()).centreId(u.getCentreId())
            .coutUnitairePrevisionnel(u.getCoutUnitairePrevisionnel()).actif(u.getActif()).build();
        if (u.getCentreId() == null) return Mono.just(dto);
        return axeRepository.findById(u.getCentreId())
            .map(c -> { dto.setCentreLibelle(c.getCode() + " - " + c.getLibelle()); return dto; })
            .defaultIfEmpty(dto);
    }
}
