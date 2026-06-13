package com.yowyob.erp.config.kafka;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

/**
 * Kafka Producer Configuration
 * Allows sending JSON objects (DetailEcriture, JournalAudit, etc.)
 * to Kafka topics defined in KafkaTopicConfig.
 */
@Configuration
@EnableKafka
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = true)
@Profile("!no-kafka")
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrap_servers;

    @Value("${spring.kafka.producer.transaction-id-prefix}")
    private String transaction_id_prefix;

    // SASL optionnel : vide en local (Kafka en clair), renseigné en prod yowyob (SASL_PLAINTEXT/SCRAM).
    @Value("${spring.kafka.security.protocol:}")
    private String security_protocol;

    @Value("${spring.kafka.properties.sasl.mechanism:}")
    private String sasl_mechanism;

    @Value("${spring.kafka.properties.sasl.jaas.config:}")
    private String sasl_jaas_config;

    /**
     * Basic Producer Configuration.
     */
    @Bean
    public Map<String, Object> producerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap_servers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16_384);
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33_554_432);

        // Required for transactional ProducerFactory
        props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, transaction_id_prefix);

        KafkaSecuritySupport.apply(props, security_protocol, sasl_mechanism, sasl_jaas_config);

        return props;
    }

    /**
     * Generic Kafka Producer Factory (Transactional).
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }

    /**
     * Transactional KafkaTemplate.
     */
    @Bean
    @Primary
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    /**
     * Non-Transactional KafkaTemplate for audit logs and events
     * that shouldn't block on Kafka transactions.
     */
    @Bean(name = "nonTransactionalKafkaTemplate")
    public KafkaTemplate<String, Object> nonTransactionalKafkaTemplate() {
        Map<String, Object> props = new HashMap<>(producerConfigs());
        props.remove(ProducerConfig.TRANSACTIONAL_ID_CONFIG);

        DefaultKafkaProducerFactory<String, Object> factory = new DefaultKafkaProducerFactory<>(props);
        return new KafkaTemplate<>(factory);
    }
}
