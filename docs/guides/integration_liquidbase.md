# Intégration de Liquibase avec ScyllaDB dans Yowyob ERP

Ce document détaille les étapes pour intégrer **Liquibase** dans le projet ERP Yowyob afin de gérer le schéma ScyllaDB pour le module comptable. Liquibase permet de versionner et d'appliquer automatiquement les schémas de base de données via des fichiers de changelog en CQL, adaptés à ScyllaDB.

## Prérequis

- **ScyllaDB** : Instance en cours d'exécution (par exemple, via Docker sur `localhost:9042`).
- **Projet Spring Boot** : Configuré avec `spring-boot-starter-data-cassandra`.
- **Maven** : Pour gérer les dépendances.
- **Kafka** : Configuré pour les événements (par exemple, `accounting.entries`, `journal.audit.created`).
- **Conformité OHADA** : Respectée via les validations dans `EcritureComptableService`.

## Étapes d'Intégration

### 1. Ajouter les Dépendances Liquibase

Modifiez le fichier `pom.xml` pour inclure les dépendances Liquibase et `liquibase-cassandra`.

```xml
<dependencies>
    <!-- Liquibase Core -->
    <dependency>
        <groupId>org.liquibase</groupId>
        <artifactId>liquibase-core</artifactId>
    </dependency>
    <!-- Liquibase Cassandra Extension -->
    <dependency>
        <groupId>org.liquibase.ext</groupId>
        <artifactId>liquibase-cassandra</artifactId>
        <version>4.29.2</version>
    </dependency>
    <!-- Pilote DataStax Cassandra -->
    <dependency>
        <groupId>com.datastax.oss</groupId>
        <artifactId>java-driver-core</artifactId>
        <version>4.17.0</version>
    </dependency>
    <!-- Spring Data Cassandra -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-cassandra</artifactId>
    </dependency>
</dependencies>
```
