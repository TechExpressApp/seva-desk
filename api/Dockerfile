# --- build ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src src
RUN mvn -B -q package -DskipTests \
 && java -Djarmode=tools -jar target/sevadesk-api-*.jar extract --layers --launcher --destination /out

# --- run ---
FROM eclipse-temurin:21-jre-alpine
RUN adduser -S -D -H -u 1001 app
WORKDIR /app
# Layers ordered from least to most frequently changed, so redeploys only ship the app layer.
COPY --from=build /out/dependencies/ ./
COPY --from=build /out/spring-boot-loader/ ./
COPY --from=build /out/snapshot-dependencies/ ./
COPY --from=build /out/application/ ./
USER app
EXPOSE 8080
# Tuned for a small VM: Serial GC (least overhead for 1-2 CPUs), C1 compiler only (less memory/CPU spent compiling),
# smaller code cache and thread stacks. Heap is a share of the container limit set in docker-compose.prod.yml.
ENV JAVA_TOOL_OPTIONS="-XX:+UseSerialGC -XX:TieredStopAtLevel=1 -XX:ReservedCodeCacheSize=48m -Xss512k \
 -XX:MaxRAMPercentage=65 -XX:+ExitOnOutOfMemoryError"
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s CMD wget -qO- http://localhost:8080/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
