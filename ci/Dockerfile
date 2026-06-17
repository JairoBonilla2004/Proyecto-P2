FROM python:3.11-slim

# Metadatos
LABEL maintainer="DevSecOps Team"
LABEL description="Security Pipeline - AI Vulnerability Detection"

# Configurar working directory
WORKDIR /app

# Instalar dependencias del sistema
RUN apt-get update && apt-get install -y --no-install-recommends \
    git \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Copiar requirements
COPY ci/requirements.txt .

# Instalar dependencias Python
RUN pip install --no-cache-dir -r requirements.txt

# Copiar código del pipeline
COPY ci/ /app/
COPY modelo/ /app/modelo/

# Crear directorio de reportes
RUN mkdir -p /app/reports

# Variables de entorno por defecto
ENV PYTHONUNBUFFERED=1
ENV BASE_BRANCH=main
ENV HEAD_BRANCH=HEAD
ENV MODEL_PATH=/app/modelo/model_artifacts
ENV REPORT_PATH=/app/reports/security_report.json

# Volúmenes
VOLUME ["/app/reports", "/app/.git"]

# Entrypoint
ENTRYPOINT ["python", "security_pipeline.py"]
