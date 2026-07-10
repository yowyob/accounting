package com.yowyob.erp.accounting.application.service;

import com.yowyob.erp.accounting.domain.model.EcritureAnalytique;
import com.yowyob.erp.accounting.domain.model.LigneImputation;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.*;
import com.yowyob.erp.accounting.infrastructure.web.dto.EcritureAnalytiqueDto;
import com.yowyob.erp.accounting.infrastructure.web.dto.LigneImputationDto;
import com.yowyob.erp.config.organization.ReactiveOrganizationContext;
import com.yowyob.erp.shared.application.service.IdempotencyService;
import com.yowyob.erp.shared.domain.exception.ConflictException;
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
public class EcritureAnalytiqueService {

    private static final String ENTITY_TYPE = "ecriture_analytique";

    private final EcritureAnalytiqueRepository ecritureRepo;
    private final LigneImputationRepository ligneRepo;
    private final JournalAnalytiqueRepository journalRepo;
    private final PeriodeAnalytiqueRepository periodeRepo;
    private final AxeAnalytiqueRepository axeRepo;
    private final CompteAnalytiqueRepository compteAnalytiqueRepo;
    private final IdempotencyService idempotencyService;

    @Transactional
    public Mono<CreateEcritureResult> create(EcritureAnalytiqueDto dto, String idempotencyKeyHeader) {
        return ReactiveOrganizationContext.getOrganizationId()
            .zipWith(ReactiveOrganizationContext.getCurrentUser().defaultIfEmpty("system"))
            .flatMap(t -> {
                UUID orgId = t.getT1();
                String user = t.getT2();
                String idempotencyKey = resolveIdempotencyKey(idempotencyKeyHeader, dto);

                return resolveExistingByIdempotency(orgId, idempotencyKey)
                    .switchIfEmpty(resolveExistingByClientId(orgId, dto.getClientId()))
                    .map(existing -> new CreateEcritureResult(existing, true))
                    .switchIfEmpty(Mono.defer(() -> persistNewEcriture(orgId, user, dto, idempotencyKey)));
            });
    }

    private String resolveIdempotencyKey(String header, EcritureAnalytiqueDto dto) {
        if (header != null && !header.isBlank()) {
            return header.trim();
        }
        if (dto.getClientMutationId() != null && !dto.getClientMutationId().isBlank()) {
            return dto.getClientMutationId().trim();
        }
        return null;
    }

    private Mono<EcritureAnalytiqueDto> resolveExistingByIdempotency(UUID orgId, String key) {
        if (key == null) {
            return Mono.empty();
        }
        return idempotencyService.findActive(orgId, key)
            .flatMap(record -> findById(record.getEntityId()));
    }

    private Mono<EcritureAnalytiqueDto> resolveExistingByClientId(UUID orgId, String clientId) {
        if (clientId == null || clientId.isBlank()) {
            return Mono.empty();
        }
        return ecritureRepo.findByOrganizationIdAndClientId(orgId, clientId)
            .flatMap(this::enrichDto);
    }

    private Mono<CreateEcritureResult> persistNewEcriture(
            UUID orgId,
            String user,
            EcritureAnalytiqueDto dto,
            String idempotencyKey) {
        EcritureAnalytique entity = EcritureAnalytique.builder()
            .id(UUID.randomUUID()).organizationId(orgId)
            .journalId(dto.getJournalId()).periodeId(dto.getPeriodeId())
            .numeroPiece(dto.getNumeroPiece()).libelle(dto.getLibelle())
            .dateEffet(dto.getDateEffet())
            .origine(dto.getOrigine() != null ? dto.getOrigine() : "MANUELLE")
            .montantTotal(dto.getMontantTotal()).natureChargeId(dto.getNatureChargeId())
            .ecriturecgRef(dto.getEcriturecgRef())
            .clientId(dto.getClientId())
            .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
            .createdBy(user).updatedBy(user).build();

        return ecritureRepo.save(entity)
            .flatMap(saved -> saveLignes(saved.getId(), dto.getLignes())
                .then(enrichDto(saved))
                .flatMap(enriched -> storeIdempotencyIfNeeded(orgId, idempotencyKey, saved.getId())
                    .thenReturn(new CreateEcritureResult(enriched, false))));
    }

    private Mono<Void> storeIdempotencyIfNeeded(UUID orgId, String key, UUID entityId) {
        return storeIdempotencyIfNeeded(orgId, key, ENTITY_TYPE, entityId, 201);
    }

    private Mono<Void> storeIdempotencyIfNeeded(
            UUID orgId, String key, String entityType, UUID entityId, int status) {
        if (key == null || key.isBlank()) {
            return Mono.empty();
        }
        return idempotencyService.store(orgId, key.trim(), entityType, entityId, status).then();
    }

