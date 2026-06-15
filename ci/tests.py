#!/usr/bin/env python3
"""
Suite de pruebas para los módulos del pipeline de seguridad.
"""

import json
import logging
import os
import tempfile
from pathlib import Path
from unittest.mock import MagicMock, patch

from feature_extractor import CodeFeatures, FeatureExtractor
from telegram_notifier import TelegramNotifier

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class TestFeatureExtractor:
    """Tests para el extractor de features."""

    def test_python_dangerous_patterns(self):
        """Test detección de patrones peligrosos en Python."""
        extractor = FeatureExtractor(language="python")

        # Código con eval
        code_with_eval = """
import os
user_input = input("Enter: ")
result = eval(user_input)
"""

        features = extractor.extract(code_with_eval, language="python")
        assert features.has_eval, "eval() should be detected"
        assert features.dangerous_functions_count > 0

        print("✓ Python eval() detection works")

    def test_java_dangerous_patterns(self):
        """Test detección de patrones peligrosos en Java."""
        extractor = FeatureExtractor(language="java")

        # Código con SQL concatenation
        code_with_sql = """
String query = "SELECT * FROM users WHERE id = " + userId;
Statement stmt = connection.createStatement();
"""

        features = extractor.extract(code_with_sql, language="java")
        # Debería detectar SQL concatenation
        print(f"✓ Java SQL pattern detection - dangerous count: {features.dangerous_functions_count}")

    def test_sanitization_detection(self):
        """Test detección de sanitización."""
        extractor = FeatureExtractor()

        # Código con PreparedStatement
        code_safe = """
String query = "SELECT * FROM users WHERE id = ?";
PreparedStatement stmt = connection.prepareStatement(query);
stmt.setInt(1, userId);
"""

        features = extractor.extract(code_safe)
        assert features.has_sanitization, "Sanitization should be detected"
        print("✓ Sanitization detection works")

    def test_validation_detection(self):
        """Test detección de validación."""
        extractor = FeatureExtractor()

        code_with_validation = """
if user_input is not None and len(user_input) > 0:
    process(user_input)
"""

        features = extractor.extract(code_with_validation)
        assert features.has_input_validation, "Input validation should be detected"
        print("✓ Input validation detection works")

    def test_token_counting(self):
        """Test conteo de tokens."""
        extractor = FeatureExtractor()

        code = "x = 1 + 2"
        features = extractor.extract(code)

        assert features.token_count > 0, "Should count tokens"
        print(f"✓ Token counting works - count: {features.token_count}")

    def test_ast_depth(self):
        """Test cálculo de profundidad AST."""
        extractor = FeatureExtractor()

        code = """
def outer():
    def inner():
        def deepest():
            pass
"""

        features = extractor.extract(code, language="python")
        assert features.ast_depth > 0, "Should calculate AST depth"
        print(f"✓ AST depth calculation works - depth: {features.ast_depth}")

    def test_empty_code(self):
        """Test manejo de código vacío."""
        extractor = FeatureExtractor()

        features = extractor.extract("")

        assert features.token_count == 0
        assert features.dangerous_functions_count == 0
        print("✓ Empty code handling works")

    def test_batch_extraction(self):
        """Test extracción en lote."""
        extractor = FeatureExtractor()

        fragments = [
            {"code": "x = eval(y)", "file": "test.py", "line": 1},
            {"code": "normal_code = 1 + 1", "file": "test.py", "line": 2},
        ]

        results = extractor.extract_batch(fragments)

        assert len(results) == 2, "Should process all fragments"
        assert results[0]["file"] == "test.py"
        print("✓ Batch extraction works")


