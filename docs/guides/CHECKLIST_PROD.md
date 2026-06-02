# ✅ Checklist de Mise en Production — YOWYOB ERP Backend

> À compléter avant chaque déploiement en production.
> Cochez chaque élément avec `[x]` après vérification.

---

## 🔴 Phase 1 — KSM_Kernel_Layer (Prérequis)

### Infrastructure
- [ ] PostgreSQL `iwm-postgres` est `healthy`
- [ ] Redis `iwm-redis` est `healthy`
- [ ] Kafka `iwm-kafka` est `healthy`
- [ ] Elasticsearch `iwm-elasticsearch` est `healthy`
- [ ] Réseau `iwm-network` est créé : `docker network inspect iwm-network`

### Application Kernel
- [ ] Service `app` (RT-comops-bootstrap) est `healthy`
- [ ] Health check Kernel OK : `curl http://localhost:8080/actuator/health`
- [ ] Gateway Nginx est up : `curl http://localhost:8080/healthz`

---

## 🟡 Phase 2 — Préparation BACKEND

### Configuration
- [ ] `.env.prod` créé et complété (copié depuis `.env.prod.example`)
- [ ] Aucune valeur `CHANGE_ME` restante dans `.env.prod`
- [ ] `AUTH_API_URL` pointe vers le Kernel (`http://app:8080` ou URL publique)
- [ ] `KAFKA_BOOTSTRAP` utilise le port **interne** `29092` (pas `9092`)
- [ ] `DB_HOST=iwm-postgres` (nom de conteneur Docker, pas `localhost`)
- [ ] `TENANT_DEFAULT` est un UUID valide

### Secrets Docker
- [ ] `ops/secrets/prod/erp_postgres_password.txt` existe et contient un mot de passe fort
- [ ] `ops/secrets/prod/erp_jwt_secret.txt` existe et contient une clé ≥ 64 chars
- [ ] `ops/secrets/prod/erp_redis_password.txt` existe (peut être vide)
- [ ] Permissions restrictives : `chmod 600 ops/secrets/prod/*`

### Base de données
- [ ] Utilisateur PostgreSQL `yowyob_admin` créé sur `iwm-postgres`
- [ ] Base `yowyob_erp` créée : `psql -c "SELECT datname FROM pg_database WHERE datname='yowyob_erp'"`
- [ ] L'utilisateur `yowyob_admin` a les droits sur `yowyob_erp`

### Scripts
- [ ] Scripts exécutables : `chmod +x scripts/*.sh`

---

## 🟢 Phase 3 — Déploiement BACKEND

### Validation
- [ ] `./scripts/validate-env.sh` passe sans erreur
- [ ] Réseau `iwm-network` vérifié dans le script

### Build
- [ ] Image Docker buildée sans erreur : `docker build -t yowyob-erp-backend:latest .`
- [ ] Taille de l'image raisonnable (< 400 Mo) : `docker images yowyob-erp-backend`

### Démarrage
- [ ] `./scripts/deploy-prod.sh` exécuté sans erreur
- [ ] Conteneur `yowyob-erp-backend` est en cours d'exécution : `docker ps`
- [ ] Statut `healthy` après ~90s : `docker ps | grep yowyob`

---

## 🔵 Phase 4 — Vérification Post-Déploiement

### Health & Santé
- [ ] Health check Spring Boot OK :
      ```bash
      curl http://localhost:8081/actuator/health | python3 -m json.tool
      ```
- [ ] Composants health OK (DB, Redis, Kafka) dans la réponse
- [ ] Pas d'erreurs dans les logs : `docker logs yowyob-erp-backend --tail 100`

### API
- [ ] Swagger UI accessible : `http://localhost:8081/swagger-ui`
- [ ] Endpoint plan comptable répond :
      ```bash
      curl -H "Authorization: Bearer <token>" \
           http://localhost:8081/api/accounting/plan-comptable
      ```
- [ ] Authentification JWT fonctionne (token du Kernel accepté)

### Migrations Liquibase
- [ ] Toutes les migrations sont appliquées :
      ```bash
      docker exec iwm-postgres psql -U yowyob_admin -d yowyob_erp \
        -c "SELECT COUNT(*) FROM databasechangelog;"
      # Doit retourner 21 (changesets 001 à 021)
      ```

### Multi-tenant
- [ ] L'en-tête `X-Organization-ID` est requis et respecté
- [ ] Isolation des données entre organisations vérifiée

---

## 🟣 Phase 5 — Observabilité

- [ ] Métriques Actuator accessibles : `curl http://localhost:8081/actuator/metrics`
- [ ] Logs structurés en JSON : `docker logs yowyob-erp-backend | head -5 | python3 -m json.tool`
- [ ] (Optionnel) Dashboard Grafana BACKEND ajouté dans le Grafana du Kernel

---

## 🔐 Sécurité

- [ ] `.env.prod` n'est **pas** commité dans Git : `git status | grep .env.prod`
- [ ] `ops/secrets/prod/` n'est **pas** commité dans Git
- [ ] Port `8081` accessible uniquement depuis le réseau interne (ou via reverse proxy)
- [ ] Logs ne contiennent pas de mots de passe ou de tokens JWT

---

## 📋 Informations à documenter après déploiement

| Information | Valeur |
|-------------|--------|
| Date de déploiement | |
| Version deployée (git tag/commit) | `git rev-parse --short HEAD` |
| Image Docker tag | |
| Nombre de migrations Liquibase | |
| URL d'accès production | |
| Responsable du déploiement | |

---

## 🔄 En cas de problème — Rollback rapide

```bash
# 1. Arrêter le BACKEND
docker compose -f docker-compose.prod.yml down

# 2. Revenir à la version précédente
export ERP_IMAGE_TAG=<tag-version-stable>
./scripts/deploy-prod.sh --skip-build

# 3. Vérifier
curl http://localhost:8081/actuator/health
```
