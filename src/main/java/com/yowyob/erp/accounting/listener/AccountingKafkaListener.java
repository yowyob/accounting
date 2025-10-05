package com.yowyob.erp.accounting.listener;

import com.yowyob.erp.common.dto.KafkaMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Listener Kafka pour la gestion des événements du domaine comptable ERP.
 * Gère : Facturation, Comptabilité, Transactions et Audit.
 */
@Component
@Slf4j
public class AccountingKafkaListener {

    /* ===========================================================
     *  🧾 FACTURATION
     * =========================================================== */
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

    /* ===========================================================
     *  📘 COMPTABILITÉ (Écritures)
     * =========================================================== */
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

    /* ===========================================================
     *  💳 TRANSACTIONS
     * =========================================================== */
    @KafkaListener(topics = "transaction.events", groupId = "${spring.kafka.consumer.group-id}")
    public void handleTransactionEvents(@Payload KafkaMessage message, Acknowledgment acknowledgment) {
        try {
            log.info("💳 [TRANSACTION] Event reçu | type={} | tenant={}", message.getEventType(), message.getTenantId());
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

    /* ===========================================================
     *  🔍 AUDIT
     * =========================================================== */
    @KafkaListener(topics = "${app.kafka.topics.audit-logs}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleAuditLogs(@Payload KafkaMessage message, Acknowledgment acknowledgment) {
        try {
            log.info("🧩 [AUDIT] Nouveau log reçu | action={} | tenant={}", message.getEventType(), message.getTenantId());
            // TODO: Enregistrer dans Elasticsearch ou MonitoringService
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Erreur traitement audit log", e);
        }
    }

    /* ===========================================================
     *  🔧 MÉTHODES PRIVÉES DE TRAITEMENT
     * =========================================================== */
    private void handleInvoiceCreated(KafkaMessage message) {
        log.info("🧾 Génération écriture comptable pour facture créée : {}", message.getPayload());
        // TODO: créer écriture (EcritureComptable + DetailEcriture)
    }

    private void handleInvoicePaid(KafkaMessage message) {
        log.info("💰 Génération écriture de règlement pour facture payée : {}", message.getPayload());
        // TODO: créer écriture et mise à jour soldes
    }

    private void handleAccountingEntryCreated(KafkaMessage message) {
        log.info("📊 Indexation d'une écriture comptable créée : {}", message.getPayload());
        // TODO: Indexation Elasticsearch ou mise en cache
    }

    private void handleAccountingEntryValidated(KafkaMessage message) {
        log.info("✅ Mise à jour des soldes suite validation : {}", message.getPayload());
        // TODO: recalculer soldes Redis / PostgreSQL
    }

    private void handleTransactionCreated(KafkaMessage message) {
        log.info("💸 Nouvelle transaction détectée : {}", message.getPayload());
        // TODO: synchroniser avec module Trésorerie
    }

    private void handleTransactionValidated(KafkaMessage message) {
        log.info("🧾 Transaction validée : {}", message.getPayload());
        // TODO: enregistrer écriture dans le Journal TR
    }
}
