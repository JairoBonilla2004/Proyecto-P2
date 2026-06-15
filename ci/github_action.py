#!/usr/bin/env python3
"""
Script de integración para GitHub Actions.

Ejecuta el pipeline de seguridad con las variables del contexto de GitHub.
"""

import json
import logging
import os
import sys
from pathlib import Path

from security_pipeline import SecurityPipeline

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
)

logger = logging.getLogger(__name__)


def setup_github_environment():
    """Configura variables de entorno desde GitHub Actions context."""
    env_vars = {
        "GITHUB_PR_NUMBER": os.getenv("GITHUB_REF", "").split("/")[2]
        if "pull" in os.getenv("GITHUB_REF", "")
        else "0",
        "GITHUB_SHA": os.getenv("GITHUB_SHA", ""),
        "GITHUB_REPOSITORY": os.getenv("GITHUB_REPOSITORY", ""),
        "GITHUB_BASE_REF": os.getenv("GITHUB_BASE_REF", "main"),
        "GITHUB_HEAD_REF": os.getenv("GITHUB_HEAD_REF", "HEAD"),
    }

    logger.info(f"GitHub Context: {json.dumps(env_vars, indent=2)}")

    return env_vars


def main():
    """Función principal."""
    try:
        logger.info("Security Pipeline GitHub Action started")

        # Setup GitHub environment
        github_env = setup_github_environment()

        # Rutas
        model_path = Path("modelo/model_artifacts")
        report_path = Path("reports/security_report.json")

        if not model_path.exists():
            logger.error(f"Model path does not exist: {model_path}")
            sys.exit(1)

        # Crear pipeline
        pipeline = SecurityPipeline(
            base_branch=github_env.get("GITHUB_BASE_REF", "main"),
            head_branch=github_env.get("GITHUB_HEAD_REF", "HEAD"),
            model_path=str(model_path),
            report_path=str(report_path),
        )

        # Ejecutar pipeline
        exit_code = pipeline.run()

        # Output para GitHub Actions
        if report_path.exists():
            with open(report_path) as f:
                report = json.load(f)

            summary = report.get("summary", {})

            print("\n::group::Security Report Summary")
            print(json.dumps(summary, indent=2))
            print("::endgroup::")

            # Set output para otros pasos del workflow
            print(
                f"::set-output name=exit_code::{exit_code}"
            )
            print(
                f"::set-output name=vulnerable_count::{summary.get('vulnerable_count', 0)}"
            )
            print(
                f"::set-output name=safe_count::{summary.get('safe_count', 0)}"
            )
            print(
                f"::set-output name=overall_decision::{summary.get('overall_decision', 'UNKNOWN')}"
            )
            print(
                f"::set-output name=risk_percentage::{summary.get('overall_risk_percentage', 0)}"
            )

            # Crear anotación si hay vulnerabilidades
            if exit_code != 0:
                print(
                    f"::error title=Security Gate Failed::{summary.get('vulnerable_count', 0)} "
                    f"vulnerabilidades detectadas"
                )

        sys.exit(exit_code)

    except Exception as e:
        logger.error(f"Pipeline error: {e}", exc_info=True)
        print(f"::error::Pipeline error: {e}")
        sys.exit(1)


if __name__ == "__main__":
    main()
