# Procédure Production Kernel + BACKEND

Ce guide décrit la configuration à appliquer sur un VPS ou serveur de production pour faire communiquer `BACKEND-ACCOUNTING` avec `KSM_Kernel_Layer`.

Objectif :

- Le Kernel démarre l'infrastructure partagée.
- Le `BACKEND-ACCOUNTING` rejoint le même réseau Docker.
- Le `BACKEND-ACCOUNTING` utilise la même base PostgreSQL et le même schéma que le Kernel.
- L'authentification du `BACKEND-ACCOUNTING` passe par le Kernel.

## Architecture Cible

```text
KSM_Kernel_Layer
  ├─ app:8080       API Kernel / auth / JWKS
  ├─ app:8081       actuator / health management
  ├─ iwm-postgres   database iwm, schema public
  ├─ iwm-redis
  ├─ iwm-kafka
  └─ iwm-elasticsearch

BACKEND
  ├─ yowyob-erp-backend:8081
  ├─ rejoint iwm-network
  ├─ DB: postgres:5432/iwm
  └─ auth: http://app:8080
```

## 1. Configuration Kernel

Dans `KSM_Kernel_Layer/.env.prod` ou via les secrets Docker, configurer :

```env
IWM_JWT_ISSUER=iwm-backend

IWM_BOOTSTRAP_CLIENT_ENABLED=true
IWM_BOOTSTRAP_CLIENT_ID=prod-platform-backend
IWM_BOOTSTRAP_CLIENT_SECRET=CHANGE_ME_LONG_STRONG_SECRET

IWM_MANAGEMENT_API_KEY=CHANGE_ME_LONG_MANAGEMENT_KEY_MIN_16_CHARS
```

Contraintes importantes :

- `IWM_MANAGEMENT_API_KEY` doit faire au moins 16 caractères.
- `IWM_BOOTSTRAP_CLIENT_SECRET` doit être identique au `KERNEL_CLIENT_SECRET` configuré côté `BACKEND`.
- Ne jamais utiliser en production les valeurs `replace-me`, `dev-api-key`, `dev-platform-backend` ou `dev-management-key`.

## 2. Démarrer le Kernel

Depuis le serveur :

```bash
cd KSM_Kernel_Layer
docker compose -f docker-compose.infrastructure.yml -f docker-compose.application.yml up -d
```

Vérifier que les services sont actifs :

```bash
docker ps --format 'table {{.Names}}\t{{.Status}}'
```

Services attendus :

- `ksm_kernel_layer-app-1`
- `iwm-postgres`
- `iwm-redis`
- `iwm-kafka`
- `iwm-elasticsearch`

Le réseau Docker attendu est :

```bash
docker network inspect iwm-network
```

## 3. Configuration BACKEND-ACCOUNTING

Dans `BACKEND/.env.prod`, configurer :

```env
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8081

DB_HOST=iwm-postgres
DB_PORT=5432
DB_NAME=iwm
POSTGRES_USER=iwm
POSTGRES_PASSWORD=CHANGE_ME_KERNEL_POSTGRES_PASSWORD

AUTH_API_URL=http://app:8080
AUTH_API_HEALTH_URL=http://app:8081/actuator/health
AUTH_API_TIMEOUT=5000
AUTH_API_RETRIES=3
AUTH_JWT_ISSUER=iwm-backend
AUTH_MOCK_ENABLED=false

KERNEL_CLIENT_ID=prod-platform-backend
KERNEL_CLIENT_SECRET=CHANGE_ME_SAME_AS_IWM_BOOTSTRAP_CLIENT_SECRET

REDIS_HOST=iwm-redis
REDIS_PORT=6379
SPRING_CACHE_TYPE=redis

KAFKA_BOOTSTRAP=iwm-kafka:29092
SPRING_KAFKA_ENABLED=true

ELASTICSEARCH_URI=http://iwm-elasticsearch:9200
SPRING_ELASTICSEARCH_ENABLED=true
```

Points clés :

- `DB_NAME=iwm`
- `POSTGRES_USER=iwm`
- Schéma utilisé : `public`
- `AUTH_MOCK_ENABLED=false` en production
- `KERNEL_CLIENT_SECRET` doit être le même que `IWM_BOOTSTRAP_CLIENT_SECRET`

## 4. Démarrer le BACKEND

Depuis le serveur :

```bash
cd BACKEND
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d
```

