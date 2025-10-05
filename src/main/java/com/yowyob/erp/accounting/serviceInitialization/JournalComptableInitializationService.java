package com.yowyob.erp.accounting.serviceInitialization;

import com.yowyob.erp.accounting.entity.JournalComptable;
import com.yowyob.erp.accounting.repository.JournalComptableRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Initialise les journaux comptables de base (OHADA)
 * - AN (Achats)
 * - VE (Ventes)
 * - TR (Trésorerie)
 * - OD (Opérations Diverses)
 */
@Service
public class JournalComptableInitializationService implements CommandLineRunner {

    private final JournalComptableRepository journalComptableRepository;
    private final UUID tenantId;

    public JournalComptableInitializationService(
            JournalComptableRepository journalComptableRepository,
            @Value("${app.tenant.default-tenant:550e8400-e29b-41d4-a716-446655440000}")
            String tenantIdStr) {
        this.journalComptableRepository = journalComptableRepository;
        this.tenantId = UUID.fromString(tenantIdStr);
    }

    @Override
    public void run(String... args) {
        createJournalIfNotExists("AN", "Journal des Achats", "ACHAT");
        createJournalIfNotExists("VE", "Journal des Ventes", "VENTE");
        createJournalIfNotExists("TR", "Journal de Trésorerie", "TRESORERIE");
        createJournalIfNotExists("OD", "Journal des Opérations Diverses", "DIVERS");
    }

    private void createJournalIfNotExists(String codeJournal, String libelle, String typeJournal) {
        if (!journalComptableRepository.existsByTenantIdAndCodeJournal(tenantId, codeJournal)) {
            JournalComptable journal = JournalComptable.builder()
                    .tenantId(tenantId)
                    .codeJournal(codeJournal)
                    .libelle(libelle)
                    .typeJournal(typeJournal)
                    .actif(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .createdBy("system")
                    .updatedBy("system")
                    .build();
            journalComptableRepository.save(journal);
        }
    }
}
