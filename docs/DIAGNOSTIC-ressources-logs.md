# Diagnostic — surconsommation de ressources & logs excessifs (backend accounting)

_Date : 2026-07-16 — contexte : signalement DevOps (CPU élevé + volume de logs anormal)._

## Résumé

La quasi-totalité du bruit (CPU + logs) provient d'un **consommateur Kafka qui n'arrive
jamais à se stabiliser avec le coordinateur de groupe**. Le reste vient d'un logging
applicatif en `DEBUG` et de l'absence de rotation des fichiers.

| # | Cause | Impact | Correctif |
|---|-------|--------|-----------|
| 1 | Thrash consommateur Kafka (`NOT_COORDINATOR`, rediscovery/heartbeat en boucle) | **~71 % des logs** + boucle chaude CPU | Appli : `concurrency 3→1`, `org.apache.kafka=WARN`. **Infra : stabiliser le broker (voir plus bas)** |
| 2 | Logging global `DEBUG` (`com.yowyob.erp`, `org.liquibase`) + log par requête | Volume de logs | Niveaux → `INFO`, filtre requête → `TRACE` |
| 3 | Aucune rotation de logs (pas de `logback-spring.xml`) | Fichiers de logs sans limite | Ajout `logback-spring.xml` (20 Mo/fichier, 7 j, cap 500 Mo) |
| 4 | Elasticsearch activé mais inutilisé | Connexions/timeouts inutiles | `spring.elasticsearch.enabled=false` |
| 5 | Double pool DB (R2DBC 10 + Hikari 10) | Connexions/mémoire gardées pour rien | `hikari.maximum-pool-size=2` |

## Preuves

Extrait de l'analyse de fréquence des logs (`run-host.log`, 11 338 lignes ;
motif identique dans `run-shared.log`) :

```
1880  Discovered group coordinator localhost:9092 ...
1842  Requesting disconnect from last known coordinator ...
1083  Group coordinator ... unavailable or invalid due to cause: error response NOT_COORDINATOR
1083  heartbeat failed since coordinator ... not started or not valid
 ...  (Re-)joining group / Successfully joined / Successfully synced  (en boucle)
```

- **8 045 / 11 338 lignes (71 %)** sont ce thrash Kafka.
- Amplificateur applicatif : `KafkaConsumerConfig` fixait `setConcurrency(3)` et il y a
  **7 `@KafkaListener`** → jusqu'à **21 consommateurs** dans le groupe
  `accounting-service-group`, tous en rejoin/heartbeat simultanés.

## Correctifs appliqués côté backend (ce dépôt)

- `src/main/resources/application.properties`
  - `logging.level.com.yowyob.erp=${LOG_LEVEL_APP:INFO}` (DEBUG surchargeable pour investigation)
  - `logging.level.org.liquibase=INFO`
  - `logging.level.org.apache.kafka=WARN` (coupe le spam coordinateur)
  - `spring.elasticsearch.enabled=${SPRING_ELASTICSEARCH_ENABLED:false}`
  - `spring.datasource.hikari.maximum-pool-size=${HIKARI_MAX_POOL:2}`
- `src/main/java/.../config/kafka/KafkaConsumerConfig.java`
  - `concurrency` → `${spring.kafka.listener.concurrency:1}` (surcharge `KAFKA_LISTENER_CONCURRENCY`)
- `src/main/java/.../config/organization/OrganizationWebFilter.java`
  - log « Context resolved » par requête : `DEBUG` → `TRACE`
- `src/main/resources/logback-spring.xml` (nouveau) : rotation taille+temps, rétention plafonnée.

> Ces changements **réduisent immédiatement** le volume de logs et la charge CPU, mais ne
> corrigent pas la cause racine infra (broker). Ils la rendent seulement silencieuse/moins coûteuse.

## Action DevOps requise — cause racine (broker Kafka)

`NOT_COORDINATOR` signifie que le broker joignable **n'est pas (ou plus) le leader de la
partition de `__consumer_offsets`** qui héberge le groupe `accounting-service-group`. Le
consommateur redécouvre le coordinateur en boucle car celui-ci n'est jamais « valide ».

Causes typiques à vérifier côté cluster :

1. **Topic `__consumer_offsets` en mauvais état** : partitions sans leader / sous-répliquées.
   - Vérifier : `kafka-topics --describe --topic __consumer_offsets --bootstrap-server <broker>`
     → chercher `Leader: -1` ou `Isr` incomplet.
   - Sur cluster mono-broker, `offsets.topic.replication.factor` doit valoir **1** (une valeur 3
     avec 1 seul broker laisse le topic sans leader → exactement ce symptôme).
2. **Bootstrap qui pointe un broker qui n'est pas dans le quorum** (ancienne IP, broker retiré) :
   vérifier `KAFKA_BOOTSTRAP` de l'app vs les brokers réellement vivants.
3. **Instabilité broker/ZooKeeper-KRaft** : redémarrages fréquents → réélections de coordinateur.
   Vérifier les logs broker et l'uptime.
4. **Un seul broker mais réplication offsets > 1** (cas fréquent en déploiement Docker/Render) :
   forcer `KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1` (+ `transaction.state.log.replication.factor=1`,
   `transaction.state.log.min.isr=1`).

Tant que ce point n'est pas réglé, l'intégration événementielle (factures → écritures,
tiers, trésorerie) reste dégradée même si les logs sont devenus silencieux.

### Leviers de secours (si le broker ne peut pas être corrigé tout de suite)

- Désactiver temporairement la consommation Kafka de ce service : `SPRING_KAFKA_ENABLED=false`
  (⚠️ suspend la synchro événementielle — à n'utiliser que pour soulager la prod en urgence).
- Garder `KAFKA_LISTENER_CONCURRENCY=1` (déjà le défaut désormais).
