package com.yowyob.erp.config.kafka;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

/**
 * Configuration Kafka Consumer
 * Permet d’écouter les événements produits (JSON) sur les topics Kafka.
 */
@Configuration
@EnableKafka
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = true)
@Profile("!no-kafka")
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:yowyob-erp-group}")
    private String groupId;

    /**
     * Configuration de base du Consumer (contient uniquement les configs Kafka de base,
     * exclut les configurations de Deserializer spécifiques à Spring pour éviter le conflit).
     */
    @Bean
    public Map<String, Object> consumerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        // NOTE IMPORTANTE : Nous avons retiré JsonDeserializer.class et JsonDeserializer.TRUSTED_PACKAGES
        // car ils sont configurés directement via l'instance de Deserializer dans consumerFactory()
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return props;
    }

    /**
     * Factory générique de consommateurs Kafka, configurant l'instance de JsonDeserializer.
     */
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        // Création et configuration explicite du Deserializer pour éviter le conflit avec les propriétés
        JsonDeserializer<Object> deserializer = new JsonDeserializer<>();
        deserializer.addTrustedPackages("com.yowyob.erp.*");
        deserializer.ignoreTypeHeaders();
        
        // La ConsumerFactory utilise désormais la Deserializer configurée directement
        return new DefaultKafkaConsumerFactory<>(consumerConfigs(), new StringDeserializer(), deserializer);
    }

    /**
     * Factory pour gérer la concurrence et les threads d’écoute Kafka.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3); // 3 threads d’écoute
        factory.getContainerProperties().setPollTimeout(3000);
        
        // Pour les consommateurs avec AckMode.MANUAL_IMMEDIATE (si non défini, AckMode.BATCH est la valeur par défaut)
        // factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        
        return factory;
    }
}
