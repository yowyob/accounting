package com.yowyob.erp.accounting.listener;

import java.util.Map;
import java.util.Optional;

import org.springframework.kafka.annotation.KafkaListener; // 💡 Import de l'entité cible
import org.springframework.kafka.support.Acknowledgment; // 💡 Import pour l'injection via constructeur
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yowyob.erp.accounting.entity.JournalAudit;
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
     * 🔍 AUDIT
     * ===========================================================
     */
    @KafkaListener(topics = "${app.kafka.topics.audit-logs}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleAuditLogs(@Payload KafkaMessage message, Acknowledgment acknowledgment) {
        try {
            log.info("🧩 [AUDIT] Nouveau log reçu | action={} | tenant={}", message.getEventType(),
                    message.getTenantId());

            // 💡 LOGIQUE AJOUTÉE POUR LA DÉSÉRIALISATION DU PAYLOAD
            Optional.ofNullable(message.getPayload())
                    .ifPresent(rawPayload -> {
                        try {
                            if (rawPayload instanceof Map) {
                                // 💡 Conversion du Map JSON en JournalAudit
                                JournalAudit auditEntry = objectMapper.convertValue(rawPayload, JournalAudit.class);

                                // TODO: Complete JournalAudit object processing
                                log.info("✅ Audit log converti : {} par {}", auditEntry.getAction(),
                                        auditEntry.getUtilisateur());

                            } else {
                                log.warn("⚠️ Payload d'audit non reconnu (attendu Map) : {}",
                                        rawPayload.getClass().getName());
                            }
                        } catch (IllegalArgumentException e) {
                            log.error("❌ Échec de conversion du payload d'audit en JournalAudit", e);
                            // NOTE : Ne pas ackowledge() si l'erreur n'est pas gérée par un ErrorHandler
                        }
                    });

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Erreur traitement audit log", e);
        }
    }

    /*
     * ===========================================================
     * 🔧 MÉTHODES PRIVÉES DE TRAITEMENT
     * ===========================================================
     */
    private void handleInvoiceCreated(KafkaMessage message) {
        log.info("🧾 Génération écriture comptable pour facture créée : {}", message.getPayload());
        // TODO: Create accounting entry (EcritureComptable + DetailEcriture)
    }

    private void handleInvoicePaid(KafkaMessage message) {
        log.info("💰 Génération écriture de règlement pour facture payée : {}", message.getPayload());
        // TODO: Create entry and update balances
    }

    private void handleAccountingEntryCreated(KafkaMessage message) {
        log.info("📊 Indexation d'une écriture comptable créée : {}", message.getPayload());
        // TODO: Elasticsearch indexing or caching
    }

    private void handleAccountingEntryValidated(KafkaMessage message) {
        log.info("✅ Mise à jour des soldes suite validation : {}", message.getPayload());
        // TODO: Recalculate Redis / PostgreSQL balances
    }

    private void handleTransactionCreated(KafkaMessage message) {
        log.info("💸 Nouvelle transaction détectée : {}", message.getPayload());
        // TODO: Synchronize with Treasury module
    }

    private void handleTransactionValidated(KafkaMessage message) {
        log.info("🧾 Transaction validée : {}", message.getPayload());
        // TODO: Record entry in TR Journal
    }
}