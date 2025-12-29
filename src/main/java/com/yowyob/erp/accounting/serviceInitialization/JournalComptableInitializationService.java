package com.yowyob.erp.accounting.serviceInitialization;

import com.yowyob.erp.accounting.entity.JournalComptable;
import com.yowyob.erp.accounting.entity.Tenant;
import com.yowyob.erp.accounting.repository.JournalComptableRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.core.annotation.Order;

/**
 * Service to initialize basic accounting journals (OHADA)
 * - AN (Purchases)
 * - VE (Sales)
 * - TR (Treasury)
 * - OD (Miscellaneous Operations)
 * 
 * @author ALD
 * @date 30.09.25
 */
@Service
@Order(1)
public class JournalComptableInitializationService implements CommandLineRunner {

    private final JournalComptableRepository journal_repository;
    private final UUID tenant_id;

    /**
     * Constructor for JournalComptableInitializationService.
     * 
     * @param journal_repository the journal repository
     * @param tenant_id_str      the default tenant ID string
     */
    public JournalComptableInitializationService(
            JournalComptableRepository journal_repository,
            @Value("${app.tenant.default-tenant:550e8400-e29b-41d4-a716-446655440000}") String tenant_id_str) {
        this.journal_repository = journal_repository;
        this.tenant_id = UUID.fromString(tenant_id_str);
    }

    @Override
    public void run(String... args) {
        createJournalIfNotExists("AN", "Journal des Achats", "ACHAT");
        createJournalIfNotExists("VE", "Journal des Ventes", "VENTE");
        createJournalIfNotExists("TR", "Journal de Trésorerie", "TRESORERIE");
        createJournalIfNotExists("OD", "Journal des Opérations Diverses", "DIVERS");
    }

    /**
     * Creates a journal if it does not already exist.
     * 
     * @param code_journal the journal code
     * @param libelle      the label
     * @param type_journal the type
     */
    private void createJournalIfNotExists(String code_journal, String libelle, String type_journal) {
        if (!journal_repository.existsByTenant_IdAndCode_journal(tenant_id, code_journal)) {

            JournalComptable journal = JournalComptable.builder()
                    .tenant(new Tenant(tenant_id))
                    .code_journal(code_journal)
                    .libelle(libelle)
                    .type_journal(type_journal)
                    .actif(true)
                    .created_at(LocalDateTime.now())
                    .updated_at(LocalDateTime.now())
                    .created_by("system")
                    .updated_by("system")
                    .build();
            journal_repository.save(journal);
        }
    }
}
