# SecureFrame Gallery — Backend

Galería de imágenes segura con detección de esteganografía, limpieza de metadatos EXIF y gestión de cuarentena de contenido.

## Características

✅ **Autenticación & Identidad (RF01)**
- Registro local con contraseñas hasheadas en Argon2id
- Login con JWT Bearer tokens (HS256)
- OAuth2 federado con Google

✅ **Gestión de Álbumes (RF02)**
- Crear, listar, aprobar/rechazar álbumes
- Estados: PENDING_REVIEW → APPROVED | REJECTED
- Visualización pública de álbumes aprobados

✅ **Detección de Esteganografía (RF03)**
- Análisis LSB (Least Significant Bit)
- Entropía de Shannon para detectar información oculta
- Chi-square test (SPA - Sample Pairs Analysis)
- Detección de datos extra tras marcadores EOF

✅ **Gestión de Cuarentena (RF04)**
- Imágenes sospechosas enviadas a cuarentena automáticamente
- Cola de revisión para supervisores
- Aprobación/rechazo por supervisor

✅ **Visualización Segura (RF05)**
- Solo se exponen imágenes CLEAN o APPROVED
- Metadatos EXIF/XMP/IPTC eliminados antes de almacenamiento
- Re-codificación garantizada a PNG limpio

## Arquitectura

```
src/main/java/ec/edu/espe/SecureFrameGallery/
├── application/
│   ├── config/          # SecurityConfig, OpenApiConfig
│   ├── security/        # JwtAuthenticationFilter, RateLimitingFilter, SecurityHeadersFilter
│   └── exceptions/      # GlobalExceptionHandler
├── modules/
│   ├── auth/            # Autenticación (User, AuthService, AuthController)
│   ├── gallery/         # Álbumes e imágenes (Album, Image, GalleryService)
│   └── steganography/   # Análisis LSB y limpieza EXIF
└── shared/
    ├── dtos/            # ApiResponse, TokenResponse
    ├── enums/           # Role, ImageStatus, AlbumStatus
    └── utils/           # JwtUtil, Argon2Util, MagicNumberUtil
```

## Requisitos

- **Java 21+**
- **Maven 3.9+**
- **PostgreSQL 12+** (producción)
- **Docker** (opcional, para ejecución containerizada)

## Instalación Local

### 1. Configurar variables de entorno

```bash
# Base de datos
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=securegallery
export DB_USERNAME=postgres
export DB_PASSWORD=changeme

# JWT
export JWT_SECRET="supersecretkey-must-be-at-least-256-bits-long-for-HS256-algo"
export JWT_EXPIRATION_MS=3600000

# Google OAuth2
export GOOGLE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com
export GOOGLE_CLIENT_SECRET=your-google-client-secret

# Cloudinary
export CLOUDINARY_CLOUD_NAME=your-cloud-name
export CLOUDINARY_API_KEY=your-api-key
export CLOUDINARY_API_SECRET=your-api-secret
```

### 2. Instalar PostgreSQL (si no está disponible)

**macOS:**
```bash
brew install postgresql@15
brew services start postgresql@15
createdb securegallery
```

**Ubuntu/Debian:**
```bash
sudo apt-get install postgresql postgresql-contrib
sudo -u postgres createdb securegallery
```

