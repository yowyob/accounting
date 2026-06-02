# 🚀 Guide de Déploiement Production

> **YOWYOB ERP Backend** — Déploiement en production après KSM_Kernel_Layer

---

## Vue d'ensemble

Le déploiement se fait en **deux phases séquentielles** :

```
Phase 1 ─► KSM_Kernel_Layer  (infrastructure + Kernel)
               ↓ expose iwm-network
Phase 2 ─► BACKEND           (ERP OHADA — rejoint iwm-network)
```

Le BACKEND réutilise **PostgreSQL, Redis, Kafka et Elasticsearch** du Kernel.
Il s'authentifie via l'API du Kernel (`AUTH_API_URL`).

---

## Prérequis

| Prérequis | Vérification |
|-----------|-------------|
| Docker ≥ 24 + Compose v2 | `docker compose version` |
| KSM_Kernel_Layer démarré | `docker network inspect iwm-network` |
| Accès au serveur cible | SSH ou accès local |
| 512 Mo RAM disponibles | `free -h` |
| Port 8081 libre | `ss -tlnp \| grep 8081` |

---

## Phase 1 — Déployer KSM_Kernel_Layer

> **Si le Kernel est déjà en production, passez directement à la Phase 2.**

```bash
# Se positionner dans KSM_Kernel_Layer
cd KSM_Kernel_Layer/

# Copier et configurer l'environnement production
cp .env.prod.example .env.prod
nano .env.prod   # remplir toutes les valeurs CHANGE_ME

# Créer les secrets Docker
mkdir -p ops/secrets/prod/
echo "MOT_DE_PASSE_FORT_ICI"          > ops/secrets/prod/iwm_r2dbc_password.txt
echo "MOT_DE_PASSE_FORT_ICI"          > ops/secrets/prod/iwm_liquibase_password.txt
echo "CLE_JWT_PRIVEE_RSA"             > ops/secrets/prod/jwt-private-key.pem
echo "CLE_API_MANAGEMENT"             > ops/secrets/prod/iwm_management_api_key.txt
echo "SECRET_CLIENT_BOOTSTRAP"        > ops/secrets/prod/iwm_bootstrap_client_secret.txt
chmod 600 ops/secrets/prod/*.txt ops/secrets/prod/*.pem

# Valider l'environnement
./scripts/validate-runtime-env.sh

# Démarrer le Kernel en production
./scripts/start-prod-stack.sh

# Vérifier que tout est healthy
docker ps --format "table {{.Names}}\t{{.Status}}"
```

**Attendez que tous les services soient `healthy` avant de passer à la Phase 2.**

```bash
# Vérifier le health du Kernel
curl http://localhost:8080/actuator/health | python3 -m json.tool
```

---

## Phase 2 — Déployer le BACKEND

### Étape 1 — Cloner et se positionner

```bash
cd KSM-ERP-YOWYOB/BACKEND/
```

### Étape 2 — Configurer l'environnement

```bash
# Copier le template
cp .env.prod.example .env.prod
```

Éditer `.env.prod` et remplir **toutes** les valeurs. Les variables critiques :

```bash
# Base de données (même PostgreSQL que le Kernel, base séparée)
DB_HOST=iwm-postgres          # nom de conteneur Docker du Kernel
DB_NAME=yowyob_erp            # base SÉPARÉE de la base "iwm" du Kernel
POSTGRES_USER=yowyob_admin
# → mot de passe via secret (voir étape 3)

# Auth API — URL interne du service app du Kernel
AUTH_API_URL=http://app:8080  # nom de service Docker interne

# Kafka — listener interne Docker (29092, pas 9092 !)
KAFKA_BOOTSTRAP=iwm-kafka:29092

# Multi-tenant
TENANT_DEFAULT=<uuid-de-votre-tenant-par-defaut>
```

### Étape 3 — Créer les secrets Docker

```bash
mkdir -p ops/secrets/prod/

# Mot de passe PostgreSQL pour yowyob_erp
echo "MOT_DE_PASSE_FORT_UNIQUE" > ops/secrets/prod/erp_postgres_password.txt

# Clé JWT (doit être compatible avec le Kernel si JWT partagé)
echo "CLE_JWT_BASE64_64_CHARS_MIN"  > ops/secrets/prod/erp_jwt_secret.txt

# Redis (laisser vide si pas de mot de passe configuré sur iwm-redis)
echo "" > ops/secrets/prod/erp_redis_password.txt

chmod 600 ops/secrets/prod/*.txt
```

> **Génération d'une clé JWT forte :**
> ```bash
> openssl rand -base64 64
> ```

