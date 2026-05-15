# ====================
# Stage 1: Build Frontend
# ====================
FROM node:20-alpine AS frontend-builder

WORKDIR /app/frontend

# Copy frontend package files
COPY frontend/package*.json ./

# Install dependencies
RUN npm ci

# Copy frontend source
COPY frontend/ ./

# Build frontend
RUN npm run build

# ====================
# Stage 2: Build Backend
# ====================
FROM maven:3.9-eclipse-temurin-17 AS backend-builder

WORKDIR /app/backend

# Set encoding for Chinese filenames
ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8

# Copy backend pom.xml first (for better caching)
COPY backend/pom.xml ./

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy backend source
COPY backend/src ./src

# Copy frontend build output to static resources
COPY --from=frontend-builder /app/frontend/dist ./src/main/resources/static

# Build backend JAR with UTF-8 encoding
RUN mvn clean package -DskipTests -B -Dproject.build.sourceEncoding=UTF-8

# ====================
# Stage 3: Extract JAR layers (bypass Spring Boot ZIP mmap I/O)
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

# Install curl for healthcheck
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

# Set locale environment
ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8

# Create non-root user
RUN groupadd -r appgroup && useradd -r -g appgroup -m appuser

# Copy extracted layers (bypasses Spring Boot ZIP memory-mapped I/O that hangs on NAS)
COPY --from=extractor /app/dependencies/ ./
COPY --from=extractor /app/spring-boot-loader/ ./
COPY --from=extractor /app/snapshot-dependencies/ ./
COPY --from=extractor /app/application/ ./

# Create data directory for SQLite database
RUN mkdir -p /app/data && chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD curl -f http://localhost:8080/api/pets || exit 1

# Environment variables with defaults
ENV SERVER_PORT=8080
ENV DB_PATH=/app/data/classpet.db
ENV JWT_SECRET=dGhpc2lzYXZlcnlsb25nc2VjcmV0a2V5Zm9yand0dG9rZW5nZW5lcmF0aW9uMjAyNA==
ENV JWT_EXPIRATION_MS=86400000

# Start application directly from extracted layers
ENTRYPOINT []/bin/sh", "-c"],
  "exec java -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 \
  -Dserver.port=${SERVER_PORT} \
  '-Dspring.datasource.url=jdbc:sqlite:${DB_PATH}?busy_timeout=30000&journal_mode=WAL&synchronous=NORMAL' \
  -Dapp.jwt.secret=${JWT_SECRET} \
  -Dapp.jwt.expiration-ms=${JWT_EXPIRATION_MS} \
  -cp 'dependencies/*:application' \
  com.classpet.ClassPetGardenApplication"
