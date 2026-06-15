#!/usr/bin/env python3
"""
Módulo de puerta de seguridad con predicción ML.

Carga el modelo entrenado, aplica el pipeline de preprocesamiento,
genera predicciones de seguridad y reportes JSON.
"""

import json
import logging
import sys
from datetime import datetime
from pathlib import Path
from typing import Optional

import joblib
import numpy as np
from sklearn.preprocessing import StandardScaler

from feature_extractor import FeatureExtractor

logger = logging.getLogger(__name__)


class SecurityGate:
    """Puerta de seguridad con modelo ML."""

    def __init__(self, model_artifacts_path: str):
        """
        Inicializa la puerta de seguridad.

        Args:
            model_artifacts_path: Ruta a la carpeta con artefactos .joblib
        """
        self.artifacts_path = Path(model_artifacts_path)
        self.model = None
        self.vectorizer = None
        self.scaler = None
        self.feature_selector = None

        self._load_artifacts()

    def _load_artifacts(self):
        """Carga los artefactos del modelo."""
        model_file = self.artifacts_path / "modelo_ensemble.joblib"
        vectorizer_file = self.artifacts_path / "vectorizador_tfidf.joblib"
        scaler_file = self.artifacts_path / "scaler_estructural.joblib"
        selector_file = self.artifacts_path / "selector_features.joblib"

        # Verificar si el modelo existe
        if not model_file.exists():
            logger.warning(
                f" Model artifacts not found at {self.artifacts_path}. "
                "Pipeline will run without ML predictions."
            )
            self.model = None
            return

        try:
            self.model = joblib.load(model_file)
            logger.info(" Model loaded successfully")

            if vectorizer_file.exists():
                self.vectorizer = joblib.load(vectorizer_file)
                logger.info(" Vectorizer loaded")

            if scaler_file.exists():
                self.scaler = joblib.load(scaler_file)
                logger.info("✅ Scaler loaded")

            if selector_file.exists():
                self.feature_selector = joblib.load(selector_file)
                logger.info("✅ Feature selector loaded")

        except Exception as e:
            logger.error(f"Error loading artifacts: {e}")
            self.model = None

    def _prepare_features(self, code: str) -> np.ndarray:
        """
        Prepara features del código.

        Args:
            code: Código fuente

        Returns:
            Array de features preparadas
        """
        extractor = FeatureExtractor(language="auto")
        features = extractor.extract(code)

        # Convertir features a array
        feature_vector = np.array(
            [
                features.token_count,
                features.ast_depth,
                int(features.has_eval),
                int(features.has_exec),
                int(features.has_sql_concat),
                int(features.has_os_system),
                int(features.has_subprocess),
                int(features.has_runtime_exec),
                int(features.has_processbuilder),
                int(features.has_input_validation),
                int(features.has_sanitization),
                int(features.has_try_catch),
                features.lines_of_code,
                features.dangerous_functions_count,
            ],
            dtype=np.float32,
        )

        return feature_vector.reshape(1, -1)

    def _preprocess_features(self, feature_vector: np.ndarray) -> np.ndarray:
        """
        Aplica el pipeline de preprocesamiento.

        Args:
            feature_vector: Features crudas

        Returns:
            Features preprocesadas
        """
        # Aplicar escalador si está disponible
        if self.scaler is not None:
            feature_vector = self.scaler.transform(feature_vector)

        # Aplicar selector de features si está disponible
        if self.feature_selector is not None:
            feature_vector = self.feature_selector.transform(feature_vector)

        return feature_vector

    def predict(
        self, code: str
    ) -> dict[str, any]:  # noqa: F821  # type: ignore
        """
        Genera predicción de seguridad.

        Args:
            code: Código fuente a analizar

        Returns:
            Dict con {prediction, probability, risk_level}
        """
        # Si no hay modelo, retornar predicción neutra
        if self.model is None:
            logger.warning("Model not available, returning neutral prediction")
            return {
                "prediction": "NO_MODEL",
                "probability_safe": 0.5,
                "probability_vulnerable": 0.5,
                "risk_level": 50,
                "decision": "REVISAR",
            }

        try:
            # Preparar features
            feature_vector = self._prepare_features(code)
            logger.debug("Features prepared")

            # Preprocesar
            processed_features = self._preprocess_features(feature_vector)
            logger.debug("Features preprocessed")

            # Predecir
            prediction = self.model.predict(processed_features)[0]
            probabilities = self.model.predict_proba(processed_features)[0]

            # Mapear predicción a etiqueta
            prediction_label = "VULNERABLE" if prediction == 1 else "SEGURO"

            # Extraer probabilidades
            prob_safe = float(probabilities[0])
            prob_vulnerable = float(probabilities[1])

            # Calcular nivel de riesgo (0-100)
            risk_level = int(prob_vulnerable * 100)

            result = {
                "prediction": prediction_label,
                "probability_safe": round(prob_safe, 4),
                "probability_vulnerable": round(prob_vulnerable, 4),
                "risk_level": risk_level,
                "decision": "RECHAZAR" if prediction == 1 else "ACEPTAR",
            }

            logger.info(
                f"Prediction: {prediction_label} (risk: {risk_level}%, confidence: {max(prob_safe, prob_vulnerable):.2%})"
            )
            return result

        except Exception as e:
            logger.error(f"Error during prediction: {e}")
            raise

    def predict_batch(
        self, code_fragments: list[str]
    ) -> list[dict]:  # type: ignore
        """
        Realiza predicciones en lote.

        Args:
            code_fragments: Lista de códigos

        Returns:
            Lista de resultados de predicción
        """
        results = []

        for idx, code in enumerate(code_fragments):
            try:
                result = self.predict(code)
                result["fragment_index"] = idx
                results.append(result)
            except Exception as e:
                logger.error(f"Error predicting fragment {idx}: {e}")
                results.append(
                    {
                        "fragment_index": idx,
                        "prediction": "ERROR",
                        "error": str(e),
                    }
                )

        return results

    def generate_report(
        self,
        predictions: list[dict],  # type: ignore
        output_file: str = "reports/security_report.json",
        pr_number: Optional[int] = None,
        commit_sha: Optional[str] = None,
    ) -> str:
        """
        Genera reporte de seguridad.

        Args:
            predictions: Lista de predicciones
            output_file: Ruta de salida
            pr_number: Número de PR (opcional)
            commit_sha: SHA del commit (opcional)

        Returns:
            Ruta del archivo generado
        """
        # Calcular estadísticas
        total_predictions = len(predictions)
        vulnerable_count = sum(
            1
            for p in predictions
            if p.get("prediction") == "VULNERABLE"
        )
        safe_count = sum(
            1
            for p in predictions
            if p.get("prediction") == "SEGURO"
        )
        no_model_count = sum(
            1
            for p in predictions
            if p.get("prediction") == "NO_MODEL"
        )

        overall_risk = (
            (vulnerable_count / total_predictions * 100)
            if total_predictions > 0
            else 0
        )

        # Determinar resultado general
        # Si hay modelo, decidir basado en vulnerabilidades
        # Si no hay modelo, decidir basado en features análisis
        if self.model is None and no_model_count > 0:
            overall_decision = "REVISAR"
            exit_code = 0  # No fallar si no hay modelo
        else:
            overall_decision = (
                "RECHAZAR" if vulnerable_count > 0 else "ACEPTAR"
            )
            exit_code = 1 if vulnerable_count > 0 else 0

        report = {
            "timestamp": datetime.utcnow().isoformat() + "Z",
            "pr_number": pr_number,
            "commit_sha": commit_sha,
            "model_available": self.model is not None,
            "summary": {
                "total_fragments": total_predictions,
                "vulnerable_count": vulnerable_count,
                "safe_count": safe_count,
                "no_model_count": no_model_count,
                "overall_risk_percentage": round(overall_risk, 2),
                "overall_decision": overall_decision,
            },
            "predictions": predictions,
            "exit_code": exit_code,
        }

        # Crear directorio si no existe
        output_path = Path(output_file)
        output_path.parent.mkdir(parents=True, exist_ok=True)

        # Guardar reporte
        with open(output_path, "w") as f:
            json.dump(report, f, indent=2, ensure_ascii=False)

        logger.info(f"Security report generated: {output_file}")
        logger.info(
            f"Overall: {safe_count} safe, {vulnerable_count} vulnerable, "
            f"{no_model_count} no_model"
        )

        return str(output_path)

    def get_exit_code(self, report: dict) -> int:  # type: ignore
        """
        Obtiene el código de salida basado en el reporte.

        Args:
            report: Reporte de seguridad

        Returns:
            0 si es seguro, 1 si es vulnerable
        """
        return report.get("exit_code", 0)


def main():
    """Función principal para testing."""
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    )

    try:
        # Inicializar gate
        gate = SecurityGate(
            "modelo/model_artifacts"
        )

        # Código de prueba
        test_code = """
import subprocess
user_input = input("Command: ")
subprocess.run(user_input, shell=True)
"""

        # Predicción
        result = gate.predict(test_code)
        print("Prediction result:")
        print(json.dumps(result, indent=2))

        # Generar reporte
        predictions = [result]
        report_path = gate.generate_report(
            predictions,
            "reports/security_report.json",
            pr_number=1,
            commit_sha="abc123",
        )

        print(f"\nReport generated: {report_path}")

    except Exception as e:
        logger.error(f"Main error: {e}")
        sys.exit(1)


if __name__ == "__main__":
    main()