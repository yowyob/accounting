package com.yowyob.erp.config.kafka;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * Configures JpaTransactionManager as the default transaction manager
 * (@Primary).
 * This is necessary when KafkaTransactionManager is also present,
 * because Spring Boot doesn't know which one to choose for
 * unqualified @Transactional annotations.
 */
@Configuration
public class PrimaryTransactionManagerConfig {

    /**
     * Creates and marks JpaTransactionManager as @Primary.
     * 
     * @param dataSource The DataSource injected by Spring Boot.
     * @return The JpaTransactionManager instance.
     */
    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        // JpaTransactionManager manages transactions for Hibernate/JPA.
        // Spring Boot would have created it automatically, but we create it here to add
        // @Primary.
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setDataSource(dataSource);
        return transactionManager;
    }
}