### Étape 4 — Créer l'utilisateur PostgreSQL pour yowyob_erp

Le BACKEND a besoin de son propre utilisateur sur le PostgreSQL du Kernel :

```bash
# Se connecter au PostgreSQL du Kernel
docker exec -it iwm-postgres psql -U iwm

-- Dans psql :
CREATE USER yowyob_admin WITH PASSWORD 'MOT_DE_PASSE_FORT_UNIQUE';
CREATE DATABASE yowyob_erp OWNER yowyob_admin ENCODING 'UTF8';
GRANT ALL PRIVILEGES ON DATABASE yowyob_erp TO yowyob_admin;
\q
```

> **Note :** Le script `deploy-prod.sh` tente de créer la base automatiquement,
> mais il est recommandé de le faire manuellement pour contrôler les permissions.

### Étape 5 — Déployer

```bash
# Rendre les scripts exécutables
chmod +x scripts/*.sh

# Lancer le déploiement complet
./scripts/deploy-prod.sh
```

Le script effectue automatiquement :
1. ✅ Validation des variables d'environnement
2. ✅ Vérification que `iwm-network` existe
3. ✅ Vérification de la santé du Kernel
4. ✅ Création de la base `yowyob_erp` si absente
5. ✅ Build de l'image Docker
6. ✅ Démarrage du conteneur
7. ✅ Health check final

### Étape 6 — Vérification post-déploiement

```bash
# Statut du conteneur
docker ps | grep yowyob-erp-backend

# Health check Spring Boot
curl http://localhost:8081/actuator/health | python3 -m json.tool

# Logs en temps réel
docker logs yowyob-erp-backend -f

# Swagger UI (ouvrir dans un navigateur)
echo "http://localhost:8081/swagger-ui"

# Test smoke — Liste des journaux comptables
curl -H "X-Organization-ID: <votre-org-id>" \
     -H "Authorization: Bearer <token>" \
     http://localhost:8081/api/accounting/journaux
```

---

## Mise à jour (Rolling Update)

Pour déployer une nouvelle version sans interruption :

```bash
# Option 1 : Re-déploiement complet (inclut rebuild)
./scripts/deploy-prod.sh

# Option 2 : Rebuild uniquement, puis redémarrage
docker compose -f docker-compose.prod.yml build
docker compose -f docker-compose.prod.yml up -d

# Option 3 : Sans rebuild (si image déjà buildée)
./scripts/deploy-prod.sh --skip-build
```

---

## Rollback

```bash
# Arrêter le BACKEND
docker compose -f docker-compose.prod.yml down

# Revenir à la version précédente
export ERP_IMAGE_TAG=v1.2.3   # tag de la version stable
./scripts/deploy-prod.sh --skip-build
```

---

## Arrêt propre

```bash
# Arrêter le BACKEND uniquement (le Kernel continue)
docker compose -f docker-compose.prod.yml down

# Arrêter le BACKEND + volumes (ATTENTION : supprime les données)
docker compose -f docker-compose.prod.yml down -v
```

---

## Dépannage

### Le BACKEND ne démarre pas

```bash
# Voir les logs complets
docker logs yowyob-erp-backend --tail 200

# Erreur de connexion PostgreSQL ?
docker exec -it iwm-postgres psql -U yowyob_admin -d yowyob_erp -c "SELECT 1"

# Vérifier le réseau
docker network inspect iwm-network | grep -A5 yowyob
```

### Erreur Liquibase au démarrage

```bash
# Les migrations échouent souvent si la DB n'est pas proprement initialisée
# Vérifier l'état Liquibase
docker exec -it iwm-postgres psql -U yowyob_admin -d yowyob_erp \
  -c "SELECT id, description, exectype FROM databasechangelog ORDER BY orderexecuted;"
```

### Erreur JWT / Auth

Vérifier que `AUTH_API_URL` est accessible depuis le conteneur BACKEND :

```bash
docker exec yowyob-erp-backend \
  curl -fsS "${AUTH_API_URL}/actuator/health"
```

---

## Fichiers de référence

| Fichier | Rôle |
|---------|------|
| `docker-compose.prod.yml` | Compose production BACKEND |
| `.env.prod.example` | Template de configuration |
| `scripts/deploy-prod.sh` | Script de déploiement principal |
| `scripts/validate-env.sh` | Validation de la configuration |
| `scripts/wait-for-services.sh` | Attente des services dépendants |
| `docs/guides/GUIDE_RESEAU_DOCKER.md` | Schéma réseau Docker |
| `docs/guides/CHECKLIST_PROD.md` | Checklist de mise en production |
