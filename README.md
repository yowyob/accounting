# YOWYOB ERP — Backend

Système ERP multi-tenant conforme aux normes **OHADA**, développé avec **Spring Boot 3** et une stack **réactive** (R2DBC + WebFlux). Déployé sur [Render](https://render.com).

[![Build](https://img.shields.io/badge/build-passing-brightgreen)](https://github.com/Delmat237/Yowyob-ERP-Accounting)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue)](https://www.postgresql.org/)

---

## Table des matières

- [Fonctionnalités](#fonctionnalités)
- [Architecture](#architecture)
- [Technologies](#technologies)
- [Prérequis](#prérequis)
- [Installation locale](#installation-locale)
- [Variables d'environnement](#variables-denvironnement)
- [Déploiement sur Render](#déploiement-sur-render)
- [Documentation API](#documentation-api)
- [Contribuer](#contribuer)

---

## Fonctionnalités

- **Multi-tenant** : Isolation des données par organisation via header `X-Organization-ID`.
- **Comptabilité OHADA** : Plan comptable, journaux, écritures à double entrée, lettrage, clôture mensuelle/annuelle.
- **Réactif** : Stack non-bloquant avec Spring WebFlux + R2DBC (PostgreSQL).
- **Authentification JWT** : Validation via API externe avec rôles (`ADMIN`, `ACCOUNTANT`, `USER`, `AUDITOR`).
- **Kafka** *(optionnel)* : Événements asynchrones pour les modules comptabilité, trésorerie, stock, tiers.
- **Elasticsearch** *(optionnel)* : Indexation des écritures comptables pour la recherche full-text.
- **Redis** *(optionnel)* : Cache pour les sessions, soldes de comptes, et tokens JWT.
- **Rapports** : Balance des comptes, grand livre, compte de résultat, bilan, tableau de flux de trésorerie.
- **Audit** : Journal complet des actions utilisateurs.
- **Swagger** : Documentation interactive de l'API.

---

## Architecture

```
Spring WebFlux (Reactive HTTP)
        │
        ├── Controllers (REST)
        ├── Services (Business Logic)
        ├── Repositories (R2DBC — Reactive PostgreSQL)
        └── Kafka Listeners (Conditional — spring.kafka.enabled)

Liquibase ──► PostgreSQL (migrations via JDBC blocking pool)
Redis      ──► Cache (désactivé sur free plan Render)
Kafka      ──► Événements asynchrones (désactivé par défaut)
Elasticsearch ► Recherche (désactivé par défaut)
```

Structure du projet :
```
src/main/java/com/yowyob/erp
├── accounting/
│   ├── controller/   → Endpoints REST par domaine
│   ├── dto/          → Data Transfer Objects
│   ├── entity/       → Entités R2DBC
│   ├── listener/     → Consommateurs Kafka (conditionnels)
│   ├── repository/   → Interfaces R2DBC
│   └── service/      → Logique métier
├── common/
│   ├── controller/   → HealthController, DebugController
│   ├── dto/          → ApiResponse, KafkaMessage
│   └── exception/    → GlobalExceptionHandler
└── config/
    ├── auth/         → JWT + SecurityConfig (WebFlux)
    ├── elasticsearch/ → Config conditionnelle
    ├── kafka/        → KafkaMessageService + Config conditionnelle
    └── redis/        → RedisService + Config
```

---

## Technologies

| Composant | Technologie |
|-----------|------------|
| Framework | Spring Boot 3.x + WebFlux |
| Runtime | Java 21 |
| Base de données | PostgreSQL 15 (R2DBC réactif) |
| Migrations | Liquibase (JDBC) |
| Cache | Redis 7.x *(optionnel)* |
| Messaging | Apache Kafka *(optionnel)* |
| Recherche | Elasticsearch 8.x *(optionnel)* |
| Sécurité | JWT (API externe) |
| Documentation | Swagger / springdoc-openapi |
| Conteneurisation | Docker |

---

## Prérequis

- **Java** : JDK 21+
- **Maven** : 3.9.x+
- **PostgreSQL** : 15.x+ (ou via Docker)
- **Docker** *(recommandé)* : pour exécuter l'infrastructure localement

---

## Installation locale

### 1. Cloner le dépôt

```bash
git clone https://github.com/Delmat237/Yowyob-ERP-Accounting.git
cd Yowyob-ERP-Accounting/BACKEND
```

### 2. Lancer PostgreSQL via Docker

```bash
docker run -d \
  --name postgres-yowyob \
  -e POSTGRES_DB=yowyob_erp \
  -e POSTGRES_USER=yowyob_admin \
  -e POSTGRES_PASSWORD=yowyob_secret \
  -p 5433:5432 \
  postgres:15
```

### 3. Compiler et lancer

```bash
mvn clean compile
mvn spring-boot:run
```

Les migrations Liquibase s'exécutent automatiquement au démarrage.

### 4. Accéder à l'application

| Service | URL |
|---------|-----|
| API REST | http://localhost:8081 |
| Swagger UI | http://localhost:8081/swagger-ui |
| Health check | http://localhost:8081/actuator/health |

---

## Variables d'environnement

Le projet utilise des variables d'environnement pour toute la configuration sensible.

### Base de données (obligatoire)

| Variable | Description | Exemple |
|----------|-------------|---------|
| `DB_HOST` | Hôte PostgreSQL | `localhost` |
| `DB_PORT` | Port PostgreSQL | `5433` |
| `DB_NAME` | Nom de la base | `yowyob_erp` |
| `POSTGRES_USER` | Utilisateur PostgreSQL | `yowyob_admin` |
| `POSTGRES_PASSWORD` | Mot de passe PostgreSQL | `yowyob_secret` |

### Sécurité (obligatoire)

| Variable | Description |
|----------|-------------|
| `JWT_SECRET` | Clé secrète pour la validation des tokens JWT |
| `AUTH_API_URL` | URL de l'API d'authentification externe |

### Services optionnels (désactivés par défaut sur Render)

| Variable | Description | Défaut |
|----------|-------------|--------|
| `SPRING_KAFKA_ENABLED` | Active/désactive Kafka | `false` |
| `SPRING_ELASTICSEARCH_ENABLED` | Active/désactive Elasticsearch | `false` |
| `SPRING_CACHE_TYPE` | Type de cache (`redis` ou `none`) | `none` |
| `REDIS_URL` | URL Redis | `redis://localhost:6379` |
| `KAFKA_BOOTSTRAP` | Serveurs Kafka | `localhost:9092` |

---

## Déploiement sur Render

Le projet inclut un fichier `render.yaml` à la racine du `BACKEND/` pour un déploiement automatique.

### Déployer via Blueprint (recommandé)

1. Pousser le code sur GitHub
2. Sur [Render Dashboard](https://dashboard.render.com) → **New** → **Blueprint**
3. Connecter le dépôt — Render détecte automatiquement `render.yaml`
4. Ajouter les variables secrètes manuellement :
   - `JWT_SECRET`
   - `AUTH_API_URL` *(si différent de la valeur par défaut)*

### Déployer manuellement

```bash
git push origin SQL_VERSION
```
Render redéploie automatiquement à chaque push.

> **Note :** Sur le plan gratuit Render, Kafka, Redis et Elasticsearch sont désactivés (`SPRING_KAFKA_ENABLED=false`, `SPRING_CACHE_TYPE=none`). La base PostgreSQL est provisionnée automatiquement par Render.

---

## Documentation API

Swagger UI disponible à : `http://localhost:8081/swagger-ui` (local) ou sur l'URL Render de ton service.

Principaux modules d'API :

| Module | Préfixe |
|--------|---------|
| Plan comptable | `/api/accounting/plan-comptable` |
| Journaux comptables | `/api/accounting/journaux` |
| Écritures comptables | `/api/accounting/ecritures` |
| Périodes comptables | `/api/accounting/periodes` |
| Exercices fiscaux | `/api/accounting/exercices` |
| Rapports financiers | `/api/accounting/rapports` |
| Déclarations fiscales | `/api/accounting/declarations` |
| Lettrage | `/api/accounting/lettrage` |
| Journal d'audit | `/api/accounting/audit` |

---

## Contribuer

1. Forker le dépôt.
2. Créer une branche : `git checkout -b feature/ma-fonctionnalite`
3. Commiter : `git commit -m "feat: description"`
4. Pousser : `git push origin feature/ma-fonctionnalite`
5. Ouvrir une Pull Request.

---

## Licence

Ce projet est sous licence MIT. Voir le fichier [LICENSE](LICENSE) pour plus de détails.