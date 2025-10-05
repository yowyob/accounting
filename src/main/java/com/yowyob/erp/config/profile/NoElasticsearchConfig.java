// Configuration pour désactiver Elasticsearch en mode test
package com.yowyob.erp.config.profile;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import lombok.extern.slf4j.Slf4j;

@Configuration
@ConditionalOnProperty(name = "spring.elasticsearch.enabled", havingValue = "false")
@Profile("test")
@Slf4j
public class NoElasticsearchConfig {
    
    public NoElasticsearchConfig() {
        log.info("Elasticsearch désactivé pour les tests");
    }
}