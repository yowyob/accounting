#!/usr/bin/env bash
# ==============================================================
# wait-for-services.sh — Attente de disponibilité des services
# YOWYOB ERP Backend-ACCOUNTING — Production
# ==============================================================
# Attend que les services du KSM_Kernel_Layer soient accessibles
# avant de lancer le BACKEND-ACCOUNTING.
#
# Usage (dans Dockerfile ENTRYPOINT) :
#   wait-for-services.sh && java -jar app.jar
#
# Variables d'environnement utilisées :
#   DB_HOST, DB_PORT          — PostgreSQL
#   REDIS_HOST, REDIS_PORT    — Redis
#   KAFKA_BOOTSTRAP           — Kafka (host:port)
#   ELASTICSEARCH_URI         — Elasticsearch (http://host:port)
# ==============================================================
set -euo pipefail

# ── Paramètres ─────────────────────────────────────────────
MAX_RETRIES="${WAIT_MAX_RETRIES:-30}"
RETRY_INTERVAL="${WAIT_RETRY_INTERVAL:-5}"  # secondes

# ── Couleurs terminal ───────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'

log_info()    { echo -e "${GREEN}[WAIT]${NC} $*"; }
log_warn()    { echo -e "${YELLOW}[WAIT]${NC} $*"; }
log_error()   { echo -e "${RED}[WAIT]${NC} $*"; }

# ── Fonction de wait TCP ────────────────────────────────────
wait_for_tcp() {
  local service_name="$1"
  local host="$2"
  local port="$3"
  local attempt=0

  log_info "Attente de ${service_name} sur ${host}:${port}..."

  until nc -z "${host}" "${port}" > /dev/null 2>&1; do
    attempt=$((attempt + 1))
    if [[ "${attempt}" -ge "${MAX_RETRIES}" ]]; then
      log_error "${service_name} (${host}:${port}) inaccessible après ${MAX_RETRIES} tentatives. Abandon."
      exit 1
    fi
    log_warn "  ${service_name} pas encore prêt (tentative ${attempt}/${MAX_RETRIES}) — retry dans ${RETRY_INTERVAL}s..."
    sleep "${RETRY_INTERVAL}"
  done

  log_info "  ✅ ${service_name} est disponible sur ${host}:${port}"
}

# ── Fonction de wait HTTP ───────────────────────────────────
wait_for_http() {
  local service_name="$1"
  local url="$2"
  local attempt=0

  log_info "Attente de ${service_name} sur ${url}..."

  until curl -fsS "${url}" > /dev/null 2>&1; do
    attempt=$((attempt + 1))
    if [[ "${attempt}" -ge "${MAX_RETRIES}" ]]; then
      log_error "${service_name} (${url}) inaccessible après ${MAX_RETRIES} tentatives. Abandon."
      exit 1
    fi
    log_warn "  ${service_name} pas encore prêt (tentative ${attempt}/${MAX_RETRIES}) — retry dans ${RETRY_INTERVAL}s..."
    sleep "${RETRY_INTERVAL}"
  done

  log_info "  ✅ ${service_name} répond sur ${url}"
}

# ──────────────────────────────────────────────────────────────
echo ""
log_info "══════════════════════════════════════════════════════"
log_info " YOWYOB ERP — Vérification des services dépendants"
log_info "══════════════════════════════════════════════════════"
echo ""

# ── 1. PostgreSQL ─────────────────────────────────────────────
PG_HOST="${DB_HOST:-iwm-postgres}"
PG_PORT="${DB_PORT:-5432}"
wait_for_tcp "PostgreSQL" "${PG_HOST}" "${PG_PORT}"

# ── 2. Redis ──────────────────────────────────────────────────
if [[ "${SPRING_CACHE_TYPE:-none}" != "none" ]]; then
  REDIS_HOST_VAL="${REDIS_HOST:-iwm-redis}"
  REDIS_PORT_VAL="${REDIS_PORT:-6379}"
  wait_for_tcp "Redis" "${REDIS_HOST_VAL}" "${REDIS_PORT_VAL}"
else
  log_info "Redis désactivé (SPRING_CACHE_TYPE=none) — skip."
fi

# ── 3. Kafka ──────────────────────────────────────────────────
if [[ "${SPRING_KAFKA_ENABLED:-false}" == "true" ]]; then
  KAFKA_HOST="${KAFKA_BOOTSTRAP:-iwm-kafka:29092}"
  KAFKA_HOST_ONLY="${KAFKA_HOST%%:*}"
  KAFKA_PORT_ONLY="${KAFKA_HOST##*:}"
  wait_for_tcp "Kafka" "${KAFKA_HOST_ONLY}" "${KAFKA_PORT_ONLY}"
else
  log_info "Kafka désactivé (SPRING_KAFKA_ENABLED=false) — skip."
fi

# ── 4. Elasticsearch ──────────────────────────────────────────
if [[ "${SPRING_ELASTICSEARCH_ENABLED:-false}" == "true" ]]; then
  ES_URI="${ELASTICSEARCH_URI:-http://iwm-elasticsearch:9200}"
  wait_for_http "Elasticsearch" "${ES_URI}/_cluster/health"
else
  log_info "Elasticsearch désactivé (SPRING_ELASTICSEARCH_ENABLED=false) — skip."
fi

# ── 5. Auth API (Kernel) ───────────────────────────────────────
AUTH_URL="${AUTH_API_URL:-}"
if [[ -n "${AUTH_URL}" ]]; then
  wait_for_http "Kernel Auth API" "${AUTH_URL}/actuator/health"
else
  log_warn "AUTH_API_URL non défini — vérification Auth API ignorée."
fi

echo ""
log_info "══════════════════════════════════════════════════════"
log_info " ✅ Tous les services sont disponibles — démarrage du BACKEND-ACCOUNTING"
log_info "══════════════════════════════════════════════════════"
echo ""
