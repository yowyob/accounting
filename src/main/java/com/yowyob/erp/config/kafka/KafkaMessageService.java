package com.yowyob.erp.config.kafka;

import com.yowyob.erp.common.dto.KafkaMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Centralized service for publishing Kafka events in the ERP.
 * Each module (accounting, billing, audit, etc.) uses it to notify its
 * actions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaMessageService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.accounting-entries}")
    private String accountingEntriesTopic;

    @Value("${app.kafka.topics.invoice-events}")
    private String invoiceEventsTopic;

    @Value("${app.kafka.topics.transaction-events:transaction.events}")
    private String transactionEventsTopic;

    @Value("${app.kafka.topics.notifications}")
    private String notificationsTopic;

    @Value("${app.kafka.topics.audit-logs}")
    private String auditLogsTopic;

    @Value("${app.kafka.topics.tenant-created}")
    private String tenantCreatedTopic;

    @Value("${app.kafka.topics.tenant-updated}")
    private String tenantUpdatedTopic;

    @Value("${app.kafka.topics.tenant-deleted}")
    private String tenantDeletedTopic;

    /*
     * ===========================================================
     * 🔧 GENERIC METHODS
     * ===========================================================
     */

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void sendMessage(String topic, String key, Object payload, String eventType, UUID tenantId) {
        KafkaMessage message = KafkaMessage.builder()
                .tenantId(tenantId)
                .eventType(eventType)
                .payload(payload)
                .timestamp(LocalDateTime.now())
                .build();

        try {
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, message);
            future.whenComplete((result, exception) -> {
                if (exception == null) {
                    log.debug("✅ Message sent → Topic [{}] | Offset [{}]", topic,
                            result.getRecordMetadata().offset());
                } else {
                    log.error("❌ Kafka send failed → Topic [{}] : {}", topic, exception.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("Kafka send error → Topic [{}]", topic, e);
            throw e;
        }
    }

    /*
     * ===========================================================
     * 🧾 BUSINESS DOMAINS
     * ===========================================================
     */

    public void sendAccountingEvent(Object payload, UUID tenantId, String type) {
        sendMessage(accountingEntriesTopic, tenantId.toString(), payload, type, tenantId);
    }

    public void sendInvoiceEvent(Object payload, UUID tenantId, String type) {
        sendMessage(invoiceEventsTopic, tenantId.toString(), payload, type, tenantId);
    }

    public void sendTransactionEvent(Object payload, UUID tenantId, String type) {
        sendMessage(transactionEventsTopic, tenantId.toString(), payload, type, tenantId);
    }

    public void sendAuditLog(Object payload, UUID tenantId, String action) {
        sendMessage(auditLogsTopic, tenantId.toString(), payload, action, tenantId);
    }

    public void sendNotification(Object payload, UUID tenantId, String type) {
        sendMessage(notificationsTopic, tenantId.toString(), payload, type, tenantId);
    }

    public void sendTenantCreated(Object payload, UUID tenantId) {
        sendMessage(tenantCreatedTopic, tenantId.toString(), payload, "TENANT_CREATED", tenantId);
    }

    public void sendTenantUpdated(Object payload, UUID tenantId) {
        sendMessage(tenantUpdatedTopic, tenantId.toString(), payload, "TENANT_UPDATED", tenantId);
    }

    public void sendTenantDeleted(Object payload, UUID tenantId) {
        sendMessage(tenantDeletedTopic, tenantId.toString(), payload, "TENANT_DELETED", tenantId);
    }
}