**Windows:**
Descargar e instalar desde [postgresql.org](https://www.postgresql.org/download/windows/)

### 3. Compilar la aplicación

```bash
cd Backend/SecureFrameGallery
mvn clean package
```

### 4. Ejecutar localmente

```bash
java -jar target/SecureFrameGallery-0.0.1-SNAPSHOT.jar
```

O con Spring Boot Maven plugin:
```bash
mvn spring-boot:run
```

La aplicación estará disponible en `http://localhost:8080`

## Endpoints Principales

### Autenticación (Públicos)

```bash
# Registrar usuario
POST /api/v1/auth/register
Content-Type: application/json

{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "SecurePass@123"
}

# Login
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "SecurePass@123"
}

# OAuth2 Google
GET /api/v1/auth/oauth2/callback?code=...&state=...
```

### Álbumes

```bash
# Listar álbumes públicos
GET /api/v1/albums

# Crear álbum
POST /api/v1/albums
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

{
  "title": "My Album",
  "description": "Beautiful photos",
  "isPublic": true
}

# Mis álbumes
GET /api/v1/albums/mine
Authorization: Bearer <JWT_TOKEN>
```

### Imágenes

```bash
# Subir imagen
POST /api/v1/albums/{albumId}/images
Authorization: Bearer <JWT_TOKEN>
Content-Type: multipart/form-data

[archivo binario]

# Listar imágenes del álbum
GET /api/v1/albums/{albumId}/images
```

### Supervisor (Requiere ROLE_SUPERVISOR)

```bash
# Listar álbumes pendientes
GET /api/v1/supervisor/albums/pending
Authorization: Bearer <SUPERVISOR_JWT>

# Aprobar álbum
PUT /api/v1/supervisor/albums/{albumId}/approve
Authorization: Bearer <SUPERVISOR_JWT>

# Listar cuarentena
GET /api/v1/supervisor/quarantine
Authorization: Bearer <SUPERVISOR_JWT>

# Aprobar imagen
PUT /api/v1/supervisor/images/{imageId}/approve
Authorization: Bearer <SUPERVISOR_JWT>
Content-Type: application/json

{
  "notes": "Imagen limpia, aprobada"
}
```

## Documentación Interactiva

Accede a Swagger UI en:

```
http://localhost:8080/swagger-ui.html
```

Para autenticarte en Swagger:
1. Click en **"Authorize"** (arriba a la derecha)
2. Pegá el token JWT obtenido tras login
3. Click en **"Authorize"** en el modal

## Seguridad (RNF-Seguridad)

✅ **Validación de Entrada**
- Magic bytes (detección real de tipo de archivo, no extensión)
- Validación de contraseñas (min. 8 caracteres, mayúscula, minúscula, número, especial)
- Saneamiento de texto (XSS prevention)

✅ **Autenticación & Autorización**
- Argon2id para hashing de contraseñas
- JWT con firma HS256
- Rate limiting: 5 req/min en /api/v1/auth
- Anti-enumeración: mensajes genéricos de error

✅ **Análisis de Seguridad**
- LSB Entropy analysis para detectar esteganografía
- Detección de datos extra (EOF anomalies)
- Chi-square test (SPA)
- Eliminación garantizada de metadatos EXIF

✅ **Headers de Seguridad**
- Content-Security-Policy: `default-src 'self'`
- X-Content-Type-Options: `nosniff`
- X-Frame-Options: `DENY`
- Strict-Transport-Security: `max-age=31536000`
- X-XSS-Protection: `1; mode=block`

✅ **CORS Restrictivo**
- Solo orígenes configurados: localhost:3000, localhost:4200

## Docker

### Build

```bash
docker build -t secureframe-gallery:latest .
```

### Run

```bash
docker run -d \
  -e DB_HOST=host.docker.internal \
  -e DB_PORT=5432 \
  -e DB_NAME=securegallery \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=changeme \
  -e JWT_SECRET="your-secret-key" \
  -e GOOGLE_CLIENT_ID="..." \
  -e GOOGLE_CLIENT_SECRET="..." \
  -e CLOUDINARY_CLOUD_NAME="..." \
  -e CLOUDINARY_API_KEY="..." \
  -e CLOUDINARY_API_SECRET="..." \
  -p 8080:8080 \
  --name secureframe \
  secureframe-gallery:latest
```

## Testing

```bash
# Ejecutar tests
mvn test

# Con cobertura
mvn test jacoco:report
```

## Build para Producción

```bash
# Compilar sin tests (velocidad)
mvn clean package -DskipTests

# JAR en target/
ls -lh target/SecureFrameGallery-0.0.1-SNAPSHOT.jar
```

## Troubleshooting

**Puerto 8080 ya en uso:**
```bash
# Cambiar puerto
java -jar target/*.jar --server.port=9090
```

**Error de conexión a PostgreSQL:**
```bash
# Verificar que PostgreSQL está corriendo
psql -h localhost -U postgres -d securegallery -c "SELECT 1"
```

**JWT inválido en Swagger:**
- Verificar que el token no ha expirado (1 hora por defecto)
- Hacer login de nuevo para obtener token fresco

## Estructura de Tablas

```sql
-- Usuarios
CREATE TABLE users (
  id UUID PRIMARY KEY,
  username VARCHAR(50) UNIQUE NOT NULL,
  email VARCHAR(150) UNIQUE NOT NULL,
  password_hash VARCHAR(255),
  role VARCHAR(20) NOT NULL,
  provider VARCHAR(20) NOT NULL,
  enabled BOOLEAN NOT NULL,
  created_at TIMESTAMP
);

-- Álbumes
CREATE TABLE albums (
  id UUID PRIMARY KEY,
  title VARCHAR(100) NOT NULL,
  description VARCHAR(500),
  approval_status VARCHAR(20) NOT NULL,
  public BOOLEAN NOT NULL,
  owner_id UUID NOT NULL REFERENCES users(id),
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);

-- Imágenes
CREATE TABLE images (
  id UUID PRIMARY KEY,
  original_name VARCHAR(255) NOT NULL,
  stored_url VARCHAR(512),
  cloudinary_public_id VARCHAR(255),
  mime_type VARCHAR(30) NOT NULL,
  image_status VARCHAR(20) NOT NULL,
  lsb_entropy_score DOUBLE,
  analysis_result VARCHAR(500),
  album_id UUID NOT NULL REFERENCES albums(id),
  uploaded_by_id UUID NOT NULL REFERENCES users(id),
  uploaded_at TIMESTAMP
);

-- Cuarentena
CREATE TABLE quarantine_logs (
  id UUID PRIMARY KEY,
  image_id UUID NOT NULL REFERENCES images(id),
  detection_reason VARCHAR(1000) NOT NULL,
  lsb_score DOUBLE,
  eof_anomaly BOOLEAN,
  reviewed_by_id UUID REFERENCES users(id),
  reviewed_at TIMESTAMP,
  supervisor_decision VARCHAR(20),
  supervisor_notes VARCHAR(500),
  created_at TIMESTAMP
);
```

## Licencia

Proyecto académico — ESPE 2026

## Contacto

Para preguntas o reportes de bugs, contactá al equipo de SecureFrame.
