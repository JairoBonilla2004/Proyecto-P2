#!/usr/bin/env python3
"""
Módulo para extraer cambios en Pull Requests.

Extrae archivos modificados y el código agregado/modificado de un PR,
ignorando archivos binarios y configuración.
"""

import json
import logging
import re
import subprocess
from pathlib import Path
from typing import Optional

logger = logging.getLogger(__name__)


class DiffExtractor:
    """Extractor de cambios en Pull Requests."""

    # Extensiones binarias a ignorar
    BINARY_EXTENSIONS = {
        ".jpg",
        ".jpeg",
        ".png",
        ".gif",
        ".bin",
        ".exe",
        ".dll",
        ".so",
        ".zip",
        ".tar",
        ".gz",
        ".pdf",
    }

    # Archivos a ignorar
    IGNORED_FILES = {
        "package-lock.json",
        "yarn.lock",
        "pom-backup.xml",
        ".DS_Store",
    }

    # Extensiones de código a analizar
    CODE_EXTENSIONS = {
        ".java",
    }

    def __init__(self, base_branch: str = "main", head_branch: Optional[str] = None):
        """
        Inicializa el extractor.

        Args:
            base_branch: Rama base para comparación (default: main)
            head_branch: Rama head (default: HEAD si no se especifica)
        """
        self.base_branch = base_branch
        self.head_branch = head_branch or "HEAD"
        logger.info(f"DiffExtractor initialized: {base_branch} -> {head_branch}")

    def _is_binary_file(self, filepath: str) -> bool:
        """Verifica si un archivo es binario."""
        ext = Path(filepath).suffix.lower()
        return ext in self.BINARY_EXTENSIONS

    def _should_ignore_file(self, filepath: str) -> bool:
        """Verifica si un archivo debe ser ignorado."""
        filename = Path(filepath).name
        if filename in self.IGNORED_FILES:
            return True
        ext = Path(filepath).suffix.lower()
        if ext not in self.CODE_EXTENSIONS:
            return True
        if self._is_binary_file(filepath):
            return True
        return False

    def _get_diff_with_fallback(self) -> str:
        """
        Obtiene el diff con fallback automático.

        Primero intenta base_branch...HEAD. Si el resultado está vacío
        (puede ocurrir en push events donde base_branch es un SHA no disponible),
        hace fallback a HEAD~1...HEAD para capturar el último commit.
        """
        try:
            diff = self._run_git_diff(self.base_branch, self.head_branch)
            if diff.strip():
                logger.debug(f"Git diff OK: {self.base_branch}...{self.head_branch}")
                return diff

            # Fallback: comparar con el commit anterior
            logger.warning(
                f"Empty diff for {self.base_branch}...{self.head_branch}, "
                "falling back to HEAD~1...HEAD"
            )
            diff_fallback = self._run_git_diff("HEAD~1", "HEAD")
            if diff_fallback.strip():
                return diff_fallback

            logger.warning("Both diffs are empty — no code changes detected")
            return ""

        except subprocess.CalledProcessError as e:
            logger.warning(f"Primary diff failed: {e.stderr}. Trying HEAD~1...")
            try:
                return self._run_git_diff("HEAD~1", "HEAD")
            except subprocess.CalledProcessError as e2:
                logger.error(f"Fallback diff also failed: {e2.stderr}")
                return ""

    def _run_git_diff(self, base: str, head: str) -> str:
        """Ejecuta git diff entre dos referencias."""
        result = subprocess.run(
            ["git", "diff", f"{base}...{head}", "--unified=5"],
            capture_output=True,
            text=True,
            check=True,
        )
        return result.stdout

    def extract_modified_files(self) -> list[dict]:
        """
        Extrae los archivos modificados del diff.

        Returns:
            Lista de diccionarios con {filename, additions, deletions}
        """
        try:
            result = subprocess.run(
                [
                    "git",
                    "diff",
                    f"{self.base_branch}...{self.head_branch}",
                    "--name-status",
                ],
                capture_output=True,
                text=True,
                check=True,
            )

            modified_files = []
            for line in result.stdout.strip().split("\n"):
                if not line:
                    continue

                parts = line.split("\t")
                status = parts[0]
                filepath = parts[1]

                if self._should_ignore_file(filepath):
                    logger.debug(f"Ignoring file: {filepath}")
                    continue

                modified_files.append(
                    {
                        "filename": filepath,
                        "status": status,  # M=modified, A=added, D=deleted
                    }
                )

            logger.info(f"Found {len(modified_files)} code files to analyze")
            return modified_files

        except subprocess.CalledProcessError as e:
            logger.error(f"Failed to extract modified files: {e.stderr}")
            raise

    def extract_code_fragments(self, min_block_lines: int = 1) -> list[dict]:
        """
        Extrae bloques de código agregado/modificado del diff.

        En lugar de retornar línea-a-línea, agrupa líneas contiguas en bloques
        para que el modelo ML tenga suficiente contexto para detectar patrones
        multi-línea (p.ej. SQL injection que abarca varias líneas).

        Returns:
            Lista de fragmentos con {file, line, code, type}
        """
        fragments = []

        def _flush_block(current_file, start_line, lines):
            """Convierte un bloque de líneas acumuladas en un fragmento."""
            if not lines:
                return
            code_block = "\n".join(lines)
            if code_block.strip():
                fragments.append(
                    {
                        "file": current_file,
                        "line": start_line,
                        "code": code_block,
                        "type": "added",
                    }
                )

        try:
            diff_output = self._get_diff_with_fallback()

            hunk_pattern = r"^@@ -\d+(?:,\d+)? \+(\d+)(?:,(\d+))? @@"

            current_file = None
            current_line_num = 1

            # Acumuladores para el bloque actual
            block_start_line = 1
            block_lines: list[str] = []

            for line in diff_output.split("\n"):
                # Nuevo archivo detectado
                if line.startswith("diff --git"):
                    # Flush bloque pendiente del archivo anterior
                    _flush_block(current_file, block_start_line, block_lines)
                    block_lines = []
                    match = re.search(r"b/(.*?)$", line)
                    if match:
                        current_file = match.group(1)

                # Nuevo hunk (bloque de cambios dentro del archivo)
                elif line.startswith("@@"):
                    # Flush bloque pendiente antes del nuevo hunk
                    _flush_block(current_file, block_start_line, block_lines)
                    block_lines = []
                    match = re.search(hunk_pattern, line)
                    if match:
                        current_line_num = int(match.group(1))
                        block_start_line = current_line_num

                # Línea agregada (+) — acumular en el bloque actual
                elif line.startswith("+") and not line.startswith("+++"):
                    if current_file and not self._should_ignore_file(current_file):
                        code = line[1:]  # Remover el prefijo '+'
                        block_lines.append(code.rstrip("\n"))
                        current_line_num += 1

                # Línea eliminada (-) — flush y resetear bloque
                elif line.startswith("-") and not line.startswith("---"):
                    _flush_block(current_file, block_start_line, block_lines)
                    block_lines = []
                    block_start_line = current_line_num
                    current_line_num += 1

                # Línea de contexto (sin cambios) — flush y resetear bloque
                elif not line.startswith("\\"):
                    _flush_block(current_file, block_start_line, block_lines)
                    block_lines = []
                    block_start_line = current_line_num
                    current_line_num += 1

            # Flush del último bloque
            _flush_block(current_file, block_start_line, block_lines)

            logger.info(f"Extracted {len(fragments)} code fragments")
            return fragments

        except subprocess.CalledProcessError as e:
            logger.error(f"Failed to extract code fragments: {e}")
            raise

    def to_json(self, fragments: list[dict]) -> str:
        """Serializa fragmentos a JSON."""
        return json.dumps(fragments, indent=2, ensure_ascii=False)


def main():
    """Función principal para testing."""
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    )

    extractor = DiffExtractor(base_branch="main", head_branch="HEAD")

    try:
        files = extractor.extract_modified_files()
        print("Modified files:")
        print(json.dumps(files, indent=2))

        fragments = extractor.extract_code_fragments()
        print("\nCode fragments:")
        print(extractor.to_json(fragments))

    except Exception as e:
        logger.error(f"Error: {e}")
        exit(1)


if __name__ == "__main__":
    main()
