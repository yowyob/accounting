package com.yowyob.erp.config.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration Kafka – Gestion automatique des topics ERP comptable.
 * 
 * Gère :
 *  - Flux comptables (écritures, détails, journaux)
 *  - Événements métiers (factures, transactions, notifications)
 *  - Audit & intégration
 */
@Configuration
@EnableKafka
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = true)
@Profile("!no-kafka")
@Slf4j
public class KafkaTopicConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        log.info("✅ Kafka Admin configuré avec serveur: {}", bootstrapServers);
        return new KafkaAdmin(configs);
    }

    /* ==============================================================
     *  🧾 DOMAINES COMPTABLES (OHADA)
     * ============================================================== */

    @Bean
    public NewTopic accountingEntriesTopic() {
        return TopicBuilder.name("accounting.entries")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic detailEcritureCreatedTopic() {
        return TopicBuilder.name("detail.ecriture.created")
                .partitions(2)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic detailEcritureUpdatedTopic() {
        return TopicBuilder.name("detail.ecriture.updated")
                .partitions(2)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic detailEcritureDeletedTopic() {
        return TopicBuilder.name("detail.ecriture.deleted")
                .partitions(2)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic journalAuditCreatedTopic() {
        return TopicBuilder.name("journal.audit.created")
                .partitions(2)
                .replicas(1)
                .build();
    }

    /* ==============================================================
     *  💳 DOMAINES FINANCIERS / FACTURATION
     * ============================================================== */

    @Bean
    public NewTopic invoiceEventsTopic() {
        return TopicBuilder.name("invoice.events")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic transactionEventsTopic() {
        return TopicBuilder.name("transaction.events")
                .partitions(3)
                .replicas(1)
                .build();
    }

    /* ==============================================================
     *  🔔 NOTIFICATIONS & JOURNAL D’AUDIT
     * ============================================================== */

    @Bean
    public NewTopic notificationsTopic() {
        return TopicBuilder.name("notifications")
                .partitions(2)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic auditLogsTopic() {
        return TopicBuilder.name("audit.logs")
                .partitions(2)
                .replicas(1)
                .build();
    }
}
