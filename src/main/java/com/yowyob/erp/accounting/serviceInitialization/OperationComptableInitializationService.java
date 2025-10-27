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

/**
 * Initialise quelques opérations comptables de base :
 * - Achat (Journal AN)
 * - Vente (Journal VE)
 * - Paiement (Journal TR)
 */
//@Service
public class OperationComptableInitializationService implements CommandLineRunner {

    private final OperationComptableRepository operationComptableRepository;
    private final JournalComptableRepository journalComptableRepository;
    private final UUID tenantId;

    public OperationComptableInitializationService(
            OperationComptableRepository operationComptableRepository,
            JournalComptableRepository journalComptableRepository,
            @Value("${app.tenant.default-tenant:550e8400-e29b-41d4-a716-446655440000}")
            String tenantIdStr) {
        this.operationComptableRepository = operationComptableRepository;
        this.journalComptableRepository = journalComptableRepository;
        this.tenantId = UUID.fromString(tenantIdStr);
    }

    @Override
    public void run(String... args) {
        JournalComptable journalAN = journalComptableRepository
                .findByTenant_IdAndCodeJournal(tenantId, "AN")
                .orElseThrow(() -> new IllegalStateException("Journal AN non trouvé"));

        JournalComptable journalVE = journalComptableRepository
                .findByTenant_IdAndCodeJournal(tenantId, "VE")
                .orElseThrow(() -> new IllegalStateException("Journal VE non trouvé"));

        JournalComptable journalTR = journalComptableRepository
                .findByTenant_IdAndCodeJournal(tenantId, "TR")
                .orElseThrow(() -> new IllegalStateException("Journal TR non trouvé"));

        createOperationIfNotExists("ACHAT", "ESPECE", "401000", false, "DEBIT",
                journalAN.getId(), "HT", BigDecimal.valueOf(1_000_000.0));

        createOperationIfNotExists("VENTE", "ESPECE", "701000", false, "CREDIT",
                journalVE.getId(), "TTC", BigDecimal.valueOf(1_000_000.0));

        createOperationIfNotExists("PAIEMENT", "VIREMENT", "512000", false, "CREDIT",
                journalTR.getId(), "TTC", BigDecimal.valueOf(5_000_000.0));
    }

    private void createOperationIfNotExists(
            String typeOperation,
            String modeReglement,
            String comptePrincipal,
            boolean estCompteStatique,
            String sensPrincipal,
            UUID journalComptableId,
            String typeMontant,
            BigDecimal plafondClient) {

        boolean exists = operationComptableRepository
                .findByTenant_IdAndTypeOperationAndModeReglement(tenantId, typeOperation, modeReglement)
                .isPresent();

        if (!exists) {
            OperationComptable operation = OperationComptable.builder()
                    .tenant(new Tenant(tenantId))
                    .typeOperation(typeOperation)
                    .modeReglement(modeReglement)
                    .comptePrincipal(comptePrincipal)
                    .estCompteStatique(estCompteStatique)
                    .sensPrincipal(sensPrincipal)
                    .journalComptable(journalComptableRepository.findById(journalComptableId)
                        .orElseThrow())
                    .typeMontant(typeMontant)
                    .plafondClient(plafondClient)
                    .actif(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .createdBy("system")
                    .updatedBy("system")
                    .build();

            operationComptableRepository.save(operation);
        }
    }
}