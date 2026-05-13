# ─────────────────────────────────────────────────────────────────────────────
# ETAPA 1: Build con Maven (imagen pesada — solo para compilar)
# ─────────────────────────────────────────────────────────────────────────────
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copiar pom.xml primero para aprovechar el cache de capas Docker
# (si solo cambia el código, no se re-descarga el índice de dependencias)
COPY pom.xml .
RUN mvn dependency:go-offline -B -q

# Copiar fuentes y compilar (skip tests — se ejecutan en CI antes del build Docker)
COPY src ./src
RUN mvn clean package -DskipTests -B -q

# ─────────────────────────────────────────────────────────────────────────────
# ETAPA 2: Runtime — JRE 21 Alpine (imagen mínima ~70MB)
# ─────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

# Metadatos de la imagen
LABEL maintainer="SecureFrame Team <jsbonilla2@espe.edu.ec>"
LABEL description="SecureFrame Gallery — Backend Spring Boot con detección de esteganografía"
LABEL version="1.0.0"
LABEL org.opencontainers.image.source="https://github.com/JairoBonilla2004/SecureFrame-Gallery"

WORKDIR /app

# Utilidades mínimas para HEALTHCHECK
RUN apk add --no-cache wget

# ── Seguridad: usuario no-root ────────────────────────────────────────────────
# Ejecutar la aplicación como usuario sin privilegios para reducir superficie de ataque
RUN addgroup -S appgroup && \
    adduser -S appuser -G appgroup

# Copiar JAR desde la etapa de build
COPY --from=builder /build/target/SecureFrameGallery-*.jar app.jar

# Cambiar propietario al usuario de la app
RUN chown appuser:appgroup app.jar

# Cambiar al usuario no-root
USER appuser

# ── Health check ──────────────────────────────────────────────────────────────
HEALTHCHECK \
    --interval=30s \
    --timeout=10s \
    --start-period=60s \
    --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Exponer puerto HTTP
EXPOSE 8080

# ── Punto de entrada con opciones JVM optimizadas para contenedores ───────────
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseG1GC", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "/app/app.jar"]
