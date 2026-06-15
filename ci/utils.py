#!/usr/bin/env python3
"""
Utilidades compartidas para el pipeline de seguridad.

Contiene funciones y constantes comunes reutilizables.
"""

import json
import logging
from datetime import datetime
from enum import Enum
from pathlib import Path
from typing import Any, Optional

logger = logging.getLogger(__name__)


class SecurityLevel(Enum):
    """Niveles de seguridad."""

    CRITICAL = 1  # 90-100%
    HIGH = 2      # 70-89%
    MEDIUM = 3    # 50-69%
    LOW = 4       # 25-49%
    INFO = 5      # 0-24%


class Decision(Enum):
    """Decisiones de seguridad."""

    RECHAZAR = "RECHAZAR"
    ACEPTAR = "ACEPTAR"
    REVISAR = "REVISAR"


# Mapa de extensiones de archivo a lenguaje
FILE_LANGUAGE_MAP = {
    ".py": "python",
    ".java": "java",
    ".js": "javascript",
    ".ts": "typescript",
    ".jsx": "javascript",
    ".tsx": "typescript",
    ".go": "go",
    ".rs": "rust",
    ".cpp": "cpp",
    ".c": "c",
    ".cs": "csharp",
    ".php": "php",
    ".rb": "ruby",
    ".sql": "sql",
}

# Patrones de archivos a ignorar
IGNORED_PATTERNS = [
    "*.min.js",
    "*.min.css",
    "*.lock",
    "package-lock.json",
    "yarn.lock",
    "*.log",
    ".DS_Store",
    "*.pyc",
]

# Límites de tamaño
MAX_FILE_SIZE = 1024 * 100  # 100 KB
MAX_CODE_FRAGMENT = 1000    # caracteres


def get_security_level(risk_percentage: float) -> SecurityLevel:
    """
    Calcula el nivel de seguridad basado en porcentaje de riesgo.

    Args:
        risk_percentage: Porcentaje de riesgo (0-100)

    Returns:
        SecurityLevel
    """
    if risk_percentage >= 90:
        return SecurityLevel.CRITICAL
    elif risk_percentage >= 70:
        return SecurityLevel.HIGH
    elif risk_percentage >= 50:
        return SecurityLevel.MEDIUM
    elif risk_percentage >= 25:
        return SecurityLevel.LOW
    else:
        return SecurityLevel.INFO


def risk_percentage_to_severity(risk: float) -> str:
    """Convierte porcentaje de riesgo a descripción de severidad."""
    level = get_security_level(risk)
    return level.name


def get_language_from_file(filepath: str) -> str:
    """
    Detecta lenguaje del archivo.

    Args:
        filepath: Ruta del archivo

    Returns:
        Lenguaje detectado o "unknown"
    """
    ext = Path(filepath).suffix.lower()
    return FILE_LANGUAGE_MAP.get(ext, "unknown")


def should_ignore_file(filepath: str) -> bool:
    """
    Verifica si el archivo debe ser ignorado.

    Args:
        filepath: Ruta del archivo

    Returns:
        True si debe ignorarse
    """
    filename = Path(filepath).name

    # Ignorar patrones
    for pattern in IGNORED_PATTERNS:
        if filename.endswith(pattern.replace("*", "")):
            return True

    # Ignorar archivos muy grandes
    try:
        if Path(filepath).stat().st_size > MAX_FILE_SIZE:
            logger.warning(f"File too large, ignoring: {filepath}")
            return True
    except OSError:
        pass

    return False


def truncate_code(code: str, max_length: int = MAX_CODE_FRAGMENT) -> str:
    """
    Trunca código si es muy largo.

    Args:
        code: Código original
        max_length: Longitud máxima

    Returns:
        Código truncado
    """
    if len(code) > max_length:
        return code[:max_length] + "..."
    return code


