#!/usr/bin/env python3
"""
Módulo de puerta de seguridad con predicción ML.

Pipeline de inferencia que replica exactamente el del notebook:
  1. Extrae features estructurales (AST + Regex) → dict de 24 features
  2. Vectoriza el código con TF-IDF (vectorizador_tfidf.joblib)
  3. Escala features estructurales con MinMaxScaler (scaler_estructural.joblib)
  4. Combina TF-IDF + estructurales con hstack (scipy.sparse)
  5. Selecciona las 3000 mejores features (selector_features.joblib)
  6. Predice con el ensemble RandomForest + XGBoost (modelo_ensemble.joblib)
"""

import ast
import json
import logging
import math
import re
import sys
from datetime import datetime
from pathlib import Path
from typing import Optional

import joblib
import numpy as np
from scipy.sparse import csr_matrix, hstack

logger = logging.getLogger(__name__)


# =============================================================================
# EXTRACCIÓN DE FEATURES — réplica exacta del Bloque 2 del notebook
# =============================================================================

def _ast_depth(tree) -> int:
    """Calcula recursivamente la profundidad máxima del árbol AST."""
    if not isinstance(tree, ast.AST):
        return 0
    children = list(ast.iter_child_nodes(tree))
    if not children:
        return 1
    return 1 + max(_ast_depth(child) for child in children)


def _shannon_entropy(text: str) -> float:
    """Calcula la entropía de Shannon de una cadena de texto."""
    if not text:
        return 0.0
    freq: dict = {}
    for c in text:
        freq[c] = freq.get(c, 0) + 1
    n = len(text)
    return -sum((f / n) * math.log2(f / n) for f in freq.values())


# Patrones peligrosos
_PAT_DANGEROUS = re.compile(
    r'\b(eval|exec|execfile|compile|__import__|getattr|setattr|delattr)\s*\(',
    re.IGNORECASE,
)
_PAT_SUBPROCESS_SHELL = re.compile(
    r'subprocess\.(call|run|Popen|check_output|check_call)\s*\([^)]*shell\s*=\s*True',
    re.IGNORECASE | re.DOTALL,
)
_PAT_SUBPROCESS_ANY = re.compile(
    r'subprocess\.(call|run|Popen|check_output|check_call|getoutput)\s*\(',
    re.IGNORECASE,
)
_PAT_OS_COMMANDS = re.compile(
    r'os\.(system|popen|execv|execve|execvp|execvpe|spawnv|spawnve)\s*\(',
    re.IGNORECASE,
)
_PAT_SQL_RAW = re.compile(
    r'(\.execute\s*\([^?%]*(\+|%|format|f[\'""]|\{)|"\'\\s*\+\s*\w+\s*\+\s*"\')',
    re.IGNORECASE,
)
_PAT_SQL_FSTRING = re.compile(
    r'(SELECT|INSERT|UPDATE|DELETE|DROP|UNION|FROM|WHERE)[^;]*"\'\\s*(%|format|f\'|f\")',
    re.IGNORECASE,
)
_PAT_PICKLE = re.compile(r'\bpickle\.(loads|load|Unpickler)\s*\(', re.IGNORECASE)
_PAT_PATH_CONCAT = re.compile(r'(open|file)\s*\(\s*[\w\'"]+\s*\+\s*\w+', re.IGNORECASE)
_PAT_WEAK_HASH = re.compile(r'hashlib\.(md5|sha1)\s*\(', re.IGNORECASE)
_PAT_INSECURE_RANDOM = re.compile(
    r'random\.(randint|random|choice|shuffle|sample)\s*\(', re.IGNORECASE
)

