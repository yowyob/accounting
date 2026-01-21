# Déploiement sur Docker Hub

Oui, votre projet est prêt pour être déployé sur Docker Hub. Voici les étapes à suivre.

## Prérequis

- Un compte [Docker Hub](https://hub.docker.com/).
- Docker installé et lancé sur votre machine.

## Étapes Manuelles

1.  **Se connecter à Docker Hub**
    Dans votre terminal, exécutez :
    ```bash
    docker login
    ```
    Entrez votre nom d'utilisateur et votre mot de passe (ou token d'accès).

2.  **Construire l'image**
    Remplacez `votre-user` par votre nom d'utilisateur Docker Hub.
    ```bash
    docker build -t votre-user/yowyob-erp-backend:latest .
    ```

3.  **Pousser l'image**
    ```bash
    docker push votre-user/yowyob-erp-backend:latest
    ```

## Utilisation par le Frontend

Pour que votre Frontend accède à l'image (l'API Backend), il faut lancer un conteneur basé sur cette image.

### 1. Lancer le Backend
Sur votre serveur ou machine locale, lancez le conteneur en exposant le port **8081** :
```bash
docker run -d -p 8081:8081 --name yowyob-backend delmat237/yowyob-erp-backend:latest
```
*(Si vous utilisez Docker Compose, mettez simplement à jour l'image dans votre fichier `docker-compose.yml`)*

### 2. Configurer le Frontend
Votre frontend (React, Angular, Vue, etc.) ne "voit" pas l'image Docker directement. Il fait des requêtes HTTP vers l'URL où tourne le conteneur.

*   **En local** : L'URL de base sera `http://localhost:8081/api` (ou juste `http://localhost:8081` selon vos routes).
*   **Sur un serveur distant** (ex: VPS avec IP `192.168.1.50`) : L'URL sera `http://192.168.1.50:8081/api`.

Dans votre code Frontend (fichier `.env` souvent), mettez à jour la variable d'API :
```properties
# Exemple .env frontend
REACT_APP_API_URL=http://votre-ip-serveur:8081
```

## Automatisation (Optionnel)

Si vous souhaitez automatiser cela avec GitHub Actions, nous pouvons créer un fichier de workflow `.github/workflows/docker-publish.yml`.

## Déploiement sur Cloud (Render)

Render est une excellente option car ils supportent Docker nativement et offrent une base de données PostgreSQL gratuite.

### Étape 1 : Préparation
J'ai configuré le fichier `render.yaml` à la racine pour :
1.  Créer automatiquement une base de données PostgreSQL.
2.  Lancer votre service backend.
3.  **Désactiver Kafka** (`SPRING_KAFKA_ENABLED=false`). 

#### Pourquoi désactiver Kafka ?
*   **Coût/Complexité** : Render ne propose pas de service Kafka gratuit. Pour l'utiliser en prod, il faudrait un service externe (ex: Confluent Cloud) qui est payant.
*   **Démarrage de l'app** : Si Kafka est activé mais que le serveur est injoignable, Spring Boot risque de crash au démarrage ou de rester dans une boucle d'erreurs, ce qui empêcherait l'accès à votre API.
*   **Modules optionnels** : Si votre comptabilité de base fonctionne sans Kafka (pour les logs ou les notifications asynchrones), il vaut mieux le désactiver pour un premier déploiement stable.

### Étape 2 : Créer le Blueprint
1.  Allez sur [dashboard.render.com](https://dashboard.render.com/).
2.  Cliquez sur **New +** -> **Blueprint**.
3.  Connectez votre dépôt GitHub `yowyob-erp-backend`.
4.  Render détectera le fichier `render.yaml`.
5.  Cliquez sur **Apply**.

Render va alors :
*   Provisionner la base PostgreSQL.
*   Construire votre Dockerfile.
*   Déployer l'application.

> **Note**: Si vous avez besoin de Redis, vous pouvez créer une instance Redis sur Render (New -> Redis) et ajouter les variables d'environnement `SPRING_DATA_REDIS_HOST` etc. dans le Dashboard Render du service backend.

### Alternative : "Juste lancer l'image" (Plus simple mais manuel)

Si vous lancez l'image "brute" avec `docker run`, elle va essayer de chercher Postgres, Redis et Kafka sur `localhost` **à l'intérieur** du conteneur. Comme ils n'y sont pas, l'application va crash ou saturer les logs d'erreurs.

Pour que cela fonctionne, vous devez passer les configurations via des drapeaux `-e` (variables d'environnement) :

```bash
docker run -d \
  -p 8081:8081 \
  --name yowyob-backend \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://<host_db>:5432/<nom_db> \
  -e SPRING_DATASOURCE_USERNAME=<user> \
  -e SPRING_DATASOURCE_PASSWORD=<password> \
  -e REDIS_URL=redis://<user_redis>:<pass>@<host_redis>:6379 \
  -e SPRING_KAFKA_ENABLED=false \
  delmat237/yowyob-erp-backend:latest
```

#### Ce qu'il faut retenir pour les services :
1.  **Base de données (Obligatoire)** : Vous devez fournir l'URL d'une base accessible par le conteneur.
2.  **Redis (Fortement recommandé)** : L'application l'utilise pour le cache. Si vous n'en avez pas, vous pouvez essayer de le désactiver via `spring.cache.type=none`, mais certaines fonctionnalités pourraient être ralenties.
3.  **Kafka (Optionnel)** : Comme expliqué, il est préférable de le mettre à `false` (`SPRING_KAFKA_ENABLED=false`) sauf si vous avez un serveur Kafka configuré ailleurs.
4.  **Elasticsearch (Optionnel)** : Utilisé pour la recherche avancée. Si l'URL (`ELASTICSEARCH_URI`) est vide, l'app ignorera ce module.

> **Astuce** : Pour un déploiement avec tous les services (DB + Redis + Kafka) en une seule fois, utilisez plutôt `docker-compose up -d`. C'est l'outil conçu pour gérer plusieurs conteneurs liés.

## Vérifications techniques effectuées
- [x] **Dockerfile** : Présent et valide (Multi-stage build avec Maven et OpenJDK 21).
- [x] **Maven Plugin** : `spring-boot-maven-plugin` est configuré pour produire un JAR exécutable.
- [x] **Docker Compose** : Le service `yowyob-erp-backend` est bien configuré pour construire depuis le contexte courant.

SPRING_DATASOURCE_PASSWORD=PSuQ5Zoc2tJbwRYjVrbtCD8W1xhRaEJr
SPRING_DATASOURCE_URL=jdbc:postgresql://dpg-d5oiekf5r7bs73dc9940-a:5432/yowyob_erp_db_maem
SPRING_DATASOURCE_USERNAME=yowyob_user
SPRING_KAFKA_ENABLED=false
SPRING_LIQUIBASE_CHANGE_LOG=classpath:db/changelog/db.changelog-master.xml
SPRING_PROFILES_ACTIVE=prod