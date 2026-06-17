#!/usr/bin/env python3
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

from feature_extractor import extraer_features_codigo

logger = logging.getLogger(__name__)


def _shannon_entropy(text: str) -> float:
    if not text:
        return 0.0
    freq: dict = {}
    for c in text:
        freq[c] = freq.get(c, 0) + 1
    n = len(text)
    return -sum((f / n) * math.log2(f / n) for f in freq.values())


# ── Vulnerability type inference patterns ──
_PAT_JNDI = re.compile(
    r'(InitialContext|Context)\.\s*(lookup|search|list)\s*\(',
    re.IGNORECASE,
)
_PAT_XXE = re.compile(
    r'DocumentBuilderFactory\.newInstance\s*\('
    r'|XMLReader\s*\('
    r'|SAXBuilder\s*\('
    r'|SAXParser\s*\(',
    re.IGNORECASE,
)
_PAT_XSS = re.compile(
    r'(PrintWriter|println)\s*\([^)]*getParameter',
    re.IGNORECASE | re.DOTALL,
)
_PAT_LDAP = re.compile(
    r'(DirContext|InitialDirContext|LdapContext)\.\s*(search|lookup)\s*\(',
    re.IGNORECASE,
)


def _infer_vulnerability_type(code: str, feat_dict: dict) -> dict:
    types = []
    checks = [
        ('SQL Injection', feat_dict.get('sql_raw_concat', 0) or feat_dict.get('sql_fstring', 0)),
        ('Command Injection', feat_dict.get('subprocess_shell', 0) or feat_dict.get('os_commands', 0)),
        ('Path Traversal', feat_dict.get('path_concat', 0)),
        ('Insecure Deserialization', feat_dict.get('pickle_usage', 0)),
        ('Weak Cryptography', feat_dict.get('weak_hash', 0) or feat_dict.get('insecure_random', 0)),
        ('JNDI Injection', 1 if _PAT_JNDI.search(code) else 0),
        ('XXE (XML External Entity)', 1 if _PAT_XXE.search(code) else 0),
        ('Cross-Site Scripting (XSS)', 1 if _PAT_XSS.search(code) else 0),
        ('LDAP Injection', 1 if _PAT_LDAP.search(code) else 0),
    ]
    types = [(name, score) for name, score in checks if score > 0]
    types.sort(key=lambda x: -x[1])
    return {
        'primary_type': types[0][0] if types else 'Ninguno',
        'all_types': [t[0] for t in types],
    }


_PAT_SANITIZATION = re.compile(
    r'\b(escape|sanitize|sanitise|is_valid|validate|clean|purify|'
    r'strip_tags|bleach|html\.escape|markupsafe|'
    r'parameterize|prepared_statement|placeholder|'
    r'allowed_extensions|whitelist|ALLOWED|safe_path|realpath|'
    r'compare_digest|escape_filter_chars|defusedxml|'
    r'safe_load|create_default_context|token_urlsafe|secrets\.)'
    r'|HtmlUtils\.htmlEscape'
    r'|StringEscapeUtils'
    r'|ESAPI\.encoder'
    r'|encoder\.(encodeFor|canonicalize)'
    r'|Pattern\.quote',
    re.IGNORECASE,
)
_PAT_SQL_PARAM = re.compile(
    r'\.execute\s*\(\s*[\'"][^\'"]*(\?|%s|:param|:\w+)[\'"]'
    r'|PreparedStatement\b'
    r'|\?\s*\)',
    re.IGNORECASE,
)
_PAT_ENV_VARS = re.compile(
    r'os\.environ\.(get|__getitem__)\s*\('
    r'|System\.getenv\s*\('
    r'|System\.getProperty\s*\(',
    re.IGNORECASE,
)
_PAT_TYPE_VALID = re.compile(
    r'isinstance\s*\(|type\s*\(\w+\)\s*==|assert\s+'
    r'|\binstanceof\b'
    r'|Pattern\.matches\s*\(',
    re.IGNORECASE,
)
_PAT_EXCEPTIONS = re.compile(
    r'\btry\s*:.*?\bexcept\b'
    r'|\btry\s*\{'
    r'|\bcatch\s*\(',
    re.IGNORECASE | re.DOTALL,
)
_PAT_SECURE_IMPORTS = re.compile(
    r'import\s+(bcrypt|argon2|cryptography|secrets|hmac|defusedxml|bleach)'
    r'|import\s+org\.owasp'
    r'|import\s+org\.springframework\.security'
    r'|import\s+javax\.crypto'
    r'|import\s+java\.security',
    re.IGNORECASE,
)
_PAT_STR_CONCAT = re.compile(r"""['"][^'"]*['"]\s*\+|\+\s*['"][^'"]*['"]""")
_PAT_SEC_COMMENTS = re.compile(
    r'(#|//|/\*).*(segur|safe|valid|sanitiz|escape|authen|authori|permis|FIX|TODO|XXX)',
    re.IGNORECASE,
)


