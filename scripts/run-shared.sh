#!/usr/bin/env bash
# ============================================================================
# run-shared.sh
# ----------------------------------------------------------------------------
# Lance le backend Yowyob ERP DIRECTEMENT sur l'hote (sans Docker), branche
# sur l'infra deja lancee par KSM_Kernel_Layer via les ports publies sur
# localhost. Utile quand le build Docker est bloque (pull des images de base
# impossible).
#
#   postgres      -> localhost:5433  (base dediee "yowyob_erp")
#   redis         -> localhost:6380  (SANS password)
#   kafka         -> localhost:9092  (listener PLAINTEXT_HOST)
#   elasticsearch -> localhost:9200
#
# Usage :
#   ./scripts/run-shared.sh            # build (si besoin) puis run
#   SKIP_BUILD=1 ./scripts/run-shared.sh   # run sans rebuild
# ============================================================================
set -euo pipefail
cd "$(dirname "$0")/.."

# --- Charger les variables non-infra du .env (JWT, AUTH_API, ORG, ...) ---
if [[ -f .env ]]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
fi

# --- Surcharges infra : pointer vers les ports publies des services iwm ---
export SPRING_PROFILES_ACTIVE=prod
export SERVER_PORT=8081

export DB_HOST=localhost
export DB_PORT=5433
export DB_NAME=yowyob_erp
export POSTGRES_USER=yowyob_admin
export POSTGRES_PASSWORD=yowyob_secret

# Redis partage : pas de mot de passe -> url sans credentials
export REDIS_URL=redis://localhost:6380
export REDIS_HOST=localhost
export REDIS_PORT=6380
export REDIS_PASSWORD=

export KAFKA_BOOTSTRAP=localhost:9092
export ELASTICSEARCH_URI=http://localhost:9200

# --- Build du JAR si absent ---
JAR=$(ls -1 target/*.jar 2>/dev/null | head -1 || true)
if [[ -z "${JAR}" && "${SKIP_BUILD:-0}" != "1" ]]; then
  echo ">> Build du JAR (./mvnw clean package -DskipTests)..."
  ./mvnw -q clean package -DskipTests
  JAR=$(ls -1 target/*.jar 2>/dev/null | head -1)
fi

echo ">> Lancement : ${JAR}"
echo ">> DB=${DB_HOST}:${DB_PORT}/${DB_NAME}  REDIS=localhost:6380  KAFKA=${KAFKA_BOOTSTRAP}  ES=${ELASTICSEARCH_URI}"
exec java -jar "${JAR}"
