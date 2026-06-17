#!/usr/bin/env python3
"""
Extrae el dataset del Juliet Test Suite for Java v1.3.

Por cada archivo *_01.java en los directorios juliet-cwe*:
  - Extrae el cuerpo del método bad() → etiqueta 1 (VULNERABLE)
  - Extrae el cuerpo del método good() y sus sub-métodos → etiqueta 0 (SEGURO)
  - Ignora goodG2B() (fuente segura + sink inseguro = mezclado)

Genera: modelo/juliet_java_dataset.csv
"""

import csv
import os
import re
import sys
from pathlib import Path

JULIET_DIR = Path(__file__).parent / "Juliet_Java"
OUTPUT_CSV = Path(__file__).parent / "juliet_java_dataset.csv"

METHOD_PAT = re.compile(
    r'(public|private|protected)\s+(static\s+)?(void|boolean|int|String|Object|\[\]|\w+)\s+(\w+)\s*\('
)

def extract_method_body(lines: list[str], start_idx: int) -> tuple[str, int]:
    """Extrae el cuerpo completo de un método desde start_idx hasta el '}' de cierre."""
    body_lines = []
    brace_depth = 0
    started = False
    i = start_idx
    while i < len(lines):
        line = lines[i]
        if not started:
            body_lines.append(line)
            if '{' in line:
                brace_depth += line.count('{') - line.count('}')
                if brace_depth > 0:
                    started = True
        else:
            body_lines.append(line)
            brace_depth += line.count('{') - line.count('}')
            if brace_depth <= 0:
                break
        i += 1
    return '\n'.join(body_lines), i


def extract_method(lines: list[str], method_name: str) -> str | None:
    """Busca y extrae el cuerpo del método con nombre dado."""
    for i, line in enumerate(lines):
        m = METHOD_PAT.search(line)
        if m and m.group(4) == method_name:
            body, _ = extract_method_body(lines, i)
            return body
    return None


def extract_good_methods(lines: list[str]) -> str | None:
    """Extrae good() y todas sus subrutinas (goodB2G, good1, good2, etc.)
    pero EXCLUYE goodG2B (mezcla fuente segura + sink inseguro)."""
    good_main = extract_method(lines, 'good')
    if not good_main:
        return None

    result_lines = [good_main]

    for name in ['goodB2G', 'good1', 'good2', 'good3', 'goodG2GB2S', 'goodB2GSink',
                 'goodG2BSink', 'goodB2GSource', 'goodG2BSource']:
        body = extract_method(lines, name)
        if body:
            result_lines.append(body)

    return '\n\n'.join(result_lines)


def extract_bad_method(lines: list[str]) -> str | None:
    """Extrae el método bad()."""
    return extract_method(lines, 'bad')


def is_bad_only_file(lines: list[str]) -> bool:
    """Verifica si el archivo solo tiene método bad() y no good()."""
    for line in lines:
        m = METHOD_PAT.search(line)
        if m and m.group(4) == 'good':
            return False
    return True


def process_file(filepath: Path, writer, stats: dict):
    """Procesa un archivo Java y escribe filas en el CSV."""
    try:
        with open(filepath, 'r', encoding='utf-8', errors='replace') as f:
            text = f.read()
    except Exception as e:
        print(f"  [!] Error leyendo {filepath}: {e}")
        stats['errors'] += 1
        return

    lines = text.split('\n')

    # Extraer bad()
    bad_body = extract_bad_method(lines)
    if not bad_body:
        print(f"  [!] No se encontró bad() en {filepath.name}")
        stats['no_bad'] += 1
        return

    # Extraer good() y sub-métodos seguros
    good_body = extract_good_methods(lines)

    has_good = good_body is not None

    if is_bad_only_file(lines):
        # Solo tiene bad() - omitir (no hay contraparte segura)
        stats['bad_only'] += 1
        return

    # Escribir bad() como vulnerable
    # Extraer juliet-cweXXX del path: .../Juliet_Java/juliet-cweXXX/...
    rel = str(filepath.relative_to(JULIET_DIR))
    cwe = rel.split(os.sep)[0]
    writer.writerow({
        'code': bad_body,
        'label': 1,
        'cwe': cwe,
        'file': str(filepath.relative_to(JULIET_DIR)),
    })
    stats['vulnerable'] += 1

    # Escribir good() como seguro
    if good_body:
        writer.writerow({
            'code': good_body,
            'label': 0,
            'cwe': cwe,
            'file': str(filepath.relative_to(JULIET_DIR)),
        })
        stats['safe'] += 1
    else:
        stats['no_good'] += 1


def main():
    if not JULIET_DIR.exists():
        print(f"❌ No se encuentra el directorio: {JULIET_DIR}")
        print("   Asegúrate de tener el Juliet Test Suite extraído en modelo/Juliet_Java/")
        sys.exit(1)

    print(f"[*] Buscando archivos Java en: {JULIET_DIR}")
    java_files = sorted(JULIET_DIR.glob('juliet-cwe*/**/src/main/java/**/*.java'))

    # Excluir archivos de soporte (no son test cases) y AbstractTestCase
    java_files = [f for f in java_files
                  if 'support' not in str(f)
                  and 'AbstractTestCase' not in f.name
                  and not f.name.startswith('Abstract')]

    print(f"[+] Archivos .java encontrados: {len(java_files)}")

    stats = {'vulnerable': 0, 'safe': 0, 'no_bad': 0, 'no_good': 0, 'bad_only': 0, 'errors': 0}

    with open(OUTPUT_CSV, 'w', newline='', encoding='utf-8') as csvfile:
        fieldnames = ['code', 'label', 'cwe', 'file']
        writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
        writer.writeheader()

        for fpath in java_files:
            process_file(fpath, writer, stats)

    # Reporte
    print(f"\n{'='*50}")
    print("EXTRACCION COMPLETADA")
    print(f"{'='*50}")
    print(f"  Vulnerables (label=1): {stats['vulnerable']}")
    print(f"  Seguros     (label=0): {stats['safe']}")
    print(f"  Total muestras:       {stats['vulnerable'] + stats['safe']}")
    print(f"\n  Omitidos:")
    print(f"    Sin bad():          {stats['no_bad']}")
    print(f"    Sin good():         {stats['no_good']}")
    print(f"    Bad-only files:     {stats['bad_only']}")
    print(f"    Errores:            {stats['errors']}")
    print(f"\n[*] CSV generado: {OUTPUT_CSV}")


if __name__ == '__main__':
    main()
