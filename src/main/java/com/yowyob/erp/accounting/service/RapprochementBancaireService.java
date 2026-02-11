package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.entity.DetailEcriture;
import com.yowyob.erp.accounting.entity.ReleveBancaire;
import com.yowyob.erp.accounting.repository.DetailEcritureRepository;
import com.yowyob.erp.accounting.repository.ReleveBancaireRepository;
import com.yowyob.erp.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for Bank Reconciliation (Rapprochement Bancaire).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RapprochementBancaireService {

    private final ReleveBancaireRepository releve_repository;
    private final DetailEcritureRepository detail_repository;

    /**
     * Imports a list of bank statement lines.
     */
    @Transactional
    public Mono<Void> importerReleve(List<ReleveBancaire> lignes) {
        return Flux.fromIterable(lignes)
                .flatMap(releve_repository::save)
                .then();
    }

    /**
     * Proposes potential matches for unreconciled bank lines.
     * Uses fuzzy matching on libelle and search window for date.
     */
    public Flux<DetailEcriture> proposerRapprochement(UUID releveId) {
        return releve_repository.findById(releveId)
                .flatMapMany(releve -> {
                    if (releve.isRapproche()) {
                        return Flux.empty();
                    }

                    LocalDateTime start = releve.getDateOperation().minusDays(15);
                    LocalDateTime end = releve.getDateOperation().plusDays(15);

                    return detail_repository.findCandidatesForPointage(
                            releve.getOrganizationId(),
                            releve.getMontant().abs(),
                            start,
                            end,
                            releve.getLibelle(),
                            releve.getDateOperation());
                });
    }

    /**
     * Formally reconciles a bank line with a ledger entry detail.
     */
    @Transactional
    public Mono<Void> validerRapprochement(UUID releveId, UUID detailId) {
        return Mono.zip(
                releve_repository.findById(releveId),
                detail_repository.findById(detailId)).flatMap(tuple -> {
                    ReleveBancaire releve = tuple.getT1();
                    DetailEcriture detail = tuple.getT2();

                    if (releve.isRapproche() || Boolean.TRUE.equals(detail.getPointee())) {
                        return Mono.error(new BusinessException("L'une des entrées est déjà rapprochée."));
                    }

                    // Sync status
                    releve.setRapproche(true);
                    releve.setDateRapprochement(LocalDateTime.now());
                    releve.setDetailEcritureId(detailId);
                    releve.setNotNew();

                    detail.setPointee(true);
                    detail.setReference_bancaire(releve.getReference());
                    detail.setNotNew();

                    return Mono.when(
                            releve_repository.save(releve),
                            detail_repository.save(detail));
                }).then();
    }
}
