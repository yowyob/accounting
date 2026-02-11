package com.yowyob.erp.config.kafka;

import com.yowyob.erp.common.dto.KafkaMessage;
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

    public Mono<Void> sendMessage(String topic, String key, Object payload, String eventType, UUID organizationId) {
        return Mono.defer(() -> {
            KafkaMessage message = KafkaMessage.builder()
                    .organizationId(organizationId)
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

    public Mono<Void> sendAccountingEvent(Object payload, UUID organizationId, String type) {
        return sendMessage(accountingEntriesTopic, organizationId.toString(), payload, type, organizationId);
    }

    public Mono<Void> sendInvoiceEvent(Object payload, UUID organizationId, String type) {
        return sendMessage(invoiceEventsTopic, organizationId.toString(), payload, type, organizationId);
    }

    public Mono<Void> sendTransactionEvent(Object payload, UUID organizationId, String type) {
        return sendMessage(transactionEventsTopic, organizationId.toString(), payload, type, organizationId);
    }

    public Mono<Void> sendAuditLog(Object payload, UUID organizationId, String action) {
        return sendMessage(auditLogsTopic, organizationId.toString(), payload, action, organizationId);
    }

    public Mono<Void> sendNotification(Object payload, UUID organizationId, String type) {
        return sendMessage(notificationsTopic, organizationId.toString(), payload, type, organizationId);
    }

    public Mono<Void> sendTenantCreated(Object payload, UUID organizationId) {
        return sendMessage(tenantCreatedTopic, organizationId.toString(), payload, "TENANT_CREATED", organizationId);
    }

    public Mono<Void> sendTenantUpdated(Object payload, UUID organizationId) {
        return sendMessage(tenantUpdatedTopic, organizationId.toString(), payload, "TENANT_UPDATED", organizationId);
    }

    public Mono<Void> sendTenantDeleted(Object payload, UUID organizationId) {
        return sendMessage(tenantDeletedTopic, organizationId.toString(), payload, "TENANT_DELETED", organizationId);
    }

    public Mono<Void> sendTreasurySync(Object payload, UUID organizationId, String type) {
        return sendMessage(treasurySyncTopic, organizationId.toString(), payload, type, organizationId);
    }

    public Mono<Void> sendStockEvent(Object payload, UUID organizationId, String type) {
        return sendMessage(stockEventsTopic, organizationId.toString(), payload, type, organizationId);
    }

    public Mono<Void> sendThirdPartyEvent(Object payload, UUID organizationId, String type) {
        return sendMessage(thirdPartyEventsTopic, organizationId.toString(), payload, type, organizationId);
    }

    public Mono<Void> sendOrganizationEvent(Object payload, UUID organizationId, String type) {
        return sendMessage(organizationEventsTopic, organizationId.toString(), payload, type, organizationId);
    }
}