Vérifier le conteneur :

```bash
docker ps --format 'table {{.Names}}\t{{.Status}}'
docker logs --tail 100 yowyob-erp-backend
```

## 5. Vérifications Réseau

Depuis le conteneur `BACKEND`, tester la communication avec le Kernel :

```bash
docker exec yowyob-erp-backend curl -i http://app:8080/.well-known/jwks.json
docker exec yowyob-erp-backend curl -i http://app:8081/actuator/health
```

Résultats attendus :

- `/.well-known/jwks.json` retourne `200 OK`.
- `/actuator/health` retourne un statut OK sur le port management `8081`.

## 6. Vérifier l'auth BACKEND -> Kernel

Depuis le serveur :

```bash
curl http://localhost:8081/api/auth/health
```

Résultat attendu :

```json
{
  "mode": "external",
  "externalAuthAvailable": true
}
```

Si le résultat indique `mode: mock`, alors le `BACKEND` ne joint pas correctement le Kernel.

## 7. Vérifier Base et Schéma

Vérifier que `BACKEND` et Kernel utilisent bien la même base :

```bash
docker exec iwm-postgres psql -U iwm -d iwm -c "select current_database() as db, current_schema() as schema;"
```

Résultat attendu :

```text
 db  | schema
-----+--------
 iwm | public
```

Vérifier les variables effectives du conteneur backend :

```bash
docker inspect yowyob-erp-backend --format '{{range .Config.Env}}{{println .}}{{end}}' \
  | awk '/^(DB_NAME|POSTGRES_USER|DB_HOST|DB_PORT|AUTH_API_URL|AUTH_API_HEALTH_URL|KERNEL_CLIENT_ID)=/ {print}'
```

Valeurs attendues :

```text
DB_HOST=iwm-postgres
DB_PORT=5432
DB_NAME=iwm
POSTGRES_USER=iwm
AUTH_API_URL=http://app:8080
AUTH_API_HEALTH_URL=http://app:8081/actuator/health
KERNEL_CLIENT_ID=prod-platform-backend
```

## 8. Ordre de Démarrage Recommandé

```bash
# 1. Kernel + infra
cd KSM_Kernel_Layer
docker compose -f docker-compose.infrastructure.yml -f docker-compose.application.yml up -d

# 2. Attendre les healthchecks
docker ps --format 'table {{.Names}}\t{{.Status}}'

# 3. Backend
cd ../BACKEND
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d

# 4. Vérification finale
curl http://localhost:8081/api/auth/health
```

## 9. Problèmes Courants

### `mode: mock`

Cause probable :

- Kernel non démarré.
- `AUTH_API_URL` incorrect.
- `AUTH_API_HEALTH_URL` pointe vers `app:8080` au lieu de `app:8081`.
- Le backend n'est pas sur `iwm-network`.

Correction :

```bash
docker exec yowyob-erp-backend curl -i http://app:8080/.well-known/jwks.json
docker exec yowyob-erp-backend curl -i http://app:8081/actuator/health
```

### Kernel ne démarre pas : clé management trop courte

Erreur :

```text
iwm.management.security.api-key must be at least 16 characters long
```

Correction :

```env
IWM_MANAGEMENT_API_KEY=une-cle-longue-et-securisee
```

### Erreur PostgreSQL ou Liquibase

Vérifier que la base partagée existe :

```bash
docker exec iwm-postgres psql -U iwm -d iwm -c "\dt"
```

Le backend doit utiliser :

```env
DB_NAME=iwm
POSTGRES_USER=iwm
```

### DNS `app` introuvable depuis BACKEND

Cause probable :

- Le conteneur Kernel `app` n'est pas démarré.
- Les deux conteneurs ne sont pas sur `iwm-network`.

Vérifier :

```bash
docker ps -a --format 'table {{.Names}}\t{{.Status}}\t{{.Networks}}'
```

## 10. Règles Production

- Ne jamais activer `AUTH_MOCK_ENABLED=true` en production.
- Ne jamais utiliser les secrets dev.
- Toujours démarrer le Kernel avant le BACKEND.
- Garder `AUTH_API_URL=http://app:8080` pour les appels API/JWKS.
- Garder `AUTH_API_HEALTH_URL=http://app:8081/actuator/health` pour le health management.
- Utiliser la base `iwm` et le schéma `public` pour Kernel et BACKEND.
