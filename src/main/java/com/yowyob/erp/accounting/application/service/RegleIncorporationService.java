package com.yowyob.erp.accounting.application.service;

import com.yowyob.erp.accounting.domain.model.RegleIncorporation;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.RegleIncorporationRepository;
import com.yowyob.erp.accounting.infrastructure.web.dto.RegleIncorporationDto;
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
public class RegleIncorporationService {

    private static final List<String> MODES_VALIDES =
        List.of("INCORPORABLE", "NON_INCORPORABLE", "SUBSTITUTION");

    private final RegleIncorporationRepository repository;

    @Transactional
    public Mono<RegleIncorporationDto> create(RegleIncorporationDto dto) {
        validateBusinessRules(dto);
        return ReactiveOrganizationContext.getOrganizationId()
            .zipWith(ReactiveOrganizationContext.getCurrentUser().defaultIfEmpty("system"))
            .flatMap(t -> {
                UUID orgId = t.getT1();
                String user = t.getT2();
                return repository.findByOrganizationIdAndCompteCgId(orgId, dto.getCompteCgId())
                    .flatMap(existing -> Mono.<RegleIncorporationDto>error(
                        new BusinessException("Une règle existe déjà pour ce compte CG.")))
                    .switchIfEmpty(Mono.defer(() -> {
                        LocalDateTime now = LocalDateTime.now();
                        RegleIncorporation entity = toEntity(dto, orgId, user, now, UUID.randomUUID());
                        return repository.save(entity).flatMap(this::enrichDto);
                    }));
            });
    }

    @Transactional
    public Mono<RegleIncorporationDto> update(UUID id, RegleIncorporationDto dto) {
        validateBusinessRules(dto);
        return ReactiveOrganizationContext.getOrganizationId()
            .zipWith(ReactiveOrganizationContext.getCurrentUser().defaultIfEmpty("system"))
            .flatMap(t -> {
                UUID orgId = t.getT1();
                String user = t.getT2();
                return repository.findById(id)
                    .filter(e -> orgId.equals(e.getOrganizationId()))
                    .switchIfEmpty(Mono.error(new ResourceNotFoundException("RegleIncorporation", id.toString())))
                    .flatMap(existing -> repository.findByOrganizationIdAndCompteCgId(orgId, dto.getCompteCgId())
                        .filter(other -> !other.getId().equals(id))
                        .flatMap(other -> Mono.<RegleIncorporation>error(
                            new BusinessException("Une règle existe déjà pour ce compte CG.")))
                        .switchIfEmpty(Mono.just(existing))
                        .flatMap(entity -> {
                            applyDto(entity, dto);
                            entity.setUpdatedAt(LocalDateTime.now());
                            entity.setUpdatedBy(user);
                            entity.setNotNew();
                            return repository.save(entity).flatMap(this::enrichDto);
                        }));
            });
    }

