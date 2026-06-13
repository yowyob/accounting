package com.yowyob.erp.config.kafka;

import java.util.Map;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SaslConfigs;

/**
 * Applique (optionnellement) la sécurité SASL aux propriétés des clients Kafka.
 *
 * <p>Les configs producteur/consommateur de ce service construisent leurs propriétés à la main et
 * n'héritent donc pas du binding automatique {@code spring.kafka.*} de Spring Boot. En local/dev le
 * Kafka est en clair (aucune de ces variables n'est renseignée -> no-op). En production yowyob le
 * broker exige SASL_PLAINTEXT / SCRAM-SHA-256 : on injecte alors {@code security.protocol},
 * {@code sasl.mechanism} et {@code sasl.jaas.config} fournis par l'environnement.
 */
public final class KafkaSecuritySupport {

    private KafkaSecuritySupport() {
    }

    public static void apply(Map<String, Object> props, String securityProtocol, String saslMechanism,
            String saslJaasConfig) {
        if (securityProtocol != null && !securityProtocol.isBlank()) {
            props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, securityProtocol.trim());
        }
        if (saslMechanism != null && !saslMechanism.isBlank()) {
            props.put(SaslConfigs.SASL_MECHANISM, saslMechanism.trim());
        }
        if (saslJaasConfig != null && !saslJaasConfig.isBlank()) {
            props.put(SaslConfigs.SASL_JAAS_CONFIG, saslJaasConfig);
        }
    }
}
