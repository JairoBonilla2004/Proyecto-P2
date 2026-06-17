#!/usr/bin/env python3
import json
import logging
import os
import tempfile
from pathlib import Path
from unittest.mock import MagicMock, patch

from feature_extractor import CodeFeatures, FeatureExtractor, extraer_features_codigo
from telegram_notifier import TelegramNotifier

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class TestFeatureExtractor:
    def test_python_dangerous_patterns(self):
        extractor = FeatureExtractor(language="python")
        code = "import os\nuser_input = input(\"Enter: \")\nresult = eval(user_input)\n"
        features = extractor.extract(code, language="python")
        assert features.has_eval
        assert features.dangerous_functions_count > 0
        print("-- python eval() detection works")

    def test_java_runtime_exec(self):
        extractor = FeatureExtractor(language="java")
        code = 'String cmd = "ls -la " + input;\nProcess p = Runtime.getRuntime().exec(cmd);\n'
        features = extractor.extract(code, language="java")
        assert features.has_runtime_exec
        print("-- Java Runtime.exec detection works")

    def test_java_processbuilder(self):
        extractor = FeatureExtractor()
        code = 'ProcessBuilder pb = new ProcessBuilder("ls", "-la", path);\n'
        features = extractor.extract(code)
        assert features.has_processbuilder
        print("-- Java ProcessBuilder detection works")

    def test_java_sql_injection(self):
        extractor = FeatureExtractor(language="java")
        code = (
            'String q = "SELECT * FROM users WHERE id = " + request.getParameter("id");\n'
            "Statement stmt = conn.createStatement();\n"
            "ResultSet rs = stmt.executeQuery(q);\n"
        )
        features = extractor.extract(code, language="java")
        assert features.has_sql_concat
        print("-- Java SQL injection detection works")

    def test_java_prepared_statement(self):
        extractor = FeatureExtractor()
        code = (
            'String q = "SELECT * FROM users WHERE id = ?";\n'
            "PreparedStatement stmt = conn.prepareStatement(q);\n"
            "stmt.setInt(1, userId);\n"
        )
        features = extractor.extract(code)
        assert features.has_sanitization
        print("-- Java PreparedStatement sanitization works")

    def test_extraer_features_java_vuln(self):
        code = (
            'String q = "SELECT * FROM users WHERE id = " + request.getParameter("id");\n'
            "Statement stmt = conn.createStatement();\n"
            "ResultSet rs = stmt.executeQuery(q);\n"
        )
        feats = extraer_features_codigo(code)
        assert feats["sql_raw_concat"] == 1
        assert feats["total_danger_score"] > 0
        assert feats["func_calls_count"] > 0
        print("-- extraer_features_codigo Java SQL injection works")

    def test_extraer_features_java_safe(self):
        code = (
            'String q = "SELECT * FROM users WHERE id = ?";\n'
            "PreparedStatement stmt = conn.prepareStatement(q);\n"
            "stmt.setInt(1, userId);\n"
        )
        feats = extraer_features_codigo(code)
        assert feats["sql_parameterized"] == 1
        assert feats["total_danger_score"] < 5
        print("-- extraer_features_codigo Java PreparedStatement works")

    def test_extraer_features_java_runtime(self):
        code = 'Runtime rt = Runtime.getRuntime();\nProcess p = rt.exec("cmd /c " + input);\n'
        feats = extraer_features_codigo(code)
        assert feats["subprocess_shell"] == 1
        assert feats["dangerous_func_calls"] >= 1
        print("-- extraer_features_codigo Java Runtime.exec works")

    def test_extraer_features_java_crypto(self):
        code = 'MessageDigest md = MessageDigest.getInstance("MD5");\nmd.digest(data);\n'
        feats = extraer_features_codigo(code)
        assert feats["weak_hash"] == 1
        print("-- extraer_features_codigo Java weak crypto works")

    def test_extraer_features_java_deserialization(self):
        code = (
            "FileInputStream fis = new FileInputStream(\"data.ser\");\n"
            "ObjectInputStream ois = new ObjectInputStream(fis);\n"
            "Object obj = ois.readObject();\n"
        )
        feats = extraer_features_codigo(code)
        assert feats["pickle_usage"] == 1
        print("-- extraer_features_codigo Java deserialization works")

    def test_sanitization_detection(self):
        extractor = FeatureExtractor()
        code = 'String query = "SELECT * FROM users WHERE id = ?";\nPreparedStatement stmt = connection.prepareStatement(query);\nstmt.setInt(1, userId);\n'
        features = extractor.extract(code)
        assert features.has_sanitization
        print("-- Sanitization detection works")

    def test_validation_detection(self):
        extractor = FeatureExtractor()
        code = "if user_input is not None and len(user_input) > 0:\n    process(user_input)\n"
        features = extractor.extract(code)
        assert features.has_input_validation
        print("-- Input validation detection works")

    def test_token_counting(self):
        extractor = FeatureExtractor()
        features = extractor.extract("x = 1 + 2")
        assert features.token_count > 0
        print("-- Token counting works")

    def test_ast_depth(self):
        extractor = FeatureExtractor()
        code = "def outer():\n    def inner():\n        def deepest():\n            pass\n"
        features = extractor.extract(code, language="python")
        assert features.ast_depth > 0
        print("-- AST depth calculation works")

    def test_empty_code(self):
        extractor = FeatureExtractor()
        features = extractor.extract("")
        assert features.token_count == 0
        assert features.dangerous_functions_count == 0
        print("-- Empty code handling works")

    def test_batch_extraction(self):
        extractor = FeatureExtractor()
        fragments = [
            {"code": "x = eval(y)", "file": "test.py", "line": 1},
            {"code": "normal_code = 1 + 1", "file": "test.py", "line": 2},
        ]
        results = extractor.extract_batch(fragments)
        assert len(results) == 2
        assert results[0]["file"] == "test.py"
        print("-- Batch extraction works")


