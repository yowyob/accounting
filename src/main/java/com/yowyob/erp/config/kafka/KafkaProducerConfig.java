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
 * Configuration Kafka Producer
 * Permet d’envoyer des objets JSON (DetailEcriture, JournalAudit, etc.)
 * vers les topics Kafka définis dans KafkaTopicConfig.
 */
@Configuration
@EnableKafka
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = true)
@Profile("!no-kafka")
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.producer.transaction-id-prefix}") // 💡 AJOUT : Injection du préfixe d'ID de transaction
    private String transactionIdPrefix;


    /**
     * Configuration de base du Producer.
     */
    @Bean
    public Map<String, Object> producerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false); // Nettoie les headers inutiles
        props.put(ProducerConfig.ACKS_CONFIG, "all"); // Garantie de livraison
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16_384);
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33_554_432);
        
        // 💡 CORRECTION : Ajout du préfixe d'ID de transaction pour rendre la ProducerFactory transactionnelle
        props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, transactionIdPrefix); 
        
        return props;
    }

    /**
     * Factory générique de producteurs Kafka.
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        // La DefaultKafkaProducerFactory devient transactionnelle car TRANSACTIONAL_ID_CONFIG est inclus dans producerConfigs()
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }

    /**
     * KafkaTemplate utilisé pour publier les événements.
     */
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
