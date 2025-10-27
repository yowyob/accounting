package com.yowyob.erp.config.kafka;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * Configure le JpaTransactionManager comme le gestionnaire de transactions par défaut (@Primary).
 * Ceci est nécessaire lorsque KafkaTransactionManager est également présent,
 * car Spring Boot ne sait pas lequel choisir pour les annotations @Transactional non qualifiées.
 */
@Configuration
public class PrimaryTransactionManagerConfig {

    /**
     * Crée et marque le JpaTransactionManager comme @Primary.
     * @param dataSource Le DataSource injecté par Spring Boot.
     * @return L'instance du JpaTransactionManager.
     */
    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        // Le JpaTransactionManager gère les transactions pour Hibernate/JPA.
        // Spring Boot l'aurait créé automatiquement, mais nous le créons ici pour ajouter @Primary.
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setDataSource(dataSource);
        return transactionManager;
    }
}
