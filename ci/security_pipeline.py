#!/usr/bin/env python3
"""
Orquestador principal del pipeline de seguridad.

Coordina la extracción de cambios, análisis de features,
predicción ML y generación de reportes.
"""

import json
import logging
import os
import sys
from pathlib import Path
from typing import Optional

from diff_extractor import DiffExtractor
from security_gate import SecurityGate
from telegram_notifier import TelegramNotifier

# Configurar logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    handlers=[
        logging.FileHandler("security_pipeline.log"),
        logging.StreamHandler(),
    ],
)

logger = logging.getLogger(__name__)


class SecurityPipeline:
    """Orquestador del pipeline de seguridad."""

    def __init__(
        self,
        base_branch: str = "main",
        head_branch: str = "HEAD",
        model_path: str = "modelo/model_artifacts",
        report_path: str = "reports/security_report.json",
    ):
        """
        Inicializa el pipeline.

        Args:
            base_branch: Rama base para comparación
            head_branch: Rama a analizar
            model_path: Ruta a artefactos del modelo
            report_path: Ruta de salida del reporte
        """
        self.base_branch = base_branch
        self.head_branch = head_branch
        self.model_path = model_path
        self.report_path = report_path

        self.diff_extractor = DiffExtractor(base_branch, head_branch)
        self.security_gate = SecurityGate(model_path)

        # Notificador es opcional
        try:
            self.telegram_notifier = TelegramNotifier()
            logger.info("Telegram notifier initialized")
        except ValueError:
            self.telegram_notifier = None
            logger.warning("Telegram notifier not available (missing env vars)")

        logger.info("SecurityPipeline initialized")

    def extract_code_to_analyze(self) -> list[dict]:
        """
        Extrae fragmentos de código a analizar.

        Returns:
            Lista de fragmentos {file, line, code, type}
        """
        logger.info("Step 1: Extracting code fragments from diff")

        try:
            fragments = self.diff_extractor.extract_code_fragments()
            logger.info(f"Extracted {len(fragments)} code fragments")

            return fragments

        except Exception as e:
            logger.error(f"Error extracting fragments: {e}")
            if self.telegram_notifier:
                self.telegram_notifier.notify_error(
                    pr_number=self._get_pr_number(),
                    repository=self._get_repository_name(),
                    error_message=f"Failed to extract code: {e}",
                )
            raise

    def predict_security(
        self, code_fragments: list[str]
    ) -> list[dict]:  # type: ignore
        """
        Realiza predicciones de seguridad.

        Args:
            code_fragments: Lista de códigos a predecir

        Returns:
            Lista de predicciones
        """
        logger.info("Step 3: Running ML security predictions")

        if not code_fragments:
            logger.warning("No code fragments to predict")
            return []

        try:
            predictions = self.security_gate.predict_batch(code_fragments)
            logger.info(f"Predictions generated for {len(predictions)} fragments")

            return predictions

        except Exception as e:
            logger.error(f"Error during predictions: {e}")
            if self.telegram_notifier:
                self.telegram_notifier.notify_error(
                    pr_number=self._get_pr_number(),
                    repository=self._get_repository_name(),
                    error_message=f"Failed to predict security: {e}",
                )
            raise

    def generate_report(
        self,
        predictions: list[dict],  # type: ignore
        code_fragments: list[dict],
    ) -> dict:  # type: ignore
        """
        Genera reporte de seguridad.

        Args:
            predictions: Lista de predicciones
            code_fragments: Fragmentos originales para contexto

        Returns:
            Diccionario con el reporte
        """
        logger.info("Step 4: Generating security report")

        try:
            # Agregar contexto a las predicciones
            enriched_predictions = []
            for pred, frag in zip(
                predictions, code_fragments
            ):
                enriched = {**pred, **frag}
                enriched_predictions.append(enriched)

            report_path = self.security_gate.generate_report(
                enriched_predictions,
                output_file=self.report_path,
                pr_number=self._get_pr_number(),
                commit_sha=self._get_commit_sha(),
            )

            # Leer el reporte generado
            with open(report_path) as f:
                report = json.load(f)

            logger.info(f"Report generated: {report_path}")

            return report

        except Exception as e:
            logger.error(f"Error generating report: {e}")
            if self.telegram_notifier:
                self.telegram_notifier.notify_error(
                    pr_number=self._get_pr_number(),
                    repository=self._get_repository_name(),
                    error_message=f"Failed to generate report: {e}",
                )
            raise

    def notify_results(self, report: dict) -> bool:  # type: ignore
        """
        Envía notificaciones de resultados.

        Args:
            report: Reporte de seguridad

        Returns:
            True si se notificó exitosamente
        """
        if not self.telegram_notifier:
            logger.warning("Telegram notifier not available, skipping notifications")
            return False

        try:
            summary = report.get("summary", {})
            decision = summary.get("overall_decision", "DESCONOCIDO")

            logger.info("Step 5: Sending notifications")

            # Notificar resultado
            success = self.telegram_notifier.notify_pr_scan_completed(
                pr_number=self._get_pr_number(),
                repository=self._get_repository_name(),
                safe_count=summary.get("safe_count", 0),
                vulnerable_count=summary.get("vulnerable_count", 0),
                overall_decision=decision,
                risk_percentage=summary.get("overall_risk_percentage", 0),
            )

            if success:
                logger.info("Notification sent successfully")
            else:
                logger.warning("Failed to send notification")

            return success

        except Exception as e:
            logger.error(f"Error sending notifications: {e}")
            return False

    def run(self) -> int:
        """
        Ejecuta el pipeline completo.

        Returns:
            0 si es seguro, 1 si hay vulnerabilidades
        """
        try:
            logger.info("=" * 60)
            logger.info("SECURITY PIPELINE STARTED")
            logger.info("=" * 60)

            # Notificar inicio
            if self.telegram_notifier:
                self.telegram_notifier.notify_pr_scan_started(
                    pr_number=self._get_pr_number(),
                    repository=self._get_repository_name(),
                    branch=self.head_branch,
                )

            # Paso 1: Extraer código
            fragments = self.extract_code_to_analyze()

            if not fragments:
                logger.info("No code changes to analyze")
                return 0

            # Extraer solo los códigos para predicción
            code_snippets = [f.get("code", "") for f in fragments]

            # Paso 2: Predicción
            predictions = self.predict_security(code_snippets)

            # Paso 3: Generar reporte
            report = self.generate_report(predictions, fragments)

            # Paso 4: Notificar
            self.notify_results(report)

            # Obtener código de salida
            exit_code = report.get("exit_code", 0)

            logger.info("=" * 60)
            if exit_code == 0:
                logger.info("✓ SECURITY GATE PASSED")
            else:
                logger.info("✗ SECURITY GATE FAILED")
            logger.info("=" * 60)

            return exit_code

        except Exception as e:
            logger.error(f"Pipeline failed: {e}", exc_info=True)
            return 1

    def _get_pr_number(self) -> int:
        """Obtiene número de PR desde variables de entorno."""
        try:
            pr = os.getenv("GITHUB_PR_NUMBER", "").strip()
            return int(pr) if pr else 0
        except (ValueError, TypeError):
            return 0

    def _get_commit_sha(self) -> Optional[str]:
        """Obtiene SHA del commit desde variables de entorno."""
        sha = os.getenv("GITHUB_SHA", "").strip()
        return sha if sha else None

    def _get_repository_name(self) -> str:
        """Obtiene nombre del repositorio desde variables de entorno."""
        repo = os.getenv("GITHUB_REPOSITORY", "").strip()
        return repo if repo else "unknown"


def main():
    """Función principal."""
    try:
        # Configurar rutas
        # BASE_BRANCH puede ser un SHA (cuando viene de un PR) o nombre de rama
        base_branch = os.getenv("BASE_BRANCH") or os.getenv("GITHUB_BASE_SHA", "HEAD~1")
        head_branch = os.getenv("HEAD_BRANCH", "HEAD")
        model_path = os.getenv("MODEL_PATH", "../modelo/model_artifacts")
        report_path = os.getenv(
            "REPORT_PATH", "../reports/security_report.json"
        )

        # Crear y ejecutar pipeline
        pipeline = SecurityPipeline(
            base_branch=base_branch,
            head_branch=head_branch,
            model_path=model_path,
            report_path=report_path,
        )

        exit_code = pipeline.run()
        sys.exit(exit_code)

    except KeyboardInterrupt:
        logger.info("Pipeline interrupted by user")
        sys.exit(130)
    except Exception as e:
        logger.error(f"Unhandled error: {e}", exc_info=True)
        sys.exit(1)


if __name__ == "__main__":
    main()