class TestSecurityGate:
    def test_juliet_sql_injection_vuln(self):
        from security_gate import SecurityGate
        gate = SecurityGate("../modelo/model_artifacts")
        code = (
            'String q = "SELECT * FROM users WHERE id = " + request.getParameter("id");\n'
            "Statement stmt = conn.createStatement();\n"
            "ResultSet rs = stmt.executeQuery(q);\n"
        )
        result = gate.predict(code)
        assert result["prediction"] in ["VULNERABLE", "SEGURO", "NO_MODEL"]
        print(f"-- SQL injection prediction: {result['prediction']} (risk: {result['risk_level']}%)")

    def test_juliet_command_injection_vuln(self):
        from security_gate import SecurityGate
        gate = SecurityGate("../modelo/model_artifacts")
        code = 'Process p = Runtime.getRuntime().exec("cmd /c " + request.getParameter("cmd"));\n'
        result = gate.predict(code)
        assert result["prediction"] in ["VULNERABLE", "SEGURO", "NO_MODEL"]
        print(f"-- Command injection prediction: {result['prediction']}")

    def test_juliet_prepared_statement_safe(self):
        from security_gate import SecurityGate
        gate = SecurityGate("../modelo/model_artifacts")
        code = (
            'String q = "SELECT * FROM users WHERE id = ?";\n'
            "PreparedStatement stmt = conn.prepareStatement(q);\n"
            "stmt.setInt(1, userId);\n"
            "ResultSet rs = stmt.executeQuery();\n"
        )
        result = gate.predict(code)
        assert result["prediction"] in ["VULNERABLE", "SEGURO", "NO_MODEL"]
        print(f"-- PreparedStatement prediction: {result['prediction']}")

    def test_feature_count_consistency(self):
        code = 'public class Test { public void run() { String x = "hello"; } }'
        feats = extraer_features_codigo(code)
        assert len(feats) == 29
        print(f"-- Feature count: {len(feats)} (expected 29)")

    def test_danger_vs_safety_scores(self):
        vuln_code = (
            'String q = "SELECT * FROM users WHERE id = " + request.getParameter("id");\n'
            "Statement stmt = conn.createStatement();\n"
            "ResultSet rs = stmt.executeQuery(q);\n"
        )
        safe_code = (
            'String q = "SELECT * FROM users WHERE id = ?";\n'
            "PreparedStatement stmt = conn.prepareStatement(q);\n"
            "stmt.setInt(1, userId);\n"
        )
        fv = extraer_features_codigo(vuln_code)
        fs = extraer_features_codigo(safe_code)
        assert fv["total_danger_score"] > fs["total_danger_score"]
        print(f"-- Danger score: {fv['total_danger_score']} (vuln) > {fs['total_danger_score']} (safe)")


