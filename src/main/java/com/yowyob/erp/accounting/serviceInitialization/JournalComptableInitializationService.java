package com.yowyob.erp.accounting.serviceInitialization;

import com.yowyob.erp.accounting.entity.JournalComptable;
import com.yowyob.erp.accounting.entity.Tenant;
import com.yowyob.erp.accounting.repository.JournalComptableRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Reactive Service to initialize basic accounting journals (OHADA).
 */
@Service
@Order(1)
@Slf4j
public class JournalComptableInitializationService implements CommandLineRunner {

    private final JournalComptableRepository journal_repository;
    private final UUID tenant_id;

    public JournalComptableInitializationService(
            JournalComptableRepository journal_repository,
            @Value("${app.tenant.default-tenant:550e8400-e29b-41d4-a716-446655440000}") String tenant_id_str) {
        this.journal_repository = journal_repository;
        this.tenant_id = UUID.fromString(tenant_id_str);
    }

    @Override
    public void run(String... args) {
        log.info("Starting accounting journals initialization...");

        Flux.concat(
                createJournalIfNotExists("AN", "Journal des Achats", "ACHAT"),
                createJournalIfNotExists("VE", "Journal des Ventes", "VENTE"),
                createJournalIfNotExists("TR", "Journal de Trésorerie", "TRESORERIE"),
                createJournalIfNotExists("OD", "Journal des Opérations Diverses", "DIVERS"))
                .then()
                .doOnSuccess(v -> log.info("Accounting journals initialization completed successfully."))
                .doOnError(e -> log.error("Error during journals initialization: {}", e.getMessage()))
                .subscribe();
    }

    private Mono<Void> createJournalIfNotExists(String code_journal, String libelle, String type_journal) {
        return journal_repository.existsByTenant_IdAndCode_journal(tenant_id, code_journal)
                .flatMap(exists -> {
                    if (!exists) {
                        log.info("Creating journal: {} - {}", code_journal, libelle);
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
                        return journal_repository.save(journal).then();
                    }
                    return Mono.empty();
                });
    }
}