class TestTelegramNotifier:
    """Tests para el notificador de Telegram."""

    def test_missing_credentials(self):
        """Test error cuando faltan credenciales."""
        with patch.dict(os.environ, {}, clear=True):
            try:
                TelegramNotifier()
                assert False, "Should raise ValueError"
            except ValueError as e:
                assert "TELEGRAM_BOT_TOKEN" in str(e)
                print("✓ Missing credentials validation works")

    def test_initialization_with_params(self):
        """Test inicialización con parámetros."""
        notifier = TelegramNotifier(
            bot_token="test_token",
            chat_id="test_chat"
        )

        assert notifier.bot_token == "test_token"
        assert notifier.chat_id == "test_chat"
        print("✓ Initialization with parameters works")

    @patch("requests.post")
    def test_send_message_success(self, mock_post):
        """Test envío de mensaje exitoso."""
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_post.return_value = mock_response

        notifier = TelegramNotifier(
            bot_token="test_token",
            chat_id="test_chat"
        )

        result = notifier.send_message("Test message")

        assert result is True, "Should return True on success"
        mock_post.assert_called_once()
        print("✓ Successful message sending works")

    @patch("requests.post")
    def test_send_message_failure(self, mock_post):
        """Test fallo en envío de mensaje."""
        mock_response = MagicMock()
        mock_response.status_code = 401
        mock_response.text = "Unauthorized"
        mock_post.return_value = mock_response

        notifier = TelegramNotifier(
            bot_token="test_token",
            chat_id="test_chat"
        )

        result = notifier.send_message("Test message")

        assert result is False, "Should return False on failure"
        print("✓ Failed message handling works")

    @patch("requests.post")
    def test_notify_pr_scan_completed(self, mock_post):
        """Test notificación de escaneo completado."""
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_post.return_value = mock_response

        notifier = TelegramNotifier(
            bot_token="test_token",
            chat_id="test_chat"
        )

        result = notifier.notify_pr_scan_completed(
            pr_number=42,
            repository="TestRepo",
            safe_count=5,
            vulnerable_count=1,
            overall_decision="RECHAZAR",
            risk_percentage=20.0,
        )

        assert result is True
        print("✓ PR scan completion notification works")


class TestCodeFeatures:
    """Tests para el contenedor de features."""

    def test_to_dict(self):
        """Test conversión a diccionario."""
        features = CodeFeatures(
            token_count=10,
            ast_depth=3,
            has_eval=True,
            has_exec=False,
            has_sql_concat=False,
            has_os_system=False,
            has_subprocess=False,
            has_runtime_exec=False,
            has_processbuilder=False,
            has_input_validation=True,
            has_sanitization=True,
            has_try_catch=True,
            lines_of_code=5,
            dangerous_functions_count=1,
        )

        result = features.to_dict()

        assert isinstance(result, dict)
        assert result["token_count"] == 10
        assert result["has_eval"] is True
        print("✓ CodeFeatures to_dict() works")

    def test_json_serializable(self):
        """Test que CodeFeatures es serializable a JSON."""
        features = CodeFeatures(
            token_count=10,
            ast_depth=3,
            has_eval=False,
            has_exec=False,
            has_sql_concat=False,
            has_os_system=False,
            has_subprocess=False,
            has_runtime_exec=False,
            has_processbuilder=False,
            has_input_validation=True,
            has_sanitization=False,
            has_try_catch=True,
            lines_of_code=5,
            dangerous_functions_count=0,
        )

        # No debería lanzar excepción
        json_str = json.dumps(features.to_dict())
        assert isinstance(json_str, str)
        print("✓ CodeFeatures is JSON serializable")


def run_tests():
    """Ejecuta todos los tests."""
    print("\n" + "=" * 60)
    print("RUNNING SECURITY PIPELINE TESTS")
    print("=" * 60 + "\n")

    test_classes = [
        TestFeatureExtractor,
        TestTelegramNotifier,
        TestCodeFeatures,
    ]

    total_tests = 0
    failed_tests = 0

    for test_class in test_classes:
        print(f"\n{test_class.__name__}:")
        print("-" * 40)

        test_instance = test_class()
        test_methods = [
            m for m in dir(test_instance)
            if m.startswith("test_")
        ]

        for method_name in test_methods:
            total_tests += 1
            try:
                method = getattr(test_instance, method_name)
                method()
            except Exception as e:
                failed_tests += 1
                print(f"✗ {method_name} failed: {e}")

    print("\n" + "=" * 60)
    print(f"TESTS COMPLETED: {total_tests - failed_tests}/{total_tests} passed")
    if failed_tests > 0:
        print(f"FAILURES: {failed_tests}")
    print("=" * 60 + "\n")

    return 0 if failed_tests == 0 else 1


if __name__ == "__main__":
    exit(run_tests())
