#!/usr/bin/env bash
# ============================================================================
# run-shared.sh
# ----------------------------------------------------------------------------
# Lance le backend Yowyob ERP DIRECTEMENT sur l'hote (sans Docker), branche
# sur l'infra deja lancee par KSM_Kernel_Layer via les ports publies sur
# localhost. Utile quand le build Docker est bloque (pull des images de base
# impossible).
#
#   postgres      -> localhost:5433  (base Kernel "iwm", schema "public")
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
export DB_NAME=iwm
export POSTGRES_USER=iwm
export POSTGRES_PASSWORD=iwm

# Redis partage : pas de mot de passe -> url sans credentials
export REDIS_URL=redis://localhost:6380
export REDIS_HOST=localhost
export REDIS_PORT=6380
export REDIS_PASSWORD=

export KAFKA_BOOTSTRAP=localhost:9092
export ELASTICSEARCH_URI=http://localhost:9200

# --- S'assurer que l'infra partagee KSM_Kernel (conteneurs iwm-*) tourne ---
# Si les conteneurs existent mais sont arretes (ex: apres reboot/arret Docker),
# on les redemarre automatiquement puis on attend que Postgres accepte les
# connexions avant de lancer le backend (sinon Liquibase echoue au demarrage).
INFRA_CONTAINERS=(iwm-postgres iwm-redis iwm-kafka iwm-elasticsearch)
if command -v docker >/dev/null 2>&1; then
  to_start=()
  for c in "${INFRA_CONTAINERS[@]}"; do
    # le conteneur existe-t-il ?
    if docker ps -a --format '{{.Names}}' | grep -qx "${c}"; then
      # tourne-t-il deja ?
      if ! docker ps --format '{{.Names}}' | grep -qx "${c}"; then
        to_start+=("${c}")
      fi
    else
      echo ">> ATTENTION : conteneur '${c}' introuvable. Lance d'abord l'infra KSM_Kernel_Layer." >&2
    fi
  done

  if [[ ${#to_start[@]} -gt 0 ]]; then
    echo ">> Demarrage des conteneurs infra arretes : ${to_start[*]}"
    docker start "${to_start[@]}" >/dev/null
  fi

  # Attendre que Postgres accepte les connexions (max ~40s)
  if docker ps --format '{{.Names}}' | grep -qx "iwm-postgres"; then
    echo -n ">> Attente de Postgres "
    for _ in $(seq 1 20); do
      if docker exec iwm-postgres pg_isready -U "${POSTGRES_USER}" -d "${DB_NAME}" 2>/dev/null | grep -q 'accepting'; then
        echo " OK"
        break
      fi
      echo -n "."
      sleep 2
    done
  fi
else
  echo ">> ATTENTION : docker introuvable, impossible de verifier l'infra partagee." >&2
fi

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
