# Proyecto Integrador Parcial II - Desarrollo de Software Seguro

**Universidad de las Fuerzas Armadas ESPE**  
**Departamento de Ciencias de la Computación**  
**Carrera de Ingeniería en Software**  
**Profesor: Geovanny Cudco**  
**Fecha: 28 de mayo de 2026**

---

## 1. Tema

Desarrollo e Implementación de un Pipeline CI/CD Seguro con integración de IA para la Detección Automática de Vulnerabilidades en código fuente mediante un Modelo de Minería de Datos.

---

## 2. Tipo de Actividad

Proyecto práctico individual o en equipo (máximo 3 personas).

---

## 3. Objetivo

Diseñar, implementar y demostrar un pipeline CI/CD completamente automatizado y seguro que integre un modelo de inteligencia artificial basado en técnicas de minería de datos capaz de clasificar código fuente como seguro o vulnerable, permitiendo que únicamente el código considerado seguro llegue a producción, garantizando así la aplicación de los principios de **Secure DevOps** y **Shift-Left Security**.

### ⚠️ IMPORTANTE - Restricciones Tecnológicas

**Está estrictamente prohibido** la incorporación de tecnologías de Large Language Models (LLM) como GPT, Claude, Llama, CodeLlama, etc.

El modelo de IA **debe ser obligatoriamente un clasificador de minería de datos tradicional** (scikit-learn, XGBoost, Random Forest, SVM, etc.) entrenado por el estudiante, usando datasets públicos o propios de código vulnerable/seguro.

---

## 4. Descripción

El proyecto consiste en crear una infraestructura CI/CD segura y automatizada que procese código fuente presentado por un usuario en una rama de testing de un repositorio Git (GitHub o GitLab).

### 4.1. Flujo de Trabajo Requerido

#### 4.1.1. Ramas Obligatorias

- **dev** → rama de desarrollo (donde el desarrollador hace push)
- **test** → rama de staging/pruebas
- **main** → rama de producción

#### 4.1.2. Trigger

El pipeline se activa **automáticamente al crear un Pull Request de `dev` → `test`**.

#### 4.1.3. Etapas del Pipeline (TODAS obligatorias y automatizadas)

##### **Etapa 1: Revisión de Seguridad con Modelo de Minería de Datos**

1. Se ejecuta un job que descarga el diff del PR
2. Se procesa el código modificado extrayendo features:
   - Tokens
   - AST simplificado
   - Patrones de llamadas a funciones peligrosas
   - Uso de sanitización
   - Etc.

3. Se clasifica el código como **SEGURO** o **VULNERABLE** utilizando exclusivamente un modelo de machine learning clásico (scikit-learn, XGBoost, etc.)

**Si el modelo devuelve "VULNERABLE":**
- El PR se marca automáticamente como rechazado y se bloquea el merge
- Se crea un comentario detallado en el PR con la probabilidad y tipo de vulnerabilidad detectada
- Se envía notificación inmediata vía Telegram al desarrollador con el detalle
- Se aplica la etiqueta **"fixing-required"**
- Se crea una issue automática vinculada

**Si el modelo devuelve "SEGURO":**
- Continúa el pipeline

##### **Etapa 2: Merge Automático a rama test + Pruebas**

1. Merge automático a `test`
2. Ejecución de pruebas unitarias e integración (pytest, Jest, JUnit, etc.)
3. Si alguna prueba falla:
   - Bloqueo del pipeline
   - Notificación Telegram
   - Aplicación de etiqueta **"tests-failed"**

##### **Etapa 3: Merge a main y Despliegue en Producción**

1. Solo si todo lo anterior pasó → merge automático a `main`
2. Build de imagen Docker y despliegue automático en proveedor gratuito:
   - **Opciones permitidas:** Render, Railway, Fly.io, Vercel (frontend), Northflank, Docker Hub + Play with Docker (demo)
   - **También permitido:** Heroku (si dispone de plan gratuito)
   - **Otra que considere el estudiante**
