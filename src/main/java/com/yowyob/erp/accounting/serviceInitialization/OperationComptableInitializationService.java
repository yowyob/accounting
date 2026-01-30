package com.yowyob.erp.accounting.serviceInitialization;

import com.yowyob.erp.accounting.entity.Compte;
import com.yowyob.erp.accounting.entity.JournalComptable;
import com.yowyob.erp.accounting.entity.OperationComptable;
import com.yowyob.erp.accounting.entity.Tenant;
import com.yowyob.erp.accounting.repository.CompteRepository;
import com.yowyob.erp.accounting.repository.JournalComptableRepository;
import com.yowyob.erp.accounting.repository.OperationComptableRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Reactive Service to initialize basic accounting operations.
 */
@Service
@Order(2)
@Slf4j
public class OperationComptableInitializationService implements CommandLineRunner {

        private final OperationComptableRepository operation_repository;
        private final JournalComptableRepository journal_repository;
        private final CompteRepository compte_repository;
        private final UUID tenant_id;

        public OperationComptableInitializationService(
                        OperationComptableRepository operation_repository,
                        JournalComptableRepository journal_repository,
                        CompteRepository compte_repository,
                        @Value("${app.tenant.default-tenant:550e8400-e29b-41d4-a716-446655440000}") String tenant_id_str) {
                this.operation_repository = operation_repository;
                this.journal_repository = journal_repository;
                this.compte_repository = compte_repository;
                this.tenant_id = UUID.fromString(tenant_id_str);
        }

        @Override
        public void run(String... args) {
                log.info("Starting accounting operations initialization...");

                Mono.zip(
                                journal_repository.findByTenant_IdAndCode_journal(tenant_id, "AN"),
                                journal_repository.findByTenant_IdAndCode_journal(tenant_id, "VE"),
                                journal_repository.findByTenant_IdAndCode_journal(tenant_id, "TR"))
                                .flatMap(tuple -> {
                                        JournalComptable journalAN = tuple.getT1();
                                        JournalComptable journalVE = tuple.getT2();
                                        JournalComptable journalTR = tuple.getT3();

                                        return Flux.concat(
                                                        createOperationIfNotExists("ACHAT", "ESPECE", "401000", false,
                                                                        "DEBIT",
                                                                        journalAN, "HT",
                                                                        BigDecimal.valueOf(1_000_000.0)),
                                                        createOperationIfNotExists("VENTE", "ESPECE", "701000", false,
                                                                        "CREDIT",
                                                                        journalVE, "TTC",
                                                                        BigDecimal.valueOf(1_000_000.0)),
                                                        createOperationIfNotExists("PAIEMENT", "VIREMENT", "512000",
                                                                        false, "CREDIT",
                                                                        journalTR, "TTC",
                                                                        BigDecimal.valueOf(5_000_000.0)))
                                                        .then();
                                })
                                .doOnSuccess(v -> log.info("Accounting operations initialization completed."))
                                .doOnError(e -> log.error("Error during accounting operations initialization: {}",
                                                e.getMessage()))
                                .subscribe();
        }

        private Mono<Void> createOperationIfNotExists(
                        String type_operation,
                        String mode_reglement,
                        String no_compte,
                        boolean est_compte_statique,
                        String sens_principal,
                        JournalComptable journal,
                        String type_montant,
                        BigDecimal plafond_client) {

                return operation_repository
                                .findByTenant_IdAndType_operationAndMode_reglement(tenant_id, type_operation,
                                                mode_reglement)
                                .hasElement()
                                .flatMap(exists -> {
                                        if (Boolean.TRUE.equals(exists)) {
                                                log.debug("Operation already exists: {} - {}", type_operation,
                                                                mode_reglement);
                                                return Mono.<Void>empty();
                                        }

                                        log.info("Creating operation: {} - {}", type_operation, mode_reglement);
                                        return compte_repository.findByTenant_IdAndNo_compte(tenant_id, no_compte)
                                                        .flatMap(compte -> {
                                                                OperationComptable operation = OperationComptable.builder()
                                                                                .id(UUID.randomUUID())
                                                                                .tenantId(tenant_id)
                                                                                .type_operation(type_operation)
                                                                                .mode_reglement(mode_reglement)
                                                                                .compte_principal_id(compte.getId())
                                                                                .est_compte_statique(est_compte_statique)
                                                                                .sens_principal(sens_principal)
                                                                                .journal_comptable_id(journal != null ? journal.getId() : null)
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
                                                        }).then();
                                });
        }
}