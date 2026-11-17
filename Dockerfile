# ---------- Etapa 1: compilar ----------
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -B -ntp dependency:go-offline
COPY src src
RUN ./mvnw -q -B -ntp -DskipTests package

# ---------- Etapa 2: ejecutar ----------
FROM eclipse-temurin:17-jre
RUN useradd --system --uid 1001 mealup \
    && mkdir -p /data/media \
    && chown -R mealup /data
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
USER mealup
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
