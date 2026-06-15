#!/usr/bin/env python3
"""
Módulo para extracción de características de código.

Extrae features estáticas como tokens, profundidad AST, funciones peligrosas,
y mecanismos de sanitización.
"""

import ast
import logging
import re
from dataclasses import asdict, dataclass
from typing import Optional

logger = logging.getLogger(__name__)


@dataclass
class CodeFeatures:
    """Contenedor de features extraídas."""

    token_count: int
    ast_depth: int
    has_eval: bool
    has_exec: bool
    has_sql_concat: bool
    has_os_system: bool
    has_subprocess: bool
    has_runtime_exec: bool
    has_processbuilder: bool
    has_input_validation: bool
    has_sanitization: bool
    has_try_catch: bool
    lines_of_code: int
    dangerous_functions_count: int

    def to_dict(self) -> dict:
        """Convierte a diccionario."""
        return asdict(self)


class FeatureExtractor:
    """Extractor de características de código."""

    # Patrones para detectar funciones peligrosas (Python)
    PYTHON_DANGEROUS_PATTERNS = {
        "eval": r"\beval\s*\(",
        "exec": r"\bexec\s*\(",
        "os_system": r"\bos\s*\.\s*system\s*\(",
        "subprocess_popen": r"\bsubprocess\s*\.\s*(Popen|call|run|check_output)\s*\(",
        "compile": r"\bcompile\s*\(",
        "pickle": r"\bpickle\s*\.\s*loads?\s*\(",
    }

    # Patrones para detectar funciones peligrosas (Java)
    JAVA_DANGEROUS_PATTERNS = {
        "runtime_exec": r"Runtime\s*\.\s*getRuntime\s*\(\s*\)\s*\.\s*exec\s*\(",
        "processbuilder": r"\bnew\s+ProcessBuilder\s*\(",
        "sql_concat": r"\".*\"\s*\+\s*(?!.*\?)\s*(?:SELECT|INSERT|UPDATE|DELETE|FROM|WHERE)",
        "reflection": r"Class\s*\.\s*forName\s*\(",
    }

    # Patrones de sanitización
    SANITIZATION_PATTERNS = {
        "parameterized_query": r"\\\?",
        "prepared_statement": r"PreparedStatement|parameterized",
        "input_validation": r"(?:validate|check|verify|sanitize|escape)\s*\(",
        "regex_validation": r"matches\s*\(|Pattern\s*\.\s*compile",
    }

    # Patrones de validación de entrada
    VALIDATION_PATTERNS = {
        "null_check": r"\s+(?:if|!=)\s+.*(?:null|None)\b",
        "type_check": r"(?:isinstance|type)\s*\(\s*\w+\s*,",
        "length_check": r"(?:len|length)\s*\(\s*\w+\s*\)\s*(?:>|<|==)",
        "range_check": r"(?:\d+\s*<\s*\w+|in\s+range)",
    }

    def __init__(self, language: str = "auto"):
        """
        Inicializa el extractor.

        Args:
            language: Lenguaje del código (python, java, auto)
        """
        self.language = language
        logger.info(f"FeatureExtractor initialized for language: {language}")

    def _detect_language(self, code: str) -> str:
        """Detecta el lenguaje del código."""
        if "import " in code or "from " in code:
            return "python"
        if "import " in code or "class " in code or "public " in code:
            return "java"
        return "unknown"

    def _count_tokens(self, code: str) -> int:
        """Cuenta tokens en el código."""
        # Tokenización simple: palabras, operadores, caracteres especiales
        tokens = re.findall(r"\w+|[(){}\[\];:,.]|[+\-*/%=<>!&|^~]", code)
        return len(tokens)

    def _calculate_ast_depth(self, code: str, language: str = "python") -> int:
        """
        Calcula la profundidad del AST.

        Returns:
            Profundidad máxima del árbol sintáctico
        """
        if language == "python":
            try:
                tree = ast.parse(code)
                return self._get_ast_depth(tree)
            except SyntaxError:
                logger.warning("Could not parse Python code for AST analysis")
                return 0
        elif language == "java":
            # Para Java, usar heurística de llaves/corchetes anidados
            max_depth = 0
            current_depth = 0
            for char in code:
                if char in "{[":
                    current_depth += 1
                    max_depth = max(max_depth, current_depth)
                elif char in "}]":
                    current_depth = max(0, current_depth - 1)
            return max_depth
        else:
            # Fallback genérico
            return code.count("{") // 2

    def _get_ast_depth(self, node: ast.AST, depth: int = 0) -> int:
        """Calcula recursivamente la profundidad del AST."""
        if not isinstance(node, ast.AST):
            return depth

        max_depth = depth
        for child in ast.iter_child_nodes(node):
            child_depth = self._get_ast_depth(child, depth + 1)
            max_depth = max(max_depth, child_depth)

        return max_depth

    def _detect_dangerous_functions(self, code: str, language: str) -> tuple[int, dict]:
        """
        Detecta funciones peligrosas.

        Returns:
            (count, details_dict)
        """
        count = 0
        details = {}

        patterns = (
            self.PYTHON_DANGEROUS_PATTERNS
            if language == "python"
            else self.JAVA_DANGEROUS_PATTERNS
        )

        for func_name, pattern in patterns.items():
            matches = re.findall(pattern, code, re.IGNORECASE | re.MULTILINE)
            if matches:
                count += len(matches)
                details[func_name] = len(matches)

        return count, details

    def _detect_sanitization(self, code: str) -> bool:
        """Detecta si hay sanitización."""
        for pattern in self.SANITIZATION_PATTERNS.values():
            if re.search(pattern, code, re.IGNORECASE):
                return True
        return False

    def _detect_input_validation(self, code: str) -> bool:
        """Detecta si hay validación de entrada."""
        for pattern in self.VALIDATION_PATTERNS.values():
            if re.search(pattern, code, re.IGNORECASE):
                return True
        return False

    def _has_try_catch(self, code: str, language: str) -> bool:
        """Detecta manejo de excepciones."""
        if language == "python":
            return bool(re.search(r"\btry\s*:|except\s+", code))
        elif language == "java":
            return bool(re.search(r"\btry\s*\{|catch\s*\(", code))
        return bool(re.search(r"try|catch|except", code, re.IGNORECASE))

    def extract(self, code: str, language: Optional[str] = None) -> CodeFeatures:
        """
        Extrae todas las características del código.

        Args:
            code: Código fuente
            language: Lenguaje (python/java/auto)

        Returns:
            CodeFeatures con todas las métricas
        """
        if not code or not code.strip():
            logger.warning("Empty code provided")
            return CodeFeatures(
                token_count=0,
                ast_depth=0,
                has_eval=False,
                has_exec=False,
                has_sql_concat=False,
                has_os_system=False,
                has_subprocess=False,
                has_runtime_exec=False,
                has_processbuilder=False,
                has_input_validation=False,
                has_sanitization=False,
                has_try_catch=False,
                lines_of_code=0,
                dangerous_functions_count=0,
            )

        # Detectar lenguaje si es necesario
        lang = language or self.language
        if lang == "auto":
            lang = self._detect_language(code)

        logger.debug(f"Analyzing code in language: {lang}")

        # Contar métricas básicas
        token_count = self._count_tokens(code)
        ast_depth = self._calculate_ast_depth(code, lang)
        lines_of_code = len(code.split("\n"))

        # Detectar funciones peligrosas
        dangerous_count, dangerous_details = self._detect_dangerous_functions(
            code, lang
        )

        # Detectar características de seguridad
        has_eval = "eval" in dangerous_details
        has_exec = "exec" in dangerous_details
        has_sql_concat = "sql_concat" in dangerous_details
        has_os_system = "os_system" in dangerous_details
        has_subprocess = "subprocess_popen" in dangerous_details
        has_runtime_exec = "runtime_exec" in dangerous_details
        has_processbuilder = "processbuilder" in dangerous_details

        has_sanitization = self._detect_sanitization(code)
        has_input_validation = self._detect_input_validation(code)
        has_try_catch = self._has_try_catch(code, lang)

        features = CodeFeatures(
            token_count=token_count,
            ast_depth=ast_depth,
            has_eval=has_eval,
            has_exec=has_exec,
            has_sql_concat=has_sql_concat,
            has_os_system=has_os_system,
            has_subprocess=has_subprocess,
            has_runtime_exec=has_runtime_exec,
            has_processbuilder=has_processbuilder,
            has_input_validation=has_input_validation,
            has_sanitization=has_sanitization,
            has_try_catch=has_try_catch,
            lines_of_code=lines_of_code,
            dangerous_functions_count=dangerous_count,
        )

        logger.debug(f"Features extracted: {dangerous_count} dangerous patterns found")
        return features

    def extract_batch(self, code_fragments: list[dict]) -> list[dict]:
        """
        Extrae features para múltiples fragmentos.

        Args:
            code_fragments: Lista de {'code': str, 'file': str, 'line': int}

        Returns:
            Lista de {'file': str, 'line': int, 'features': dict}
        """
        results = []

        for fragment in code_fragments:
            code = fragment.get("code", "")
            file = fragment.get("file", "unknown")
            line = fragment.get("line", 0)

            try:
                features = self.extract(code)
                results.append(
                    {"file": file, "line": line, "features": features.to_dict()}
                )
            except Exception as e:
                logger.error(f"Error extracting features from {file}:{line}: {e}")
                results.append(
                    {
                        "file": file,
                        "line": line,
                        "error": str(e),
                        "features": None,
                    }
                )

        return results


def main():
    """Función principal para testing."""
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    )

    # Ejemplo Python
    python_code = """
import os
user_input = input("Enter query: ")
query = "SELECT * FROM users WHERE id = " + user_input
result = eval(user_input)
"""

    extractor = FeatureExtractor(language="auto")
    features = extractor.extract(python_code, language="python")
    print("Python Code Features:")
    print(features.to_dict())


if __name__ == "__main__":
    main()
