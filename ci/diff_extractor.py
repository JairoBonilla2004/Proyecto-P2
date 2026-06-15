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
        ".py",
        ".java",
        ".js",
        ".ts",
        ".jsx",
        ".tsx",
        ".go",
        ".rs",
        ".cpp",
        ".c",
        ".cs",
        ".php",
        ".rb",
        ".sql",
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

    def get_diff(self) -> str:
        """
        Obtiene el diff entre branches.

        Returns:
            String con el diff en formato unified

        Raises:
            subprocess.CalledProcessError: Si git falla
        """
        try:
            result = subprocess.run(
                [
                    "git",
                    "diff",
                    f"{self.base_branch}...{self.head_branch}",
                    "--unified=1",
                ],
                capture_output=True,
                text=True,
                check=True,
            )
            logger.debug("Git diff retrieved successfully")
            return result.stdout
        except subprocess.CalledProcessError as e:
            logger.error(f"Git diff failed: {e.stderr}")
            raise

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

    def extract_code_fragments(self) -> list[dict]:
        """
        Extrae fragmentos de código agregado/modificado.

        Returns:
            Lista de fragmentos con {file, lines, code, type}
        """
        fragments = []

        try:
            diff_output = self.get_diff()

            # Expresión regular para parsear el diff
            # @@@ ... @@ indica inicio de bloque
            file_pattern = r"^diff --git a/(.*?) b/\1"
            hunk_pattern = r"^@@ -\d+(?:,\d+)? \+(\d+)(?:,(\d+))? @@"

            current_file = None
            current_line_num = 1

            for line in diff_output.split("\n"):
                # Detectar nuevo archivo
                if line.startswith("diff --git"):
                    match = re.search(r"b/(.*?)$", line)
                    if match:
                        current_file = match.group(1)

                # Detectar hunk (bloque de cambios)
                if line.startswith("@@"):
                    match = re.search(hunk_pattern, line)
                    if match:
                        current_line_num = int(match.group(1))

                # Capturar líneas agregadas (+) o modificadas
                elif line.startswith("+") and not line.startswith("+++"):
                    if current_file and not self._should_ignore_file(current_file):
                        code = line[1:]  # Remover el prefijo '+'

                        if code.strip():  # Ignorar líneas vacías
                            fragments.append(
                                {
                                    "file": current_file,
                                    "line": current_line_num,
                                    "code": code.rstrip("\n"),
                                    "type": "added",
                                }
                            )

                        current_line_num += 1

                elif line.startswith("-") and not line.startswith("---"):
                    current_line_num += 1

                elif not line.startswith("\\"):
                    current_line_num += 1

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
