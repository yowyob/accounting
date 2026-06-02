# 🌐 Guide du Réseau Docker — Architecture Bi-Projet

> Explication du modèle réseau Docker partagé entre **KSM_Kernel_Layer** et **BACKEND**.

---

## Schéma général

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                          HÔTE DOCKER (Serveur de production)                  │
│                                                                                │
│  ┌─────────────────────────── KSM_Kernel_Layer ──────────────────────────┐   │
│  │                                                                         │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                 │   │
│  │  │ iwm-postgres │  │  iwm-redis   │  │  iwm-kafka   │                 │   │
│  │  │ :5432        │  │  :6379       │  │  :29092      │                 │   │
│  │  └──────────────┘  └──────────────┘  └──────────────┘                 │   │
│  │  ┌───────────────────────┐  ┌─────────────────────────────────────┐   │   │
│  │  │ iwm-elasticsearch     │  │  app (RT-comops-bootstrap)           │   │   │
│  │  │ :9200                 │  │  :8080 (API) :8081 (management)      │   │   │
│  │  └───────────────────────┘  └─────────────────────────────────────┘   │   │
│  │  ┌───────────────────────┐  ┌─────────────────────────────────────┐   │   │
│  │  │ iwm-prometheus :9090  │  │  iwm-grafana :3000                  │   │   │
│  │  └───────────────────────┘  └─────────────────────────────────────┘   │   │
│  │                                                                         │   │
│  │           ╔════════════════════════════╗                               │   │
│  │           ║  Réseau : iwm-network      ║  ◄── Créé par le Kernel       │   │
│  │           ╚════════════════════════════╝                               │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                │                                                │
│                   external: true  (Le BACKEND rejoint ce réseau)               │
│                                │                                                │
│  ┌─────────────────────────── BACKEND ──────────────────────────────────┐   │
│  │                                                                         │   │
│  │  ┌─────────────────────────────────────────────────────────────────┐  │   │
│  │  │ yowyob-erp-backend (Spring Boot OHADA)                          │  │   │
│  │  │ :8081 (API REST + Actuator)                                      │  │   │
│  │  │                                                                   │  │   │
│  │  │  Connexions vers Kernel (via iwm-network) :                       │  │   │
│  │  │    PostgreSQL  → iwm-postgres:5432  (base yowyob_erp)            │  │   │
│  │  │    Redis       → iwm-redis:6379                                   │  │   │
│  │  │    Kafka       → iwm-kafka:29092    (listener interne)            │  │   │
│  │  │    Elasticsearch → iwm-elasticsearch:9200                         │  │   │
│  │  │    Auth API    → app:8080           (service Kernel)              │  │   │
│  │  └─────────────────────────────────────────────────────────────────┘  │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────────────────────────────┘
```

---

## Pourquoi un réseau partagé ?

| Approche | Avantage | Inconvénient |
|----------|----------|-------------|
| **Réseau partagé `iwm-network`** ✅ | Communication directe, latence ~0ms, noms DNS stables | Couplage réseau entre les deux projets |
| Exposition via ports hôte | Isolation totale | Latence réseau hôte, sécurité réduite |
| Reverse proxy Nginx entre projets | Isolation + SSL | Complexité, latence supplémentaire |

Le réseau partagé est la **solution recommandée** pour une production mono-serveur.

---

## Configuration réseau

### Dans KSM_Kernel_Layer — `docker-compose.infrastructure.yml`

```yaml
networks:
  iwm:
    name: iwm-network   # ← Le réseau est nommé "iwm-network"
```

Ce réseau est **créé par le Kernel** au premier `docker compose up`.

### Dans BACKEND — `docker-compose.prod.yml`

```yaml
networks:
  iwm-network:
    external: true      # ← Le BACKEND rejoint le réseau existant
    name: iwm-network   # ← Doit correspondre exactement
```

---

## Noms de services à utiliser dans `.env.prod`

| Service | Nom de conteneur | Port interne | Commentaire |
|---------|-----------------|-------------|-------------|
| PostgreSQL | `iwm-postgres` | `5432` | Base `yowyob_erp` (pas `iwm`) |
| Redis | `iwm-redis` | `6379` | Pas de mot de passe par défaut |
| Kafka | `iwm-kafka` | **`29092`** | ⚠️ Utiliser le port INTERNE, pas 9092 |
| Elasticsearch | `iwm-elasticsearch` | `9200` | |
| Kernel App | `app` | `8080` | Pour `AUTH_API_URL` |

> **⚠️ Kafka : port 29092 vs 9092**
>
> Kafka expose deux listeners dans `docker-compose.infrastructure.yml` :
> - `PLAINTEXT://0.0.0.0:29092` → listener **interne Docker** (entre conteneurs)
> - `PLAINTEXT_HOST://0.0.0.0:9092` → listener **externe hôte** (depuis le PC local)
>
> Le BACKEND étant **dans le réseau Docker**, il doit utiliser `iwm-kafka:29092`.

---

## Isolation des bases de données

Le BACKEND partage l'**instance PostgreSQL** du Kernel, mais utilise une **base séparée** :

```
iwm-postgres (conteneur)
  ├── base: iwm           ← Base du Kernel (NE PAS TOUCHER)
  └── base: yowyob_erp    ← Base du BACKEND ERP OHADA
```

Cette isolation garantit que :
- Les migrations Liquibase du BACKEND n'impactent pas le schéma du Kernel
- Les données ERP sont séparées des données du Kernel
- Une restauration peut être faite indépendamment

---

## Vérification du réseau

```bash
# Lister tous les conteneurs sur iwm-network
docker network inspect iwm-network --format '{{range .Containers}}{{.Name}} {{end}}'

# Tester la connectivité BACKEND → PostgreSQL Kernel
docker exec yowyob-erp-backend nc -zv iwm-postgres 5432

# Tester la connectivité BACKEND → Kafka Kernel
docker exec yowyob-erp-backend nc -zv iwm-kafka 29092

# Tester la connectivité BACKEND → Auth API Kernel
docker exec yowyob-erp-backend curl -fsS http://app:8080/actuator/health
```

---

## Multi-serveurs (déploiement distribué)

Si le Kernel et le BACKEND sont sur des **serveurs différents**, remplacez les noms de conteneurs par les IPs/hostnames publics du serveur Kernel :

```env
# .env.prod (BACKEND sur serveur B, Kernel sur serveur A)
DB_HOST=10.0.1.10           # IP privée du serveur A
REDIS_HOST=10.0.1.10
KAFKA_BOOTSTRAP=10.0.1.10:9092   # Port externe dans ce cas
ELASTICSEARCH_URI=http://10.0.1.10:9200
AUTH_API_URL=https://api.kernel.yowyob.com   # URL publique
```

> Dans ce cas, le réseau Docker partagé n'est plus utilisé, et `external: true` doit être retiré du `docker-compose.prod.yml`.
