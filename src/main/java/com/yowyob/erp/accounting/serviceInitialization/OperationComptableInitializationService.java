package com.yowyob.erp.accounting.serviceInitialization;

import com.yowyob.erp.accounting.entity.JournalComptable;
import com.yowyob.erp.accounting.entity.OperationComptable;
import com.yowyob.erp.accounting.entity.Tenant;
import com.yowyob.erp.accounting.repository.JournalComptableRepository;
import com.yowyob.erp.accounting.repository.OperationComptableRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.math.BigDecimal;

import org.springframework.core.annotation.Order;

/**
 * Service to initialize basic accounting operations:
 * - Purchase (Journal AN)
 * - Sale (Journal VE)
 * - Payment (Journal TR)
 * Follows snake_case naming and English Javadoc as per development charter.
 *
 * @author ALD
 * @date 30.09.25
 */
@Service
@Order(2)
public class OperationComptableInitializationService implements CommandLineRunner {

        private final OperationComptableRepository operation_repository;
        private final JournalComptableRepository journal_repository;
        private final UUID tenant_id;

        public OperationComptableInitializationService(
                        OperationComptableRepository operation_repository,
                        JournalComptableRepository journal_repository,
                        @Value("${app.tenant.default-tenant:550e8400-e29b-41d4-a716-446655440000}") String tenant_id_str) {
                this.operation_repository = operation_repository;
                this.journal_repository = journal_repository;
                this.tenant_id = UUID.fromString(tenant_id_str);
        }

        @Override
        public void run(String... args) {
                JournalComptable journal_an = journal_repository
                                .findByTenant_IdAndCode_journal(tenant_id, "AN")
                                .orElseThrow(() -> new IllegalStateException("Journal AN not found"));

                JournalComptable journal_ve = journal_repository
                                .findByTenant_IdAndCode_journal(tenant_id, "VE")
                                .orElseThrow(() -> new IllegalStateException("Journal VE not found"));

                JournalComptable journal_tr = journal_repository
                                .findByTenant_IdAndCode_journal(tenant_id, "TR")
                                .orElseThrow(() -> new IllegalStateException("Journal TR not found"));

                createOperationIfNotExists("ACHAT", "ESPECE", "401000", false, "DEBIT",
                                journal_an.getId(), "HT", BigDecimal.valueOf(1_000_000.0));

                createOperationIfNotExists("VENTE", "ESPECE", "701000", false, "CREDIT",
                                journal_ve.getId(), "TTC", BigDecimal.valueOf(1_000_000.0));

                createOperationIfNotExists("PAIEMENT", "VIREMENT", "512000", false, "CREDIT",
                                journal_tr.getId(), "TTC", BigDecimal.valueOf(5_000_000.0));
        }

        /**
         * Creates an accounting operation if it doesn't already exist for the default
         * tenant.
         *
         * @param type_operation      the type of operation (e.g., ACHAT, VENTE)
         * @param mode_reglement      the settlement mode (e.g., ESPECE, VIREMENT)
         * @param compte_principal    the principal account number
         * @param est_compte_statique whether the account is static
         * @param sens_principal      the primary direction (DEBIT/CREDIT)
         * @param journal_id          the journal ID
         * @param type_montant        the amount type (HT, TTC, etc.)
         * @param plafond_client      the client ceiling amount
         */
        private void createOperationIfNotExists(
                        String type_operation,
                        String mode_reglement,
                        String compte_principal,
                        boolean est_compte_statique,
                        String sens_principal,
                        UUID journal_id,
                        String type_montant,
                        BigDecimal plafond_client) {

                boolean exists = operation_repository
                                .findByTenant_IdAndType_operationAndMode_reglement(tenant_id, type_operation,
                                                mode_reglement)
                                .isPresent();

                if (!exists) {
                        OperationComptable operation = OperationComptable.builder()
                                        .tenant(new Tenant(tenant_id))
                                        .type_operation(type_operation)
                                        .mode_reglement(mode_reglement)
                                        .compte_principal(compte_principal)
                                        .est_compte_statique(est_compte_statique)
                                        .sens_principal(sens_principal)
                                        .journal_comptable(journal_repository.findById(journal_id)
                                                        .orElseThrow())
                                        .type_montant(type_montant)
                                        .plafond_client(plafond_client)
                                        .actif(true)
                                        .created_at(LocalDateTime.now())
                                        .updated_at(LocalDateTime.now())
                                        .created_by("system")
                                        .updated_by("system")
                                        .build();

                        operation_repository.save(operation);
                }
        }
}