def create_report_summary(
    predictions: list[dict],  # type: ignore
) -> dict:  # type: ignore
    """
    Crea resumen de reportes.

    Args:
        predictions: Lista de predicciones

    Returns:
        Diccionario con estadísticas
    """
    total = len(predictions)
    vulnerable = sum(
        1 for p in predictions
        if p.get("prediction") == "VULNERABLE"
    )
    safe = sum(
        1 for p in predictions
        if p.get("prediction") == "SEGURO"
    )
    errors = sum(1 for p in predictions if "error" in p)

    risk_percentage = (
        (vulnerable / total * 100) if total > 0 else 0
    )

    return {
        "total": total,
        "vulnerable": vulnerable,
        "safe": safe,
        "errors": errors,
        "risk_percentage": round(risk_percentage, 2),
        "severity_level": risk_percentage_to_severity(
            risk_percentage
        ),
        "decision": (
            "RECHAZAR" if vulnerable > 0 else "ACEPTAR"
        ),
    }


def save_json_report(
    data: dict,  # type: ignore
    filepath: str,
) -> Path:
    """
    Guarda reporte en JSON.

    Args:
        data: Datos a guardar
        filepath: Ruta de destino

    Returns:
        Path del archivo guardado
    """
    output_path = Path(filepath)
    output_path.parent.mkdir(parents=True, exist_ok=True)

    with open(output_path, "w") as f:
        json.dump(
            data,
            f,
            indent=2,
            ensure_ascii=False,
            default=str,
        )

    logger.info(f"Report saved: {output_path}")
    return output_path


def load_json_report(filepath: str) -> Optional[dict]:  # type: ignore
    """
    Carga reporte desde JSON.

    Args:
        filepath: Ruta del archivo

    Returns:
        Diccionario o None si no existe
    """
    try:
        path = Path(filepath)
        if path.exists():
            with open(path) as f:
                return json.load(f)
    except Exception as e:
        logger.error(f"Error loading report: {e}")

    return None


def format_timestamp(
    dt: Optional[datetime] = None,
    iso_format: bool = True,
) -> str:
    """
    Formatea timestamp.

    Args:
        dt: Datetime (default: now)
        iso_format: Usar formato ISO

    Returns:
        String formateado
    """
    if dt is None:
        dt = datetime.utcnow()

    if iso_format:
        return dt.isoformat() + "Z"
    else:
        return dt.strftime("%Y-%m-%d %H:%M:%S UTC")


def log_summary(summary: dict) -> None:  # type: ignore
    """
    Registra un resumen en el logger.

    Args:
        summary: Diccionario con resumen
    """
    logger.info("=" * 60)
    logger.info("SECURITY ANALYSIS SUMMARY")
    logger.info("=" * 60)
    logger.info(f"Total fragments: {summary.get('total', 0)}")
    logger.info(f"Safe: {summary.get('safe', 0)}")
    logger.info(f"Vulnerable: {summary.get('vulnerable', 0)}")
    logger.info(f"Risk level: {summary.get('severity_level', 'N/A')}")
    logger.info(f"Decision: {summary.get('decision', 'N/A')}")
    logger.info("=" * 60)


def validate_environment_variables(
    required_vars: list[str],
) -> bool:
    """
    Valida que las variables de entorno requeridas existan.

    Args:
        required_vars: Lista de nombres de variables

    Returns:
        True si todas existen
    """
    import os

    missing = []
    for var in required_vars:
        if not os.getenv(var):
            missing.append(var)

    if missing:
        logger.error(f"Missing environment variables: {', '.join(missing)}")
        return False

    return True


if __name__ == "__main__":
    # Tests simples
    print("Testing utilities...")

    # Test language detection
    assert get_language_from_file("test.py") == "python"
    assert get_language_from_file("test.java") == "java"
    print("✓ Language detection works")

    # Test security level
    assert get_security_level(95) == SecurityLevel.CRITICAL
    assert get_security_level(25) == SecurityLevel.LOW
    print("✓ Security level detection works")

    # Test truncate
    long_code = "x" * 2000
    truncated = truncate_code(long_code)
    assert len(truncated) <= 1010  # 1000 + "..."
    print("✓ Code truncation works")

    # Test timestamp
    ts = format_timestamp()
    assert ts.endswith("Z")
    print("✓ Timestamp formatting works")

    print("\n✅ All utility tests passed")
