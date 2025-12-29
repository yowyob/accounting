package com.yowyob.erp.config.kafka;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Deserializer;
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
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import com.yowyob.erp.common.dto.KafkaMessage;

/**
 * Kafka Consumer Configuration.
 * Includes ErrorHandlingDeserializer for resilience against corrupted messages.
 * 
 * @author ALD
 * @date 30.09.25
 */
@Configuration
@EnableKafka
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = true)
@Profile("!no-kafka")
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrap_servers;

    @Value("${spring.kafka.consumer.group-id:yowyob-erp-group}")
    private String group_id;

    /**
     * Common consumer configuration properties.
     * 
     * @return a map of properties
     */
    @Bean
    public Map<String, Object> consumerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap_servers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, group_id);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return props;
    }

    /**
     * Creates a value deserializer that wraps JsonDeserializer in
     * ErrorHandlingDeserializer.
     * 
     * @return the resilient deserializer
     */
    @SuppressWarnings("unchecked")
    private Deserializer<Object> kafkaMessageErrorHandlingDeserializer() {
        // 1. Internal deserializer (real domain type is KafkaMessage)
        JsonDeserializer<KafkaMessage> json_delegate = new JsonDeserializer<>(
                com.yowyob.erp.common.dto.KafkaMessage.class);
        json_delegate.addTrustedPackages("com.yowyob.erp.*");

        // 2. Wrap JSON deserializer in ErrorHandlingDeserializer
        // This allows capturing SerializationException and avoids stopping the
        // consumer.
        ErrorHandlingDeserializer<KafkaMessage> error_handling_deserializer = new ErrorHandlingDeserializer<>(
                json_delegate);

        // Cast to Deserializer<Object> for ConsumerFactory compatibility
        return (Deserializer<Object>) (Deserializer<?>) error_handling_deserializer;
    }

    /**
     * Generic Kafka consumer factory using the resilient deserializer.
     * 
     * @return the consumer factory
     */
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        // Key Deserializer is simple
        StringDeserializer key_deserializer = new StringDeserializer();

        // Resilience-enhanced value deserializer
        Deserializer<Object> value_deserializer = kafkaMessageErrorHandlingDeserializer();

        return new DefaultKafkaConsumerFactory<>(
                consumerConfigs(),
                key_deserializer,
                value_deserializer);
    }

    /**
     * Factory for handling concurrency and Kafka listener threads.
     * 
     * @return the container factory
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);
        factory.getContainerProperties().setPollTimeout(3000);

        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

        return factory;
    }
}