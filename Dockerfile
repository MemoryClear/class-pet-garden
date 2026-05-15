# ====================
# Stage 1: Build Frontend
# ====================
FROM node:20-alpine AS frontend-builder

WORKDIR /app/frontend

COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# ====================
# Stage 2: Build Backend
# ====================
FROM maven:3.9-eclipse-temurin-17 AS backend-builder

WORKDIR /app/backend

ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8

COPY backend/pom.xml ./
RUN mvn dependency:go-offline -B

COPY backend/src ./src
COPY --from=frontend-builder /app/frontend/dist ./src/main/resources/static

RUN mvn clean package -DskipTests -B -Dproject.build.sourceEncoding=UTF-8

# ====================
# Stage 3: Extract JAR layers
# ====================
FROM eclipse-temurin:17-jre AS extractor

WORKDIR /app
COPY --from=backend-builder /app/backend/target/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract && rm app.jar

# ====================
# Stage 4: Runtime
# ====================
FROM eclipse-temurin:17-jre

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8

RUN groupadd -r appgroup && useradd -r -g appgroup -m appuser

COPY --from=extractor /app/dependencies/ ./
COPY --from=extractor /app/spring-boot-loader/ ./
COPY --from=extractor /app/snapshot-dependencies/ ./
COPY --from=extractor /app/application/ ./

RUN mkdir -p /app/data && chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD curl -f http://localhost:8080/api/pets || exit 1

ENV SERVER_PORT=8080
ENV DB_PATH=/app/data/classpet.db
ENV JWT_SECRET=dGhpc2lzYXZlcnlsb25nc2VjcmV0a2V5Zm9yand0dG9rZW5nZW5lcmF0aW9uMjAyNA==
ENV JWT_EXPIRATION_MS=86400000

# Use full classpath including all directories
ENTRYPOINT ["/bin/sh", "-c", "exec java -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -Dserver.port=${SERVER_PORT} '-Dspring.datasource.url=jdbc:sqlite:${DB_PATH}?busy_timeout=30000&journal_mode=WAL&synchronous=NORMAL' -Dapp.jwt.secret=${JWT_SECRET} -Dapp.jwt.expiration-ms=${JWT_EXPIRATION_MS} -cp dependencies/:snapshot-dependencies/:spring-boot-loader/:application:dependencies/*:snapshot-dependencies/*:spring-boot-loader/* com.classpet.ClassPetGardenApplication"]