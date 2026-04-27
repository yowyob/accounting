package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.dto.JournalAuditDto;
import com.yowyob.erp.accounting.entity.Compte;
import com.yowyob.erp.accounting.entity.DetailEcriture;
import com.yowyob.erp.accounting.entity.JournalAudit;
import com.yowyob.erp.accounting.entity.Organization;
import com.yowyob.erp.accounting.repository.CompteRepository;
import com.yowyob.erp.accounting.repository.DetailEcritureRepository;
import com.yowyob.erp.accounting.repository.JournalAuditRepository;
import com.yowyob.erp.config.kafka.KafkaMessageService;
import com.yowyob.erp.config.redis.RedisService;
import com.yowyob.erp.config.organization.ReactiveOrganizationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Reactive Service for generating accounting reports (balance sheet, income
 * statement).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RapportService {

        private final CompteRepository compte_repository;
        private final DetailEcritureRepository detail_repository;
        private final RedisService redis_service;
        private final KafkaMessageService kafka_service;
        private final JournalAuditRepository audit_repository;

        private static final String CACHE_BILAN = "rapport:bilan:";
        private static final String CACHE_RESULTAT = "rapport:resultat:";

        /**
         * Generates a balance sheet (assets/liabilities) for a given organization.
         */
        public Mono<com.yowyob.erp.accounting.dto.report.BilanDto> generateBilan(UUID organization_id,
                        String date_debut, String date_fin) {
                LocalDate start = LocalDate.parse(date_debut);
                LocalDate end = LocalDate.parse(date_fin);
                String cache_key = CACHE_BILAN + organization_id + ":" + start + ":" + end;

                return redis_service.get(cache_key, com.yowyob.erp.accounting.dto.report.BilanDto.class)
                                .switchIfEmpty(Mono.defer(() -> {
                                        log.info("📊 Generating balance sheet from {} to {} for organization {}", start,
                                                        end, organization_id);

                                        return detail_repository
                                                        .findByOrganization_IdAndDateRange(organization_id,
                                                                        start.atStartOfDay(),
                                                                        end.plusDays(1).atStartOfDay())
                                                        .collectList()
                                                        .flatMap(allDetails -> compte_repository
                                                                        .findAllByOrganization_Id(organization_id)
                                                                        .flatMap(compte -> {
                                                                                BigDecimal solde = calculateAccountBalance(
                                                                                                allDetails, compte);
                                                                                compte.setSolde(solde);
                                                                                return compte_repository.save(compte)
                                                                                                .thenReturn(compte);
                                                                        })
                                                                        .collectList()
                                                                        .flatMap(comptes -> {
                                                                                List<com.yowyob.erp.accounting.dto.report.ReportItemDto> actifs = comptes
                                                                                                .stream()
                                                                                                .filter(c -> c.getClasse() != null
                                                                                                                && (c.getClasse() == 1
                                                                                                                                || c.getClasse() == 2
                                                                                                                                || c.getClasse() == 5))
                                                                                                .map(c -> new com.yowyob.erp.accounting.dto.report.ReportItemDto(
                                                                                                                c.getNo_compte(),
                                                                                                                c.getLibelle(),
                                                                                                                calculateDebit(allDetails,
                                                                                                                                c),
                                                                                                                calculateCredit(allDetails,
                                                                                                                                c),
                                                                                                                c.getSolde()))
                                                                                                .collect(java.util.stream.Collectors
                                                                                                                .toList());

                                                                                List<com.yowyob.erp.accounting.dto.report.ReportItemDto> passifs = comptes
                                                                                                .stream()
                                                                                                .filter(c -> c.getClasse() != null
                                                                                                                && (c.getClasse() == 3
                                                                                                                                || c.getClasse() == 4))
                                                                                                .map(c -> new com.yowyob.erp.accounting.dto.report.ReportItemDto(
                                                                                                                c.getNo_compte(),
                                                                                                                c.getLibelle(),
                                                                                                                calculateDebit(allDetails,
                                                                                                                                c),
                                                                                                                calculateCredit(allDetails,
                                                                                                                                c),
                                                                                                                c.getSolde()))
                                                                                                .collect(java.util.stream.Collectors
                                                                                                                .toList());

                                                                                // For the sake of simplification and
                                                                                // fitting into frontend mock structure
                                                                                // We map class 1 (capital) specifically
                                                                                // to Capitaux Propres if preferred,
                                                                                // but in original code class 1 was
                                                                                // mixed into actifs? Wait, class 1 is
                                                                                // passif/capitaux propres in SYSCOHADA.
                                                                                // Assuming class 1 = Capitaux propres,
                                                                                // class 2 = Actif immo, class 3 = Stock
                                                                                // (Actif), class 4 = Tiers
                                                                                // (Passif/Actif), class 5 = Trésorerie
                                                                                // (Actif/Passif).
                                                                                // Let's create a better separation
                                                                                // based on standard OHADA.
                                                                                List<com.yowyob.erp.accounting.dto.report.ReportItemDto> capitauxPropresList = comptes
                                                                                                .stream()
                                                                                                .filter(c -> c.getClasse() != null
                                                                                                                && c.getClasse() == 1)
                                                                                                .map(c -> new com.yowyob.erp.accounting.dto.report.ReportItemDto(
                                                                                                                c.getNo_compte(),
                                                                                                                c.getLibelle(),
                                                                                                                calculateDebit(allDetails,
                                                                                                                                c),
                                                                                                                calculateCredit(allDetails,
                                                                                                                                c),
                                                                                                                c.getSolde()))
                                                                                                .collect(java.util.stream.Collectors
                                                                                                                .toList());

                                                                                // Re-adjust actifs/passifs
                                                                                List<com.yowyob.erp.accounting.dto.report.ReportItemDto> filteredActifs = comptes
                                                                                                .stream()
                                                                                                .filter(c -> c.getClasse() != null
                                                                                                                && (c.getClasse() == 2
                                                                                                                                || c.getClasse() == 3
                                                                                                                                || c.getClasse() == 5)) // Simplified
                                                                                                                                                        // Actif
                                                                                                .map(c -> new com.yowyob.erp.accounting.dto.report.ReportItemDto(
                                                                                                                c.getNo_compte(),
                                                                                                                c.getLibelle(),
                                                                                                                calculateDebit(allDetails,
                                                                                                                                c),
                                                                                                                calculateCredit(allDetails,
                                                                                                                                c),
                                                                                                                c.getSolde()))
                                                                                                .collect(java.util.stream.Collectors
                                                                                                                .toList());

                                                                                List<com.yowyob.erp.accounting.dto.report.ReportItemDto> filteredPassifs = comptes
                                                                                                .stream()
                                                                                                .filter(c -> c.getClasse() != null
                                                                                                                && c.getClasse() == 4) // Simplified
                                                                                                                                       // Passif
                                                                                                .map(c -> new com.yowyob.erp.accounting.dto.report.ReportItemDto(
                                                                                                                c.getNo_compte(),
                                                                                                                c.getLibelle(),
                                                                                                                calculateDebit(allDetails,
                                                                                                                                c),
                                                                                                                calculateCredit(allDetails,
                                                                                                                                c),
                                                                                                                c.getSolde()))
                                                                                                .collect(java.util.stream.Collectors
                                                                                                                .toList());

                                                                                com.yowyob.erp.accounting.dto.report.BilanDto bilan = com.yowyob.erp.accounting.dto.report.BilanDto
                                                                                                .builder()
                                                                                                .actifs(filteredActifs)
                                                                                                .passifs(filteredPassifs)
                                                                                                .capitauxPropres(
                                                                                                                capitauxPropresList)
                                                                                                .build();

                                                                                return redis_service.save(cache_key,
                                                                                                bilan,
                                                                                                Duration.ofMinutes(30))
                                                                                                .then(ReactiveOrganizationContext
                                                                                                                .getCurrentOrganizationAsOrganization()
                                                                                                                .flatMap(organization -> ReactiveOrganizationContext
                                                                                                                                .getCurrentUser()
                                                                                                                                .defaultIfEmpty("system")
                                                                                                                                .flatMap(user -> logAudit(
                                                                                                                                                organization,
                                                                                                                                                user,
                                                                                                                                                "BILAN_GENERATED",
                                                                                                                                                "Balance sheet generated from "
                                                                                                                                                                + date_debut
                                                                                                                                                                + " to "
                                                                                                                                                                + date_fin))))
                                                                                                .thenReturn(bilan);
                                                                        }));
                                }));
        }

        /**
         * Generates an income statement (expenses/revenues) for a given organization.
         */
        public Mono<com.yowyob.erp.accounting.dto.report.CompteResultatDto> generateCompteResultat(UUID organization_id,
                        String date_debut,
                        String date_fin) {
                LocalDate start = LocalDate.parse(date_debut);
                LocalDate end = LocalDate.parse(date_fin);
                String cache_key = CACHE_RESULTAT + organization_id + ":" + start + ":" + end;

                return redis_service.get(cache_key, com.yowyob.erp.accounting.dto.report.CompteResultatDto.class)
                                .switchIfEmpty(Mono.defer(() -> {
                                        log.info("📘 Generating income statement from {} to {} for organization {}",
                                                        start,
                                                        end, organization_id);

                                        return detail_repository
                                                        .findByOrganization_IdAndDateRange(organization_id,
                                                                        start.atStartOfDay(),
                                                                        end.plusDays(1).atStartOfDay())
                                                        .collectList()
                                                        .flatMap(allDetails -> compte_repository
                                                                        .findAllByOrganization_Id(organization_id)
                                                                        .flatMap(compte -> {
                                                                                BigDecimal solde = calculateAccountBalance(
                                                                                                allDetails, compte);
                                                                                compte.setSolde(solde);
                                                                                return compte_repository.save(compte)
                                                                                                .thenReturn(compte);
                                                                        })
                                                                        .collectList()
                                                                        .flatMap(comptes -> {
                                                                                List<com.yowyob.erp.accounting.dto.report.ReportItemDto> chargesList = comptes
                                                                                                .stream()
                                                                                                .filter(c -> c.getClasse() != null
                                                                                                                && c.getClasse() == 6)
                                                                                                .map(c -> new com.yowyob.erp.accounting.dto.report.ReportItemDto(
                                                                                                                c.getNo_compte(),
                                                                                                                c.getLibelle(),
                                                                                                                calculateDebit(allDetails,
                                                                                                                                c),
                                                                                                                calculateCredit(allDetails,
                                                                                                                                c),
                                                                                                                c.getSolde().abs()
                                                                                                                                .negate()))
                                                                                                .collect(java.util.stream.Collectors
                                                                                                                .toList());

                                                                                List<com.yowyob.erp.accounting.dto.report.ReportItemDto> produitsList = comptes
                                                                                                .stream()
                                                                                                .filter(c -> c.getClasse() != null
                                                                                                                && c.getClasse() == 7)
                                                                                                .map(c -> new com.yowyob.erp.accounting.dto.report.ReportItemDto(
                                                                                                                c.getNo_compte(),
                                                                                                                c.getLibelle(),
                                                                                                                calculateDebit(allDetails,
                                                                                                                                c),
                                                                                                                calculateCredit(allDetails,
                                                                                                                                c),
                                                                                                                c.getSolde().abs()))
                                                                                                .collect(java.util.stream.Collectors
                                                                                                                .toList());

                                                                                com.yowyob.erp.accounting.dto.report.CompteResultatDto compteResultat = com.yowyob.erp.accounting.dto.report.CompteResultatDto
                                                                                                .builder()
                                                                                                .charges(chargesList)
                                                                                                .produits(produitsList)
                                                                                                .build();

                                                                                return redis_service.save(cache_key,
                                                                                                compteResultat,
                                                                                                Duration.ofMinutes(30))
                                                                                                .then(ReactiveOrganizationContext
                                                                                                                .getCurrentOrganizationAsOrganization()
                                                                                                                .flatMap(organization -> ReactiveOrganizationContext
                                                                                                                                .getCurrentUser()
                                                                                                                                .defaultIfEmpty("system")
                                                                                                                                .flatMap(user -> logAudit(
                                                                                                                                                organization,
                                                                                                                                                user,
                                                                                                                                                "COMPTE_RESULTAT_GENERATED",
                                                                                                                                                "Income statement generated from "
                                                                                                                                                                + date_debut
                                                                                                                                                                + " to "
                                                                                                                                                                + date_fin))))
                                                                                                .thenReturn(compteResultat);
                                                                        }));
                                }));
        }

        /**
         * Tableau des Flux de Trésorerie OHADA (TFT) — implémentation réelle.
         *
         * Section A — Flux liés aux activités opérationnelles
         *   A1 : Résultat net de l'exercice (produits cl.7 – charges cl.6)
         *   A2 : Dotations aux amortissements/provisions (681/685/691)
         *   A3 : Variation des créances clients (41x : ↑créance = utilisation)
         *   A4 : Variation des dettes fournisseurs (40x : ↑dette = ressource)
         *   A5 : Variation des stocks (3x : ↑stock = utilisation)
         *
         * Section B — Flux liés aux activités d'investissement
         *   B1 : Acquisitions d'immobilisations (débit cl.2)
         *   B2 : Cessions d'immobilisations (crédit cl.2 ou 48x)
         *
         * Section C — Flux liés aux activités de financement
         *   C1 : Augmentation de capital (crédit 10x/11x)
         *   C2 : Emprunts contractés (crédit 16x)
         *   C3 : Remboursements d'emprunts (débit 16x)
         *   C4 : Distribution de dividendes (débit 131 ou crédit 465)
         */
        public Mono<com.yowyob.erp.accounting.dto.report.CashFlowDto> generateCashFlow(UUID organization_id,
                        String date_debut, String date_fin) {
                LocalDate start = LocalDate.parse(date_debut);
                LocalDate end = LocalDate.parse(date_fin);
                log.info("📈 Generating real TFT for organization {} from {} to {}", organization_id, start, end);

                return detail_repository
                                .findByOrganization_IdAndDateRange(organization_id, start.atStartOfDay(),
                                                end.plusDays(1).atStartOfDay())
                                .collectList()
                                .flatMap(details -> compte_repository.findAllByOrganization_Id(organization_id)
                                                .collectList()
                                                .map(comptes -> {
                                                        Map<UUID, Compte> compteMap = new HashMap<>();
                                                        for (Compte c : comptes) compteMap.put(c.getId(), c);

                                                        // ─── Résultat net ───
                                                        BigDecimal totalProduits = BigDecimal.ZERO;
                                                        BigDecimal totalCharges = BigDecimal.ZERO;
                                                        BigDecimal dotAmort = BigDecimal.ZERO;
                                                        BigDecimal varCreances = BigDecimal.ZERO;
                                                        BigDecimal varDettes = BigDecimal.ZERO;
                                                        BigDecimal varStocks = BigDecimal.ZERO;
                                                        BigDecimal acquisImmo = BigDecimal.ZERO;
                                                        BigDecimal cessionsImmo = BigDecimal.ZERO;
                                                        BigDecimal augmCapital = BigDecimal.ZERO;
                                                        BigDecimal empruntsContractes = BigDecimal.ZERO;
                                                        BigDecimal rembEmprunt = BigDecimal.ZERO;
                                                        BigDecimal dividendes = BigDecimal.ZERO;

                                                        for (var d : details) {
                                                                Compte c = compteMap.get(d.getCompte_id());
                                                                if (c == null || c.getNo_compte() == null) continue;
                                                                String no = c.getNo_compte();
                                                                BigDecimal debit = d.getMontant_debit() != null ? d.getMontant_debit() : BigDecimal.ZERO;
                                                                BigDecimal credit = d.getMontant_credit() != null ? d.getMontant_credit() : BigDecimal.ZERO;

                                                                if (no.startsWith("7")) totalProduits = totalProduits.add(credit.subtract(debit));
                                                                else if (no.startsWith("6")) {
                                                                        totalCharges = totalCharges.add(debit.subtract(credit));
                                                                        if (no.startsWith("681") || no.startsWith("685") || no.startsWith("691"))
                                                                                dotAmort = dotAmort.add(debit.subtract(credit));
                                                                }
                                                                else if (no.startsWith("41")) varCreances = varCreances.add(debit.subtract(credit));
                                                                else if (no.startsWith("40")) varDettes = varDettes.add(credit.subtract(debit));
                                                                else if (no.startsWith("3")) varStocks = varStocks.add(debit.subtract(credit));
                                                                else if (no.startsWith("2") && !no.startsWith("28")) {
                                                                        acquisImmo = acquisImmo.add(debit);
                                                                        cessionsImmo = cessionsImmo.add(credit);
                                                                }
                                                                else if (no.startsWith("10") || no.startsWith("11"))
                                                                        augmCapital = augmCapital.add(credit.subtract(debit));
                                                                else if (no.startsWith("16")) {
                                                                        empruntsContractes = empruntsContractes.add(credit);
                                                                        rembEmprunt = rembEmprunt.add(debit);
                                                                }
                                                                else if (no.startsWith("131") || no.startsWith("465"))
                                                                        dividendes = dividendes.add(debit);
                                                        }

                                                        BigDecimal resultatNet = totalProduits.subtract(totalCharges);

                                                        // ─── Section A ───
                                                        List<com.yowyob.erp.accounting.dto.report.CashFlowDto.CashFlowItemDto> operationnel = java.util.Arrays.asList(
                                                                new com.yowyob.erp.accounting.dto.report.CashFlowDto.CashFlowItemDto("A1",
                                                                        "Résultat net de l'exercice", resultatNet, "operationnel"),
                                                                new com.yowyob.erp.accounting.dto.report.CashFlowDto.CashFlowItemDto("A2",
                                                                        "Dotations aux amortissements et provisions", dotAmort, "operationnel"),
                                                                new com.yowyob.erp.accounting.dto.report.CashFlowDto.CashFlowItemDto("A3",
                                                                        "Variation des créances clients (41x)", varCreances.negate(), "operationnel"),
                                                                new com.yowyob.erp.accounting.dto.report.CashFlowDto.CashFlowItemDto("A4",
                                                                        "Variation des dettes fournisseurs (40x)", varDettes, "operationnel"),
                                                                new com.yowyob.erp.accounting.dto.report.CashFlowDto.CashFlowItemDto("A5",
                                                                        "Variation des stocks (3x)", varStocks.negate(), "operationnel")
                                                        );

                                                        // ─── Section B ───
                                                        List<com.yowyob.erp.accounting.dto.report.CashFlowDto.CashFlowItemDto> investissement = java.util.Arrays.asList(
                                                                new com.yowyob.erp.accounting.dto.report.CashFlowDto.CashFlowItemDto("B1",
                                                                        "Acquisitions d'immobilisations (cl.2)", acquisImmo.negate(), "investissement"),
                                                                new com.yowyob.erp.accounting.dto.report.CashFlowDto.CashFlowItemDto("B2",
                                                                        "Cessions d'immobilisations", cessionsImmo, "investissement")
                                                        );

                                                        // ─── Section C ───
                                                        List<com.yowyob.erp.accounting.dto.report.CashFlowDto.CashFlowItemDto> financement = java.util.Arrays.asList(
                                                                new com.yowyob.erp.accounting.dto.report.CashFlowDto.CashFlowItemDto("C1",
                                                                        "Augmentation de capital (10x/11x)", augmCapital, "financement"),
                                                                new com.yowyob.erp.accounting.dto.report.CashFlowDto.CashFlowItemDto("C2",
                                                                        "Emprunts contractés (16x)", empruntsContractes, "financement"),
                                                                new com.yowyob.erp.accounting.dto.report.CashFlowDto.CashFlowItemDto("C3",
                                                                        "Remboursements d'emprunts (16x)", rembEmprunt.negate(), "financement"),
                                                                new com.yowyob.erp.accounting.dto.report.CashFlowDto.CashFlowItemDto("C4",
                                                                        "Dividendes distribués", dividendes.negate(), "financement")
                                                        );

                                                        return com.yowyob.erp.accounting.dto.report.CashFlowDto.builder()
                                                                        .operationnel(operationnel)
                                                                        .investissement(investissement)
                                                                        .financement(financement)
                                                                        .build();
                                                }));
        }

        /**
         * Balance âgée OHADA : créances clients (41x) et dettes fournisseurs (40x)
         * classées par ancienneté — 0-30j, 31-60j, 61-90j, >90j.
         */
        public Mono<com.yowyob.erp.accounting.dto.report.BalanceAgeeDto> generateBalanceAgee(
                        UUID organization_id, String date_reference_str) {
                LocalDate dateRef = date_reference_str != null
                        ? LocalDate.parse(date_reference_str) : LocalDate.now();

                return detail_repository
                        .findByOrganization_IdAndDateRange(organization_id,
                                LocalDate.of(2000, 1, 1).atStartOfDay(),
                                dateRef.plusDays(1).atStartOfDay())
                        .collectList()
                        .flatMap(allDetails -> compte_repository.findAllByOrganization_Id(organization_id)
                                .filter(c -> c.getNo_compte() != null &&
                                        (c.getNo_compte().startsWith("41") || c.getNo_compte().startsWith("40")))
                                .collectList()
                                .map(comptes -> {
                                        List<com.yowyob.erp.accounting.dto.report.BalanceAgeeDto.LigneBalanceAgeeDto> lignesClients = new java.util.ArrayList<>();
                                        List<com.yowyob.erp.accounting.dto.report.BalanceAgeeDto.LigneBalanceAgeeDto> lignesFournisseurs = new java.util.ArrayList<>();

                                        for (Compte c : comptes) {
                                                List<DetailEcriture> details = allDetails.stream()
                                                        .filter(d -> c.getId().equals(d.getCompte_id()))
                                                        .collect(java.util.stream.Collectors.toList());
                                                if (details.isEmpty()) continue;

                                                // Solde non lettré = dette/créance en cours
                                                BigDecimal soldeTotal = calculateDebit(details, c)
                                                        .subtract(calculateCredit(details, c));
                                                if (soldeTotal.compareTo(BigDecimal.ZERO) == 0) continue;

                                                // Répartition par ancienneté basée sur la date de la dernière écriture
                                                BigDecimal t0_30 = BigDecimal.ZERO, t31_60 = BigDecimal.ZERO,
                                                        t61_90 = BigDecimal.ZERO, sup90 = BigDecimal.ZERO;
                                                for (DetailEcriture d : details) {
                                                        if (d.getDate_ecriture() == null) continue;
                                                        long age = java.time.temporal.ChronoUnit.DAYS.between(
                                                                d.getDate_ecriture().toLocalDate(), dateRef);
                                                        BigDecimal net = (d.getMontant_debit() != null ? d.getMontant_debit() : BigDecimal.ZERO)
                                                                .subtract(d.getMontant_credit() != null ? d.getMontant_credit() : BigDecimal.ZERO);
                                                        if (age <= 30) t0_30 = t0_30.add(net);
                                                        else if (age <= 60) t31_60 = t31_60.add(net);
                                                        else if (age <= 90) t61_90 = t61_90.add(net);
                                                        else sup90 = sup90.add(net);
                                                }

                                                var ligne = com.yowyob.erp.accounting.dto.report.BalanceAgeeDto.LigneBalanceAgeeDto.builder()
                                                        .noCompte(c.getNo_compte())
                                                        .libelle(c.getLibelle())
                                                        .soldeTotal(soldeTotal)
                                                        .tranche0_30(t0_30)
                                                        .tranche31_60(t31_60)
                                                        .tranche61_90(t61_90)
                                                        .tranches90Plus(sup90)
                                                        .build();

                                                if (c.getNo_compte().startsWith("41")) lignesClients.add(ligne);
                                                else lignesFournisseurs.add(ligne);
                                        }

                                        return com.yowyob.erp.accounting.dto.report.BalanceAgeeDto.builder()
                                                .dateReference(dateRef)
                                                .clients(lignesClients)
                                                .fournisseurs(lignesFournisseurs)
                                                .build();
                                }));
        }

        public Mono<com.yowyob.erp.accounting.dto.report.ExecutiveSummaryDto> generateExecutiveSummary(
                        UUID organization_id, String date_debut, String date_fin) {
                return Mono.zip(
                                generateBilan(organization_id, date_debut, date_fin),
                                generateCompteResultat(organization_id, date_debut, date_fin),
                                generateCashFlow(organization_id, date_debut, date_fin)).map(tuple -> {
                                        com.yowyob.erp.accounting.dto.report.BilanDto bilanDto = tuple.getT1();
                                        com.yowyob.erp.accounting.dto.report.CompteResultatDto resultatDto = tuple
                                                        .getT2();
                                        com.yowyob.erp.accounting.dto.report.CashFlowDto cashFlowDto = tuple.getT3();

                                        BigDecimal totalActifs = bilanDto.getActifs().stream().map(
                                                        com.yowyob.erp.accounting.dto.report.ReportItemDto::getSolde)
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                        BigDecimal totalPassifs = bilanDto.getPassifs().stream().map(
                                                        com.yowyob.erp.accounting.dto.report.ReportItemDto::getSolde)
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                        BigDecimal totalCapitaux = bilanDto.getCapitauxPropres().stream().map(
                                                        com.yowyob.erp.accounting.dto.report.ReportItemDto::getSolde)
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                                        List<com.yowyob.erp.accounting.dto.report.ExecutiveSummaryDto.SummaryItemDto> bilanSummary = java.util.Arrays
                                                        .asList(
                                                                        new com.yowyob.erp.accounting.dto.report.ExecutiveSummaryDto.SummaryItemDto(
                                                                                        "Actifs", totalActifs,
                                                                                        "Total des actifs"),
                                                                        new com.yowyob.erp.accounting.dto.report.ExecutiveSummaryDto.SummaryItemDto(
                                                                                        "Passifs", totalPassifs,
                                                                                        "Total des passifs"),
                                                                        new com.yowyob.erp.accounting.dto.report.ExecutiveSummaryDto.SummaryItemDto(
                                                                                        "Capitaux Propres",
                                                                                        totalCapitaux,
                                                                                        "Total des capitaux propres"));

                                        BigDecimal totalProduits = resultatDto.getProduits().stream().map(
                                                        com.yowyob.erp.accounting.dto.report.ReportItemDto::getSolde)
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                        BigDecimal totalCharges = resultatDto.getCharges().stream().map(
                                                        com.yowyob.erp.accounting.dto.report.ReportItemDto::getSolde)
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                        BigDecimal resultatNet = totalProduits.subtract(totalCharges);

                                        List<com.yowyob.erp.accounting.dto.report.ExecutiveSummaryDto.SummaryItemDto> resultatSummary = java.util.Arrays
                                                        .asList(
                                                                        new com.yowyob.erp.accounting.dto.report.ExecutiveSummaryDto.SummaryItemDto(
                                                                                        "Chiffre d'Affaires",
                                                                                        totalProduits,
                                                                                        "Total des produits"),
                                                                        new com.yowyob.erp.accounting.dto.report.ExecutiveSummaryDto.SummaryItemDto(
                                                                                        "Dépenses", totalCharges,
                                                                                        "Total des charges"),
                                                                        new com.yowyob.erp.accounting.dto.report.ExecutiveSummaryDto.SummaryItemDto(
                                                                                        "Marge Nette", resultatNet,
                                                                                        "Bénéfice ou perte"));

                                        BigDecimal totalCashFlowOp = cashFlowDto.getOperationnel().stream().map(
                                                        com.yowyob.erp.accounting.dto.report.CashFlowDto.CashFlowItemDto::getAmount)
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                        BigDecimal totalCashFlowInv = cashFlowDto.getInvestissement().stream().map(
                                                        com.yowyob.erp.accounting.dto.report.CashFlowDto.CashFlowItemDto::getAmount)
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                        BigDecimal totalCashFlowFin = cashFlowDto.getFinancement().stream().map(
                                                        com.yowyob.erp.accounting.dto.report.CashFlowDto.CashFlowItemDto::getAmount)
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                        BigDecimal fluxTrésoNet = totalCashFlowOp.add(totalCashFlowInv)
                                                        .add(totalCashFlowFin);

                                        List<com.yowyob.erp.accounting.dto.report.ExecutiveSummaryDto.SummaryItemDto> cashFlowSummary = java.util.Arrays
                                                        .asList(
                                                                        new com.yowyob.erp.accounting.dto.report.ExecutiveSummaryDto.SummaryItemDto(
                                                                                        "Flux Opérationnel",
                                                                                        totalCashFlowOp,
                                                                                        "Généré par les opérations"),
                                                                        new com.yowyob.erp.accounting.dto.report.ExecutiveSummaryDto.SummaryItemDto(
                                                                                        "Flux d'Investissement",
                                                                                        totalCashFlowInv,
                                                                                        "Lié aux investissements"),
                                                                        new com.yowyob.erp.accounting.dto.report.ExecutiveSummaryDto.SummaryItemDto(
                                                                                        "Trésorerie Nette",
                                                                                        fluxTrésoNet,
                                                                                        "Evolution de la trésorerie"));

                                        return com.yowyob.erp.accounting.dto.report.ExecutiveSummaryDto.builder()
                                                        .bilan(bilanSummary)
                                                        .compteResultat(resultatSummary)
                                                        .fluxTresorerie(cashFlowSummary)
                                                        .build();
                                });
        }

        /**
         * Generates the General Ledger (Grand Livre) for a given period.
         */
        public Mono<List<com.yowyob.erp.accounting.dto.GrandLivreDto>> generateGrandLivre(UUID organization_id,
                        String date_debut,
                        String date_fin) {
                LocalDate start = LocalDate.parse(date_debut);
                LocalDate end = LocalDate.parse(date_fin);

                return compte_repository.findAllByOrganization_Id(organization_id)
                                .flatMap(compte -> {
                                        // 1. Calculate opening balance (all entries before start date)
                                        return detail_repository
                                                        .findByOrganization_IdAndDateRange(organization_id,
                                                                        LocalDate.of(2000, 1, 1).atStartOfDay(),
                                                                        start.atStartOfDay())
                                                        .filter(d -> d.getCompte_id().equals(compte.getId()))
                                                        .collectList()
                                                        .flatMap(openingDetails -> {
                                                                BigDecimal soldeOuverture = calculateAccountBalance(
                                                                                openingDetails, compte);

                                                                // 2. Get period details
                                                                return detail_repository
                                                                                .findByOrganization_IdAndDateRange(
                                                                                                organization_id,
                                                                                                start.atStartOfDay(),
                                                                                                end.plusDays(1)
                                                                                                                .atStartOfDay())
                                                                                .filter(d -> d.getCompte_id()
                                                                                                .equals(compte.getId()))
                                                                                .collectList()
                                                                                .map(periodDetails -> {
                                                                                        BigDecimal totalDebit = periodDetails
                                                                                                        .stream()
                                                                                                        .map(d -> d.getMontant_debit() != null
                                                                                                                        ? d.getMontant_debit()
                                                                                                                        : BigDecimal.ZERO)
                                                                                                        .reduce(BigDecimal.ZERO,
                                                                                                                        BigDecimal::add);

                                                                                        BigDecimal totalCredit = periodDetails
                                                                                                        .stream()
                                                                                                        .map(d -> d.getMontant_credit() != null
                                                                                                                        ? d.getMontant_credit()
                                                                                                                        : BigDecimal.ZERO)
                                                                                                        .reduce(BigDecimal.ZERO,
                                                                                                                        BigDecimal::add);

                                                                                        // Calculate closing balance
                                                                                        // properly respecting account
                                                                                        // type
                                                                                        BigDecimal waitingBalance = periodDetails
                                                                                                        .stream()
                                                                                                        .map(d -> {
                                                                                                                BigDecimal debit = d
                                                                                                                                .getMontant_debit() != null
                                                                                                                                                ? d.getMontant_debit()
                                                                                                                                                : BigDecimal.ZERO;
                                                                                                                BigDecimal credit = d
                                                                                                                                .getMontant_credit() != null
                                                                                                                                                ? d.getMontant_credit()
                                                                                                                                                : BigDecimal.ZERO;
                                                                                                                return "ACTIF".equals(
                                                                                                                                compte
                                                                                                                                                .getType_compte())
                                                                                                                                || "CHARGE".equals(
                                                                                                                                                compte.getType_compte())
                                                                                                                                                                ? debit.subtract(
                                                                                                                                                                                credit)
                                                                                                                                                                : credit.subtract(
                                                                                                                                                                                debit);
                                                                                                        })
                                                                                                        .reduce(BigDecimal.ZERO,
                                                                                                                        BigDecimal::add);

                                                                                        BigDecimal soldeCloture = soldeOuverture
                                                                                                        .add(waitingBalance);

                                                                                        List<com.yowyob.erp.accounting.dto.GrandLivreDto.LigneGrandLivreDto> lignes = periodDetails
                                                                                                        .stream()
                                                                                                        .map(d -> com.yowyob.erp.accounting.dto.GrandLivreDto.LigneGrandLivreDto
                                                                                                                        .builder()
                                                                                                                        .ecritureId(d.getEcriture_id())
                                                                                                                        .date(d.getCreated_at())
                                                                                                                        .libelle(d.getLibelle())
                                                                                                                        .debit(d.getMontant_debit())
                                                                                                                        .credit(d.getMontant_credit())
                                                                                                                        .build())
                                                                                                        .collect(java.util.stream.Collectors
                                                                                                                        .toList());

                                                                                        return com.yowyob.erp.accounting.dto.GrandLivreDto
                                                                                                        .builder()
                                                                                                        .noCompte(compte.getNo_compte())
                                                                                                        .libelleCompte(compte
                                                                                                                        .getLibelle())
                                                                                                        .soldeOuverture(soldeOuverture)
                                                                                                        .totalDebit(totalDebit)
                                                                                                        .totalCredit(totalCredit)
                                                                                                        .soldeCloture(soldeCloture)
                                                                                                        .lignes(lignes)
                                                                                                        .build();
                                                                                });
                                                        });
                                })
                                .collectList();
        }

        /**
         * Generates the Trial Balance (Balance des Comptes) for a given period.
         */
        public Mono<com.yowyob.erp.accounting.dto.BalanceDesComptesDto> generateBalanceDesComptes(UUID organization_id,
                        String date_debut,
                        String date_fin) {
                LocalDate start = LocalDate.parse(date_debut);
                LocalDate end = LocalDate.parse(date_fin);

                return compte_repository.findAllByOrganization_Id(organization_id)
                                .flatMap(compte -> {
                                        // 1. Calculate opening balance details
                                        return detail_repository
                                                        .findByOrganization_IdAndDateRange(organization_id,
                                                                        LocalDate.of(2000, 1, 1).atStartOfDay(),
                                                                        start.atStartOfDay())
                                                        .filter(d -> d.getCompte_id().equals(compte.getId()))
                                                        .collectList()
                                                        .flatMap(openingDetails -> {
                                                                BigDecimal soldeOuverture = calculateAccountBalance(
                                                                                openingDetails, compte);
                                                                BigDecimal debitOuverture = openingDetails.stream()
                                                                                .map(d -> d.getMontant_debit() != null
                                                                                                ? d.getMontant_debit()
                                                                                                : BigDecimal.ZERO)
                                                                                .reduce(BigDecimal.ZERO,
                                                                                                BigDecimal::add);
                                                                BigDecimal creditOuverture = openingDetails.stream()
                                                                                .map(d -> d.getMontant_credit() != null
                                                                                                ? d.getMontant_credit()
                                                                                                : BigDecimal.ZERO)
                                                                                .reduce(BigDecimal.ZERO,
                                                                                                BigDecimal::add);

                                                                // 2. Get period details
                                                                return detail_repository
                                                                                .findByOrganization_IdAndDateRange(
                                                                                                organization_id,
                                                                                                start.atStartOfDay(),
                                                                                                end.plusDays(1)
                                                                                                                .atStartOfDay())
                                                                                .filter(d -> d.getCompte_id()
                                                                                                .equals(compte.getId()))
                                                                                .collectList()
                                                                                .map(periodDetails -> {
                                                                                        BigDecimal mvmtDebit = periodDetails
                                                                                                        .stream()
                                                                                                        .map(d -> d.getMontant_debit() != null
                                                                                                                        ? d.getMontant_debit()
                                                                                                                        : BigDecimal.ZERO)
                                                                                                        .reduce(BigDecimal.ZERO,
                                                                                                                        BigDecimal::add);

                                                                                        BigDecimal mvmtCredit = periodDetails
                                                                                                        .stream()
                                                                                                        .map(d -> d.getMontant_credit() != null
                                                                                                                        ? d.getMontant_credit()
                                                                                                                        : BigDecimal.ZERO)
                                                                                                        .reduce(BigDecimal.ZERO,
                                                                                                                        BigDecimal::add);

                                                                                        BigDecimal soldeCloture = soldeOuverture
                                                                                                        .add(calculateAccountBalance(
                                                                                                                        periodDetails,
                                                                                                                        compte));

                                                                                        return com.yowyob.erp.accounting.dto.BalanceDesComptesDto.LigneBalanceDto
                                                                                                        .builder()
                                                                                                        .noCompte(compte.getNo_compte())
                                                                                                        .libelle(compte.getLibelle())
                                                                                                        .soldeOuvertureDebit(
                                                                                                                        soldeOuverture.compareTo(
                                                                                                                                        BigDecimal.ZERO) >= 0
                                                                                                                                                        ? soldeOuverture
                                                                                                                                                        : BigDecimal.ZERO)
                                                                                                        .soldeOuvertureCredit(
                                                                                                                        soldeOuverture.compareTo(
                                                                                                                                        BigDecimal.ZERO) < 0
                                                                                                                                                        ? soldeOuverture.abs()
                                                                                                                                                        : BigDecimal.ZERO)
                                                                                                        .mouvementDebit(mvmtDebit)
                                                                                                        .mouvementCredit(
                                                                                                                        mvmtCredit)
                                                                                                        .soldeClotureDebit(
                                                                                                                        soldeCloture.compareTo(
                                                                                                                                        BigDecimal.ZERO) >= 0
                                                                                                                                                        ? soldeCloture
                                                                                                                                                        : BigDecimal.ZERO)
                                                                                                        .soldeClotureCredit(
                                                                                                                        soldeCloture.compareTo(
                                                                                                                                        BigDecimal.ZERO) < 0
                                                                                                                                                        ? soldeCloture.abs()
                                                                                                                                                        : BigDecimal.ZERO)
                                                                                                        .build();
                                                                                });
                                                        });
                                })
                                .collectList()
                                .map(lignes -> {
                                        BigDecimal totDebitOuv = lignes.stream()
                                                        .map(com.yowyob.erp.accounting.dto.BalanceDesComptesDto.LigneBalanceDto::getSoldeOuvertureDebit)
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                        BigDecimal totCreditOuv = lignes.stream()
                                                        .map(com.yowyob.erp.accounting.dto.BalanceDesComptesDto.LigneBalanceDto::getSoldeOuvertureCredit)
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                        BigDecimal totMvmtDebit = lignes.stream()
                                                        .map(com.yowyob.erp.accounting.dto.BalanceDesComptesDto.LigneBalanceDto::getMouvementDebit)
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                        BigDecimal totMvmtCredit = lignes.stream()
                                                        .map(com.yowyob.erp.accounting.dto.BalanceDesComptesDto.LigneBalanceDto::getMouvementCredit)
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                        BigDecimal totDebitClo = lignes.stream()
                                                        .map(com.yowyob.erp.accounting.dto.BalanceDesComptesDto.LigneBalanceDto::getSoldeClotureDebit)
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                        BigDecimal totCreditClo = lignes.stream()
                                                        .map(com.yowyob.erp.accounting.dto.BalanceDesComptesDto.LigneBalanceDto::getSoldeClotureCredit)
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                                        return com.yowyob.erp.accounting.dto.BalanceDesComptesDto.builder()
                                                        .lignes(lignes)
                                                        .totalDebitOuverture(totDebitOuv)
                                                        .totalCreditOuverture(totCreditOuv)
                                                        .totalDebitMouvement(totMvmtDebit)
                                                        .totalCreditMouvement(totMvmtCredit)
                                                        .totalDebitCloture(totDebitClo)
                                                        .totalCreditCloture(totCreditClo)
                                                        .build();
                                });
        }

        private BigDecimal calculateAccountBalance(List<DetailEcriture> details, Compte compte) {
                return details.stream()
                                .filter(d -> d.getCompte_id() != null && d.getCompte_id().equals(compte.getId()))
                                .map(d -> {
                                        BigDecimal debit = d.getMontant_debit() != null ? d.getMontant_debit()
                                                        : BigDecimal.ZERO;
                                        BigDecimal credit = d.getMontant_credit() != null ? d.getMontant_credit()
                                                        : BigDecimal.ZERO;
                                        return "ACTIF".equals(compte.getType_compte())
                                                        || "CHARGE".equals(compte.getType_compte())
                                                                        ? debit.subtract(credit)
                                                                        : credit.subtract(debit);
                                })
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        private BigDecimal calculateDebit(List<DetailEcriture> details, Compte compte) {
                return details.stream()
                                .filter(d -> d.getCompte_id() != null && d.getCompte_id().equals(compte.getId()))
                                .map(d -> d.getMontant_debit() != null ? d.getMontant_debit() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        private BigDecimal calculateCredit(List<DetailEcriture> details, Compte compte) {
                return details.stream()
                                .filter(d -> d.getCompte_id() != null && d.getCompte_id().equals(compte.getId()))
                                .map(d -> d.getMontant_credit() != null ? d.getMontant_credit() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        private Mono<Void> logAudit(Organization organization, String utilisateur, String action, String details) {
                JournalAudit audit = JournalAudit.builder()
                                .id(UUID.randomUUID())
                                .organizationId(organization.getId())
                                .action(action)
                                .utilisateur(utilisateur)
                                .details(details)
                                .date_action(LocalDateTime.now())
                                .created_at(LocalDateTime.now())
                                .updated_at(LocalDateTime.now())
                                .created_by("system")
                                .updated_by("system")
                                .build();

                return audit_repository.save(audit)
                                .flatMap(saved -> {
                                        JournalAuditDto auditDto = JournalAuditDto.builder()
                                                        .action(saved.getAction())
                                                        .utilisateur(saved.getUtilisateur())
                                                        .details(saved.getDetails())
                                                        .date_action(saved.getDate_action())
                                                        .build();

                                        kafka_service.sendAuditLog(auditDto, organization.getId(), action);
                                        return Mono.empty();
                                });
        }
}