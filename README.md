# Proyecto Integrador Parcial II - Desarrollo de Software Seguro

**Pipeline CI/CD Seguro con Detección Automática de Vulnerabilidades via ML**

[![Secure CI/CD Pipeline](https://github.com/JairoBonilla2004/Proyecto-P2/actions/workflows/security-pipeline.yml/badge.svg)](https://github.com/JairoBonilla2004/Proyecto-P2/actions/workflows/security-pipeline.yml)

---

## Descripción

Pipeline CI/CD automatizado que integra un modelo de Machine Learning (RandomForest + XGBoost, ensemble) entrenado con el **Juliet Test Suite for Java v1.3** para clasificar código fuente como **SEGURO** o **VULNERABLE** en Pull Requests de `dev` → `test`.

Arquitectura:

```
dev ──PR──> test  ──merge──> main  ──deploy──> Producción
              │
        Security Gate (ML)
         ┌────┴────┐
       SEGURO   VULNERABLE
         │          ├── PR rechazado
       merge       ├── Issue automática
         │         ├── Etiqueta fixing-required
       tests       └── Notificación Telegram
         │
    ┌───┴───┐
   PASS    FAIL
     │       └── tests-failed
   merge a main
     │
   Docker build + push
     │
   Notificación Telegram
```

---

## Requisitos

- Python 3.11+
- Java 21 (para el backend)
- Maven 3.9+
- Docker (para deploy)
- Git

---

## Setup del Pipeline

### 1. Clonar repositorio

```bash
git clone https://github.com/JairoBonilla2004/Proyecto-P2.git
cd Proyecto-P2
```

### 2. Instalar dependencias del pipeline

```bash
pip install -r ci/requirements.txt
```

### 3. Configurar variables de entorno

```bash
cp ci/.env.example ci/.env.local
# Editar .env.local con:
#   TELEGRAM_BOT_TOKEN=tu_token
#   TELEGRAM_CHAT_ID=tu_chat_id
```

### 4. Configurar GitHub Secrets

| Secret | Descripción |
|--------|-------------|
| `TELEGRAM_BOT_TOKEN` | Token del bot de Telegram |
| `TELEGRAM_CHAT_ID` | ID del chat de Telegram |
| `DOCKERHUB_USERNAME` | (Opcional) Usuario de DockerHub |
| `DOCKERHUB_TOKEN` | (Opcional) Token de DockerHub |

---

## Entrenamiento del Modelo

### Dataset

El modelo se entrena con **Juliet Test Suite for Java v1.3** (NIST):
- 17,612 muestras (8,806 vulnerables + 8,806 seguras)
- 38 CWEs de seguridad
- Pares `bad()` / `good()` por archivo

### Ejecutar entrenamiento

```bash
# Versión script (recomendada para CI)
python modelo/train_model.py

# Versión notebook (para exploración visual)
# Abrir modelo/clasificador_vulnerabilidades_cvefixes.ipynb en Jupyter
```

El entrenamiento:
1. Carga el CSV con 17,612 muestras
2. Extrae 29 features estructurales por muestra
3. Vectoriza con TF-IDF (8000 features, ngramas 1-3)
4. GridSearch para RandomForest y XGBoost (3-fold CV)
5. Ensemble VotingClassifier (RF + XGB, soft voting)
6. SMOTE dentro de ImbPipeline (sin data leakage)
7. Evalúa con 5-fold Cross-Validation
8. Exporta artefactos a `modelo/model_artifacts/`

### Resultados

| Métrica | Valor |
|---------|-------|
| Accuracy (5-fold CV) | **99.99%** |
| Precision | 100% |
| Recall | 100% |
| AUC-ROC | 100% |
| **Umbral requerido** | **≥ 82%** ✅ |

### Artefactos generados

```
modelo/model_artifacts/
  modelo_ensemble.joblib     # VotingClassifier entrenado
  vectorizador_tfidf.joblib  # TF-IDF vectorizer
  scaler_estructural.joblib  # MinMaxScaler
  selector_features.joblib   # SelectKBest (chi2)
  model_metadata.json        # Métricas y configuración
```

---

## Ejecutar Tests

```bash
python ci/tests.py
```

Resultado esperado: **28/28 tests pasando** (FeatureExtractor + SecurityGate + TelegramNotifier + CodeFeatures).

---

## Uso del Pipeline

### Flujo normal (código seguro)

1. Desarrollador hace push a `dev`
2. Crea PR de `dev` → `test`
3. Pipeline se activa automáticamente
4. Security Gate clasifica el código
5. Si **SEGURO** → merge a `test` → ejecuta tests → merge a `main` → build Docker → deploy
6. Notificación Telegram en cada etapa

### Flujo de rechazo (vulnerabilidad detectada)

1. Security Gate clasifica como **VULNERABLE**
2. PR se marca como rechazado
3. Se crea comentario en el PR con tipo y probabilidad
4. Se aplica etiqueta `fixing-required`
5. Se crea Issue automática vinculada
6. Notificación Telegram al desarrollador
7. Merge bloqueado hasta corregir

---

## Notificaciones Telegram

El bot envía mensajes en cada etapa del pipeline:

| Evento | Mensaje |
|--------|---------|
| Inicio de revisión | 🔍 Inicio de Revisión de Seguridad |
| Clasificación VULNERABLE | 🚫 PR RECHAZADO — Vulnerabilidad Detectada |
| Clasificación SEGURO | ✅ Código Clasificado como SEGURO |
| Merge a test | 🔀 Merge automático a test completado |
| Pruebas exitosas | ✅ Pruebas Exitosas en rama test |
| Pruebas fallidas | ❌ Pruebas Fallidas |
| Despliegue exitoso | 🚀 Despliegue en Producción Exitoso |
| Despliegue fallido | ❌ Despliegue en Producción FALLIDO |

---

## Despliegue en Producción

- **Proveedor:** [Render](https://render.com) / Railway
- **URL:** (pendiente de configurar)
- **Imagen Docker:** `docker pull <dockerhub-user>/proyecto-p2-backend`

---

## Estructura del Proyecto

```
Proyecto-P2/
  .github/workflows/
    security-pipeline.yml     # Pipeline CI/CD (3 etapas)
  Backend/
    docker-compose.yml        # Docker Compose del backend
    Mueblerix/                # Backend Spring Boot (Java 21)
  ci/
    feature_extractor.py      # Extracción de 29 features
    security_gate.py          # Clasificador con guardia anti-FP
    security_pipeline.py      # Orquestador del pipeline
    diff_extractor.py         # Extracción de diff del PR
    telegram_notifier.py      # Notificaciones Telegram
    tests.py                  # Tests (28 tests)
    requirements.txt          # Dependencias Python
  modelo/
    train_model.py            # Entrenamiento del modelo
    extract_juliet.py         # Extracción dataset Juliet
    juliet_java_dataset.csv   # Dataset (17,612 muestras)
    model_artifacts/          # Modelos exportados (.joblib)
    clasificador_vulnerabilidades_cvefixes.ipynb  # Notebook
  docs/
    instructions.md           # Enunciado del proyecto
    PROY_PARCIAL_II_DesSeguro.pdf
```

---

## Tecnologías

- **Modelo ML:** scikit-learn, XGBoost, imbalanced-learn
- **Backend:** Spring Boot 3.3.5, Java 21, PostgreSQL
- **CI/CD:** GitHub Actions
- **Notificaciones:** Telegram Bot API
- **Contenedores:** Docker
- **Dataset:** Juliet Test Suite for Java v1.3

---

## Autores

- Jairo Bonilla
- [Nombre del compañero]

**Universidad de las Fuerzas Armadas ESPE**  
**Departamento de Ciencias de la Computación**  
**Carrera de Ingeniería en Software**  
**Junio 2026**
