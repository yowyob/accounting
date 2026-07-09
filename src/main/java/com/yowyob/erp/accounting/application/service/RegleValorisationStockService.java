package com.yowyob.erp.accounting.application.service;

import com.yowyob.erp.accounting.domain.model.HistoriqueValorisationStock;
import com.yowyob.erp.accounting.domain.model.RegleValorisationStock;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.HistoriqueValorisationStockRepository;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.RegleValorisationStockRepository;
import com.yowyob.erp.accounting.infrastructure.web.dto.HistoriqueValorisationDto;
import com.yowyob.erp.accounting.infrastructure.web.dto.RegleValorisationStockDto;
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
public class RegleValorisationStockService {

    private static final List<String> METHODES_VALIDES =
        List.of("CUMP_PERIODE", "CUMP_ENTREE", "FIFO", "LIFO");

    private final RegleValorisationStockRepository repository;
    private final HistoriqueValorisationStockRepository historiqueRepo;

    @Transactional
    public Mono<RegleValorisationStockDto> create(RegleValorisationStockDto dto) {
        validateBusinessRules(dto);
        return ReactiveOrganizationContext.getOrganizationId()
            .zipWith(ReactiveOrganizationContext.getCurrentUser().defaultIfEmpty("system"))
            .flatMap(t -> {
                UUID orgId = t.getT1();
                String user = t.getT2();
                LocalDateTime now = LocalDateTime.now();
                RegleValorisationStock entity = RegleValorisationStock.builder()
                    .id(UUID.randomUUID())
                    .organizationId(orgId)
                    .familleId(dto.getFamilleId().trim())
                    .familleLibelle(dto.getFamilleLibelle().trim())
                    .methode(dto.getMethode())
                    .dateApplication(dto.getDateApplication())
                    .actif(dto.getActif() != null ? dto.getActif() : true)
                    .createdAt(now)
                    .updatedAt(now)
                    .createdBy(user)
                    .updatedBy(user)
                    .build();
                return repository.save(entity)
                    .flatMap(saved -> saveHistorique(saved.getId(), dto.getHistorique())
                        .then(enrichDto(saved)));
            });
    }

    @Transactional
    public Mono<RegleValorisationStockDto> update(UUID id, RegleValorisationStockDto dto) {
        validateBusinessRules(dto);
        return ReactiveOrganizationContext.getOrganizationId()
            .zipWith(ReactiveOrganizationContext.getCurrentUser().defaultIfEmpty("system"))
            .flatMap(t -> {
                UUID orgId = t.getT1();
                String user = t.getT2();
                return repository.findById(id)
                    .filter(e -> orgId.equals(e.getOrganizationId()))
                    .switchIfEmpty(Mono.error(new ResourceNotFoundException("RegleValorisationStock", id.toString())))
                    .flatMap(existing -> {
                        existing.setFamilleId(dto.getFamilleId().trim());
                        existing.setFamilleLibelle(dto.getFamilleLibelle().trim());
                        existing.setMethode(dto.getMethode());
                        existing.setDateApplication(dto.getDateApplication());
                        existing.setActif(dto.getActif() != null ? dto.getActif() : true);
                        existing.setUpdatedAt(LocalDateTime.now());
                        existing.setUpdatedBy(user);
                        existing.setNotNew();
                        return repository.save(existing)
                            .flatMap(saved -> historiqueRepo.deleteByRegleId(saved.getId())
                                .then(saveHistorique(saved.getId(), dto.getHistorique()))
                                .then(enrichDto(saved)));
                    });
            });
    }

    @Transactional
    public Mono<Void> delete(UUID id) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMap(orgId -> repository.findById(id)
                .filter(e -> orgId.equals(e.getOrganizationId()))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("RegleValorisationStock", id.toString())))
                .flatMap(existing -> historiqueRepo.deleteByRegleId(existing.getId())
                    .then(repository.delete(existing))));
    }

    public Mono<RegleValorisationStockDto> findById(UUID id) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMap(orgId -> repository.findById(id)
                .filter(e -> orgId.equals(e.getOrganizationId()))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("RegleValorisationStock", id.toString())))
                .flatMap(this::enrichDto));
    }

    public Flux<RegleValorisationStockDto> getAll() {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMapMany(repository::findByOrganizationId)
            .flatMap(this::enrichDto);
    }

    private void validateBusinessRules(RegleValorisationStockDto dto) {
        if (dto.getMethode() != null && !METHODES_VALIDES.contains(dto.getMethode())) {
            throw new BusinessException("Méthode de valorisation invalide : " + dto.getMethode());
        }
    }

    private Mono<Void> saveHistorique(UUID regleId, List<HistoriqueValorisationDto> historique) {
        if (historique == null || historique.isEmpty()) {
            return Mono.empty();
        }
        return Flux.fromIterable(historique)
            .map(h -> HistoriqueValorisationStock.builder()
                .id(UUID.randomUUID())
                .regleId(regleId)
                .methode(h.getMethode())
                .dateDu(h.getDu())
                .dateAu(h.getAu())
                .build())
            .flatMap(historiqueRepo::save)
            .then();
    }

    private Mono<RegleValorisationStockDto> enrichDto(RegleValorisationStock entity) {
        return historiqueRepo.findByRegleId(entity.getId())
            .map(h -> HistoriqueValorisationDto.builder()
                .methode(h.getMethode())
                .du(h.getDateDu())
                .au(h.getDateAu())
                .build())
            .collectList()
            .map(historique -> RegleValorisationStockDto.builder()
                .id(entity.getId())
                .familleId(entity.getFamilleId())
                .familleLibelle(entity.getFamilleLibelle())
                .methode(entity.getMethode())
                .dateApplication(entity.getDateApplication())
                .actif(entity.getActif())
                .historique(historique)
                .build());
    }
}
