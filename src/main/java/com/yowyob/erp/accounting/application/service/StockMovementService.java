package com.yowyob.erp.accounting.application.service;
import com.yowyob.erp.accounting.domain.port.in.PeriodeComptableUseCase;

import com.yowyob.erp.accounting.domain.model.*;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.EcritureComptableRepository;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.DetailEcritureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Reactive Service to handle accounting stock movements.
 * Converts stock movements into accounting entries dynamic configuration.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StockMovementService {

    private final EcritureComptableRepository ecritureRepository;
    private final DetailEcritureRepository detailRepository;
    private final PeriodeComptableUseCase periodeComptableService;

    /**
     * Records a stock movement and generates the corresponding accounting entry.
     * 
     * @param mouvement The stock movement data
     * @param organization    The context organization
     */
    @Transactional
    public Mono<Void> recordStockMovement(MouvementStockComptable mouvement, Organization organization) {
        log.info("Processing reactive stock movement for Organization: {}", organization.getCode());

        // 1. Validate movement data
        if (mouvement.getQuantite() <= 0 || mouvement.getCout_unitaire().compareTo(BigDecimal.ZERO) <= 0) {
            return Mono.error(new IllegalArgumentException("Quantity and Unit Cost must be positive"));
        }

        // 2. Resolve Accounting Period and Create Entry
        return periodeComptableService.getByDate(mouvement.get_date())
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                        "No accounting period found for date: " + mouvement.get_date())))
                .flatMap(periodeDto -> {
                    EcritureComptable ecriture = EcritureComptable.builder()
                            .organizationId(organization.getId())
                            .libelle(mouvement.get_description())
                            .date_ecriture(mouvement.get_date())
                            .journal_id(mouvement.get_journal_comptable_id())
                            .periode_id(periodeDto.getId())
                            .validee(true)
                            .numero_ecriture("MVT-" + UUID.randomUUID().toString().substring(0, 8))
                            .attachment_ids(mouvement.get_attachment_ids())
                            .build();

                    return ecritureRepository.save(ecriture)
                            .flatMap(savedEcriture -> {
                                List<DetailEcriture> details = mouvement.generate_ecriture_details(organization,
                                        savedEcriture);
                                // Set back links if needed for persistence
                                details.forEach(d -> {
                                    d.setEcriture_id(savedEcriture.getId());
                                    d.setOrganizationId(organization.getId());
                                });
                                return detailRepository.saveAll(details).collectList()
                                        .doOnSuccess(savedDetails -> log.info(
                                                "✅ Accounting entry {} created for stock movement with {} details",
                                                savedEcriture.getNumero_ecriture(), savedDetails.size()))
                                        .then();
                            });
                });
    }

    /**
     * Crée un mouvement de stock (Reactive).
     */
    @Transactional
    public Mono<java.util.Map<String, Object>> creerMouvementStock(java.util.Map<String, Object> mouvement,
            String user) {
        return Mono.just(java.util.Map.of(
                "mouvement_id", UUID.randomUUID(),
                "message", "Mouvement de stock créé - implémentation partielle réactive"));
    }

    /**
     * Récupère les mouvements de stock (Reactive).
     */
    public Flux<java.util.Map<String, Object>> getMouvements(UUID organization_id, String type, String produit_id) {
        return Flux.empty();
    }

    /**
     * Récupère l'impact comptable d'un mouvement (Reactive).
     */
    public Mono<java.util.Map<String, Object>> getImpactComptable(UUID mouvement_id) {
        return Mono.just(java.util.Map.of(
                "mouvement_id", mouvement_id,
                "ecritures", List.of(),
                "message", "Impact comptable - implémentation partielle réactive"));
    }
}
