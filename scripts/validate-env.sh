#!/usr/bin/env bash
# ==============================================================
# validate-env.sh — Validation des variables d'environnement
# YOWYOB ERP Backend-ACCOUNTING — Production
# ==============================================================
# Usage : ./scripts/validate-env.sh [--env-file <path>]
# ==============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${SCRIPT_DIR}/../.env.prod"

# Accepter un fichier .env personnalisé
if [[ "${1:-}" == "--env-file" && -n "${2:-}" ]]; then
  ENV_FILE="$2"
fi

# Charger le fichier .env si présent
if [[ -f "${ENV_FILE}" ]]; then
  # shellcheck disable=SC1090
  set -a; source "${ENV_FILE}"; set +a
fi

# ────────────────────────────────────────────────────────────
# Variables OBLIGATOIRES
# ────────────────────────────────────────────────────────────
required_vars=(
  SPRING_PROFILES_ACTIVE
  DB_HOST
  DB_PORT
  DB_NAME
  POSTGRES_USER
  AUTH_API_URL
  AUTH_API_HEALTH_URL
  AUTH_JWT_ISSUER
  AUTH_MOCK_ENABLED
  KERNEL_CLIENT_ID
  KERNEL_CLIENT_SECRET
  TENANT_DEFAULT
)

# ────────────────────────────────────────────────────────────
# Valeurs interdites (placeholders non remplacés)
# ────────────────────────────────────────────────────────────
forbidden_values=(
  "CHANGE_ME"
  "change_me"
  "replace_me"
  "replace-me"
  "your_secret"
  "password"
  "secret"
  "todo"
)

ERRORS=0

echo ""
echo "══════════════════════════════════════════════════════"
echo " 🔍 Validation de l'environnement — YOWYOB ERP Backend-ACCOUNTING"
echo "══════════════════════════════════════════════════════"
echo ""

# Vérification des variables obligatoires
echo "📋 Vérification des variables obligatoires..."
for var in "${required_vars[@]}"; do
  value="${!var:-}"
  if [[ -z "${value}" ]]; then
    echo "  ❌ MANQUANT : ${var}"
    ERRORS=$((ERRORS + 1))
  else
    echo "  ✅ OK       : ${var}=${value:0:20}..."
  fi
done

echo ""

# Vérification des valeurs interdites
echo "🔐 Vérification des valeurs de sécurité..."
for var in "${required_vars[@]}"; do
  value="${!var:-}"
  for forbidden in "${forbidden_values[@]}"; do
    if [[ "${value,,}" == *"${forbidden,,}"* ]]; then
      echo "  ❌ VALEUR PLACEHOLDER détectée pour ${var} : '${value:0:30}...'"
      echo "     Remplacez cette valeur avant de déployer en production !"
      ERRORS=$((ERRORS + 1))
      break
    fi
  done
done

echo ""

# Vérification du profil production
if [[ "${SPRING_PROFILES_ACTIVE:-}" != "prod" ]]; then
  echo "  ⚠️  AVERTISSEMENT : SPRING_PROFILES_ACTIVE='${SPRING_PROFILES_ACTIVE:-}' (attendu: 'prod')"
fi

# Vérification de la connectivité réseau (si BACKEND doit démarrer maintenant)
echo "🌐 Vérification de l'accessibilité du réseau Docker..."
if docker network inspect iwm-network > /dev/null 2>&1; then
  echo "  ✅ Réseau iwm-network existe"
else
  echo "  ❌ Réseau iwm-network introuvable — KSM_Kernel_Layer doit être démarré en premier !"
  ERRORS=$((ERRORS + 1))
fi

# Vérification des fichiers de secrets
echo ""
echo "🗝️  Vérification des fichiers de secrets..."
SECRETS_DIR="${SCRIPT_DIR}/../ops/secrets/prod"
secret_files=(
  "erp_postgres_password.txt"
  "erp_jwt_secret.txt"
)
for f in "${secret_files[@]}"; do
  path="${SECRETS_DIR}/${f}"
  if [[ ! -f "${path}" ]]; then
    echo "  ❌ Secret manquant : ${path}"
    ERRORS=$((ERRORS + 1))
  else
    size=$(wc -c < "${path}" | tr -d ' ')
    if [[ "${size}" -lt 16 ]]; then
      echo "  ⚠️  Secret trop court (< 16 chars) : ${f}"
    else
      echo "  ✅ OK : ${f} (${size} chars)"
    fi
  fi
done

echo ""
echo "══════════════════════════════════════════════════════"
if [[ "${ERRORS}" -eq 0 ]]; then
  echo " ✅ Environnement valide — prêt pour le déploiement !"
else
  echo " ❌ ${ERRORS} erreur(s) détectée(s) — déploiement bloqué."
  echo "    Corrigez les erreurs ci-dessus avant de continuer."
fi
echo "══════════════════════════════════════════════════════"
echo ""

exit "${ERRORS}"
