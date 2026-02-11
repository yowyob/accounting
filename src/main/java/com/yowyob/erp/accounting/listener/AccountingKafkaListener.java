package com.yowyob.erp.accounting.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yowyob.erp.accounting.dto.JournalAuditDto;
import com.yowyob.erp.accounting.entity.JournalAudit;
import com.yowyob.erp.accounting.repository.JournalAuditRepository;
import com.yowyob.erp.accounting.repository.JournalComptableRepository;
import com.yowyob.erp.accounting.service.CompteService;
import com.yowyob.erp.accounting.service.EcritureComptableService;
import com.yowyob.erp.common.dto.KafkaMessage;
import com.yowyob.erp.config.kafka.KafkaMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Reactive Listener Kafka for accounting events.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AccountingKafkaListener {

    private final ObjectMapper objectMapper;
    private final JournalAuditRepository journalAuditRepository;
    private final EcritureComptableService ecritureComptableService;
    private final CompteService compteService;
    private final JournalComptableRepository journalComptableRepository;
    private final KafkaMessageService kafkaMessageService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.yowyob.erp.config.elasticsearch.ElasticsearchService elasticsearchService;

    @KafkaListener(topics = "${app.kafka.topics.invoice-events}", groupId = "${spring.kafka.consumer.group-id}")
    public Mono<Void> handleInvoiceEvents(
            @Payload KafkaMessage message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            Acknowledgment acknowledgment) {

        log.info("📄 [INVOICE] Event received on topic={} partition={} | type={} | tenant={}",
                topic, partition, message.getEventType(), message.getOrganizationId());

        Mono<Void> process = switch (message.getEventType()) {
            case "INVOICE_CREATED" -> handleInvoiceCreated(message);
            case "INVOICE_PAID" -> handleInvoicePaid(message);
            default -> {
                log.warn("⚠️ Unsupported invoice event type: {}", message.getEventType());
                yield Mono.empty();
            }
        };

        return process
                .doOnSuccess(v -> acknowledgment.acknowledge())
                .doOnError(e -> log.error("❌ Error processing invoice event: {}", e.getMessage()))
                .then();
    }

    @KafkaListener(topics = "${app.kafka.topics.accounting-entries}", groupId = "${spring.kafka.consumer.group-id}")
    public Mono<Void> handleAccountingEvents(@Payload KafkaMessage message, Acknowledgment acknowledgment) {
        log.info("📘 [COMPTA] Event received | type={} | tenant={}", message.getEventType(), message.getOrganizationId());

        Mono<Void> process = switch (message.getEventType()) {
            case "ACCOUNTING_ENTRY_CREATED" -> handleAccountingEntryCreated(message);
            case "ACCOUNTING_ENTRY_VALIDATED" -> handleAccountingEntryValidated(message);
            default -> {
                log.warn("⚠️ Unsupported accounting event type: {}", message.getEventType());
                yield Mono.empty();
            }
        };

        return process
                .doOnSuccess(v -> acknowledgment.acknowledge())
                .doOnError(e -> log.error("❌ Error processing accounting event", e))
                .then();
    }

    @KafkaListener(topics = "transaction.events", groupId = "${spring.kafka.consumer.group-id}")
    public Mono<Void> handleTransactionEvents(@Payload KafkaMessage message, Acknowledgment acknowledgment) {
        log.info("💳 [TRANSACTION] Event received | type={} | tenant={}", message.getEventType(),
                message.getOrganizationId());

        Mono<Void> process = switch (message.getEventType()) {
            case "TRANSACTION_CREATED" -> handleTransactionCreated(message);
            case "TRANSACTION_VALIDATED" -> handleTransactionValidated(message);
            default -> {
                log.warn("⚠️ Unsupported transaction event type: {}", message.getEventType());
                yield Mono.empty();
            }
        };

        return process
                .doOnSuccess(v -> acknowledgment.acknowledge())
                .doOnError(e -> log.error("❌ Error processing transaction event", e))
                .then();
    }

    @KafkaListener(topics = "${app.kafka.topics.audit-logs}", groupId = "${spring.kafka.consumer.group-id}")
    public Mono<Void> handleAuditLogs(@Payload KafkaMessage message, Acknowledgment acknowledgment) {
        log.info("🧩 [AUDIT] New log received | action={} | tenant={}", message.getEventType(), message.getOrganizationId());

        if (message.getPayload() instanceof Map<?, ?> rawPayload) {
            return Mono.fromCallable(() -> {
                // Proper casting to Map<String, Object> for convertValue compatibility
                @SuppressWarnings("unchecked")
                Map<String, Object> typedPayload = (Map<String, Object>) rawPayload;
                JournalAuditDto dto = objectMapper.convertValue(typedPayload, JournalAuditDto.class);
                if (dto.getAction() == null) {
                    dto.setAction(message.getEventType());
                    if (dto.getDetails() == null) {
                        dto.setDetails("Action automatique via Kafka: " + message.getEventType());
                    }
                    if (dto.getUtilisateur() == null) {
                        dto.setUtilisateur("system");
                    }
                    try {
                        dto.setDonnees_apres(objectMapper.writeValueAsString(rawPayload));
                    } catch (Exception e) {
                        log.warn("Could not serialize payload for audit log");
                    }
                }
                return dto;
            })
                    .flatMap(dto -> {
                        UUID auditId = dto.getId() != null ? dto.getId() : UUID.randomUUID();

                        return journalAuditRepository.existsById(auditId)
                                .flatMap(exists -> {
                                    if (Boolean.TRUE.equals(exists)) {
                                        log.debug("⏭️ Audit log already exists, skipping save: {}", auditId);
                                        return Mono.empty();
                                    }

                                    JournalAudit auditEntry = JournalAudit.builder()
                                            .id(auditId)
                                            .organizationId(message.getOrganizationId())
                                            .action(dto.getAction())
                                            .utilisateur(dto.getUtilisateur())
                                            .details(dto.getDetails())
                                            .date_action(dto.getDate_action() != null ? dto.getDate_action()
                                                    : LocalDateTime.now())
                                            .ecriture_comptable_id(dto.getEcriture_comptable_id())
                                            .adresse_ip(dto.getAdresse_ip())
                                            .donnees_avant(dto.getDonnees_avant())
                                            .donnees_apres(dto.getDonnees_apres())
                                            .created_at(LocalDateTime.now())
                                            .updated_at(LocalDateTime.now())
                                            .created_by("KAFKA_CONSUMER")
                                            .updated_by("KAFKA_CONSUMER")
                                            .build();

                                    return journalAuditRepository.save(auditEntry)
                                            .onErrorResume(
                                                    org.springframework.dao.DataIntegrityViolationException.class,
                                                    e -> {
                                                        log.debug(
                                                                "⏭️ Audit log already exists (race condition), skipping: {}",
                                                                auditId);
                                                        return Mono.empty();
                                                    })
                                            .onErrorResume(org.springframework.dao.DuplicateKeyException.class, e -> {
                                                log.debug("⏭️ Audit log duplicate key (race condition), skipping: {}",
                                                        auditId);
                                                return Mono.empty();
                                            });
                                });
                    })
                    .doOnSuccess(v -> acknowledgment.acknowledge())
                    .doOnError(e -> log.error("❌ Critical error processing audit log: {}", e.getMessage()))
                    .then();
        } else {
            log.warn("⚠️ Unrecognized audit payload");
            acknowledgment.acknowledge();
            return Mono.empty();
        }
    }

    private Mono<Void> handleInvoiceCreated(KafkaMessage message) {
        com.yowyob.erp.accounting.entity.FactureComptable facture = objectMapper.convertValue(message.getPayload(),
                com.yowyob.erp.accounting.entity.FactureComptable.class);
        return ecritureComptableService.generateFromComptableObject(facture).then();
    }

    private Mono<Void> handleInvoicePaid(KafkaMessage message) {
        com.yowyob.erp.accounting.entity.TransactionComptable transaction = objectMapper
                .convertValue(message.getPayload(), com.yowyob.erp.accounting.entity.TransactionComptable.class);
        return ecritureComptableService.generateFromComptableObject(transaction).then();
    }

    private Mono<Void> handleAccountingEntryCreated(KafkaMessage message) {
        if (elasticsearchService != null) {
            elasticsearchService.indexAccountingEntry(message.getPayload(), message.getOrganizationId().toString());
        }
        return Mono.empty();
    }

    private Mono<Void> handleAccountingEntryValidated(KafkaMessage message) {
        Mono<Void> balanceUpdate = Mono.empty();
        if (message.getPayload() instanceof Map<?, ?> payload) {
            Object idObj = payload.get("id");
            if (idObj != null) {
                UUID entryId = UUID.fromString(idObj.toString());
                balanceUpdate = compteService.updateBalances(message.getOrganizationId(), entryId);
            }
        }

        if (elasticsearchService != null) {
            elasticsearchService.indexAccountingEntry(message.getPayload(), message.getOrganizationId().toString());
        }
        return balanceUpdate;
    }

    private Mono<Void> handleTransactionCreated(KafkaMessage message) {
        com.yowyob.erp.accounting.entity.TransactionComptable transaction = objectMapper
                .convertValue(message.getPayload(), com.yowyob.erp.accounting.entity.TransactionComptable.class);

        Mono<com.yowyob.erp.accounting.entity.TransactionComptable> prepareTransaction;
        if (transaction.getJournal_comptable_id() == null) {
            prepareTransaction = journalComptableRepository.findByTenant_IdAndCode_journal(message.getOrganizationId(), "TR")
                    .map(j -> {
                        transaction.setJournal_comptable_id(j.getId());
                        return transaction;
                    })
                    .defaultIfEmpty(transaction);
        } else {
            prepareTransaction = Mono.just(transaction);
        }

        return prepareTransaction
                .flatMap(prepared -> ecritureComptableService.generateFromComptableObject(prepared)
                        .flatMap(ecriture -> kafkaMessageService.sendTreasurySync(prepared, message.getOrganizationId(),
                                "TREASURY_SYNC_SUCCESS")))
                .then();
    }

    private Mono<Void> handleTransactionValidated(KafkaMessage message) {
        Mono<Void> update = Mono.empty();
        if (message.getPayload() instanceof Map<?, ?> payload) {
            Object entryId = payload.get("ecriture_id");
            if (entryId != null) {
                update = compteService.updateBalances(message.getOrganizationId(), UUID.fromString(entryId.toString()));
            }
        }
        if (elasticsearchService != null) {
            elasticsearchService.indexAccountingEntry(message.getPayload(), message.getOrganizationId().toString());
        }
        return update
                .then(Mono.defer(() -> kafkaMessageService.sendTreasurySync(message.getPayload(), message.getOrganizationId(),
                        "TREASURY_VALIDATION_NOTIFIED")))
                .then();
    }

    @KafkaListener(topics = "${app.kafka.topics.stock-events:stock.events}", groupId = "${spring.kafka.consumer.group-id}")
    public Mono<Void> handleStockEvents(@Payload KafkaMessage message, Acknowledgment acknowledgment) {
        log.info("📦 [STOCK] Event received | type={} | tenant={}", message.getEventType(), message.getOrganizationId());
        acknowledgment.acknowledge();
        return Mono.empty();
    }

    @KafkaListener(topics = "${app.kafka.topics.thirdparty-events:thirdparty.events}", groupId = "${spring.kafka.consumer.group-id}")
    public Mono<Void> handleThirdPartyEvents(@Payload KafkaMessage message, Acknowledgment acknowledgment) {
        log.info("👥 [TIERS] Event received | type={} | tenant={}", message.getEventType(), message.getOrganizationId());
        acknowledgment.acknowledge();
        return Mono.empty();
    }

    @KafkaListener(topics = "${app.kafka.topics.organization-events:organization.events}", groupId = "${spring.kafka.consumer.group-id}")
    public Mono<Void> handleOrganizationEvents(@Payload KafkaMessage message, Acknowledgment acknowledgment) {
        log.info("🏢 [ORGANISATION] Event received | type={} | tenant={}", message.getEventType(),
                message.getOrganizationId());
        acknowledgment.acknowledge();
        return Mono.empty();
    }
}