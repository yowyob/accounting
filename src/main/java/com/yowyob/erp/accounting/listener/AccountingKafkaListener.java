package com.yowyob.erp.accounting.listener;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.springframework.kafka.annotation.KafkaListener; // 💡 Import de l'entité cible
import org.springframework.kafka.support.Acknowledgment; // 💡 Import pour l'injection via constructeur
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yowyob.erp.accounting.dto.JournalAuditDto;
import com.yowyob.erp.accounting.entity.JournalAudit;
import com.yowyob.erp.accounting.entity.Tenant;
import com.yowyob.erp.common.dto.KafkaMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Listener Kafka pour la gestion des événements du domaine comptable ERP.
 * Gère : Facturation, Comptabilité, Transactions et Audit.
 */
@Component
@RequiredArgsConstructor // 💡 Ajout pour l'injection par constructeur (ObjectMapper)
@Slf4j
public class AccountingKafkaListener {

    // 💡 Déclaration et injection de l'ObjectMapper pour la désérialisation du
    // payload interne
    private final ObjectMapper objectMapper;
    private final com.yowyob.erp.accounting.repository.JournalAuditRepository journalAuditRepository;
    private final com.yowyob.erp.accounting.service.EcritureComptableService ecritureComptableService;
    private final com.yowyob.erp.accounting.service.CompteService compteService;
    private final com.yowyob.erp.accounting.repository.JournalComptableRepository journalComptableRepository;
    private final com.yowyob.erp.config.kafka.KafkaMessageService kafkaMessageService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.yowyob.erp.config.elasticsearch.ElasticsearchService elasticsearchService;

    /*
     * ===========================================================
     * 🧾 FACTURATION
     * ===========================================================
     */
    @KafkaListener(topics = "${app.kafka.topics.invoice-events}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleInvoiceEvents(
            @Payload KafkaMessage message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            Acknowledgment acknowledgment) {

        try {
            log.info("📄 [INVOICE] Event reçu sur topic={} partition={} | type={} | tenant={}",
                    topic, partition, message.getEventType(), message.getTenantId());

            switch (message.getEventType()) {
                case "INVOICE_CREATED" -> handleInvoiceCreated(message);
                case "INVOICE_PAID" -> handleInvoicePaid(message);
                default -> log.warn("⚠️ Type d'événement facture non géré : {}", message.getEventType());
            }

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("❌ Erreur lors du traitement de l'événement facture : {}", e.getMessage(), e);
        }
    }

    /*
     * ===========================================================
     * 📘 COMPTABILITÉ (Écritures)
     * ===========================================================
     */
    @KafkaListener(topics = "${app.kafka.topics.accounting-entries}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleAccountingEvents(@Payload KafkaMessage message, Acknowledgment acknowledgment) {
        try {
            log.info("📘 [COMPTA] Event reçu | type={} | tenant={}", message.getEventType(), message.getTenantId());

            switch (message.getEventType()) {
                case "ACCOUNTING_ENTRY_CREATED" -> handleAccountingEntryCreated(message);
                case "ACCOUNTING_ENTRY_VALIDATED" -> handleAccountingEntryValidated(message);
                default -> log.warn("⚠️ Type d'événement comptable non géré : {}", message.getEventType());
            }

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("❌ Erreur lors du traitement d’un événement comptable", e);
        }
    }

    /*
     * ===========================================================
     * 💳 TRANSACTIONS
     * ===========================================================
     */
    @KafkaListener(topics = "transaction.events", groupId = "${spring.kafka.consumer.group-id}")
    public void handleTransactionEvents(@Payload KafkaMessage message, Acknowledgment acknowledgment) {
        try {
            log.info("💳 [TRANSACTION] Event reçu | type={} | tenant={}", message.getEventType(),
                    message.getTenantId());
            switch (message.getEventType()) {
                case "TRANSACTION_CREATED" -> handleTransactionCreated(message);
                case "TRANSACTION_VALIDATED" -> handleTransactionValidated(message);
                default -> log.warn("⚠️ Type d'événement transaction non géré : {}", message.getEventType());
            }
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("❌ Erreur lors du traitement d’un événement transaction", e);
        }
    }

