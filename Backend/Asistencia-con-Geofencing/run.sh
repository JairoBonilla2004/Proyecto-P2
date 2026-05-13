#!/bin/bash
# Cargar variables de entorno desde .env
set -a
source .env
set +a

# Ejecutar la aplicación
./mvnw spring-boot:run
