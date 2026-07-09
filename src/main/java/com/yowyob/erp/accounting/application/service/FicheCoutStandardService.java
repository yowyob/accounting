package com.yowyob.erp.accounting.application.service;

import com.yowyob.erp.accounting.domain.model.FicheCoutStandard;
import com.yowyob.erp.accounting.domain.model.LigneCoutStandard;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.AxeAnalytiqueRepository;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.FicheCoutStandardRepository;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.LigneCoutStandardRepository;
import com.yowyob.erp.accounting.infrastructure.web.dto.FicheCoutStandardDto;
import com.yowyob.erp.accounting.infrastructure.web.dto.LigneCoutStandardDto;
import com.yowyob.erp.config.organization.ReactiveOrganizationContext;
import com.yowyob.erp.shared.domain.exception.BusinessException;
import com.yowyob.erp.shared.domain.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FicheCoutStandardService {

    private final FicheCoutStandardRepository repository;
    private final LigneCoutStandardRepository ligneRepo;
    private final AxeAnalytiqueRepository axeRepo;

    @Transactional
    public Mono<FicheCoutStandardDto> create(FicheCoutStandardDto dto) {
        validateBusinessRules(dto);
        return ReactiveOrganizationContext.getOrganizationId()
            .zipWith(ReactiveOrganizationContext.getCurrentUser().defaultIfEmpty("system"))
            .flatMap(t -> {
                UUID orgId = t.getT1();
                String user = t.getT2();
                LocalDateTime now = LocalDateTime.now();
                FicheCoutStandard entity = FicheCoutStandard.builder()
                    .id(UUID.randomUUID())
                    .organizationId(orgId)
                    .produitCode(dto.getProduitCode().trim())
                    .produitLibelle(dto.getProduitLibelle().trim())
                    .periodeRefId(dto.getPeriodeRefId())
                    .planAnalytiqueId(dto.getPlanAnalytiqueId().trim())
                    .periodeCommencee(dto.getPeriodeCommencee() != null ? dto.getPeriodeCommencee() : false)
                    .createdAt(now)
                    .updatedAt(now)
                    .createdBy(user)
                    .updatedBy(user)
                    .build();
                return repository.save(entity)
                    .flatMap(saved -> saveLignes(saved.getId(), dto.getLignes())
                        .then(enrichDto(saved)));
            });
    }

    @Transactional
    public Mono<FicheCoutStandardDto> update(UUID id, FicheCoutStandardDto dto) {
        validateBusinessRules(dto);
        return ReactiveOrganizationContext.getOrganizationId()
            .zipWith(ReactiveOrganizationContext.getCurrentUser().defaultIfEmpty("system"))
            .flatMap(t -> {
                UUID orgId = t.getT1();
                String user = t.getT2();
                return repository.findById(id)
                    .filter(e -> orgId.equals(e.getOrganizationId()))
                    .switchIfEmpty(Mono.error(new ResourceNotFoundException("FicheCoutStandard", id.toString())))
                    .flatMap(existing -> {
                        if (Boolean.TRUE.equals(existing.getPeriodeCommencee())) {
                            return Mono.error(new BusinessException(
                                "Cette fiche est verrouillée : la période a déjà démarré."));
                        }
                        existing.setProduitCode(dto.getProduitCode().trim());
                        existing.setProduitLibelle(dto.getProduitLibelle().trim());
                        existing.setPeriodeRefId(dto.getPeriodeRefId());
                        existing.setPlanAnalytiqueId(dto.getPlanAnalytiqueId().trim());
                        existing.setPeriodeCommencee(
                            dto.getPeriodeCommencee() != null ? dto.getPeriodeCommencee() : false);
                        existing.setUpdatedAt(LocalDateTime.now());
                        existing.setUpdatedBy(user);
                        existing.setNotNew();

                        return repository.save(existing)
                            .flatMap(saved -> ligneRepo.deleteByFicheId(saved.getId())
                                .then(saveLignes(saved.getId(), dto.getLignes()))
                                .then(enrichDto(saved)));
                    });
            });
    }

    @Transactional
    public Mono<Void> delete(UUID id) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMap(orgId -> repository.findById(id)
                .filter(e -> orgId.equals(e.getOrganizationId()))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("FicheCoutStandard", id.toString())))
                .flatMap(existing -> {
                    if (Boolean.TRUE.equals(existing.getPeriodeCommencee())) {
                        return Mono.error(new BusinessException(
                            "Impossible de supprimer une fiche verrouillée."));
                    }
                    return ligneRepo.deleteByFicheId(existing.getId())
                        .then(repository.delete(existing));
                }));
    }

    public Mono<FicheCoutStandardDto> findById(UUID id) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMap(orgId -> repository.findById(id)
                .filter(e -> orgId.equals(e.getOrganizationId()))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("FicheCoutStandard", id.toString())))
                .flatMap(this::enrichDto));
    }

    public Flux<FicheCoutStandardDto> getAll(UUID periodeRefId) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMapMany(orgId -> periodeRefId != null
                ? repository.findByOrganizationIdAndPeriodeRefId(orgId, periodeRefId)
                : repository.findByOrganizationId(orgId))
            .flatMap(this::enrichDto);
    }

    private void validateBusinessRules(FicheCoutStandardDto dto) {
        List<String> composantes = List.of("MATIERES", "MOD", "CHARGES_INDIRECTES");
        if (dto.getLignes() != null) {
            for (LigneCoutStandardDto ligne : dto.getLignes()) {
                if (ligne.getComposante() != null && !composantes.contains(ligne.getComposante())) {
                    throw new BusinessException("Composante invalide : " + ligne.getComposante());
                }
                if ("CHARGES_INDIRECTES".equals(ligne.getComposante()) && ligne.getCentreId() == null) {
                    throw new BusinessException(
                        "Un centre d'analyse est requis pour les charges indirectes.");
                }
                if (ligne.getQuantiteStandard() != null
                    && ligne.getQuantiteStandard().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BusinessException("La quantité standard doit être strictement positive.");
                }
                if (ligne.getCoutUnitaireStandard() != null
                    && ligne.getCoutUnitaireStandard().compareTo(BigDecimal.ZERO) < 0) {
                    throw new BusinessException("Le coût unitaire standard ne peut pas être négatif.");
                }
            }
        }
    }

    private Mono<Void> saveLignes(UUID ficheId, List<LigneCoutStandardDto> lignes) {
        if (lignes == null || lignes.isEmpty()) {
            return Mono.empty();
        }
        return Flux.fromIterable(lignes)
            .map(l -> {
                BigDecimal total = l.getCoutStandardTotal();
                if (total == null && l.getQuantiteStandard() != null && l.getCoutUnitaireStandard() != null) {
                    total = l.getQuantiteStandard()
                        .multiply(l.getCoutUnitaireStandard())
                        .setScale(2, RoundingMode.HALF_UP);
                }
                return LigneCoutStandard.builder()
                    .id(l.getId() != null ? l.getId() : UUID.randomUUID())
                    .ficheId(ficheId)
                    .composante(l.getComposante())
                    .centreId(l.getCentreId())
                    .libelle(l.getLibelle().trim())
                    .quantiteStandard(l.getQuantiteStandard())
                    .coutUnitaireStandard(l.getCoutUnitaireStandard())
                    .coutStandardTotal(total != null ? total : BigDecimal.ZERO)
                    .activiteNormale(l.getActiviteNormale())
                    .build();
            })
            .flatMap(ligneRepo::save)
            .then();
    }

    private Mono<FicheCoutStandardDto> enrichDto(FicheCoutStandard fiche) {
        return ligneRepo.findByFicheId(fiche.getId())
            .flatMap(ligne -> {
                Mono<String> centreLibelle = ligne.getCentreId() != null
                    ? axeRepo.findById(ligne.getCentreId()).map(a -> a.getLibelle()).defaultIfEmpty("")
                    : Mono.just("");
                return centreLibelle.map(lib -> LigneCoutStandardDto.builder()
                    .id(ligne.getId())
                    .composante(ligne.getComposante())
                    .centreId(ligne.getCentreId())
                    .centreLibelle(lib)
                    .libelle(ligne.getLibelle())
                    .quantiteStandard(ligne.getQuantiteStandard())
                    .coutUnitaireStandard(ligne.getCoutUnitaireStandard())
                    .coutStandardTotal(ligne.getCoutStandardTotal())
                    .activiteNormale(ligne.getActiviteNormale())
                    .build());
            })
            .collectList()
            .map(lignes -> FicheCoutStandardDto.builder()
                .id(fiche.getId())
                .produitCode(fiche.getProduitCode())
                .produitLibelle(fiche.getProduitLibelle())
                .periodeRefId(fiche.getPeriodeRefId())
                .planAnalytiqueId(fiche.getPlanAnalytiqueId())
                .periodeCommencee(fiche.getPeriodeCommencee())
                .lignes(lignes)
                .build());
    }
}