# Patrones de sanitización (seguro)
_PAT_SANITIZATION = re.compile(
    r'\b(escape|sanitize|sanitise|is_valid|validate|clean|purify|'
    r'strip_tags|bleach|html\.escape|markupsafe|'
    r'parameterize|prepared_statement|placeholder|'
    r'allowed_extensions|whitelist|ALLOWED|safe_path|realpath|'
    r'compare_digest|escape_filter_chars|defusedxml|'
    r'safe_load|create_default_context|token_urlsafe|secrets\.)\b',
    re.IGNORECASE,
)
_PAT_SQL_PARAM = re.compile(
    r'\.execute\s*\(\s*[\'"][^\'"]*(\?|%s|:param|:\w+)[\'"]',
    re.IGNORECASE,
)
_PAT_ENV_VARS = re.compile(r'os\.environ\.(get|__getitem__)\s*\(', re.IGNORECASE)
_PAT_TYPE_VALID = re.compile(
    r'isinstance\s*\(|type\s*\(\w+\)\s*==|assert\s+', re.IGNORECASE
)
_PAT_EXCEPTIONS = re.compile(r'\btry\s*:.*?\bexcept\b', re.IGNORECASE | re.DOTALL)
_PAT_SECURE_IMPORTS = re.compile(
    r'import\s+(bcrypt|argon2|cryptography|secrets|hmac|defusedxml|bleach)',
    re.IGNORECASE,
)
_PAT_STR_CONCAT = re.compile(r"""['"][^'"]*['"]\s*\+|\+\s*['"][^'"]*['"]""")
_PAT_SEC_COMMENTS = re.compile(
    r'#.*(segur|safe|valid|sanitiz|escape|authen|authori|permis)', re.IGNORECASE
)


def extraer_features_codigo(codigo: str) -> dict:
    """
    Extrae el vector de 24 features estructurales del código fuente.
    Réplica exacta de la función del notebook (Bloque 2).
    """
    features: dict = {}

    # --- AST ---
    try:
        tree = ast.parse(codigo)
        features["ast_depth"] = _ast_depth(tree)
        features["ast_node_count"] = sum(1 for _ in ast.walk(tree))
        features["ast_parse_error"] = 0
        features["func_calls_count"] = sum(
            1 for node in ast.walk(tree) if isinstance(node, ast.Call)
        )
        branch_nodes = (
            ast.If, ast.For, ast.While, ast.Try, ast.ExceptHandler,
            ast.With, ast.Assert, ast.comprehension,
        )
        features["cyclomatic_complexity"] = (
            sum(1 for node in ast.walk(tree) if isinstance(node, branch_nodes)) + 1
        )
    except SyntaxError:
        features["ast_depth"] = 0
        features["ast_node_count"] = 0
        features["ast_parse_error"] = 1
        features["func_calls_count"] = 0
        features["cyclomatic_complexity"] = 1

    # --- Llamadas peligrosas ---
    features["dangerous_func_calls"] = len(_PAT_DANGEROUS.findall(codigo))
    features["subprocess_shell"] = 1 if _PAT_SUBPROCESS_SHELL.search(codigo) else 0
    features["subprocess_any"] = 1 if _PAT_SUBPROCESS_ANY.search(codigo) else 0
    features["os_commands"] = 1 if _PAT_OS_COMMANDS.search(codigo) else 0
    features["sql_raw_concat"] = 1 if _PAT_SQL_RAW.search(codigo) else 0
    features["sql_fstring"] = 1 if _PAT_SQL_FSTRING.search(codigo) else 0
    features["pickle_usage"] = 1 if _PAT_PICKLE.search(codigo) else 0
    features["path_concat"] = 1 if _PAT_PATH_CONCAT.search(codigo) else 0
    features["weak_hash"] = 1 if _PAT_WEAK_HASH.search(codigo) else 0
    features["insecure_random"] = 1 if _PAT_INSECURE_RANDOM.search(codigo) else 0

    # --- Sanitización ---
    features["sanitization_present"] = 1 if _PAT_SANITIZATION.search(codigo) else 0
    features["sql_parameterized"] = 1 if _PAT_SQL_PARAM.search(codigo) else 0
    features["env_vars_used"] = 1 if _PAT_ENV_VARS.search(codigo) else 0
    features["type_validation"] = 1 if _PAT_TYPE_VALID.search(codigo) else 0
    features["exception_handling"] = 1 if _PAT_EXCEPTIONS.search(codigo) else 0
    features["secure_imports"] = 1 if _PAT_SECURE_IMPORTS.search(codigo) else 0

    # --- Métricas estructurales ---
    features["shannon_entropy"] = _shannon_entropy(codigo)
    features["lines_count"] = codigo.count("\n") + 1
    features["string_concat_count"] = len(_PAT_STR_CONCAT.findall(codigo))
    features["security_comments"] = len(_PAT_SEC_COMMENTS.findall(codigo))
    features["raise_count"] = len(re.findall(r'\braise\b', codigo))

    # --- Scores compuestos ---
    features["total_danger_score"] = (
        features["dangerous_func_calls"] * 3
        + features["subprocess_shell"] * 3
        + features["subprocess_any"] * 1
        + features["os_commands"] * 2
        + features["sql_raw_concat"] * 3
        + features["sql_fstring"] * 3
        + features["pickle_usage"] * 2
        + features["path_concat"] * 2
        + features["weak_hash"] * 1
        + features["insecure_random"] * 1
        + features["string_concat_count"] * 1
    )
    features["total_safety_score"] = (
        features["sanitization_present"] * 3
        + features["sql_parameterized"] * 3
        + features["env_vars_used"] * 2
        + features["type_validation"] * 2
        + features["exception_handling"] * 1
        + features["secure_imports"] * 3
        + features["security_comments"] * 1
        + features["raise_count"] * 1
    )

    return features