    /*
     * ===========================================================
     * 🔍 AUDIT (Version Sécurisée et Robuste)
     * ===========================================================
     */
    @KafkaListener(topics = "${app.kafka.topics.audit-logs}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleAuditLogs(@Payload KafkaMessage message, Acknowledgment acknowledgment) {
        try {
            log.info("🧩 [AUDIT] Nouveau log reçu | action={} | tenant={}", message.getEventType(),
                    message.getTenantId());

            if (message.getPayload() instanceof Map<?, ?> rawPayload) {
                // 1. Conversion sécurisée en DTO
                JournalAuditDto dto = objectMapper.convertValue(rawPayload, JournalAuditDto.class);

                // 2. FILTRE ANTI-POISON & RÉSILIENCE : Si l'action est nulle, on tente de
                // reconstruire le log
                if (dto.getAction() == null) {
                    log.warn("⚠️ Message d'audit sans action explicite. Reconstruction depuis l'événement Kafka : {}",
                            message.getEventType());

                    dto.setAction(message.getEventType());

                    // Tentative d'extraction intelligente des détails
                    if (dto.getDetails() == null) {
                        if (rawPayload.containsKey("libelle")) {
                            dto.setDetails(String.valueOf(rawPayload.get("libelle")));
                        } else if (rawPayload.containsKey("type_operation")) {
                            dto.setDetails("Type d'opération: " + rawPayload.get("type_operation"));
                        } else if (rawPayload.containsKey("code_journal")) {
                            dto.setDetails("Journal: " + rawPayload.get("code_journal"));
                        } else {
                            dto.setDetails("Action automatique via Kafka: " + message.getEventType());
                        }
                    }

                    // Tentative d'extraction de l'utilisateur
                    if (dto.getUtilisateur() == null) {
                        if (rawPayload.containsKey("updated_by")) {
                            dto.setUtilisateur(String.valueOf(rawPayload.get("updated_by")));
                        } else if (rawPayload.containsKey("created_by")) {
                            dto.setUtilisateur(String.valueOf(rawPayload.get("created_by")));
                        } else {
                            dto.setUtilisateur("system");
                        }
                    }

                    // On sauvegarde l'état complet de l'objet pour la traçabilité
                    try {
                        dto.setDonnees_apres(objectMapper.writeValueAsString(rawPayload));
                    } catch (Exception e) {
                        log.warn("Impossible de sérialiser le payload pour l'audit : {}", e.getMessage());
                    }
                }

                // 3. RECONSTRUCTION DE L'ENTITÉ (Mapping Manuel)
                // On ne passe PAS d'ID ici -> force l'auto-génération par PostgreSQL (Garantit
                // l'INSERT)
                JournalAudit auditEntry = JournalAudit.builder()
                        .tenant(new Tenant(message.getTenantId()))
                        .action(dto.getAction())
                        .utilisateur(dto.getUtilisateur())
                        .details(dto.getDetails())
                        .date_action(dto.getDate_action() != null ? dto.getDate_action() : LocalDateTime.now())
                        .ecriture_comptable_id(dto.getEcriture_comptable_id())
                        .adresse_ip(dto.getAdresse_ip())
                        .donnees_avant(dto.getDonnees_avant())
                        .donnees_apres(dto.getDonnees_apres())
                        .created_by("KAFKA_CONSUMER")
                        .updated_by("KAFKA_CONSUMER")
                        .build();

                // 4. SAUVEGARDE
                // Comme l'ID est nul, Hibernate fera un "INSERT INTO" sans conflit de version
                // (Optimistic Lock)
                journalAuditRepository.save(auditEntry);
                log.info("✅ Audit log sauvegardé en base pour l'action : {}", auditEntry.getAction());

            } else {
                log.warn("⚠️ Payload d'audit non reconnu (attendu Map) : {}",
                        message.getPayload() != null ? message.getPayload().getClass().getName() : "null");
            }

            // 5. ACKNOWLEDGMENT : On confirme le succès à Kafka
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("❌ Erreur critique lors du traitement de l'audit log : {}", e.getMessage());
            // En cas d'erreur de conversion Jackson, on acknowledge quand même pour ne pas
            // bloquer la file
            if (e instanceof IllegalArgumentException) {
                log.error("👉 Message mal formé définitivement ignoré.");
                acknowledgment.acknowledge();
            }
            // Pour les autres erreurs (ex: DB Down), on ne fait pas d'ACK pour permettre un
            // retry automatique
        }
    }

