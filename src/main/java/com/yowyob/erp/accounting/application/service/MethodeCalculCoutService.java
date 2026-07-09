package com.yowyob.erp.accounting.application.service;

import com.yowyob.erp.accounting.domain.model.ActiviteNormaleMethode;
import com.yowyob.erp.accounting.domain.model.MethodeCalculCout;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.ActiviteNormaleMethodeRepository;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.AxeAnalytiqueRepository;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.MethodeCalculCoutRepository;
import com.yowyob.erp.accounting.infrastructure.web.dto.ActiviteNormaleDto;
import com.yowyob.erp.accounting.infrastructure.web.dto.MethodeCalculCoutDto;
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
public class MethodeCalculCoutService {

    private static final List<String> METHODES_VALIDES = List.of(
        "COUTS_COMPLETS", "COUTS_VARIABLES", "IMPUTATION_RATIONNELLE", "COUTS_DIRECTS");
    private static final List<String> STATUTS_VALIDES = List.of("ACTIF", "ARCHIVE");

    private final MethodeCalculCoutRepository repository;
    private final ActiviteNormaleMethodeRepository activiteRepo;
    private final AxeAnalytiqueRepository axeRepo;

    @Transactional
    public Mono<MethodeCalculCoutDto> create(MethodeCalculCoutDto dto) {
        validateBusinessRules(dto);
        return ReactiveOrganizationContext.getOrganizationId()
            .zipWith(ReactiveOrganizationContext.getCurrentUser().defaultIfEmpty("system"))
            .flatMap(t -> {
                UUID orgId = t.getT1();
                String user = t.getT2();
                Mono<Void> archiveOthers = "ACTIF".equals(dto.getStatut())
                    ? archiveActiveForPlan(orgId, dto.getPlanAnalytiqueId(), user)
                    : Mono.empty();
                return archiveOthers.then(Mono.defer(() -> {
                    LocalDateTime now = LocalDateTime.now();
                    MethodeCalculCout entity = MethodeCalculCout.builder()
                        .id(UUID.randomUUID())
                        .organizationId(orgId)
                        .methode(dto.getMethode())
                        .planAnalytiqueId(dto.getPlanAnalytiqueId().trim())
                        .dateApplication(dto.getDateApplication())
                        .statut(dto.getStatut())
                        .description(dto.getDescription())
                        .createdAt(now)
                        .updatedAt(now)
                        .createdBy(user)
                        .updatedBy(user)
                        .build();
                    return repository.save(entity)
                        .flatMap(saved -> saveActivites(saved.getId(), dto.getActivitesNormales())
                            .then(enrichDto(saved)));
                }));
            });
    }

    @Transactional
    public Mono<MethodeCalculCoutDto> update(UUID id, MethodeCalculCoutDto dto) {
        validateBusinessRules(dto);
        return ReactiveOrganizationContext.getOrganizationId()
            .zipWith(ReactiveOrganizationContext.getCurrentUser().defaultIfEmpty("system"))
            .flatMap(t -> {
                UUID orgId = t.getT1();
                String user = t.getT2();
                Mono<Void> archiveOthers = "ACTIF".equals(dto.getStatut())
                    ? archiveActiveForPlan(orgId, dto.getPlanAnalytiqueId(), user, id)
                    : Mono.empty();
                return archiveOthers.then(Mono.defer(() -> repository.findById(id)
                    .filter(e -> orgId.equals(e.getOrganizationId()))
                    .switchIfEmpty(Mono.error(new ResourceNotFoundException("MethodeCalculCout", id.toString())))
                    .flatMap(existing -> {
                        existing.setMethode(dto.getMethode());
                        existing.setPlanAnalytiqueId(dto.getPlanAnalytiqueId().trim());
                        existing.setDateApplication(dto.getDateApplication());
                        existing.setStatut(dto.getStatut());
                        existing.setDescription(dto.getDescription());
                        existing.setUpdatedAt(LocalDateTime.now());
                        existing.setUpdatedBy(user);
                        existing.setNotNew();
                        return repository.save(existing)
                            .flatMap(saved -> activiteRepo.deleteByMethodeCalculId(saved.getId())
                                .then(saveActivites(saved.getId(), dto.getActivitesNormales()))
                                .then(enrichDto(saved)));
                    })));
            });
    }

