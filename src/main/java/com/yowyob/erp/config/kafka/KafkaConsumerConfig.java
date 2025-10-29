package com.yowyob.erp.config.kafka;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Deserializer; // Import générique
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
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer; // 💡 Import du Deserializer de gestion d'erreurs
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import com.yowyob.erp.common.dto.KafkaMessage;

/**
 * Configuration Kafka Consumer
 * Ajout de l'ErrorHandlingDeserializer pour la résilience aux messages corrompus.
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

    @Bean
    public Map<String, Object> consumerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        
        // Nous retirons les classes de Deserializer d'ici car elles sont configurées
        // via les instances de Deserializer dans consumerFactory()
        // props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        // props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class); 
        
        return props;
    }

    /**
     * Crée le Deserializer de valeur, en enveloppant le JsonDeserializer 
     * dans l'ErrorHandlingDeserializer.
     */
    private Deserializer<Object> kafkaMessageErrorHandlingDeserializer() {
        // 1. Désérialiseur interne (le type métier réel est KafkaMessage)
        JsonDeserializer<KafkaMessage> jsonDelegate = new JsonDeserializer<>(
            com.yowyob.erp.common.dto.KafkaMessage.class 
        );
        jsonDelegate.addTrustedPackages("com.yowyob.erp.*");

        // 2. Enveloppe le désérialiseur JSON dans l'ErrorHandlingDeserializer
        // Ceci permet de capturer les SerializationException et d'éviter l'arrêt du consommateur.
        ErrorHandlingDeserializer<KafkaMessage> errorHandlingDeserializer = 
            new ErrorHandlingDeserializer<>(jsonDelegate);
            

        // On retourne l'objet typé pour la ConsumerFactory
        return (Deserializer<Object>) (Deserializer<?>) errorHandlingDeserializer;
    }
    
    /**
     * Factory générique de consommateurs Kafka, utilisant le Deserializer résilient.
     */
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        
        // 💡 Le Key Deserializer reste simple
        StringDeserializer keyDeserializer = new StringDeserializer();
        
        // 💡 Le Value Deserializer est le désérialiseur résilient que nous venons de créer
        Deserializer<Object> valueDeserializer = kafkaMessageErrorHandlingDeserializer();
        
    
        return new DefaultKafkaConsumerFactory<>(
            consumerConfigs(), 
            keyDeserializer, 
            valueDeserializer
        );
    }

    /**
     * Factory pour gérer la concurrence et les threads d’écoute Kafka.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);
        factory.getContainerProperties().setPollTimeout(3000);

        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        
        return factory;
    }
}