# =============================================================================
# SECURITY GATE
# =============================================================================

class SecurityGate:
    """Puerta de seguridad con modelo ML (pipeline idéntico al notebook)."""

    def __init__(self, model_artifacts_path: str):
        self.artifacts_path = Path(model_artifacts_path)
        self.model = None
        self.vectorizer = None
        self.scaler = None
        self.feature_selector = None
        self._load_artifacts()

    def _load_artifacts(self):
        """Carga los 4 artefactos del modelo."""
        model_file = self.artifacts_path / "modelo_ensemble.joblib"
        vectorizer_file = self.artifacts_path / "vectorizador_tfidf.joblib"
        scaler_file = self.artifacts_path / "scaler_estructural.joblib"
        selector_file = self.artifacts_path / "selector_features.joblib"

        if not model_file.exists():
            logger.warning(
                f" Model artifacts not found at {self.artifacts_path}. "
                "Pipeline will run without ML predictions."
            )
            return

        try:
            self.model = joblib.load(model_file)
            logger.info("✅ Ensemble model loaded")

            if vectorizer_file.exists():
                self.vectorizer = joblib.load(vectorizer_file)
                logger.info("✅ TF-IDF vectorizer loaded")

            if scaler_file.exists():
                self.scaler = joblib.load(scaler_file)
                logger.info("✅ MinMaxScaler loaded")

            if selector_file.exists():
                self.feature_selector = joblib.load(selector_file)
                logger.info("✅ SelectKBest selector loaded")

        except Exception as e:
            logger.error(f"Error loading artifacts: {e}")
            self.model = None

    def _build_feature_matrix(self, code: str):
        """
        Replica el pipeline de inferencia del notebook:
          TF-IDF(code) | MinMaxScaler(struct_features) → hstack → selector.transform
        """
        # 1. Features estructurales
        feat_dict = extraer_features_codigo(code)
        feat_arr = np.array(list(feat_dict.values()), dtype=float).reshape(1, -1)

        # 2. Escalar features estructurales con MinMaxScaler
        if self.scaler is not None:
            feat_arr = self.scaler.transform(feat_arr)
        X_struct_sparse = csr_matrix(feat_arr)

        # 3. Vectorizar texto con TF-IDF
        if self.vectorizer is not None:
            X_tfidf = self.vectorizer.transform([code])
        else:
            # Fallback: matriz vacía compatible
            X_tfidf = csr_matrix((1, 0))

        # 4. Combinar horizontalmente (exactamente como en el notebook)
        X_combined = hstack([X_tfidf, X_struct_sparse])

        # 5. Selección de features
        if self.feature_selector is not None:
            X_combined = self.feature_selector.transform(X_combined)

        return X_combined, feat_dict

    def predict(self, code: str) -> dict:
        """
        Genera predicción de seguridad para un fragmento de código.

        Returns:
            Dict con prediction, probability_safe, probability_vulnerable,
            risk_level, decision, y features_criticas.
        """
        if self.model is None:
            logger.warning("Model not available, returning neutral prediction")
            return {
                "prediction": "NO_MODEL",
                "probability_safe": 0.5,
                "probability_vulnerable": 0.5,
                "risk_level": 50,
                "decision": "REVISAR",
                "features_criticas": {},
            }

        try:
            X, feat_dict = self._build_feature_matrix(code)

            prediction = self.model.predict(X)[0]
            probabilities = self.model.predict_proba(X)[0]

            prediction_label = "VULNERABLE" if prediction == 1 else "SEGURO"
            prob_safe = float(probabilities[0])
            prob_vulnerable = float(probabilities[1])
            risk_level = int(prob_vulnerable * 100)

            result = {
                "prediction": prediction_label,
                "probability_safe": round(prob_safe, 4),
                "probability_vulnerable": round(prob_vulnerable, 4),
                "risk_level": risk_level,
                "decision": "RECHAZAR" if prediction == 1 else "ACEPTAR",
                "features_criticas": {
                    "danger_score": feat_dict.get("total_danger_score", 0),
                    "safety_score": feat_dict.get("total_safety_score", 0),
                    "ast_depth": feat_dict.get("ast_depth", 0),
                    "dangerous_calls": feat_dict.get("dangerous_func_calls", 0),
                    "sql_raw": feat_dict.get("sql_raw_concat", 0),
                    "subprocess_shell": feat_dict.get("subprocess_shell", 0),
                    "os_commands": feat_dict.get("os_commands", 0),
                    "sanitization": feat_dict.get("sanitization_present", 0),
                },
            }

            logger.info(
                f"Prediction: {prediction_label} "
                f"(risk: {risk_level}%, "
                f"danger_score: {feat_dict.get('total_danger_score', 0)})"
            )
            return result

        except Exception as e:
            logger.error(f"Error during prediction: {e}")
            raise

    def predict_batch(self, code_fragments: list) -> list:
        """Realiza predicciones en lote."""
        results = []
        for idx, code in enumerate(code_fragments):
            try:
                result = self.predict(code)
                result["fragment_index"] = idx
                results.append(result)
            except Exception as e:
                logger.error(f"Error predicting fragment {idx}: {e}")
                results.append(
                    {"fragment_index": idx, "prediction": "ERROR", "error": str(e)}
                )
        return results

    def generate_report(
        self,
        predictions: list,
        output_file: str = "reports/security_report.json",
        pr_number: Optional[int] = None,
        commit_sha: Optional[str] = None,
    ) -> str:
        """Genera reporte JSON de seguridad."""
        total_predictions = len(predictions)
        vulnerable_count = sum(
            1 for p in predictions if p.get("prediction") == "VULNERABLE"
        )
        safe_count = sum(1 for p in predictions if p.get("prediction") == "SEGURO")
        no_model_count = sum(
            1 for p in predictions if p.get("prediction") == "NO_MODEL"
        )

        overall_risk = (
            (vulnerable_count / total_predictions * 100) if total_predictions > 0 else 0
        )

        if self.model is None and no_model_count > 0:
            overall_decision = "REVISAR"
            exit_code = 0
        else:
            overall_decision = "RECHAZAR" if vulnerable_count > 0 else "ACEPTAR"
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

        output_path = Path(output_file)
        output_path.parent.mkdir(parents=True, exist_ok=True)

        with open(output_path, "w") as f:
            json.dump(report, f, indent=2, ensure_ascii=False)

        logger.info(f"Security report generated: {output_file}")
        logger.info(
            f"Overall: {safe_count} safe, {vulnerable_count} vulnerable, "
            f"{no_model_count} no_model"
        )
        return str(output_path)

    def get_exit_code(self, report: dict) -> int:
        """Obtiene el código de salida basado en el reporte."""
        return report.get("exit_code", 0)


def main():
    """Función principal para testing."""
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    )

    try:
        gate = SecurityGate("../modelo/model_artifacts")

        test_vulnerable = """
import subprocess
user_input = input("Command: ")
subprocess.run(user_input, shell=True)
"""

        test_safe = """
import subprocess, shlex
def list_dir(path):
    safe_path = shlex.quote(path)
    return subprocess.run(['ls', '-la', safe_path], capture_output=True, text=True).stdout
"""

        for name, code in [("VULNERABLE", test_vulnerable), ("SAFE", test_safe)]:
            result = gate.predict(code)
            print(f"\n--- {name} ---")
            print(json.dumps(result, indent=2, ensure_ascii=False))

    except Exception as e:
        logger.error(f"Main error: {e}")
        sys.exit(1)


if __name__ == "__main__":
    main()