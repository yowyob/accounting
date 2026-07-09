package com.yowyob.erp.accounting.application.service;

import com.yowyob.erp.accounting.domain.model.CleRepartition;
import com.yowyob.erp.accounting.domain.model.CleRepartitionLigne;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.AxeAnalytiqueRepository;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.CleRepartitionLigneRepository;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.CleRepartitionRepository;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.UniteOeuvreRepository;
import com.yowyob.erp.accounting.infrastructure.web.dto.CleRepartitionDto;
import com.yowyob.erp.accounting.infrastructure.web.dto.CleRepartitionLigneDto;
import com.yowyob.erp.config.organization.ReactiveOrganizationContext;
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
public class CleRepartitionService {

    private final CleRepartitionRepository repository;
    private final CleRepartitionLigneRepository ligneRepo;
    private final AxeAnalytiqueRepository axeRepo;
    private final UniteOeuvreRepository uoRepo;

    @Transactional
    public Mono<CleRepartitionDto> create(CleRepartitionDto dto) {
        return ReactiveOrganizationContext.getOrganizationId()
            .zipWith(ReactiveOrganizationContext.getCurrentUser().defaultIfEmpty("system"))
            .flatMap(t -> {
                UUID orgId = t.getT1(); String user = t.getT2();
                CleRepartition entity = CleRepartition.builder()
                    .id(UUID.randomUUID()).organizationId(orgId)
                    .code(dto.getCode()).libelle(dto.getLibelle())
                    .type(dto.getType()).actif(dto.getActif() != null ? dto.getActif() : true)
                    .createdAt(LocalDateTime.now()).createdBy(user).build();

                return repository.save(entity)
                    .flatMap(saved -> saveLignes(saved.getId(), dto.getLignes())
                        .then(enrichDto(saved)));
            });
    }

    @Transactional
    public Mono<CleRepartitionDto> update(UUID id, CleRepartitionDto dto) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMap(orgId -> repository.findById(id)
                .filter(c -> orgId.equals(c.getOrganizationId()))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("CleRepartition", id.toString())))
                .flatMap(existing -> {
                    existing.setCode(dto.getCode());
                    existing.setLibelle(dto.getLibelle());
                    existing.setType(dto.getType());
                    existing.setActif(dto.getActif() != null ? dto.getActif() : existing.getActif());
                    existing.setNotNew();

                    return repository.save(existing)
                        .flatMap(saved -> ligneRepo.deleteByCleId(saved.getId())
                            .then(saveLignes(saved.getId(), dto.getLignes()))
                            .then(enrichDto(saved)));
                }));
    }

    @Transactional
    public Mono<Void> delete(UUID id) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMap(orgId -> repository.findById(id)
                .filter(c -> orgId.equals(c.getOrganizationId()))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("CleRepartition", id.toString())))
                .flatMap(existing -> ligneRepo.deleteByCleId(existing.getId())
                    .then(repository.delete(existing))));
    }

    public Mono<CleRepartitionDto> findById(UUID id) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMap(orgId -> repository.findById(id)
                .filter(c -> orgId.equals(c.getOrganizationId()))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("CleRepartition", id.toString())))
                .flatMap(this::enrichDto));
    }

    public Flux<CleRepartitionDto> getAll() {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMapMany(orgId -> repository.findByOrganizationId(orgId).flatMap(this::enrichDto));
    }

    public Flux<CleRepartitionDto> getActive() {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMapMany(orgId -> repository.findByOrganizationIdAndActif(orgId, true).flatMap(this::enrichDto));
    }

    private Mono<Void> saveLignes(UUID cleId, List<CleRepartitionLigneDto> lignes) {
        if (lignes == null || lignes.isEmpty()) return Mono.empty();
        return Flux.fromIterable(lignes)
            .map(l -> CleRepartitionLigne.builder()
                .id(UUID.randomUUID()).cleId(cleId)
                .centreDestinataireId(l.getCentreDestinataireId())
                .pourcentage(l.getPourcentage()).uniteOeuvreId(l.getUniteOeuvreId())
                .build())
            .flatMap(ligneRepo::save)
            .then();
    }

    private Mono<CleRepartitionDto> enrichDto(CleRepartition cle) {
        return ligneRepo.findByCleId(cle.getId()).collectList()
            .flatMap(lignes -> {
                Mono<List<CleRepartitionLigneDto>> listMono = Flux.fromIterable(lignes)
                    .flatMap(l -> {
                        Mono<String> centreLibelle = l.getCentreDestinataireId() != null
                            ? axeRepo.findById(l.getCentreDestinataireId()).map(a -> a.getCode() + " - " + a.getLibelle()).defaultIfEmpty("")
                            : Mono.just("");
                        Mono<String> uoLibelle = l.getUniteOeuvreId() != null
                            ? uoRepo.findById(l.getUniteOeuvreId()).map(u -> u.getCode() + " - " + u.getLibelle()).defaultIfEmpty("")
                            : Mono.just("");

                        return Mono.zip(centreLibelle, uoLibelle)
                            .map(t -> CleRepartitionLigneDto.builder()
                                .id(l.getId()).cleId(l.getCleId())
                                .centreDestinataireId(l.getCentreDestinataireId()).centreDestinataireLibelle(t.getT1())
                                .pourcentage(l.getPourcentage()).uniteOeuvreId(l.getUniteOeuvreId()).uniteOeuvreLibelle(t.getT2())
                                .build());
                    }).collectList();

                return listMono.map(ldtos -> CleRepartitionDto.builder()
                    .id(cle.getId()).code(cle.getCode()).libelle(cle.getLibelle())
                    .type(cle.getType()).actif(cle.getActif()).lignes(ldtos).build());
            });
    }
}
