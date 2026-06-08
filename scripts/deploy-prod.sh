#!/usr/bin/env bash
# ==============================================================
# deploy-prod.sh — Déploiement Production YOWYOB ERP Backend -ACCOUNTING
# ==============================================================
# Ce script orchestre le déploiement complet du BACKEND en
# s'assurant que KSM_Kernel_Layer est déjà opérationnel.
#
# Prérequis :
#   - KSM_Kernel_Layer démarré (iwm-network disponible)
#   - .env.prod configuré
#   - ops/secrets/prod/*.txt créés
#   - Docker installé
#
# Usage :
#   ./scripts/deploy-prod.sh [--skip-build] [--skip-validation]
# ==============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="${SCRIPT_DIR}/.."
ENV_FILE="${ROOT_DIR}/.env.prod"
COMPOSE_PROD="${ROOT_DIR}/docker-compose.prod.yml"
IMAGE_NAME="yowyob-erp-backend"
IMAGE_TAG="${ERP_IMAGE_TAG:-latest}"

SKIP_BUILD=false
SKIP_VALIDATION=false

# ── Couleurs ──────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
BLUE='\033[0;34m'; BOLD='\033[1m'; NC='\033[0m'

step()    { echo -e "\n${BOLD}${BLUE}▶ $*${NC}"; }
ok()      { echo -e "  ${GREEN}✅ $*${NC}"; }
warn()    { echo -e "  ${YELLOW}⚠️  $*${NC}"; }
error()   { echo -e "  ${RED}❌ $*${NC}"; }
die()     { error "$*"; exit 1; }

# ── Arguments ─────────────────────────────────────────────────
while [[ $# -gt 0 ]]; do
  case "$1" in
    --skip-build)      SKIP_BUILD=true ;;
    --skip-validation) SKIP_VALIDATION=true ;;
    -h|--help)
      echo "Usage: $0 [--skip-build] [--skip-validation]"
      echo ""
      echo "  --skip-build       Ne pas rebuilder l'image Docker (utilise la dernière)"
      echo "  --skip-validation  Ignorer la validation des variables d'environnement"
      exit 0
      ;;
    *) die "Argument inconnu : $1" ;;
  esac
  shift
done

# ──────────────────────────────────────────────────────────────
echo ""
echo -e "${BOLD}╔══════════════════════════════════════════════════════╗${NC}"
echo -e "${BOLD}║   🚀 YOWYOB ERP Backend-ACCOUNTING  — Déploiement Production    ║${NC}"
echo -e "${BOLD}╚══════════════════════════════════════════════════════╝${NC}"
echo ""

# ── Prérequis système ─────────────────────────────────────────
step "Vérification des prérequis..."

command -v docker > /dev/null 2>&1 || die "Docker n'est pas installé."
ok "Docker $(docker --version | cut -d' ' -f3 | tr -d ',')"

[[ -f "${COMPOSE_PROD}" ]] || die "docker-compose.prod.yml introuvable : ${COMPOSE_PROD}"
ok "docker-compose.prod.yml trouvé"

# ── Fichier .env.prod ─────────────────────────────────────────
step "Chargement de la configuration..."
if [[ -f "${ENV_FILE}" ]]; then
  ok ".env.prod trouvé : ${ENV_FILE}"
  # shellcheck disable=SC1090
  set -a; source "${ENV_FILE}"; set +a
else
  die ".env.prod introuvable. Copiez .env.prod.example vers .env.prod et configurez-le."
fi

# ── Validation de l'environnement ─────────────────────────────
if [[ "${SKIP_VALIDATION}" == "false" ]]; then
  step "Validation des variables d'environnement..."
  "${SCRIPT_DIR}/validate-env.sh" --env-file "${ENV_FILE}" || \
    die "Validation échouée. Corrigez les erreurs avant de continuer."
  ok "Environnement valide"
else
  warn "Validation ignorée (--skip-validation)"
fi

# ── Vérification du réseau Kernel ─────────────────────────────
step "Vérification du réseau KSM_Kernel_Layer (iwm-network)..."
if ! docker network inspect iwm-network > /dev/null 2>&1; then
  die "Le réseau Docker 'iwm-network' n'existe pas.
       Assurez-vous que KSM_Kernel_Layer est démarré :
         cd KSM_Kernel_Layer && ./scripts/start-prod-stack.sh"
fi
ok "Réseau iwm-network disponible"

# ── Vérification santé du Kernel ──────────────────────────────
step "Vérification de la santé du KSM_Kernel_Layer..."
KERNEL_HEALTH_URL="${AUTH_API_URL:-http://app:8080}/actuator/health"

# Tenter via un conteneur temporaire sur iwm-network
if docker run --rm --network iwm-network \
     curlimages/curl:8.8.0 -fsS "${KERNEL_HEALTH_URL}" > /dev/null 2>&1; then
  ok "KSM_Kernel_Layer est healthy (${KERNEL_HEALTH_URL})"
else
  warn "Impossible de vérifier la santé du Kernel via ${KERNEL_HEALTH_URL}"
  warn "Le déploiement continue — le BACKEND réessaiera au démarrage."
fi