    @Transactional
    public Mono<Void> delete(UUID id) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMap(orgId -> repository.findById(id)
                .filter(e -> orgId.equals(e.getOrganizationId()))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("MethodeCalculCout", id.toString())))
                .flatMap(existing -> {
                    if ("ACTIF".equals(existing.getStatut())) {
                        return Mono.error(new BusinessException(
                            "Impossible de supprimer une méthode active — archivez-la d'abord."));
                    }
                    return activiteRepo.deleteByMethodeCalculId(existing.getId())
                        .then(repository.delete(existing));
                }));
    }

    public Mono<MethodeCalculCoutDto> findById(UUID id) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMap(orgId -> repository.findById(id)
                .filter(e -> orgId.equals(e.getOrganizationId()))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("MethodeCalculCout", id.toString())))
                .flatMap(this::enrichDto));
    }

    public Flux<MethodeCalculCoutDto> getAll(String planAnalytiqueId, String statut) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMapMany(orgId -> {
                if (planAnalytiqueId != null && statut != null) {
                    return repository.findByOrganizationIdAndPlanAnalytiqueId(orgId, planAnalytiqueId)
                        .filter(m -> statut.equals(m.getStatut()));
                }
                if (planAnalytiqueId != null) {
                    return repository.findByOrganizationIdAndPlanAnalytiqueId(orgId, planAnalytiqueId);
                }
                if (statut != null) {
                    return repository.findByOrganizationIdAndStatut(orgId, statut);
                }
                return repository.findByOrganizationId(orgId);
            })
            .flatMap(this::enrichDto);
    }

    private Mono<Void> archiveActiveForPlan(UUID orgId, String planId, String user) {
        return archiveActiveForPlan(orgId, planId, user, null);
    }

    private Mono<Void> archiveActiveForPlan(UUID orgId, String planId, String user, UUID excludeId) {
        return repository.findByOrganizationIdAndPlanAnalytiqueId(orgId, planId)
            .filter(m -> "ACTIF".equals(m.getStatut()))
            .filter(m -> excludeId == null || !excludeId.equals(m.getId()))
            .flatMap(m -> {
                m.setStatut("ARCHIVE");
                m.setUpdatedAt(LocalDateTime.now());
                m.setUpdatedBy(user);
                m.setNotNew();
                return repository.save(m);
            })
            .then();
    }

    private void validateBusinessRules(MethodeCalculCoutDto dto) {
        if (dto.getMethode() != null && !METHODES_VALIDES.contains(dto.getMethode())) {
            throw new BusinessException("Méthode de calcul invalide : " + dto.getMethode());
        }
        if (dto.getStatut() != null && !STATUTS_VALIDES.contains(dto.getStatut())) {
            throw new BusinessException("Statut invalide : " + dto.getStatut());
        }
        if ("IMPUTATION_RATIONNELLE".equals(dto.getMethode())
            && (dto.getActivitesNormales() == null || dto.getActivitesNormales().isEmpty())) {
            throw new BusinessException(
                "Les activités normales sont requises pour l'imputation rationnelle.");
        }
    }

    private Mono<Void> saveActivites(UUID methodeId, List<ActiviteNormaleDto> activites) {
        if (activites == null || activites.isEmpty()) {
            return Mono.empty();
        }
        return Flux.fromIterable(activites)
            .flatMap(a -> {
                Mono<String> centreLibelle = a.getCentreId() != null
                    ? axeRepo.findById(a.getCentreId()).map(ax -> ax.getLibelle()).defaultIfEmpty("")
                    : Mono.just(a.getCentreLibelle() != null ? a.getCentreLibelle() : "");
                return centreLibelle.map(lib -> ActiviteNormaleMethode.builder()
                    .id(UUID.randomUUID())
                    .methodeCalculId(methodeId)
                    .centreId(a.getCentreId())
                    .centreLibelle(lib)
                    .activiteNormale(a.getActiviteNormale())
                    .unite(a.getUnite())
                    .build());
            })
            .flatMap(activiteRepo::save)
            .then();
    }

    private Mono<MethodeCalculCoutDto> enrichDto(MethodeCalculCout entity) {
        return activiteRepo.findByMethodeCalculId(entity.getId())
            .map(a -> ActiviteNormaleDto.builder()
                .centreId(a.getCentreId())
                .centreLibelle(a.getCentreLibelle())
                .activiteNormale(a.getActiviteNormale())
                .unite(a.getUnite())
                .build())
            .collectList()
            .map(activites -> MethodeCalculCoutDto.builder()
                .id(entity.getId())
                .methode(entity.getMethode())
                .planAnalytiqueId(entity.getPlanAnalytiqueId())
                .dateApplication(entity.getDateApplication())
                .statut(entity.getStatut())
                .description(entity.getDescription())
                .activitesNormales(activites)
                .build());
    }
}