class SecurityGate:
    def __init__(self, model_artifacts_path: str):
        self.artifacts_path = Path(model_artifacts_path)
        self.model = None
        self.vectorizer = None
        self.scaler = None
        self.feature_selector = None
        self._load_artifacts()

    def _load_artifacts(self):
        model_file = self.artifacts_path / "modelo_ensemble.joblib"
        vectorizer_file = self.artifacts_path / "vectorizador_tfidf.joblib"
        scaler_file = self.artifacts_path / "scaler_estructural.joblib"
        selector_file = self.artifacts_path / "selector_features.joblib"

        if not model_file.exists():
            logger.warning(f"Model artifacts not found at {self.artifacts_path}. Pipeline will run without ML predictions.")
            return

        try:
            self.model = joblib.load(model_file)
            logger.info("Ensemble model loaded")
            if vectorizer_file.exists():
                self.vectorizer = joblib.load(vectorizer_file)
                logger.info("TF-IDF vectorizer loaded")
            if scaler_file.exists():
                self.scaler = joblib.load(scaler_file)
                logger.info("MinMaxScaler loaded")
            if selector_file.exists():
                self.feature_selector = joblib.load(selector_file)
                logger.info("SelectKBest selector loaded")
        except Exception as e:
            logger.error(f"Error loading artifacts: {e}")
            self.model = None

    def _build_feature_matrix(self, code: str):
        feat_dict = extraer_features_codigo(code)
        feat_arr = np.array(list(feat_dict.values()), dtype=float).reshape(1, -1)

        if self.scaler is not None:
            feat_arr = self.scaler.transform(feat_arr)
        X_struct_sparse = csr_matrix(feat_arr)

        if self.vectorizer is not None:
            X_tfidf = self.vectorizer.transform([code])
        else:
            X_tfidf = csr_matrix((1, 0))

        X_combined = hstack([X_tfidf, X_struct_sparse])

        if self.feature_selector is not None:
            X_combined = self.feature_selector.transform(X_combined)

        return X_combined, feat_dict

    def predict(self, code: str) -> dict:
        if self.model is None:
            logger.warning("Model not available, returning neutral prediction")
            return {
                "prediction": "NO_MODEL",
                "probability_safe": 0.5,
                "probability_vulnerable": 0.5,
                "risk_level": 50,
                "decision": "REVISAR",
                "guard_triggered": False,
                "features_criticas": {},
                "vulnerability_type": "Ninguno",
                "vulnerability_types": [],
            }

        try:
            feat_dict = extraer_features_codigo(code)
            vuln_info = _infer_vulnerability_type(code, feat_dict)

            # Guardia: sin patrones peligrosos → SEGURO sin pasar por el modelo
            _danger_keys = [
                'sql_raw_concat', 'sql_fstring', 'subprocess_shell', 'os_commands',
                'path_concat', 'pickle_usage', 'weak_hash', 'insecure_random',
                'dangerous_func_calls',
            ]
            if all(feat_dict.get(k, 0) == 0 for k in _danger_keys) and feat_dict.get('total_danger_score', 0) == 0:
                logger.info("Guard triggered: no dangerous patterns → SEGURO")
                return {
                    "prediction": "SEGURO",
                    "probability_safe": 1.0,
                    "probability_vulnerable": 0.0,
                    "risk_level": 0,
                    "decision": "ACEPTAR",
                    "guard_triggered": True,
                    "features_criticas": {
                        "danger_score": feat_dict.get("total_danger_score", 0),
                        "safety_score": feat_dict.get("total_safety_score", 0),
                        "ast_depth": feat_dict.get("ast_depth", 0),
                        "dangerous_calls": feat_dict.get("dangerous_func_calls", 0),
                        "sql_raw": feat_dict.get("sql_raw_concat", 0),
                        "cmd_injection": feat_dict.get("subprocess_shell", 0),
                        "path_traversal": feat_dict.get("path_concat", 0),
                        "sanitization": feat_dict.get("sanitization_present", 0),
                    },
                    "vulnerability_type": vuln_info['primary_type'],
                    "vulnerability_types": vuln_info['all_types'],
                }

            X, _ = self._build_feature_matrix(code)

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
                "guard_triggered": False,
                "features_criticas": {
                    "danger_score": feat_dict.get("total_danger_score", 0),
                    "safety_score": feat_dict.get("total_safety_score", 0),
                    "ast_depth": feat_dict.get("ast_depth", 0),
                    "dangerous_calls": feat_dict.get("dangerous_func_calls", 0),
                    "sql_raw": feat_dict.get("sql_raw_concat", 0),
                    "cmd_injection": feat_dict.get("subprocess_shell", 0),
                    "path_traversal": feat_dict.get("path_concat", 0),
                    "sanitization": feat_dict.get("sanitization_present", 0),
                },
                "vulnerability_type": vuln_info['primary_type'],
                "vulnerability_types": vuln_info['all_types'],
            }

            logger.info(f"Prediction: {prediction_label} (risk: {risk_level}%, type: {vuln_info['primary_type']})")
            return result

        except Exception as e:
            logger.error(f"Error during prediction: {e}")
            raise

    def predict_batch(self, code_fragments: list) -> list:
        results = []
        for idx, code in enumerate(code_fragments):
            try:
                result = self.predict(code)
                result["fragment_index"] = idx
                results.append(result)
            except Exception as e:
                logger.error(f"Error predicting fragment {idx}: {e}")
                results.append({"fragment_index": idx, "prediction": "ERROR", "error": str(e)})
        return results

    def generate_report(self, predictions: list, output_file: str = "reports/security_report.json", pr_number: Optional[int] = None, commit_sha: Optional[str] = None) -> str:
        total_predictions = len(predictions)
        vulnerable_count = sum(1 for p in predictions if p.get("prediction") == "VULNERABLE")
        safe_count = sum(1 for p in predictions if p.get("prediction") == "SEGURO")
        no_model_count = sum(1 for p in predictions if p.get("prediction") == "NO_MODEL")
        overall_risk = (vulnerable_count / total_predictions * 100) if total_predictions > 0 else 0

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
        logger.info(f"Overall: {safe_count} safe, {vulnerable_count} vulnerable, {no_model_count} no_model")
        return str(output_path)

    def get_exit_code(self, report: dict) -> int:
        return report.get("exit_code", 0)


def main():
    logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(name)s - %(levelname)s - %(message)s")

    try:
        gate = SecurityGate("../modelo/model_artifacts")

        test_vulnerable = """
String query = "SELECT * FROM users WHERE id = " + request.getParameter("id");
Statement stmt = connection.createStatement();
ResultSet rs = stmt.executeQuery(query);
"""
        test_safe = """
String query = "SELECT * FROM users WHERE id = ?";
PreparedStatement stmt = connection.prepareStatement(query);
stmt.setInt(1, Integer.parseInt(request.getParameter("id")));
ResultSet rs = stmt.executeQuery();
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