    @Transactional
    public Mono<Void> delete(UUID id) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMap(orgId -> repository.findById(id)
                .filter(e -> orgId.equals(e.getOrganizationId()))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("RegleIncorporation", id.toString())))
                .flatMap(existing -> repository.existsEcrituresForCompteCg(orgId, existing.getCompteCgId())
                    .flatMap(hasEcritures -> {
                        if (Boolean.TRUE.equals(hasEcritures)) {
                            return Mono.error(new BusinessException(
                                "Impossible de supprimer — des écritures ont déjà utilisé cette règle."));
                        }
                        return repository.delete(existing);
                    })));
    }

    public Mono<RegleIncorporationDto> findById(UUID id) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMap(orgId -> repository.findById(id)
                .filter(e -> orgId.equals(e.getOrganizationId()))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("RegleIncorporation", id.toString())))
                .flatMap(this::enrichDto));
    }

    public Flux<RegleIncorporationDto> getAll() {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMapMany(repository::findByOrganizationId)
            .flatMap(this::enrichDto);
    }

    /**
     * Détermine si un compte CG est incorporable selon les règles explicites ou les défauts OHADA (66x/67x/69x).
     */
    public Mono<Boolean> isIncorporable(UUID orgId, String noCompte) {
        if (noCompte == null || noCompte.isBlank()) {
            return Mono.just(false);
        }
        return repository.findByOrganizationId(orgId)
            .filter(r -> noCompte.equals(r.getCompteCgNo())
                || noCompte.startsWith(r.getCompteCgNo()))
            .sort((a, b) -> Integer.compare(b.getCompteCgNo().length(), a.getCompteCgNo().length()))
            .next()
            .map(rule -> switch (rule.getMode()) {
                case "NON_INCORPORABLE" -> false;
                case "INCORPORABLE", "SUBSTITUTION" -> true;
                default -> ImportCgHelper.isIncorporableByDefault(noCompte);
            })
            .defaultIfEmpty(ImportCgHelper.isIncorporableByDefault(noCompte));
    }

    private void validateBusinessRules(RegleIncorporationDto dto) {
        if (dto.getMode() != null && !MODES_VALIDES.contains(dto.getMode())) {
            throw new BusinessException("Mode d'incorporation invalide : " + dto.getMode());
        }
        if ("NON_INCORPORABLE".equals(dto.getMode())
            && (dto.getJustification() == null || dto.getJustification().isBlank())) {
            throw new BusinessException("La justification est obligatoire pour une charge non incorporable.");
        }
        if ("SUBSTITUTION".equals(dto.getMode())) {
            boolean hasTaux = dto.getTauxSubstitution() != null && dto.getTauxSubstitution().signum() > 0;
            boolean hasMontant = dto.getMontantSubstitution() != null && dto.getMontantSubstitution().signum() > 0;
            if (!hasTaux && !hasMontant) {
                throw new BusinessException("Un taux ou un montant de substitution est requis.");
            }
        }
        if (dto.getCompteCgNo() != null && !ImportCgHelper.isIncorporableByDefault(dto.getCompteCgNo())
            && "INCORPORABLE".equals(dto.getMode())
            && (dto.getJustification() == null || dto.getJustification().isBlank())) {
            throw new BusinessException(
                "Les comptes 66x/67x/69x nécessitent une justification pour être marqués incorporables.");
        }
    }

    private RegleIncorporation toEntity(
            RegleIncorporationDto dto, UUID orgId, String user, LocalDateTime now, UUID id) {
        return RegleIncorporation.builder()
            .id(id)
            .organizationId(orgId)
            .compteCgId(dto.getCompteCgId())
            .compteCgNo(dto.getCompteCgNo().trim())
            .libelle(dto.getLibelle().trim())
            .mode(dto.getMode())
            .tauxSubstitution(dto.getTauxSubstitution())
            .montantSubstitution(dto.getMontantSubstitution())
            .baseCalcul(dto.getBaseCalcul())
            .justification(dto.getJustification())
            .compteEcart97(dto.getCompteEcart97())
            .periodeId(dto.getPeriodeId())
            .dateDebut(dto.getDateDebut())
            .dateFin(dto.getDateFin())
            .createdAt(now)
            .updatedAt(now)
            .createdBy(user)
            .updatedBy(user)
            .build();
    }

    private void applyDto(RegleIncorporation entity, RegleIncorporationDto dto) {
        entity.setCompteCgId(dto.getCompteCgId());
        entity.setCompteCgNo(dto.getCompteCgNo().trim());
        entity.setLibelle(dto.getLibelle().trim());
        entity.setMode(dto.getMode());
        entity.setTauxSubstitution(dto.getTauxSubstitution());
        entity.setMontantSubstitution(dto.getMontantSubstitution());
        entity.setBaseCalcul(dto.getBaseCalcul());
        entity.setJustification(dto.getJustification());
        entity.setCompteEcart97(dto.getCompteEcart97());
        entity.setPeriodeId(dto.getPeriodeId());
        entity.setDateDebut(dto.getDateDebut());
        entity.setDateFin(dto.getDateFin());
    }

    private Mono<RegleIncorporationDto> enrichDto(RegleIncorporation entity) {
        return repository.existsEcrituresForCompteCg(entity.getOrganizationId(), entity.getCompteCgId())
            .map(hasEcritures -> RegleIncorporationDto.builder()
                .id(entity.getId())
                .compteCgId(entity.getCompteCgId())
                .compteCgNo(entity.getCompteCgNo())
                .libelle(entity.getLibelle())
                .mode(entity.getMode())
                .tauxSubstitution(entity.getTauxSubstitution())
                .montantSubstitution(entity.getMontantSubstitution())
                .baseCalcul(entity.getBaseCalcul())
                .justification(entity.getJustification())
                .compteEcart97(entity.getCompteEcart97())
                .periodeId(entity.getPeriodeId())
                .dateDebut(entity.getDateDebut())
                .dateFin(entity.getDateFin())
                .hasEcritures(Boolean.TRUE.equals(hasEcritures))
                .build());
    }
}