    public Flux<EcritureAnalytiqueDto> getAll(String statut, UUID periodeId) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMapMany(orgId -> {
                if (statut != null && periodeId != null)
                    return ecritureRepo.findByOrganizationIdAndStatutAndPeriodeId(orgId, statut, periodeId);
                if (statut != null)
                    return ecritureRepo.findByOrganizationIdAndStatut(orgId, statut);
                if (periodeId != null)
                    return ecritureRepo.findByOrganizationIdAndPeriodeId(orgId, periodeId);
                return ecritureRepo.findByOrganizationId(orgId);
            })
            .flatMap(this::enrichDto);
    }

    public Mono<EcritureAnalytiqueDto> findById(UUID id) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMap(orgId -> ecritureRepo.findById(id)
                .filter(e -> orgId.equals(e.getOrganizationId()))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("EcritureAnalytique", id.toString())))
                .flatMap(this::enrichDto));
    }

    @Transactional
    public Mono<EcritureAnalytiqueDto> valider(UUID id) {
        return valider(id, null);
    }

    @Transactional
    public Mono<EcritureAnalytiqueDto> valider(UUID id, String idempotencyKey) {
        return ReactiveOrganizationContext.getOrganizationId()
            .zipWith(ReactiveOrganizationContext.getCurrentUser().defaultIfEmpty("system"))
            .flatMap(t -> {
                UUID orgId = t.getT1();
                String user = t.getT2();
                String key = blankToNull(idempotencyKey);
                return resolveExistingByIdempotency(orgId, key)
                    .switchIfEmpty(Mono.defer(() -> ecritureRepo.findById(id)
                        .filter(e -> orgId.equals(e.getOrganizationId()))
                        .switchIfEmpty(Mono.error(new ResourceNotFoundException("EcritureAnalytique", id.toString())))
                        .flatMap(e -> {
                            if ("VALIDEE".equals(e.getStatut())) {
                                return enrichDto(e);
                            }
                            e.setStatut("VALIDEE");
                            e.setValidatedAt(LocalDateTime.now());
                            e.setValidatedBy(user);
                            e.setNotNew();
                            return ecritureRepo.save(e)
                                .flatMap(saved -> enrichDto(saved)
                                    .flatMap(dto -> storeIdempotencyIfNeeded(
                                            orgId, key, "ecriture_analytique_validate", saved.getId(), 200)
                                        .thenReturn(dto)));
                        })));
            });
    }

    @Transactional
    public Mono<EcritureAnalytiqueDto> rejeter(UUID id, String raison) {
        return rejeter(id, raison, null);
    }

    @Transactional
    public Mono<EcritureAnalytiqueDto> rejeter(UUID id, String raison, String idempotencyKey) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMap(orgId -> {
                String key = blankToNull(idempotencyKey);
                return resolveExistingByIdempotency(orgId, key)
                    .switchIfEmpty(Mono.defer(() -> ecritureRepo.findById(id)
                        .filter(e -> orgId.equals(e.getOrganizationId()))
                        .switchIfEmpty(Mono.error(new ResourceNotFoundException("EcritureAnalytique", id.toString())))
                        .flatMap(e -> {
                            if ("REJETEE".equals(e.getStatut())) {
                                return enrichDto(e);
                            }
                            e.setStatut("REJETEE");
                            e.setRejectReason(raison);
                            e.setNotNew();
                            return ecritureRepo.save(e)
                                .flatMap(saved -> enrichDto(saved)
                                    .flatMap(dto -> storeIdempotencyIfNeeded(
                                            orgId, key, "ecriture_analytique_reject", saved.getId(), 200)
                                        .thenReturn(dto)));
                        })));
            });
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    @Transactional
    public Mono<EcritureAnalytiqueDto> update(UUID id, EcritureAnalytiqueDto dto) {
        return ReactiveOrganizationContext.getOrganizationId()
            .zipWith(ReactiveOrganizationContext.getCurrentUser().defaultIfEmpty("system"))
            .flatMap(t -> {
                UUID orgId = t.getT1();
                String user = t.getT2();
                return ecritureRepo.findById(id)
                    .filter(e -> orgId.equals(e.getOrganizationId()))
                    .switchIfEmpty(Mono.error(new ResourceNotFoundException("EcritureAnalytique", id.toString())))
                    .flatMap(e -> {
                        if ("VALIDEE".equals(e.getStatut())) {
                            return Mono.error(new com.yowyob.erp.shared.domain.exception.BusinessException(
                                    "Impossible de modifier une écriture analytique validée"));
                        }
                        // Conflit optimiste soft via updatedAt client
                        if (dto.getUpdatedAt() != null && e.getUpdatedAt() != null
                                && e.getUpdatedAt().isAfter(dto.getUpdatedAt())) {
                            return Mono.error(new ConflictException(
                                    "L'écriture a été modifiée sur le serveur (conflit offline)"));
                        }
                        e.setJournalId(dto.getJournalId());
                        e.setPeriodeId(dto.getPeriodeId());
                        e.setNumeroPiece(dto.getNumeroPiece());
                        e.setLibelle(dto.getLibelle());
                        e.setDateEffet(dto.getDateEffet());
                        if (dto.getOrigine() != null) e.setOrigine(dto.getOrigine());
                        e.setMontantTotal(dto.getMontantTotal());
                        e.setNatureChargeId(dto.getNatureChargeId());
                        e.setEcriturecgRef(dto.getEcriturecgRef());
                        e.setUpdatedAt(LocalDateTime.now());
                        e.setUpdatedBy(user);
                        e.setNotNew();
                        return ligneRepo.deleteByEcritureId(id)
                            .then(ecritureRepo.save(e))
                            .flatMap(saved -> saveLignes(saved.getId(), dto.getLignes())
                                .then(enrichDto(saved)));
                    });
            });
    }

    @Transactional
    public Mono<Void> delete(UUID id) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMap(orgId -> ecritureRepo.findById(id)
                .filter(e -> orgId.equals(e.getOrganizationId()))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("EcritureAnalytique", id.toString())))
                .flatMap(e -> {
                    if ("VALIDEE".equals(e.getStatut())) {
                        return Mono.error(new com.yowyob.erp.shared.domain.exception.BusinessException(
                                "Impossible de supprimer une écriture analytique validée"));
                    }
                    return ligneRepo.deleteByEcritureId(id).then(ecritureRepo.delete(e));
                }));
    }

    private Mono<Void> saveLignes(UUID ecritureId, List<LigneImputationDto> lignes) {
        if (lignes == null || lignes.isEmpty()) return Mono.empty();
        return Flux.fromIterable(lignes)
            .map(l -> LigneImputation.builder()
                .id(UUID.randomUUID()).ecritureId(ecritureId)
                .centreId(l.getCentreId()).uniteOeuvreId(l.getUniteOeuvreId())
                .montant(l.getMontant()).quantiteUo(l.getQuantiteUo())
                .sens(l.getSens() != null ? l.getSens() : "DEBIT")
                .libelle(l.getLibelle()).cleRepartitionId(l.getCleRepartitionId())
                .build())
            .flatMap(ligneRepo::save)
            .then();
    }

    public Mono<EcritureAnalytiqueDto> enrichForImport(EcritureAnalytique e) {
        return enrichDto(e);
    }

    private Mono<EcritureAnalytiqueDto> enrichDto(EcritureAnalytique e) {
        return ligneRepo.findByEcritureId(e.getId()).collectList()
            .flatMap(lignes -> {
                Mono<List<LigneImputationDto>> lignesDtoMono = Flux.fromIterable(lignes)
                    .flatMap(l -> {
                        Mono<String> centreLibelle = l.getCentreId() != null
                            ? axeRepo.findById(l.getCentreId()).map(a -> a.getCode() + " - " + a.getLibelle()).defaultIfEmpty("")
                            : Mono.just("");
                        return centreLibelle.map(cl -> LigneImputationDto.builder()
                            .id(l.getId()).ecritureId(l.getEcritureId())
                            .centreId(l.getCentreId()).centreLibelle(cl)
                            .uniteOeuvreId(l.getUniteOeuvreId()).montant(l.getMontant())
                            .quantiteUo(l.getQuantiteUo()).sens(l.getSens())
                            .libelle(l.getLibelle()).build());
                    }).collectList();

                return lignesDtoMono.map(ldtos -> EcritureAnalytiqueDto.builder()
                    .id(e.getId()).clientId(e.getClientId())
                    .journalId(e.getJournalId()).periodeId(e.getPeriodeId())
                    .numeroPiece(e.getNumeroPiece()).libelle(e.getLibelle())
                    .dateEffet(e.getDateEffet()).origine(e.getOrigine()).statut(e.getStatut())
                    .ecriturecgRef(e.getEcriturecgRef()).montantTotal(e.getMontantTotal())
                    .natureChargeId(e.getNatureChargeId()).validatedAt(e.getValidatedAt())
                    .validatedBy(e.getValidatedBy()).rejectReason(e.getRejectReason())
                    .updatedAt(e.getUpdatedAt())
                    .lignes(ldtos).build());
            });
    }
}
