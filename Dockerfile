# Étape 1 : Build avec Maven + JDK 21
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# 1. Copie du POM
# Si pom.xml ne change pas, le cache pour les dépendances est conservé.
COPY pom.xml .

# 2. Téléchargement des dépendances
# Cette étape est mise en cache tant que le pom.xml ne change pas.
RUN mvn dependency:go-offline -B # Utilisation de -B (batch mode) pour éviter les interactions

# 3. Copie de TOUT le reste du code
# Si votre code source change, seul ce qui suit sera reconstruit.
# C'est l'étape la plus susceptible de changer, elle doit donc être tardive.
COPY src ./src

# 4. Le build final de l'application
RUN mvn clean package -DskipTests

# Étape 2 : Image finale uniquement avec le JDK
FROM eclipse-temurin:21-jdk
WORKDIR /app

# Copier seulement le JAR final
COPY --from=build /app/target/*.jar app.jar

# Config
EXPOSE 8081
ENV SPRING_PROFILES_ACTIVE=prod

# Lancer l'application
ENTRYPOINT ["java", "-jar", "app.jar"]

