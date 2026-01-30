package com.yowyob.erp.config.kafka;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.ReactiveTransactionManager;
import io.r2dbc.spi.ConnectionFactory;

/**
 * Configures R2dbcTransactionManager as the default reactive transaction manager
 * (@Primary).
 * This is necessary when KafkaTransactionManager is also present,
 * because Spring Boot doesn't know which one to choose for
 * unqualified @Transactional annotations.
 */
@Configuration
public class PrimaryTransactionManagerConfig {

    /**
     * Creates and marks R2dbcTransactionManager as @Primary.
     * 
     * @param connectionFactory The R2DBC ConnectionFactory injected by Spring Boot.
     * @return The ReactiveTransactionManager instance.
     */
    @Bean
    @Primary
    public ReactiveTransactionManager transactionManager(ConnectionFactory connectionFactory) {
        // R2dbcTransactionManager manages transactions for R2DBC (reactive database access).
        return new R2dbcTransactionManager(connectionFactory);
    }
}
