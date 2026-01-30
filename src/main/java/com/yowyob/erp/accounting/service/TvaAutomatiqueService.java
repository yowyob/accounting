package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.entity.DetailEcriture;
import com.yowyob.erp.accounting.entity.EcritureComptable;
import com.yowyob.erp.accounting.entity.Taxe;
import com.yowyob.erp.accounting.repository.CompteRepository;
import com.yowyob.erp.accounting.repository.DetailEcritureRepository;
import com.yowyob.erp.accounting.repository.TaxeRepository;
import com.yowyob.erp.common.enums.Sens;
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
import java.util.Map;

/**
 * Reactive Service for applying automatic tax calculations (VAT and
 * Withholding)
 * on accounting entries based on configured taxes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TvaAutomatiqueService {

        private final CompteRepository compte_repository;
        private final DetailEcritureRepository detail_repository;
        private final TaxeRepository taxe_repository;

        /**
         * Applies taxes to an accounting entry based on active tax configurations.
         * Handles both TVA (VAT) and RETENUE (Withholding Tax).
         */
        @Transactional
        public Mono<Void> appliquerTvaSurEcriture(EcritureComptable ecriture) {
                return Mono.zip(
                                detail_repository
                                                .findByTenant_IdAndEcriture_Id(ecriture.getTenantId(), ecriture.getId())
                                                .collectList(),
                                taxe_repository.findByTenant_IdAndActifTrue(ecriture.getTenantId()).collectList(),
                                compte_repository.findAllByTenant_Id(ecriture.getTenantId())
                                                .collectMap(compte -> compte.getId(), compte -> compte.getNo_compte()))
                                .flatMap(tuple -> {
                                        List<DetailEcriture> details = tuple.getT1();
                                        List<Taxe> active_taxes = tuple.getT2();
                                        Map<java.util.UUID, String> id_to_no = tuple.getT3();

                                        if (active_taxes.isEmpty() || details.isEmpty()) {
                                                return Mono.empty();
                                        }

                                        return Flux.fromIterable(active_taxes)
                                                        .flatMap(taxe -> {
                                                                // Calculate base for this specific tax (Class 6 or 7)
                                                                BigDecimal base_facture = details.stream()
                                                                                .filter(d -> d.getCompte_id() != null)
                                                                                .map(d -> {
                                                                                        String no = id_to_no.get(d
                                                                                                        .getCompte_id());
                                                                                        if (no != null && (no
                                                                                                        .startsWith("7")
                                                                                                        || no.startsWith(
                                                                                                                        "6"))) {
                                                                                                return d.getMontant_debit()
                                                                                                                .add(d.getMontant_credit());
                                                                                        }
                                                                                        return BigDecimal.ZERO;
                                                                                })
                                                                                .reduce(BigDecimal.ZERO,
                                                                                                BigDecimal::add);

                                                                if (base_facture.compareTo(BigDecimal.ZERO) <= 0) {
                                                                        return Mono.empty();
                                                                }

                                                                BigDecimal tax_amount = base_facture
                                                                                .multiply(taxe.getTaux())
                                                                                .divide(BigDecimal.valueOf(100), 2,
                                                                                                RoundingMode.HALF_UP);

                                                                // Determine if it's a sale or purchase
                                                                boolean is_sale = details.stream().anyMatch(d -> {
                                                                        String no = id_to_no.get(d.getCompte_id());
                                                                        return no != null && no.startsWith("7");
                                                                });

                                                                String target_compte_no;
                                                                Sens target_sens;

                                                                // Logic for TVA vs RETENUE
                                                                if ("RETENUE".equalsIgnoreCase(taxe.getType_taxe())) {
                                                                        // RETENUE usually reduces the amount payable to
                                                                        // the tier
                                                                        // Purchase (6) -> Retenue is on CREDIT (reduces
                                                                        // debt)
                                                                        // Sale (7) -> Retenue is on DEBIT (reduces
                                                                        // income received)
                                                                        target_compte_no = is_sale
                                                                                        ? taxe.getCompte_deductible()
                                                                                        : taxe.getCompte_collecte();
                                                                        target_sens = is_sale ? Sens.DEBIT
                                                                                        : Sens.CREDIT;
                                                                } else {
                                                                        // Standard TVA
                                                                        target_compte_no = is_sale
                                                                                        ? taxe.getCompte_collecte()
                                                                                        : taxe.getCompte_deductible();
                                                                        target_sens = is_sale ? Sens.CREDIT
                                                                                        : Sens.DEBIT;
                                                                }

                                                                if (target_compte_no == null) {
                                                                        log.warn("Tax {} ({}) is missing its target account",
                                                                                        taxe.getCode(),
                                                                                        taxe.getType_taxe());
                                                                        return Mono.empty();
                                                                }

                                                                return compte_repository
                                                                                .findByTenant_IdAndNo_compte(
                                                                                                ecriture.getTenantId(),
                                                                                                target_compte_no)
                                                                                .flatMap(compte -> {
                                                                                        DetailEcriture ligne_taxe = DetailEcriture
                                                                                                        .builder()
                                                                                                        .ecriture_id(ecriture
                                                                                                                        .getId())
                                                                                                        .tenantId(ecriture
                                                                                                                        .getTenantId())
                                                                                                        .compte_id(compte
                                                                                                                        .getId())
                                                                                                        .libelle("Auto Tax "
                                                                                                                        + taxe.getCode()
                                                                                                                        + " ("
                                                                                                                        + taxe.getTaux()
                                                                                                                        + "%)")
                                                                                                        .sens(target_sens)
                                                                                                        .montant_debit(target_sens == Sens.DEBIT
                                                                                                                        ? tax_amount
                                                                                                                        : BigDecimal.ZERO)
                                                                                                        .montant_credit(target_sens == Sens.CREDIT
                                                                                                                        ? tax_amount
                                                                                                                        : BigDecimal.ZERO)
                                                                                                        .date_ecriture(LocalDateTime
                                                                                                                        .now())
                                                                                                        .created_at(LocalDateTime
                                                                                                                        .now())
                                                                                                        .updated_at(LocalDateTime
                                                                                                                        .now())
                                                                                                        .created_by("system")
                                                                                                        .updated_by("system")
                                                                                                        .build();

                                                                                        return detail_repository.save(
                                                                                                        ligne_taxe);
                                                                                });
                                                        })
                                                        .then();
                                });
        }
}