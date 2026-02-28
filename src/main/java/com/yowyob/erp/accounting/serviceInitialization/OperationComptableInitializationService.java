package com.yowyob.erp.accounting.serviceInitialization;

import com.yowyob.erp.accounting.entity.JournalComptable;
import com.yowyob.erp.accounting.entity.OperationComptable;
import com.yowyob.erp.accounting.repository.CompteRepository;
import com.yowyob.erp.accounting.repository.JournalComptableRepository;
import com.yowyob.erp.accounting.repository.OperationComptableRepository;
import com.yowyob.erp.accounting.entity.Contrepartie;
import com.yowyob.erp.accounting.repository.ContrepartieRepository;
import com.yowyob.erp.config.redis.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Reactive Service to initialize basic accounting operations.
 */
@Service
@Order(5)
@Slf4j
public class OperationComptableInitializationService implements CommandLineRunner {

        private final OperationComptableRepository operation_repository;
        private final ContrepartieRepository contrepartie_repository;
        private final JournalComptableRepository journal_repository;
        private final CompteRepository compte_repository;
        private final RedisService redis_service;
        private final UUID organization_id;

        public OperationComptableInitializationService(
                        OperationComptableRepository operation_repository,
                        ContrepartieRepository contrepartie_repository,
                        JournalComptableRepository journal_repository,
                        CompteRepository compte_repository,
                        RedisService redis_service,
                        @Value("${app.organization.default-organization:4e177ff2-89b8-4d24-926a-5763dfa1b19a}") String organization_id_str) {
                this.operation_repository = operation_repository;
                this.contrepartie_repository = contrepartie_repository;
                this.journal_repository = journal_repository;
                this.compte_repository = compte_repository;
                this.redis_service = redis_service;
                this.organization_id = UUID.fromString(organization_id_str);
        }

        @Override
        public void run(String... args) {
                log.info("Starting accounting operations initialization...");

                Mono.zip(
                                journal_repository.findByOrganization_IdAndCode_journal(organization_id, "AN"),
                                journal_repository.findByOrganization_IdAndCode_journal(organization_id, "VE"),
                                journal_repository.findByOrganization_IdAndCode_journal(organization_id, "TR"))
                                .flatMap(tuple -> {
                                        JournalComptable journalAN = tuple.getT1();
                                        JournalComptable journalVE = tuple.getT2();
                                        JournalComptable journalTR = tuple.getT3();

                                        return Flux.concat(
                                                        createOperationWithContreparties("ACHAT", "ESPECE", "601100",
                                                                        false,
                                                                        "DEBIT", journalAN, "HT",
                                                                        BigDecimal.valueOf(1_000_000.0),
                                                                        List.of(new CPDef("401000", "CREDIT", "TTC",
                                                                                        false))),
                                                        createOperationWithContreparties("VENTE", "ESPECE", "701100",
                                                                        false,
                                                                        "CREDIT", journalVE, "TTC",
                                                                        BigDecimal.valueOf(1_000_000.0),
                                                                        List.of(new CPDef("411000", "DEBIT", "TTC",
                                                                                        false))),
                                                        createOperationWithContreparties("PAIEMENT", "VIREMENT",
                                                                        "521000",
                                                                        false, "CREDIT", journalTR, "TTC",
                                                                        BigDecimal.valueOf(5_000_000.0),
                                                                        List.of(new CPDef("401000", "DEBIT", "TTC",
                                                                                        false))))
                                                        .then(redis_service.delete("operations:all:" + organization_id))
                                                        .then();
                                })
                                .doOnSuccess(v -> log.info("Accounting operations initialization completed."))
                                .doOnError(e -> log.error("Error during accounting operations initialization: {}",
                                                e.getMessage()))
                                .block();
        }

        private record CPDef(String noCompte, String sens, String typeMontant, boolean estTiers) {
        }

