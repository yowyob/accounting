package com.yowyob.erp.accounting.application.service;

import com.yowyob.erp.accounting.domain.model.PrixCessionInterne;
import com.yowyob.erp.accounting.domain.model.PrixCessionVersion;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.AxeAnalytiqueRepository;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.PrixCessionInterneRepository;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.PrixCessionVersionRepository;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.UniteOeuvreRepository;
import com.yowyob.erp.accounting.infrastructure.web.dto.PrixCessionInterneDto;
import com.yowyob.erp.accounting.infrastructure.web.dto.PrixCessionVersionDto;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PrixCessionInterneService {

    private final PrixCessionInterneRepository repository;
    private final PrixCessionVersionRepository versionRepo;
    private final AxeAnalytiqueRepository axeRepo;
    private final UniteOeuvreRepository uniteRepo;

    @Transactional
    public Mono<PrixCessionInterneDto> create(PrixCessionInterneDto dto) {
        validateBusinessRules(dto);
        return ReactiveOrganizationContext.getOrganizationId()
            .zipWith(ReactiveOrganizationContext.getCurrentUser().defaultIfEmpty("system"))
            .flatMap(t -> {
                UUID orgId = t.getT1();
                String user = t.getT2();
                return assertUniqueness(orgId, dto, null)
                    .then(Mono.defer(() -> {
                        LocalDateTime now = LocalDateTime.now();
                        PrixCessionInterne entity = PrixCessionInterne.builder()
                            .id(UUID.randomUUID())
                            .organizationId(orgId)
                            .centreCedantId(dto.getCentreCedantId())
                            .centreBeneficiaireId(dto.getCentreBeneficiaireId())
                            .prestationLibelle(dto.getPrestationLibelle().trim())
                            .methode(dto.getMethode())
                            .prixUnitaire(dto.getPrixUnitaire())
                            .uniteId(dto.getUniteId())
                            .dateDebut(dto.getDateDebut())
                            .dateFin(dto.getDateFin())
                            .hasImputations(false)
                            .createdAt(now)
                            .updatedAt(now)
                            .createdBy(user)
                            .updatedBy(user)
                            .build();
                        return repository.save(entity).flatMap(this::enrichDto);
                    }));
            });
    }

    @Transactional
    public Mono<PrixCessionInterneDto> update(UUID id, PrixCessionInterneDto dto) {
        validateBusinessRules(dto);
        return ReactiveOrganizationContext.getOrganizationId()
            .zipWith(ReactiveOrganizationContext.getCurrentUser().defaultIfEmpty("system"))
            .flatMap(t -> {
                UUID orgId = t.getT1();
                String user = t.getT2();
                return repository.findById(id)
                    .filter(e -> orgId.equals(e.getOrganizationId()))
                    .switchIfEmpty(Mono.error(new ResourceNotFoundException("PrixCessionInterne", id.toString())))
                    .flatMap(existing -> assertUniqueness(orgId, dto, id)
                        .then(Mono.defer(() -> {
                            boolean priceChanged = existing.getPrixUnitaire() != null
                                && dto.getPrixUnitaire() != null
                                && existing.getPrixUnitaire().compareTo(dto.getPrixUnitaire()) != 0;

                            Mono<Void> versionMono = priceChanged
                                ? createVersion(existing, LocalDate.now())
                                : Mono.empty();

                            existing.setCentreCedantId(dto.getCentreCedantId());
                            existing.setCentreBeneficiaireId(dto.getCentreBeneficiaireId());
                            existing.setPrestationLibelle(dto.getPrestationLibelle().trim());
                            existing.setMethode(dto.getMethode());
                            existing.setPrixUnitaire(dto.getPrixUnitaire());
                            existing.setUniteId(dto.getUniteId());
                            existing.setDateDebut(dto.getDateDebut());
                            existing.setDateFin(dto.getDateFin());
                            existing.setUpdatedAt(LocalDateTime.now());
                            existing.setUpdatedBy(user);
                            existing.setNotNew();

                            return versionMono
                                .then(repository.save(existing))
                                .flatMap(this::enrichDto);
                        })));
            });
    }

    @Transactional
    public Mono<Void> delete(UUID id) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMap(orgId -> repository.findById(id)
                .filter(e -> orgId.equals(e.getOrganizationId()))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("PrixCessionInterne", id.toString())))
                .flatMap(existing -> {
                    if (Boolean.TRUE.equals(existing.getHasImputations())) {
                        return Mono.error(new BusinessException(
                            "Impossible de supprimer ce tarif : des imputations l'ont déjà utilisé."));
                    }
                    return repository.delete(existing);
                }));
    }

    public Mono<PrixCessionInterneDto> findById(UUID id) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMap(orgId -> repository.findById(id)
                .filter(e -> orgId.equals(e.getOrganizationId()))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("PrixCessionInterne", id.toString())))
                .flatMap(this::enrichDto));
    }

    public Flux<PrixCessionInterneDto> getAll(UUID centreCedantId, UUID centreBeneficiaireId) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMapMany(orgId -> {
                if (centreCedantId != null) {
                    return repository.findByOrganizationIdAndCentreCedantId(orgId, centreCedantId);
                }
                if (centreBeneficiaireId != null) {
                    return repository.findByOrganizationIdAndCentreBeneficiaireId(orgId, centreBeneficiaireId);
                }
                return repository.findByOrganizationId(orgId);
            })
            .flatMap(this::enrichDto);
    }

    private void validateBusinessRules(PrixCessionInterneDto dto) {
        if (dto.getCentreCedantId() != null && dto.getCentreCedantId().equals(dto.getCentreBeneficiaireId())) {
            throw new BusinessException("Le centre cédant et le centre bénéficiaire doivent être distincts.");
        }
        if (dto.getPrixUnitaire() != null && dto.getPrixUnitaire().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Le prix unitaire doit être strictement positif.");
        }
        if (dto.getDateFin() != null && dto.getDateDebut() != null && dto.getDateFin().isBefore(dto.getDateDebut())) {
            throw new BusinessException("La date de fin ne peut pas être antérieure à la date de début.");
        }
        List<String> methodes = List.of("COUT_COMPLET", "PRIX_MARCHE", "PRIX_CONVENTIONNEL");
        if (dto.getMethode() != null && !methodes.contains(dto.getMethode())) {
            throw new BusinessException("Méthode de valorisation invalide : " + dto.getMethode());
        }
    }

    private Mono<Void> assertUniqueness(UUID orgId, PrixCessionInterneDto dto, UUID excludeId) {
        if (!isActif(dto.getDateFin())) {
            return Mono.empty();
        }
        return repository
            .findByOrganizationIdAndCentreCedantIdAndCentreBeneficiaireIdAndPrestationLibelleIgnoreCase(
                orgId,
                dto.getCentreCedantId(),
                dto.getCentreBeneficiaireId(),
                dto.getPrestationLibelle().trim())
            .filter(existing -> excludeId == null || !existing.getId().equals(excludeId))
            .filter(existing -> isActif(existing.getDateFin()))
            .hasElement()
            .flatMap(conflict -> conflict
                ? Mono.error(new BusinessException(
                    "Un tarif actif existe déjà pour ce couple cédant/bénéficiaire et cette prestation."))
                : Mono.empty());
    }

    private boolean isActif(LocalDate dateFin) {
        return dateFin == null || !dateFin.isBefore(LocalDate.now());
    }

    private Mono<Void> createVersion(PrixCessionInterne existing, LocalDate dateFin) {
        PrixCessionVersion version = PrixCessionVersion.builder()
            .id(UUID.randomUUID())
            .prixCessionId(existing.getId())
            .prixUnitaire(existing.getPrixUnitaire())
            .methode(existing.getMethode())
            .dateDebut(existing.getDateDebut())
            .dateFin(dateFin)
            .createdAt(LocalDateTime.now())
            .build();
        return versionRepo.save(version).then();
    }

    private Mono<PrixCessionInterneDto> enrichDto(PrixCessionInterne entity) {
        Mono<String> cedantLibelle = entity.getCentreCedantId() != null
            ? axeRepo.findById(entity.getCentreCedantId()).map(a -> a.getLibelle()).defaultIfEmpty("")
            : Mono.just("");
        Mono<String> beneficiaireLibelle = entity.getCentreBeneficiaireId() != null
            ? axeRepo.findById(entity.getCentreBeneficiaireId()).map(a -> a.getLibelle()).defaultIfEmpty("")
            : Mono.just("");
        Mono<String> uniteLibelle = entity.getUniteId() != null
            ? uniteRepo.findById(entity.getUniteId()).map(u -> u.getLibelle()).defaultIfEmpty("")
            : Mono.just("");

        return Mono.zip(cedantLibelle, beneficiaireLibelle, uniteLibelle)
            .flatMap(t -> versionRepo.findByPrixCessionIdOrderByDateDebutDesc(entity.getId())
                .map(v -> PrixCessionVersionDto.builder()
                    .id(v.getId())
                    .prixUnitaire(v.getPrixUnitaire())
                    .methode(v.getMethode())
                    .du(v.getDateDebut())
                    .au(v.getDateFin())
                    .build())
                .collectList()
                .map(versions -> PrixCessionInterneDto.builder()
                    .id(entity.getId())
                    .centreCedantId(entity.getCentreCedantId())
                    .centreCedantLibelle(t.getT1())
                    .centreBeneficiaireId(entity.getCentreBeneficiaireId())
                    .centreBeneficiaireLibelle(t.getT2())
                    .prestationLibelle(entity.getPrestationLibelle())
                    .methode(entity.getMethode())
                    .prixUnitaire(entity.getPrixUnitaire())
                    .uniteId(entity.getUniteId())
                    .uniteLibelle(t.getT3())
                    .dateDebut(entity.getDateDebut())
                    .dateFin(entity.getDateFin())
                    .hasImputations(entity.getHasImputations())
                    .versions(versions)
                    .build()));
    }
}
