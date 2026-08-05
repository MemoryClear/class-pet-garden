# ====================
# Stage 1: Build Frontend
# ====================
# Build-time default for VITE_CLASSROOM_ENABLED.
# This is the FALLBACK: at runtime, the backend injects window.__APP_CONFIG__
# (see FeaturesController.index) which overrides this value.
# Pass --build-arg CLASSROOM_ENABLED=true|false to change the build-time default.
ARG CLASSROOM_ENABLED=false
ENV VITE_CLASSROOM_ENABLED=${CLASSROOM_ENABLED}

FROM node:20-alpine AS frontend-builder
ARG CLASSROOM_ENABLED=false
ENV VITE_CLASSROOM_ENABLED=${CLASSROOM_ENABLED}

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
# Stage 2: Build Backend AND Extract JAR
# ====================
FROM maven:3.9-eclipse-temurin-17 AS backend-builder

WORKDIR /app/backend

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

# Build backend JAR
RUN mvn clean package -DskipTests -B -Dproject.build.sourceEncoding=UTF-8

# Extract ONLY the fat JAR (not the .original one)
RUN cd target && jar xf $(ls *.jar | grep -v '\.original$') && rm -f *.jar

# ====================
# Stage 3: Runtime (run extracted JAR)
# ====================
FROM eclipse-temurin:17-jre

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends curl gosu python3 python3-pip && \
    pip3 install --break-system-packages --no-cache-dir edge-tts && \
    rm -rf /var/lib/apt/lists/*

ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8

RUN groupadd -r appgroup && useradd -r -g appgroup -m appuser

# Copy extracted JAR contents directly from builder stage
COPY --from=backend-builder /app/backend/target/BOOT-INF/ /app/BOOT-INF/
COPY --from=backend-builder /app/backend/target/org/ /app/org/
COPY --from=backend-builder /app/backend/target/META-INF/ /app/META-INF/

RUN mkdir -p /app/data && chown -R appuser:appgroup /app

COPY <<'EOF' /app/entrypoint.sh
#!/bin/sh
# Fix permissions for mounted volume (NAS may map to unknown uid)
chown -R appuser:appgroup /app/data 2>/dev/null || true
exec gosu appuser java -cp . org.springframework.boot.loader.launch.JarLauncher
EOF
RUN chmod +x /app/entrypoint.sh

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 CMD curl -f http://localhost:8080/api/pets || exit 1

ENV SERVER_PORT=8080
ENV DB_PATH=/app/data/classpet.db
ENV JWT_SECRET=dGhpc2lzYXZlcnlsb25nc2VjcmV0a2V5Zm9yand0dG9rZW5lZW5lcmF0aW9uMjAyNA==
ENV JWT_EXPIRATION_MS=86400000
# 课堂功能开关（运行时生效；前端通过 /api/features + window.__APP_CONFIG__ 读取）
ENV CLASSROOM_ENABLED=false

ENTRYPOINT ["/app/entrypoint.sh"]