        private Mono<Void> createOperationWithContreparties(
                        String type_operation,
                        String mode_reglement,
                        String no_compte,
                        boolean est_compte_statique,
                        String sens_principal,
                        JournalComptable journal,
                        String type_montant,
                        BigDecimal plafond_client,
                        List<CPDef> cpDefs) {

                return operation_repository
                                .findByOrganization_IdAndType_operationAndMode_reglement(organization_id,
                                                type_operation,
                                                mode_reglement)
                                .flatMap(existing -> {
                                        // If exists but broken (null compte_principal_id), fix it
                                        if (existing.getCompte_principal_id() == null) {
                                                log.info("Fixing operation: {} - {}", type_operation, mode_reglement);
                                                return compte_repository
                                                                .findByOrganization_IdAndNo_compte(organization_id,
                                                                                no_compte)
                                                                .flatMap(compte -> {
                                                                        existing.setCompte_principal_id(compte.getId());
                                                                        existing.setJournal_comptable_id(journal != null
                                                                                        ? journal.getId()
                                                                                        : existing.getJournal_comptable_id());
                                                                        existing.setUpdated_at(LocalDateTime.now());
                                                                        existing.setNotNew();
                                                                        return operation_repository.save(existing);
                                                                });
                                        }
                                        existing.setNotNew();
                                        return Mono.just(existing);
                                })
                                .switchIfEmpty(Mono.defer(() -> {
                                        log.info("Creating operation: {} - {}", type_operation, mode_reglement);
                                        return compte_repository
                                                        .findByOrganization_IdAndNo_compte(organization_id, no_compte)
                                                        .flatMap(compte -> {
                                                                OperationComptable operation = OperationComptable
                                                                                .builder()
                                                                                .id(UUID.randomUUID())
                                                                                .organizationId(organization_id)
                                                                                .type_operation(type_operation)
                                                                                .mode_reglement(mode_reglement)
                                                                                .compte_principal_id(compte.getId())
                                                                                .est_compte_statique(
                                                                                                est_compte_statique)
                                                                                .sens_principal(sens_principal)
                                                                                .journal_comptable_id(journal != null
                                                                                                ? journal.getId()
                                                                                                : null)
                                                                                .type_montant(type_montant)
                                                                                .plafond_client(plafond_client)
                                                                                .actif(true)
                                                                                .created_at(LocalDateTime.now())
                                                                                .updated_at(LocalDateTime.now())
                                                                                .created_by("system")
                                                                                .updated_by("system")
                                                                                .isNew(true)
                                                                                .build();
                                                                return operation_repository.save(operation);
                                                        });
                                }))
                                .flatMap(saved -> {
                                        // Check and add counterparties
                                        return contrepartie_repository
                                                        .findByOrganization_IdAndOperation_comptable_Id(organization_id,
                                                                        saved.getId())
                                                        .collectList()
                                                        .flatMap(existingCps -> {
                                                                if (existingCps.isEmpty()) {
                                                                        return Flux.fromIterable(cpDefs)
                                                                                        .flatMap(cpDef -> compte_repository
                                                                                                        .findByOrganization_IdAndNo_compte(
                                                                                                                        organization_id,
                                                                                                                        cpDef.noCompte())
                                                                                                        .flatMap(compte -> {
                                                                                                                Contrepartie cp = Contrepartie
                                                                                                                                .builder()
                                                                                                                                .id(UUID.randomUUID())
                                                                                                                                .organizationId(organization_id)
                                                                                                                                .operation_comptable_id(
                                                                                                                                                saved.getId())
                                                                                                                                .compte_id(compte
                                                                                                                                                .getId())
                                                                                                                                .sens(cpDef.sens())
                                                                                                                                .type_montant(cpDef
                                                                                                                                                .typeMontant())
                                                                                                                                .est_compte_tiers(
                                                                                                                                                cpDef.estTiers())
                                                                                                                                .journal_comptable_id(
                                                                                                                                                saved.getJournal_comptable_id())
                                                                                                                                .created_at(LocalDateTime
                                                                                                                                                .now())
                                                                                                                                .updated_at(LocalDateTime
                                                                                                                                                .now())
                                                                                                                                .created_by("system")
                                                                                                                                .updated_by("system")
                                                                                                                                .isNew(true)
                                                                                                                                .build();
                                                                                                                return contrepartie_repository
                                                                                                                                .save(cp);
                                                                                                        }))
                                                                                        .collectList()
                                                                                        .then();
                                                                }
                                                                return Mono.empty();
                                                        });
                                }).then();
        }
}