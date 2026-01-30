package com.yowyob.erp.config.kafka;

import com.yowyob.erp.common.dto.KafkaMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Centralized Reactive service for publishing Kafka events in the ERP.
 */
@Service
@Slf4j
public class KafkaMessageService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaMessageService(
            @org.springframework.beans.factory.annotation.Qualifier("nonTransactionalKafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

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

    @Value("${app.kafka.topics.treasury-sync:treasury.sync.events}")
    private String treasurySyncTopic;

    @Value("${app.kafka.topics.stock-events:stock.events}")
    private String stockEventsTopic;

    @Value("${app.kafka.topics.thirdparty-events:thirdparty.events}")
    private String thirdPartyEventsTopic;

    @Value("${app.kafka.topics.organization-events:organization.events}")
    private String organizationEventsTopic;

    public Mono<Void> sendMessage(String topic, String key, Object payload, String eventType, UUID tenantId) {
        return Mono.defer(() -> {
            KafkaMessage message = KafkaMessage.builder()
                    .tenantId(tenantId)
                    .eventType(eventType)
                    .payload(payload)
                    .timestamp(LocalDateTime.now())
                    .build();

            return Mono.fromFuture(kafkaTemplate.send(topic, key, message))
                    .doOnSuccess(result -> log.debug("✅ Message sent → Topic [{}] | Offset [{}]", topic,
                            result.getRecordMetadata().offset()))
                    .doOnError(e -> log.error("❌ Kafka send failed → Topic [{}]: {}", topic, e.getMessage()))
                    .then();
        });
    }

    public Mono<Void> sendAccountingEvent(Object payload, UUID tenantId, String type) {
        return sendMessage(accountingEntriesTopic, tenantId.toString(), payload, type, tenantId);
    }

    public Mono<Void> sendInvoiceEvent(Object payload, UUID tenantId, String type) {
        return sendMessage(invoiceEventsTopic, tenantId.toString(), payload, type, tenantId);
    }

    public Mono<Void> sendTransactionEvent(Object payload, UUID tenantId, String type) {
        return sendMessage(transactionEventsTopic, tenantId.toString(), payload, type, tenantId);
    }

    public Mono<Void> sendAuditLog(Object payload, UUID tenantId, String action) {
        return sendMessage(auditLogsTopic, tenantId.toString(), payload, action, tenantId);
    }

    public Mono<Void> sendNotification(Object payload, UUID tenantId, String type) {
        return sendMessage(notificationsTopic, tenantId.toString(), payload, type, tenantId);
    }

    public Mono<Void> sendTenantCreated(Object payload, UUID tenantId) {
        return sendMessage(tenantCreatedTopic, tenantId.toString(), payload, "TENANT_CREATED", tenantId);
    }

    public Mono<Void> sendTenantUpdated(Object payload, UUID tenantId) {
        return sendMessage(tenantUpdatedTopic, tenantId.toString(), payload, "TENANT_UPDATED", tenantId);
    }

    public Mono<Void> sendTenantDeleted(Object payload, UUID tenantId) {
        return sendMessage(tenantDeletedTopic, tenantId.toString(), payload, "TENANT_DELETED", tenantId);
    }

    public Mono<Void> sendTreasurySync(Object payload, UUID tenantId, String type) {
        return sendMessage(treasurySyncTopic, tenantId.toString(), payload, type, tenantId);
    }

    public Mono<Void> sendStockEvent(Object payload, UUID tenantId, String type) {
        return sendMessage(stockEventsTopic, tenantId.toString(), payload, type, tenantId);
    }

    public Mono<Void> sendThirdPartyEvent(Object payload, UUID tenantId, String type) {
        return sendMessage(thirdPartyEventsTopic, tenantId.toString(), payload, type, tenantId);
    }

    public Mono<Void> sendOrganizationEvent(Object payload, UUID tenantId, String type) {
        return sendMessage(organizationEventsTopic, tenantId.toString(), payload, type, tenantId);
    }
}
