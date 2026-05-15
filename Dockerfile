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
# Stage 2: Build Backend (no JAR, run with Maven directly)
# ====================
FROM maven:3.9-eclipse-temurin-17 AS backend-builder

WORKDIR /app/backend

ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8

COPY backend/pom.xml ./
RUN mvn dependency:go-offline -B

COPY backend/src ./src
COPY --from=frontend-builder /app/frontend/dist ./src/main/resources/static

# Don't build JAR - we will run with mvn spring-boot:run

# ====================
# Stage 3: Runtime
# ====================
FROM maven:3.9-eclipse-temurin-17

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8

RUN groupadd -r appgroup && useradd -r -g appgroup -m appuser

COPY --from=backend-builder /app/backend/ /app/backend/

RUN mkdir -p /app/data && chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/api/pets || exit 1

ENV SERVER_PORT=8080
ENV DB_PATH=/app/data/classpet.db
ENV JWT_SECRET=dGhpc2lzYXZlcnlsb25nc2VjcmV0a2V5Zm9yand0dG9rZW5nZW5lcmF0aW9uMjAyNA==
ENV JWT_EXPIRATION_MS=86400000

# Run directly with mvn - no JAR, no memory-mapped I/O issues
CMD ["mvn", "spring-boot:run"]