// Profile configuration for tests
package com.yowyob.erp.config.profile;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("no-kafka")
@Slf4j
public class NoKafkaConfiguration {

    public NoKafkaConfiguration() {
        log.info("No-Kafka configuration enabled for tests");
    }
}