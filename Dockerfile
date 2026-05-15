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
# Stage 2: Build Backend (package as JAR)
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
# Stage 3: Extract JAR (avoid mmap issue)
# ====================
FROM eclipse-temurin:17-jre AS extractor

WORKDIR /app

COPY --from=backend-builder /app/backend/target/*.jar app.jar

# Extract the JAR (no mmap at runtime)
RUN jar xf app.jar && rm app.jar

# ====================
# Stage 4: Runtime (run extracted JAR with correct classpath)
# ====================
FROM eclipse-temurin:17-jre

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8

RUN groupadd -r appgroup && useradd -r -g appgroup -m appuser

# Copy extracted JAR contents
COPY --from=extractor /app/BOOT-INF/ /app/BOOT-INF/
COPY --from=extractor /app/org/ /app/org/
COPY --from=extractor /app/META-INF/ /app/META-INF/

RUN mkdir -p /app/data && chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/api/pets || exit 1

ENV SERVER_PORT=8080
ENV DB_PATH=/app/data/classpet.db
ENV JWT_SECRET=dGhpc2lzYXZlcnlsb25nc2VjcmV0a2V5Zm9yand0dG9rZW5nZW5lcmF0aW9uMjAyNA==
ENV JWT_EXPIRATION_MS=86400000

# Run with JarLauncher (correct classpath for Spring Boot)
CMD ["java", "-cp", "BOOT-INF/classes:BOOT-INF/lib/*:org/", "org.springframework.boot.loader.launch.JarLauncher"]
