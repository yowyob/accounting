package com.yowyob.erp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Yowyob ERP Backend Application
 * 
 * Backend principal du système ERP modulaire Yowyob.
 * Version SQL (PostgreSQL + Liquibase)
 * 
 * Modules actifs :
 *  - Comptabilité (OHADA)
 *  - Stock & Facturation (Kafka Events)
 *  - Audit & Synchronisation Offline
 * 
 * Technologies :
 *  - Spring Boot 3.5.3
 *  - PostgreSQL + JPA + Liquibase
 *  - Redis (cache)
 *  - Kafka (messaging)
 *  - Elasticsearch (recherche)
 * 
 * Auteur : AZANGUE LEONEL DELMAT
 * Licence : Interne - © 2025 Yowyob Technologies
 */
@SpringBootApplication
@EnableCaching
@EnableKafka
@EnableAsync
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.yowyob.erp.accounting.repository")
public class YowyobErpBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(YowyobErpBackendApplication.class, args);
    }
}
