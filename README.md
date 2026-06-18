# Proyecto Integrador Parcial II — Desarrollo de Software Seguro

**Pipeline CI/CD Seguro con Detección Automática de Vulnerabilidades mediante Modelo de Minería de Datos**

[![Secure CI/CD Pipeline](https://github.com/JairoBonilla2004/Proyecto-P2/actions/workflows/security-pipeline.yml/badge.svg)](https://github.com/JairoBonilla2004/Proyecto-P2/actions/workflows/security-pipeline.yml)
![Python](https://img.shields.io/badge/Python-3.11-blue)
![Java](https://img.shields.io/badge/Java-21-orange)
![Modelo](https://img.shields.io/badge/Modelo-RandomForest%20%2B%20XGBoost-green)
![Accuracy](https://img.shields.io/badge/Accuracy-99.99%25-brightgreen)
![Deploy](https://img.shields.io/badge/Deploy-Render-blueviolet)

---

## Equipo de Desarrollo

| Nombre | Institución |
|--------|-------------|
| Bonilla Hidalgo Jairo Smith | Universidad de las Fuerzas Armadas ESPE |
| Tipán Ávila Reishel Dayelin | Universidad de las Fuerzas Armadas ESPE |
| Viche Castillo Julio Enrique | Universidad de las Fuerzas Armadas ESPE |

**Docente:** Ph.D. Geovanny Cudco &nbsp;·&nbsp; **NRC:** 30735 &nbsp;·&nbsp; **Junio 2026**

---

## Descripción

Pipeline CI/CD completamente automatizado que integra un modelo de Machine Learning clásico **(RandomForest + XGBoost ensemble)** entrenado con el **Juliet Test Suite for Java v1.3 (NIST)** para clasificar código fuente como **SEGURO** o **VULNERABLE** en cada Pull Request de `dev` → `test`. Solo el código aprobado por el modelo llega a producción.

> ⚠️ **IMPORTANTE:** No se utiliza ningún LLM (GPT, Claude, Llama, etc.). El modelo es un clasificador tradicional de minería de datos, entrenado íntegramente por el equipo con datos públicos del NIST.

---

## Arquitectura del Pipeline

```
╔═══════════════════════════════════════════════════════════════════════════╗
║                           DESARROLLADOR                                   ║
║              push código → rama dev  /  crea PR dev → test               ║
╚══════════════════════════════════╤════════════════════════════════════════╝
                                   │  PR abierto  ← trigger automático
                                   ▼
╔═══════════════════════════════════════════════════════════════════════════╗
║  ETAPA 1 · Revisión de Seguridad con Modelo ML                           ║
║                                                                           ║
║  ① Descarga diff del PR                                                  ║
║  ② Extrae 29 features por fragmento                                      ║
║     · AST depth / nodos / complejidad ciclomática                        ║
║     · Llamadas peligrosas (Runtime.exec, eval, pickle, etc.)             ║
║     · SQL sin parametrizar · Path traversal · Crypto débil               ║
║     · Presencia de sanitización / PreparedStatement                      ║
║     · TF-IDF (8 000 features, n-gramas 1-3)                             ║
║  ③ Clasifica con RandomForest + XGBoost (VotingClassifier soft)         ║
║                                                                           ║
║          ┌──────────────────────────┴──────────────────────────┐         ║
║          ▼                                                      ▼         ║
║    ✅  SEGURO                                          🚫  VULNERABLE    ║
║    Continúa pipeline                                  ├─ Merge bloqueado ║
║                                                       ├─ Comentario PR   ║
║                                                       │  tipo + prob.    ║
║                                                       ├─ Label: fixing-  ║
║                                                       │  required        ║
║                                                       ├─ Issue automática║
║                                                       └─ 📱 Telegram     ║
╚══════════════════════════════════╤════════════════════════════════════════╝
                                   │  solo si ✅ SEGURO
                                   ▼
╔═══════════════════════════════════════════════════════════════════════════╗
║  ETAPA 2 · Merge automático dev → test  +  Pruebas                      ║
║                                                                           ║
║  ① git merge origin/dev → test  (auto, --no-ff)                         ║
║  ② 📱 Telegram: "Merge a test completado, ejecutando pruebas"           ║
║  ③ mvn clean verify  (JUnit 5 + Mockito, reportes Surefire/Failsafe)   ║
║                                                                           ║
║          ┌──────────────────────────┴──────────────────────────┐         ║
║          ▼                                                      ▼         ║
║    ✅  PASS                                              ❌  FAIL        ║
║    Continúa pipeline                                  ├─ Label: tests-   ║
║    📱 Telegram: pruebas OK                            │  failed          ║
║                                                       ├─ Comentario PR   ║
║                                                       └─ 📱 Telegram     ║
╚══════════════════════════════════╤════════════════════════════════════════╝
                                   │  solo si ✅ todos los tests pasan
                                   ▼
╔═══════════════════════════════════════════════════════════════════════════╗
║  ETAPA 3 · Merge automático test → main  +  Despliegue en Producción    ║
║                                                                           ║
║  ① git merge origin/test → main  (auto, --no-ff)                        ║
║  ② Docker build → jsbonilla2/proyecto-p2-backend:sha-XXXXXXX           ║
║  ③ Push imagen a Docker Hub  (:sha-XXXXXXX  y  :latest)                ║
║  ④ Render deploy hook → actualiza el servicio en producción             ║
║  ⑤ 📱 Telegram: "Despliegue en Producción Exitoso"                      ║
║                                                                           ║
║  🌐  https://proyecto-p2.onrender.com/api/v1                            ║
╚═══════════════════════════════════════════════════════════════════════════╝
```

---

## Requisitos previos

| Herramienta | Versión mínima |
|-------------|----------------|
| Python | 3.11+ |
| Java (JDK) | 21 |
| Maven | 3.9+ |
| Docker | 24+ |
| Git | 2.40+ |

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

### 3. Configurar variables de entorno (local)

```bash
cp ci/.env.example ci/.env.local
# Editar .env.local con:
#   TELEGRAM_BOT_TOKEN=tu_token
#   TELEGRAM_CHAT_ID=tu_chat_id
```

### 4. Configurar GitHub Secrets

| Secret | Descripción | Obligatorio |
|--------|-------------|-------------|
| `TELEGRAM_BOT_TOKEN` | Token del bot Seguridad-Backend | ✅ |
| `TELEGRAM_CHAT_ID` | ID del chat de destino | ✅ |
| `DOCKERHUB_USERNAME` | Usuario de Docker Hub | ✅ |
| `DOCKERHUB_TOKEN` | Token de acceso a Docker Hub | ✅ |
| `RENDER_DEPLOY_HOOK_URL` | URL del deploy hook de Render | ✅ |

---

## Entrenamiento del Modelo

### Dataset

El modelo se entrena con **Juliet Test Suite for Java v1.3** (NIST — fuente pública):

| Característica | Valor |
|----------------|-------|
| Total de muestras | 17,612 |
| Muestras vulnerables | 8,806 |
| Muestras seguras | 8,806 |
| CWEs cubiertos | 38 |
| Fuente | [NIST SARD](https://samate.nist.gov/SARD/test-suites/111) |

### Ejecutar entrenamiento

```bash
# Versión script (recomendada para CI)
python modelo/train_model.py

# Versión notebook (para exploración visual)
# Abrir modelo/clasificador_vulnerabilidades_cvefixes.ipynb en Jupyter
```

### Pasos del entrenamiento

1. Carga el CSV con 17,612 muestras
2. Extrae 29 features estructurales por muestra
3. Vectoriza con TF-IDF (8,000 features, n-gramas 1–3)
4. GridSearch para RandomForest y XGBoost (3-fold CV)
5. Ensemble VotingClassifier (RF + XGB, soft voting)
6. SMOTE dentro de ImbPipeline (sin data leakage)
7. Evaluación con 5-fold Cross-Validation
8. Exporta artefactos a `modelo/model_artifacts/`

### Resultados del modelo

| Métrica | Valor |
|---------|-------|
| Accuracy (5-fold CV) | **99.99%** |
| Precision | **100%** |
| Recall | **100%** |
| AUC-ROC | **1.00** |
| Umbral mínimo requerido | ≥ 82% ✅ |

### Artefactos generados

```
modelo/model_artifacts/
  modelo_ensemble.joblib      # VotingClassifier entrenado
  vectorizador_tfidf.joblib   # TF-IDF vectorizer
  scaler_estructural.joblib   # MinMaxScaler
  selector_features.joblib    # SelectKBest (chi2)
  model_metadata.json         # Métricas y configuración
```

---

## Ejecutar Tests del Pipeline

```bash
python ci/tests.py
```

Resultado esperado: **15/15 tests pasando** (FeatureExtractor + SecurityGate + TelegramNotifier).

---

## Uso del Pipeline

### Flujo feliz — código seguro

1. Desarrollador hace push a `dev`
2. Crea PR `dev` → `test`
3. Pipeline se activa automáticamente
4. Etapa 1: modelo clasifica → **SEGURO** → notificación Telegram
5. Etapa 2: merge a `test` → pruebas pasan → notificación Telegram
6. Etapa 3: merge a `main` → Docker build → push → Render deploy → notificación Telegram

### Flujo de rechazo — vulnerabilidad detectada

1. Etapa 1: modelo clasifica → **VULNERABLE**
2. Merge bloqueado automáticamente
3. Comentario en el PR con tipo de vulnerabilidad y probabilidad
4. Label `fixing-required` aplicada
5. Issue automática creada y vinculada al PR
6. Notificación Telegram inmediata al equipo
7. El desarrollador corrige y vuelve a abrir el PR

---

## Notificaciones Telegram — Bot: *Seguridad-Backend*

| Evento | Mensaje |
|--------|---------|
| Inicio de revisión | 🔍 Inicio de Revisión de Seguridad con Modelo ML |
| Clasificación VULNERABLE | 🚫 PR RECHAZADO — tipo + probabilidad + riesgo |
| Clasificación SEGURO | ✅ Código Clasificado como SEGURO |
| Merge a `test` | 🔀 Merge automático a test completado |
| Pruebas exitosas | ✅ Pruebas Exitosas en rama test |
| Pruebas fallidas | ❌ Pruebas Fallidas — pipeline detenido |
| Despliegue exitoso | 🚀 Despliegue en Producción Exitoso |
| Despliegue fallido | ❌ Despliegue en Producción FALLIDO |

---

## Despliegue en Producción

| Campo | Valor |
|-------|-------|
| Proveedor | [Render](https://render.com) |
| URL pública | https://proyecto-p2.onrender.com/api/v1 |
| Imagen Docker | `jsbonilla2/proyecto-p2-backend:latest` |
| Trigger | Automático vía deploy hook tras cada PR aprobado |

```bash
# Descargar imagen manualmente
docker pull jsbonilla2/proyecto-p2-backend:latest
```

---

## Estructura del Proyecto

```
Proyecto-P2/
├── .github/
│   └── workflows/
│       └── security-pipeline.yml    # Pipeline CI/CD (3 etapas)
├── Backend/
│   ├── docker-compose.yml
│   └── Mueblerix/                   # Backend Spring Boot (Java 21)
│       ├── src/main/java/           # Código fuente con módulos de seguridad
│       ├── src/test/java/           # Suite de pruebas (JUnit 5 + Mockito)
│       └── Dockerfile
├── ci/
│   ├── feature_extractor.py         # Extracción de 29 features
│   ├── security_gate.py             # Clasificador + guardia anti-FP
│   ├── security_pipeline.py         # Orquestador del análisis
│   ├── diff_extractor.py            # Extracción del diff del PR
│   ├── telegram_notifier.py         # Notificaciones Telegram
│   ├── tests.py                     # 15 tests del pipeline
│   └── requirements.txt
├── modelo/
│   ├── train_model.py               # Entrenamiento reproducible
│   ├── extract_juliet.py            # Preprocesamiento dataset Juliet
│   ├── juliet_java_dataset.csv      # Dataset (17,612 muestras)
│   ├── model_artifacts/             # Artefactos .joblib exportados
│   └── clasificador_vulnerabilidades_cvefixes.ipynb  # Notebook
├── INFORME/
│   ├── informe_proyecto_p2.tex      # Informe técnico LaTeX
│   └── img/
└── docs/
```

---

## Tecnologías

| Capa | Tecnología |
|------|------------|
| Modelo ML | scikit-learn, XGBoost, imbalanced-learn |
| Backend | Spring Boot 3.3.5, Java 21, PostgreSQL |
| CI/CD | GitHub Actions |
| Notificaciones | Telegram Bot API |
| Contenedores | Docker, Docker Hub |
| Despliegue | Render (free tier) |
| Dataset | Juliet Test Suite for Java v1.3 (NIST) |

---

