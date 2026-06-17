package com.yowyob.erp.accounting.infrastructure.initialization;

import com.yowyob.erp.accounting.domain.model.Compte;
import com.yowyob.erp.accounting.domain.model.JournalComptable;
import com.yowyob.erp.accounting.domain.model.OperationComptable;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.CompteRepository;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.JournalComptableRepository;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.OperationComptableRepository;
import com.yowyob.erp.accounting.domain.model.Contrepartie;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.ContrepartieRepository;
import com.yowyob.erp.config.redis.RedisService;
import com.yowyob.erp.shared.domain.constants.AppConstants;
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
import java.util.Optional;
import java.util.UUID;

/**
 * Reactive Service to initialize basic accounting operation templates (OHADA).
 *
 * <p>Historically this ran only for the configured <em>default</em> organization at boot, which
 * meant any other organization ended up with zero operation templates and the billing→accounting
 * chain (INVOICE_POSTED → draft entry) silently produced no accounting entry. The provisioning is
 * now exposed per-organization via {@link #provisionForOrganization(UUID)} and wired into the plan
 * comptable initialization so every organization that sets up its accounting also gets the
 * required templates.
 *
 * <p>Two latent bugs were fixed here as well:
 * <ul>
 *   <li>journals are now resolved by <b>type</b> (VENTE/ACHAT/BANQUE) instead of by hard-coded
 *       codes (AN/VE/TR) that are not consistent across organizations and never included "TR";</li>
 *   <li>the sales and payment templates now use the <b>VIREMENT</b> settlement mode, which is what
 *       the kernel consumer looks up ({@code findByTypeOperation("VENTE","VIREMENT")} /
 *       {@code ("PAIEMENT","VIREMENT")}). The previous VENTE/ESPECE template was never matched.</li>
 * </ul>
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
        private final UUID default_organization_id;

        /**
         * When {@code false} (default, incl. production) no default organization is seeded at boot;
         * each organization gets its templates on demand via {@link #provisionForOrganization(UUID)}
         * called from the plan-comptable initialization. Set to {@code true} only for local dev.
         */
        @Value("${app.organization.seed-default:false}")
        private boolean seedDefault;

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
                this.default_organization_id = UUID.fromString(organization_id_str);
        }

        /** Ledger accounts the operation templates rely on (number, libellé, type, OHADA class). */
        private static final List<EssentialAccount> ESSENTIAL_ACCOUNTS = List.of(
                        new EssentialAccount("401000", "Fournisseurs", "PASSIF", 4),
                        new EssentialAccount("411000", "Clients", "ACTIF", 4),
                        new EssentialAccount("445710", "TVA Collectée", "PASSIF", 4),
                        new EssentialAccount("445660", "TVA Déductible", "ACTIF", 4),
                        new EssentialAccount("521000", "Banques", "ACTIF", 5),
                        new EssentialAccount("571000", "Caisse", "ACTIF", 5),
                        new EssentialAccount("601100", "Achats de marchandises", "CHARGE", 6),
                        new EssentialAccount("701100", "Ventes de marchandises", "PRODUIT", 7));

        @Override
        public void run(String... args) {
                if (!seedDefault) {
                        log.info("Default-organization seeding disabled (app.organization.seed-default=false) "
                                        + "— skipping boot provisioning; organizations are provisioned on demand.");
                        return;
                }
                log.info("Starting accounting operations initialization for default organization {}...",
                                default_organization_id);
                provisionForOrganization(default_organization_id)
                                .doOnSuccess(v -> log.info("Accounting operations initialization completed."))
                                .doOnError(e -> log.error("Error during accounting operations initialization: {}",
                                                e.getMessage()))
                                .onErrorResume(e -> Mono.empty())
                                .block();
        }

        /**
         * Provisions, idempotently, the essential ledger accounts and the standard OHADA operation
         * templates (ACHAT, VENTE, PAIEMENT) for the given organization. Safe to call repeatedly.
         */
        public Mono<Void> provisionForOrganization(UUID organization_id) {
                return ensureEssentialAccounts(organization_id)
                                .then(Mono.zip(
                                                journalByType(organization_id, AppConstants.JournalTypes.SALES),
                                                journalByType(organization_id, AppConstants.JournalTypes.PURCHASES),
                                                journalByType(organization_id, AppConstants.JournalTypes.BANK)))
                                .flatMap(journals -> {
                                        JournalComptable saleJournal = journals.getT1().orElse(null);
                                        JournalComptable purchaseJournal = journals.getT2().orElse(null);
                                        JournalComptable bankJournal = journals.getT3().orElse(null);

                                        return Flux.concat(
                                                        createOperationWithContreparties(organization_id, "VENTE",
                                                                        "VIREMENT", "701100", false, "CREDIT",
                                                                        saleJournal, "TTC",
                                                                        BigDecimal.valueOf(1_000_000.0),
                                                                        List.of(new CPDef("411000", "DEBIT", "TTC",
                                                                                        false))),
                                                        createOperationWithContreparties(organization_id, "ACHAT",
                                                                        "VIREMENT", "601100", false, "DEBIT",
                                                                        purchaseJournal, "HT",
                                                                        BigDecimal.valueOf(1_000_000.0),
                                                                        List.of(new CPDef("401000", "CREDIT", "TTC",
                                                                                        false))),
                                                        createOperationWithContreparties(organization_id, "PAIEMENT",
                                                                        "VIREMENT", "521000", false, "CREDIT",
                                                                        bankJournal, "TTC",
                                                                        BigDecimal.valueOf(5_000_000.0),
                                                                        List.of(new CPDef("401000", "DEBIT", "TTC",
                                                                                        false))))
                                                        .then(redis_service.delete("operations:all:" + organization_id))
                                                        .then();
                                });
        }

        private Mono<Optional<JournalComptable>> journalByType(UUID organization_id, String type_journal) {
                return journal_repository.findByOrganization_IdAndType_journal(organization_id, type_journal)
                                .next()
                                .map(Optional::of)
                                .defaultIfEmpty(Optional.empty());
        }

        private Mono<Void> ensureEssentialAccounts(UUID organization_id) {
                return Flux.fromIterable(ESSENTIAL_ACCOUNTS)
                                .concatMap(account -> compte_repository
                                                .existsByOrganization_IdAndNo_compte(organization_id, account.noCompte())
                                                .flatMap(exists -> {
                                                        if (Boolean.TRUE.equals(exists)) {
                                                                return Mono.empty();
                                                        }
                                                        log.info("Provisioning essential account {} - {} for org {}",
                                                                        account.noCompte(), account.libelle(),
                                                                        organization_id);
                                                        Compte compte = Compte.builder()
                                                                        .id(UUID.randomUUID())
                                                                        .organizationId(organization_id)
                                                                        .no_compte(account.noCompte())
                                                                        .libelle(account.libelle())
                                                                        .type_compte(account.type())
                                                                        .classe(account.classe())
                                                                        .solde(BigDecimal.ZERO)
                                                                        .actif(true)
                                                                        .created_at(LocalDateTime.now())
                                                                        .updated_at(LocalDateTime.now())
                                                                        .created_by("system")
                                                                        .updated_by("system")
                                                                        .isNew(true)
                                                                        .build();
                                                        return compte_repository.save(compte).then();
                                                }))
                                .then();
        }

        private record EssentialAccount(String noCompte, String libelle, String type, Integer classe) {
        }

        private record CPDef(String noCompte, String sens, String typeMontant, boolean estTiers) {
        }

        private Mono<Void> createOperationWithContreparties(
                        UUID organization_id,
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
                                                type_operation, mode_reglement)
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
                                        log.info("Creating operation: {} - {} for org {}", type_operation,
                                                        mode_reglement, organization_id);
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
                                .flatMap(saved -> contrepartie_repository
                                                .findByOrganization_IdAndOperation_comptable_Id(organization_id,
                                                                saved.getId())
                                                .collectList()
                                                .flatMap(existingCps -> {
                                                        if (!existingCps.isEmpty()) {
                                                                return Mono.empty();
                                                        }
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
                                                }))
                                .then();
        }
}
