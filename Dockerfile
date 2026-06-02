# syntax=docker/dockerfile:1.7

# ══════════════════════════════════════════════════════════════
# Stage 1 — Build
# ══════════════════════════════════════════════════════════════
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /workspace

# Copier le POM en premier pour profiter du cache des couches Docker
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B -q

# Copier le code source et builder
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -DskipTests -q

# ══════════════════════════════════════════════════════════════
# Stage 2 — Runtime
# ══════════════════════════════════════════════════════════════
FROM eclipse-temurin:21-jre-alpine AS runtime

# Métadonnées OCI
LABEL org.opencontainers.image.title="YOWYOB ERP Backend" \
      org.opencontainers.image.description="ERP OHADA multi-tenant — Spring Boot 3 + WebFlux" \
      org.opencontainers.image.vendor="Yowyob" \
      org.opencontainers.image.source="https://github.com/Delmat237/Yowyob-ERP-Accounting"

# Installer curl pour les health-checks + nc pour wait-for
RUN apk add --no-cache curl netcat-openbsd bash

# Créer un utilisateur non-root dédié
RUN addgroup --system erp && \
    adduser --system --ingroup erp --home /home/erp erp

WORKDIR /app

# Copier le JAR depuis le stage build
COPY --from=build /workspace/target/*.jar app.jar

# Copier les scripts opérationnels
COPY scripts/wait-for-services.sh /usr/local/bin/wait-for-services.sh
RUN chmod +x /usr/local/bin/wait-for-services.sh

# Propriétés des fichiers
RUN chown -R erp:erp /app

# Utiliser l'utilisateur non-root
USER erp

# Ports : 8081 = API principale
EXPOSE 8081

# Variables d'environnement JVM optimisées pour les conteneurs
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:InitialRAMPercentage=25.0 \
               -XX:+ExitOnOutOfMemoryError \
               -XX:+HeapDumpOnOutOfMemoryError \
               -Djava.security.egd=file:/dev/./urandom"

ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
