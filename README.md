# YOWYOB ERP — Backend

Système ERP multi-tenant conforme aux normes **OHADA**, développé avec **Spring Boot 3** et une architecture **réactive** (R2DBC + WebFlux).

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green?logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue?logo=postgresql)](https://www.postgresql.org/)
[![Liquibase](https://img.shields.io/badge/Liquibase-4.x-red)](https://www.liquibase.org/)
[![License: MIT](https://img.shields.io/badge/Licence-MIT-yellow)](LICENSE)

---

## 📋 Table des matières

- [Fonctionnalités](#fonctionnalités)
- [Architecture](#architecture)
- [Structure du projet](#structure-du-projet)
- [Technologies](#technologies)
- [Prérequis](#prérequis)
- [Installation locale](#installation-locale)
- [Variables d'environnement](#variables-denvironnement)
- [Déploiement sur Render](#déploiement-sur-render)
- [Documentation API](#documentation-api)
- [Contribuer](#contribuer)

---

## ✨ Fonctionnalités

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

## 🏛️ Architecture

Le projet suit une **architecture hexagonale** (Ports & Adapters) organisée en modules :

```
Spring WebFlux (Reactive HTTP)
        │
        ├── [web]        Controllers REST  ──────────────────────────────────┐
        ├── [application] Services (Use Cases)                              │
        ├── [domain]      Modèles & Ports (interfaces)       Architecture   │
        ├── [infra]       Adapters : R2DBC, Kafka, HTTP       Hexagonale    │
        └── [config]      Configuration transverse ──────────────────────────┘

Liquibase      ──► PostgreSQL (migrations JDBC)
Redis          ──► Cache distribué     (optionnel)
Kafka          ──► Événements async    (optionnel)
Elasticsearch  ──► Recherche full-text (optionnel)
```

---

## 📁 Structure du projet

```
BACKEND/
├── 📄 pom.xml                    → Dépendances Maven
├── 🐳 Dockerfile                 → Image Docker de production
├── 🐳 docker-compose.yml         → Stack locale complète
├── ⚙️  render.yaml               → Déploiement Render
├── 📜 README.md                  → Ce fichier
│
├── docs/                         → Documentation du projet
│   ├── README.md                 → Index de la documentation
│   ├── conception/               → Cahier de conception (LaTeX + PDF)
│   ├── architecture/             → Diagrammes d'architecture (PNG)
│   ├── reports/                  → Rapports PDF et sources LaTeX
│   ├── guides/                   → Guides techniques (Markdown)
│   └── data/                     → Données de référence (CSV, JSON)
│
├── scripts/                      → Scripts d'administration
│
└── src/
    ├── main/
    │   ├── java/com/yowyob/erp/
    │   │   ├── accounting/        → Module comptabilité OHADA
    │   │   │   ├── application/   → Services (use cases)
    │   │   │   │   ├── service/   → Logique métier
    │   │   │   │   └── util/      → Utilitaires applicatifs
    │   │   │   ├── domain/        → Modèles & contrats
    │   │   │   │   ├── model/     → Entités du domaine
    │   │   │   │   └── port/      → Ports in/out (interfaces)
    │   │   │   └── infrastructure/ → Adapters techniques
    │   │   │       ├── web/       → Controllers REST + DTOs
    │   │   │       ├── persistence/ → R2DBC Repositories + Adapters
    │   │   │       ├── messaging/ → Kafka Listeners
    │   │   │       ├── initialization/ → Données initiales
    │   │   │       └── notification/   → Notifications
    │   │   ├── shared/            → Code partagé (exceptions, DTOs, utils)
    │   │   │   ├── domain/        → Constantes, enums, exceptions communes
    │   │   │   ├── application/   → Services partagés
    │   │   │   └── infrastructure/ → Persistence & Web partagés
    │   │   └── config/            → Configuration Spring
    │   │       ├── auth/          → JWT + Spring Security
    │   │       ├── r2dbc/         → R2DBC + Liquibase
    │   │       ├── redis/         → Redis Cache
    │   │       ├── kafka/         → Kafka (conditionnel)
    │   │       ├── elasticsearch/ → ES (conditionnel)
    │   │       ├── swagger/       → Springdoc OpenAPI
    │   │       ├── cors/          → CORS
    │   │       ├── mail/          → Email
    │   │       └── async/         → Exécution asynchrone
    │   └── resources/
    │       ├── application.properties    → Configuration principale
    │       ├── plan_comptable_ohada_713.csv → Plan OHADA (713 comptes)
    │       └── db/changelog/             → Migrations Liquibase (021 changesets)
    └── test/                      → Tests unitaires et d'intégration
```

---

## 🛠️ Technologies

| Composant        | Technologie                          |
|------------------|--------------------------------------|
| Framework        | Spring Boot 3.x + WebFlux            |
| Runtime          | Java 21                              |
| Base de données  | PostgreSQL 15 (R2DBC réactif)        |
| Migrations       | Liquibase (JDBC)                     |
| Cache            | Redis 7.x *(optionnel)*              |
| Messaging        | Apache Kafka *(optionnel)*           |
| Recherche        | Elasticsearch 8.x *(optionnel)*      |
| Sécurité         | JWT (API externe)                    |
| Documentation    | Swagger / springdoc-openapi          |
| Conteneurisation | Docker + Docker Compose              |
| CI/CD            | Render Blueprint                     |

---

## ✅ Prérequis

- **Java** : JDK 21+
- **Maven** : 3.9.x+
- **Docker** *(recommandé)* : pour exécuter l'infrastructure localement

---

## 🚀 Installation locale

### 1. Cloner le dépôt

```bash
git clone https://github.com/Delmat237/Yowyob-ERP-Accounting.git
cd Yowyob-ERP-Accounting/BACKEND
```

### 2. Copier le fichier d'environnement

```bash
cp .env.example .env
# Éditer .env avec vos valeurs
```

### 3. Lancer l'infrastructure via Docker

```bash
docker-compose up -d postgres
```

### 4. Compiler et lancer

```bash
./mvnw spring-boot:run
```

Les migrations Liquibase s'exécutent automatiquement au démarrage.

### 5. Accéder à l'application

| Service      | URL                                        |
|--------------|--------------------------------------------|
| API REST     | http://localhost:8081                      |
| Swagger UI   | http://localhost:8081/swagger-ui           |
| Health check | http://localhost:8081/actuator/health      |

---

## 🔐 Variables d'environnement

Voir `.env.example` pour la liste complète. Variables obligatoires :

### Base de données

| Variable            | Description            | Exemple        |
|---------------------|------------------------|----------------|
| `DB_HOST`           | Hôte PostgreSQL        | `localhost`    |
| `DB_PORT`           | Port PostgreSQL        | `5433`         |
| `DB_NAME`           | Nom de la base         | `yowyob_erp`   |
| `POSTGRES_USER`     | Utilisateur PostgreSQL | `yowyob_admin` |
| `POSTGRES_PASSWORD` | Mot de passe           | `****`         |

### Sécurité

| Variable       | Description                                 |
|----------------|---------------------------------------------|
| `JWT_SECRET`   | Clé secrète pour la validation des tokens   |
| `AUTH_API_URL` | URL de l'API d'authentification externe     |

### Services optionnels

| Variable                       | Description                    | Défaut  |
|--------------------------------|--------------------------------|---------|
| `SPRING_KAFKA_ENABLED`         | Active/désactive Kafka         | `false` |
| `SPRING_ELASTICSEARCH_ENABLED` | Active/désactive Elasticsearch | `false` |
| `SPRING_CACHE_TYPE`            | Type de cache                  | `none`  |
| `REDIS_URL`                    | URL Redis                      | —       |

---

## ☁️ Déploiement sur Render

Le fichier `render.yaml` à la racine du `BACKEND/` permet un déploiement automatique.

```bash
# Déployer en poussant sur la branche principale
git push origin main
```

> **Note :** Sur le plan gratuit Render, Kafka, Redis et Elasticsearch sont désactivés. La base PostgreSQL est provisionnée automatiquement.

Voir [`docs/guides/DEPLOYMENT.md`](docs/guides/DEPLOYMENT.md) pour les instructions détaillées.

---

## 📚 Documentation API

Swagger UI : `http://localhost:8081/swagger-ui`

| Module                | Préfixe API                              |
|-----------------------|------------------------------------------|
| Plan comptable        | `/api/accounting/plan-comptable`         |
| Journaux comptables   | `/api/accounting/journaux`               |
| Écritures comptables  | `/api/accounting/ecritures`              |
| Périodes comptables   | `/api/accounting/periodes`               |
| Exercices fiscaux     | `/api/accounting/exercices`              |
| Rapports financiers   | `/api/accounting/rapports`               |
| Déclarations fiscales | `/api/accounting/declarations`           |
| Lettrage              | `/api/accounting/lettrage`               |
| Journal d'audit       | `/api/accounting/audit`                  |
| Budget                | `/api/accounting/budget`                 |
| Immobilisations       | `/api/accounting/immobilisations`        |
| Brouillards           | `/api/accounting/brouillards`            |

---

## 🤝 Contribuer

1. Forker le dépôt
2. Créer une branche : `git checkout -b feature/ma-fonctionnalite`
3. Commiter (convention [Conventional Commits](https://www.conventionalcommits.org/)) : `git commit -m "feat: description"`
4. Pousser : `git push origin feature/ma-fonctionnalite`
5. Ouvrir une Pull Request

---

## 📄 Licence

Ce projet est sous licence MIT. Voir le fichier [LICENSE](LICENSE) pour plus de détails.