3. Notificación final de éxito vía Telegram y/o email

#### 4.1.4. Notificaciones Obligatorias en Todas las Fases

Deben enviarse mensajes vía **Telegram (bot propio)** o correo electrónico en los siguientes eventos:

- ✅ Inicio de revisión de seguridad
- ✅ Resultado de la clasificación del modelo (seguro/vulnerable + probabilidad)
- ✅ Merge a test realizado
- ✅ Resultado de pruebas
- ✅ Despliegue en producción exitoso o fallido
- ✅ Rechazo por vulnerabilidad (con detalle)

---

## 5. Requisitos

### Modelo de Minería de Datos

a. Modelo de minería de datos entrenado por el estudiante (deben entregar el `.pkl` o `.joblib`)

b. Dataset utilizado debe ser público (recomendados: Kaggle, Big-Vul, DiverseVul, CVEFixes, o synthetic con Juliet Test Suite)

c. Features mínimas:
   - Tokens
   - AST depth
   - Llamadas a funciones peligrosas (eval, exec, subprocess, SQL raw, etc.)
   - Presencia de sanitización/escapes

d. **Accuracy mínima demostrada:** 82% en validación cruzada (debe mostrarse en el README)

### Infraestructura y Automatización

e. Telegram Bot propio (token en GitHub Secrets)

f. Despliegue real y funcional (debe estar online y accesible)

g. Branch protection rules activadas en `test` y `main` (requerir revisión de seguridad aprobada)

---

## 6. Formato de Entrega

### a. Repositorio

GitHub o GitLab público o con acceso otorgado al profesor

### b. README.md Completo

Debe incluir:
- Instrucciones de setup del pipeline
- Cómo entrenaron el modelo (notebook incluido)
- Capturas y enlace al bot de Telegram
- Enlace al despliegue en producción

### c. Informe Técnico

En formato LaTeX (se adjunta el formato del informe)

### d. Exposición

Presentación de 8-12 minutos mostrando:
- Código vulnerable → rechazo automático
- Código seguro → flujo completo hasta producción

---

## 7. Fecha de Entrega

**Fecha de Entrega:** 18 de junio de 2026, 23:59 horas

### ⚠️ Nota Importante

Bajo ningún concepto se recibirán actividades fuera del plazo establecido. No se otorgarán prórrogas individuales ni se aceptarán entregas tardías por ningún medio.

Es responsabilidad del estudiante gestionar oportunamente su trabajo y asegurar el cumplimiento del cronograma.

---

## 8. Criterios de Evaluación

### Puntuación

| Criterio | Puntos |
|----------|--------|
| Funcionalidad completa del pipeline (automatización total) | 6 |
| Modelo de minería de datos propio y efectivo (prohibido LLM) | 6 |
| Notificaciones Telegram/correo en todas las fases + issues automáticas | 3 |
| Despliegue automático en proveedor gratuito y funcional | 3 |
| Calidad del informe y documentación (README + notebook) | 2 |
| **TOTAL** | **20** |

### Penalizaciones

| Penalización | Puntos |
|--------------|--------|
| Uso de LLM (incluso parcial) | -20 (nota 0 automático) |
| Pipeline no completamente automático | -4 a -6 |
| Sin despliegue real | -3 |
| Otras | Variable |

---

## 📋 Resumen de Tareas Clave

- [ ] Entrenar modelo de ML clásico con accuracy ≥ 82%
- [ ] Configurar repos con ramas dev, test, main
- [ ] Implementar pipeline CI/CD con todas las etapas
- [ ] Crear bot de Telegram para notificaciones
- [ ] Configurar extractor de features y clasificador
- [ ] Configurar branch protection rules
- [ ] Realizar despliegue en proveedor gratuito
- [ ] Preparar documentación completa (README, notebook, informe)
- [ ] Realizar presentación demostrativa
