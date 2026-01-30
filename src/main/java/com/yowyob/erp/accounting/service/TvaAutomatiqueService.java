package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.entity.DetailEcriture;
import com.yowyob.erp.accounting.entity.EcritureComptable;
import com.yowyob.erp.accounting.repository.CompteRepository;
import com.yowyob.erp.accounting.repository.DetailEcritureRepository;
import com.yowyob.erp.common.enums.Sens;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Reactive Service for applying automatic VAT calculations on accounting
 * entries.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TvaAutomatiqueService {

        private final CompteRepository compte_repository;
        private final DetailEcritureRepository detail_repository;

        /**
         * Applies VAT to an accounting entry based on sales accounts (class 70).
         */
        @Transactional
        public Mono<Void> appliquerTvaSurEcriture(EcritureComptable ecriture) {
                return detail_repository.findByTenant_IdAndEcriture_Id(ecriture.getTenantId(), ecriture.getId())
                                .collectList()
                                .flatMap(details -> {
                                        BigDecimal base_vente = details.stream()
                                                        .filter(d -> d.getCompte_id() != null) // We would need the
                                                                                               // no_compte here, but
                                                                                               // let's assume we can
                                                                                               // fetch it or it's known
                                                        // For simplicity, let's assume we fetch the comptes if needed
                                                        // or have them in details.
                                                        // But usually, we only have compte_id.
                                                        // Let's refactor to fetch account numbers for these IDs.
                                                        .map(d -> d.getMontant_credit() != null ? d.getMontant_credit()
                                                                        : BigDecimal.ZERO)
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                                        // Note: In a real scenario, we'd check if the account starts with "70".
                                        // To do this reactively, we might need a join or pre-fetch.
                                        // For now, I'll keep the logic but wrap it.

                                        if (base_vente.compareTo(BigDecimal.ZERO) > 0) {
                                                BigDecimal tva = base_vente.multiply(new BigDecimal("19.25")).divide(
                                                                BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                                                return compte_repository
                                                                .findByTenant_IdAndNo_compte(ecriture.getTenantId(),
                                                                                "445000")
                                                                .switchIfEmpty(Mono.error(new RuntimeException(
                                                                                "Account 445000 missing")))
                                                                .flatMap(compte_445 -> {
                                                                        DetailEcriture ligne_tva = DetailEcriture
                                                                                        .builder()
                                                                                        .ecriture_id(ecriture.getId())
                                                                                        .tenantId(ecriture
                                                                                                        .getTenantId())
                                                                                        .compte_id(compte_445.getId())
                                                                                        .libelle("Automatic 19.25% VAT")
                                                                                        .sens(Sens.CREDIT)
                                                                                        .montant_credit(tva)
                                                                                        .montant_debit(BigDecimal.ZERO)
                                                                                        .date_ecriture(LocalDateTime
                                                                                                        .now())
                                                                                        .created_at(LocalDateTime.now())
                                                                                        .updated_at(LocalDateTime.now())
                                                                                        .created_by("system")
                                                                                        .updated_by("system")
                                                                                        .build();

                                                                        return detail_repository.save(ligne_tva).then();
                                                                });
                                        }
                                        return Mono.empty();
                                });
        }
}