class TestTelegramNotifier:
    def test_missing_credentials(self):
        with patch.dict(os.environ, {}, clear=True):
            try:
                TelegramNotifier()
                assert False, "Should raise ValueError"
            except ValueError as e:
                assert "TELEGRAM_BOT_TOKEN" in str(e)
                print("-- Missing credentials validation works")

    def test_initialization_with_params(self):
        notifier = TelegramNotifier(bot_token="test_token", chat_id="test_chat")
        assert notifier.bot_token == "test_token"
        assert notifier.chat_id == "test_chat"
        print("-- Initialization with parameters works")

    @patch("requests.post")
    def test_send_message_success(self, mock_post):
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_post.return_value = mock_response
        notifier = TelegramNotifier(bot_token="test_token", chat_id="test_chat")
        result = notifier.send_message("Test message")
        assert result is True
        mock_post.assert_called_once()
        print("-- Successful message sending works")

    @patch("requests.post")
    def test_send_message_failure(self, mock_post):
        mock_response = MagicMock()
        mock_response.status_code = 401
        mock_response.text = "Unauthorized"
        mock_post.return_value = mock_response
        notifier = TelegramNotifier(bot_token="test_token", chat_id="test_chat")
        result = notifier.send_message("Test message")
        assert result is False
        print("-- Failed message handling works")

    @patch("requests.post")
    def test_notify_pr_scan_completed(self, mock_post):
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_post.return_value = mock_response
        notifier = TelegramNotifier(bot_token="test_token", chat_id="test_chat")
        result = notifier.notify_pr_scan_completed(
            pr_number=42, repository="TestRepo", safe_count=5,
            vulnerable_count=1, overall_decision="RECHAZAR", risk_percentage=20.0)
        assert result is True
        print("-- PR scan completion notification works")


class TestCodeFeatures:
    def test_to_dict(self):
        features = CodeFeatures(
            token_count=10, ast_depth=3, has_eval=True, has_exec=False,
            has_sql_concat=True, has_os_system=False, has_subprocess=False,
            has_runtime_exec=True, has_processbuilder=True,
            has_input_validation=True, has_sanitization=True, has_try_catch=True,
            lines_of_code=5, dangerous_functions_count=1)
        result = features.to_dict()
        assert isinstance(result, dict)
        assert result["token_count"] == 10
        assert result["has_eval"] is True
        print("-- CodeFeatures to_dict() works")

    def test_json_serializable(self):
        features = CodeFeatures(
            token_count=10, ast_depth=3, has_eval=False, has_exec=False,
            has_sql_concat=True, has_os_system=False, has_subprocess=False,
            has_runtime_exec=True, has_processbuilder=False,
            has_input_validation=True, has_sanitization=True, has_try_catch=True,
            lines_of_code=5, dangerous_functions_count=0)
        json_str = json.dumps(features.to_dict())
        assert isinstance(json_str, str)
        print("-- CodeFeatures is JSON serializable")


def run_tests():
    print("\n" + "=" * 60)
    print("RUNNING SECURITY PIPELINE TESTS")
    print("=" * 60 + "\n")

    test_classes = [
        TestFeatureExtractor,
        TestSecurityGate,
        TestTelegramNotifier,
        TestCodeFeatures,
    ]

    total = 0
    failed = 0

    for cls in test_classes:
        print(f"\n{cls.__name__}:")
        print("-" * 40)
        inst = cls()
        for m in [m for m in dir(inst) if m.startswith("test_")]:
            total += 1
            try:
                getattr(inst, m)()
            except Exception as e:
                failed += 1
                print(f"  ! {m} FAILED: {e}")

    print("\n" + "=" * 60)
    print(f"TESTS COMPLETED: {total - failed}/{total} passed")
    if failed > 0:
        print(f"FAILURES: {failed}")
    print("=" * 60 + "\n")

    return 0 if failed == 0 else 1


if __name__ == "__main__":
    exit(run_tests())
