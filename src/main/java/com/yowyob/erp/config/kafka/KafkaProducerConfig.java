package com.yowyob.erp.config.kafka;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

    @Value("${spring.kafka.producer.transaction-id-prefix}") // ADDITION: Transaction ID prefix injection
    private String transaction_id_prefix;

    /**
     * Basic Producer Configuration.
     */
    @Bean
    public Map<String, Object> producerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap_servers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false); // Cleans useless headers
        props.put(ProducerConfig.ACKS_CONFIG, "all"); // Delivery guarantee
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16_384);
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33_554_432);

        // FIX: Adding transaction ID prefix to make ProducerFactory transactional
        props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, transaction_id_prefix);

        return props;
    }

    /**
     * Generic Kafka Producer Factory.
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        // DefaultKafkaProducerFactory becomes transactional because
        // TRANSACTIONAL_ID_CONFIG is included in producerConfigs()
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }

    /**
     * KafkaTemplate used to publish events.
     */
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