    /*
     * ===========================================================
     * 🔧 MÉTHODES PRIVÉES DE TRAITEMENT
     * ===========================================================
     */
    private void handleInvoiceCreated(KafkaMessage message) {
        log.info("🧾 Génération écriture comptable pour facture créée : {}", message.getPayload());
        com.yowyob.erp.accounting.entity.FactureComptable facture = objectMapper.convertValue(message.getPayload(),
                com.yowyob.erp.accounting.entity.FactureComptable.class);
        ecritureComptableService.generateFromComptableObject(facture);
    }

    private void handleInvoicePaid(KafkaMessage message) {
        log.info("💰 Génération écriture de règlement pour facture payée : {}", message.getPayload());
        com.yowyob.erp.accounting.entity.TransactionComptable transaction = objectMapper
                .convertValue(message.getPayload(), com.yowyob.erp.accounting.entity.TransactionComptable.class);
        ecritureComptableService.generateFromComptableObject(transaction);
    }

    private void handleAccountingEntryCreated(KafkaMessage message) {
        log.info("📊 Indexation d'une écriture comptable créée : {}", message.getPayload());
        if (elasticsearchService != null) {
            elasticsearchService.indexAccountingEntry(message.getPayload(), message.getTenantId().toString());
        }
    }

    private void handleAccountingEntryValidated(KafkaMessage message) {
        log.info("✅ Mise à jour des soldes suite validation : {}", message.getPayload());

        // 1. Recalculate balances in DB/Redis
        if (message.getPayload() instanceof Map<?, ?> payload) {
            Object idObj = payload.get("id");
            if (idObj != null) {
                UUID entryId = UUID.fromString(idObj.toString());
                compteService.updateBalances(message.getTenantId(), entryId);
            }
        }

        // 2. Re-index in Elasticsearch for search visibility
        if (elasticsearchService != null) {
            elasticsearchService.indexAccountingEntry(message.getPayload(), message.getTenantId().toString());
        }
    }

    private void handleTransactionCreated(KafkaMessage message) {
        log.info("💸 Nouvelle transaction détectée : {}", message.getPayload());
        com.yowyob.erp.accounting.entity.TransactionComptable transaction = objectMapper
                .convertValue(message.getPayload(), com.yowyob.erp.accounting.entity.TransactionComptable.class);

        // 1. Generate the accounting entry
        // Ensure journal is set to TR (Treasury) if not provided
        if (transaction.get_journal_comptable_id() == null) {
            journalComptableRepository.findByTenant_IdAndCode_journal(message.getTenantId(), "TR")
                    .ifPresent(j -> transaction.setJournal_comptable_id(j.getId()));
        }

        ecritureComptableService.generateFromComptableObject(transaction);

        // 2. Synchronize with Treasury module
        kafkaMessageService.sendTreasurySync(transaction, message.getTenantId(), "TREASURY_SYNC_SUCCESS");
        log.info("✅ Treasury synchronization successful for transaction: {}", transaction.get_id());
    }

    private void handleTransactionValidated(KafkaMessage message) {
        log.info("🧾 Transaction validée : {}", message.getPayload());

        // 1. If this validation implies balance update (e.g. if it validates an entry)
        if (message.getPayload() instanceof Map<?, ?> payload) {
            Object entryId = payload.get("ecriture_id"); // or "id" depending on payload structure
            if (entryId != null) {
                compteService.updateBalances(message.getTenantId(), UUID.fromString(entryId.toString()));
            }
        }

        // 2. Re-index or log validation
        if (elasticsearchService != null) {
            elasticsearchService.indexAccountingEntry(message.getPayload(), message.getTenantId().toString());
        }

        // 3. Notify Treasury of validation
        kafkaMessageService.sendTreasurySync(message.getPayload(), message.getTenantId(),
                "TREASURY_VALIDATION_NOTIFIED");
        log.info("✅ Treasury validation notified for correlation ID: {}", message.getCorrelationId());
    }
}