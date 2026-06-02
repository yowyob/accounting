# 🔐 Secrets Docker

Ce dossier contient les **fichiers de secrets** utilisés par Docker Compose.

## Structure

```
ops/secrets/
├── README.md           ← Ce fichier
├── prod/               ← Secrets de production (JAMAIS commités)
│   ├── erp_postgres_password.txt
│   ├── erp_jwt_secret.txt
│   └── erp_redis_password.txt
└── preprod/            ← Secrets de pré-production (JAMAIS commités)
    ├── erp_postgres_password.txt
    ├── erp_jwt_secret.txt
    └── erp_redis_password.txt
```

## ⚠️ IMPORTANT

- Les fichiers `*.txt` et `*.pem` dans `prod/` et `preprod/` sont exclus de Git (`.gitignore`)
- Ne jamais commiter de secrets réels dans le dépôt
- Utiliser `chmod 600` pour restreindre l'accès aux fichiers

## Création des secrets

```bash
# Production
mkdir -p ops/secrets/prod/
echo "MOT_DE_PASSE_FORT"        > ops/secrets/prod/erp_postgres_password.txt
echo "$(openssl rand -base64 64)" > ops/secrets/prod/erp_jwt_secret.txt
echo ""                          > ops/secrets/prod/erp_redis_password.txt
chmod 600 ops/secrets/prod/*.txt
```