# ── Vérification de la base de données Kernel partagée ─────────
step "Vérification de la base de données Kernel partagée..."
DB_HOST_VAL="${DB_HOST:-iwm-postgres}"
DB_PORT_VAL="${DB_PORT:-5432}"
DB_NAME_VAL="${DB_NAME:-iwm}"
PG_USER="${POSTGRES_USER:-iwm}"

# Lire le mot de passe depuis le fichier secret si disponible
PG_PASS_FILE="${ROOT_DIR}/ops/secrets/prod/erp_postgres_password.txt"
if [[ -f "${PG_PASS_FILE}" ]]; then
  PG_PASS="$(cat "${PG_PASS_FILE}")"
else
  PG_PASS="${POSTGRES_PASSWORD:-}"
fi

if docker run --rm --network iwm-network \
     -e PGPASSWORD="${PG_PASS}" \
     postgres:18.3-alpine \
     psql -h "${DB_HOST_VAL}" -p "${DB_PORT_VAL}" -U "${PG_USER}" -tc \
     "SELECT 1 FROM pg_database WHERE datname='${DB_NAME_VAL}'" | grep -q 1 2>/dev/null; then
  ok "Base de données '${DB_NAME_VAL}' existe déjà"
else
  warn "Base '${DB_NAME_VAL}' inexistante — création..."
  docker run --rm --network iwm-network \
    -e PGPASSWORD="${PG_PASS}" \
    postgres:18.3-alpine \
    psql -h "${DB_HOST_VAL}" -p "${DB_PORT_VAL}" -U "${PG_USER}" \
    -c "CREATE DATABASE ${DB_NAME_VAL} ENCODING='UTF8' LC_COLLATE='fr_FR.UTF-8' LC_CTYPE='fr_FR.UTF-8' TEMPLATE=template0;" \
    2>/dev/null || \
  docker run --rm --network iwm-network \
    -e PGPASSWORD="${PG_PASS}" \
    postgres:18.3-alpine \
    psql -h "${DB_HOST_VAL}" -p "${DB_PORT_VAL}" -U "${PG_USER}" \
    -c "CREATE DATABASE ${DB_NAME_VAL};"
  ok "Base de données '${DB_NAME_VAL}' créée"
fi

# ── Build de l'image Docker ────────────────────────────────────
if [[ "${SKIP_BUILD}" == "false" ]]; then
  step "Construction de l'image Docker ${IMAGE_NAME}:${IMAGE_TAG}..."
  cd "${ROOT_DIR}"
  DOCKER_BUILDKIT=1 docker build \
    --tag "${IMAGE_NAME}:${IMAGE_TAG}" \
    --tag "${IMAGE_NAME}:latest" \
    --label "build.date=$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
    --label "build.commit=$(git rev-parse --short HEAD 2>/dev/null || echo 'unknown')" \
    .
  ok "Image ${IMAGE_NAME}:${IMAGE_TAG} construite"
else
  warn "Build ignoré (--skip-build) — utilisation de l'image existante"
fi

# ── Déploiement ───────────────────────────────────────────────
step "Déploiement du BACKEND..."
cd "${ROOT_DIR}"

docker compose \
  -f "${COMPOSE_PROD}" \
  --env-file "${ENV_FILE}" \
  up -d --remove-orphans

ok "Conteneur yowyob-erp-backend démarré"

# ── Attente et vérification ───────────────────────────────────
step "Vérification du démarrage (max 120s)..."
HEALTH_URL="http://localhost:${SERVER_PORT:-8081}/actuator/health"
MAX_WAIT=120
ELAPSED=0
INTERVAL=5

until curl -fsS "${HEALTH_URL}" > /dev/null 2>&1; do
  ELAPSED=$((ELAPSED + INTERVAL))
  if [[ "${ELAPSED}" -ge "${MAX_WAIT}" ]]; then
    error "BACKEND n'a pas démarré après ${MAX_WAIT}s."
    echo ""
    echo "📋 Logs du conteneur :"
    docker logs yowyob-erp-backend --tail 50
    die "Déploiement échoué — consultez les logs ci-dessus."
  fi
  echo "  ⏳ En attente... (${ELAPSED}s/${MAX_WAIT}s)"
  sleep "${INTERVAL}"
done

echo ""
echo -e "${BOLD}${GREEN}╔══════════════════════════════════════════════════════╗${NC}"
echo -e "${BOLD}${GREEN}║   ✅ Déploiement réussi !                           ║${NC}"
echo -e "${BOLD}${GREEN}╚══════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "  🌐 API REST   : ${GREEN}http://localhost:${SERVER_PORT:-8081}${NC}"
echo -e "  📖 Swagger UI : ${GREEN}http://localhost:${SERVER_PORT:-8081}/swagger-ui${NC}"
echo -e "  🩺 Health     : ${GREEN}${HEALTH_URL}${NC}"
echo ""
echo "  Commandes utiles :"
echo "    Logs         : docker logs yowyob-erp-backend -f"
echo "    Arrêt        : docker compose -f docker-compose.prod.yml down"
echo "    Redémarrage  : docker compose -f docker-compose.prod.yml restart"
echo ""
