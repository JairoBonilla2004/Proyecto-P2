#!/usr/bin/env python3
import ast
import logging
import math
import re
from dataclasses import asdict, dataclass
from typing import Optional

logger = logging.getLogger(__name__)


@dataclass
class CodeFeatures:
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
        return asdict(self)


# ── Language detection ──
_PAT_JAVA_DETECT = re.compile(
    r'\b(public|private|protected|class\s+\w+|import\s+java\.'
    r'|String\s+\w+\s*[=;]'
    r'|(Statement|PreparedStatement|ResultSet|Connection)\s+\w+'
    r'|System\.(out|getProperty|getenv)'
    r'|Runtime\.|ProcessBuilder|ObjectInputStream'
    r'|MessageDigest|FileInputStream|FileWriter'
    r'|\.getInstance\s*\('
    r'|\.getRuntime\s*\('
    r'|throws\s+\w+'
    r'|new\s+[A-Z]\w*[a-z]\w*\s*\('
    r')',
    re.IGNORECASE,
)

# ── Python patterns ──
_PAT_DANGEROUS_PY = re.compile(
    r'\b(eval|exec|execfile|compile|__import__|getattr|setattr|delattr)\s*\(',
    re.IGNORECASE,
)
_PAT_SUBPROCESS_SHELL_PY = re.compile(
    r'subprocess\.(call|run|Popen|check_output|check_call)\s*\([^)]*shell\s*=\s*True',
    re.IGNORECASE | re.DOTALL,
)
_PAT_SUBPROCESS_ANY_PY = re.compile(
    r'subprocess\.(call|run|Popen|check_output|check_call|getoutput)\s*\(',
    re.IGNORECASE,
)
_PAT_OS_COMMANDS_PY = re.compile(
    r'os\.(system|popen|execv|execve|execvp|execvpe|spawnv|spawnve)\s*\(',
    re.IGNORECASE,
)
_PAT_SQL_RAW_PY = re.compile(
    r'(\.execute\s*\([^?%]*(\+|%|format|f[\'"]|\{)|"\'\\s*\+\s*\w+\s*\+\s*"\')',
    re.IGNORECASE,
)
_PAT_SQL_FSTRING_PY = re.compile(
    r'(SELECT|INSERT|UPDATE|DELETE|DROP|UNION|FROM|WHERE)[^;]*"\'\\s*(%|format|f\'|f")',
    re.IGNORECASE,
)
_PAT_PATH_CONCAT_PY = re.compile(
    r'(open|file)\s*\(\s*[\w\'"]+\s*\+\s*\w+', re.IGNORECASE
)
_PAT_WEAK_HASH_PY = re.compile(r'hashlib\.(md5|sha1)\s*\(', re.IGNORECASE)

# ── Java patterns ──
_PAT_DANGEROUS_JJ = re.compile(
    r'(Runtime\s*\.\s*(getRuntime|exec)\s*\('
    r'|ProcessBuilder\s*\('
    r'|Class\.forName\s*\('
    r'|Method\s*\.\s*invoke\s*\('
    r'|java\.lang\.reflect\.)',
    re.IGNORECASE,
)
_PAT_CMD_EXEC_JJ = re.compile(
    r'Runtime\s*\.\s*(?:getRuntime\s*\(\s*\)\s*\.\s*)?exec\s*\('
    r'|\w+\.\s*(?:exec|start)\s*\(',
    re.IGNORECASE,
)
_PAT_SQL_CONCAT_JJ = re.compile(
    r'["\'](?:SELECT|INSERT|UPDATE|DELETE|DROP|ALTER|CREATE|EXEC|MERGE|CALL)'
    r'[^"\']*["\']\s*\+',
    re.IGNORECASE,
)
_PAT_PATH_TRAVERSAL_JJ = re.compile(
    r'new\s+(File|FileInputStream|FileOutputStream|FileReader|FileWriter|RandomAccessFile)'
    r'\s*\([^)]*(?:\+|getParameter|getHeader|getQueryString)',
    re.IGNORECASE,
)
_PAT_WEAK_CRYPTO_JJ = re.compile(
    r'MessageDigest\.getInstance\s*\(\s*["\'](MD5|SHA-1|SHA1|SHA)["\']'
    r'|Cipher\.getInstance\s*\(\s*["\'].*?(DES|ECB)["\']'
    r'|javax\.crypto\.'
    r'|new\s+java\.util\.Random\s*\('
    r'|Math\.random\s*\(',
    re.IGNORECASE,
)
_PAT_DESERIALIZATION_JJ = re.compile(
    r'(?:ObjectInputStream|readObject|readUnshared)\s*\.\s*(readObject|readUnshared)\s*\('
    r'|new\s+ObjectInputStream',
    re.IGNORECASE,
)
_PAT_JNDI_LOOKUP_JJ = re.compile(
    r'(InitialContext|Context)\.\s*(lookup|search|list)\s*\(',
    re.IGNORECASE,
)
_PAT_XXE_JJ = re.compile(
    r'DocumentBuilderFactory\.newInstance\s*\('
    r'|XMLReader\s*\('
    r'|SAXBuilder\s*\('
    r'|SAXParser\s*\('
    r'|XPath\.\s*evaluate\s*\(',
    re.IGNORECASE,
)
_PAT_XSS_JJ = re.compile(
    r'(PrintWriter|println)\s*\([^)]*getParameter',
    re.IGNORECASE | re.DOTALL,
)
_PAT_LDAP_JJ = re.compile(
    r'(DirContext|InitialDirContext|LdapContext)\.\s*(search|lookup)\s*\(',
    re.IGNORECASE,
)

# ── Shared patterns ──
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
    r'PreparedStatement\b'
    r'|\?\s*\)'
    r'|setString\s*\(\s*\d+\s*,'
    r'|setInt\s*\(\s*\d+\s*,'
    r'|setObject\s*\(\s*\d+\s*,'
    r'|\.execute\s*\(\s*[\'"][^\'"]*(\?|%s|:param|:\w+)[\'"]',
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
_PAT_SECURE_CRYPTO = re.compile(
    r'MessageDigest\.getInstance\s*\(\s*["\']SHA-256["\']'
    r'|MessageDigest\.getInstance\s*\(\s*["\']SHA-512["\']'
    r'|Cipher\.getInstance\s*\(\s*["\'].*?AES.*?GCM["\']'
    r'|SecureRandom\b'
    r'|KeyGenerator\.getInstance\s*\(\s*["\']AES["\']'
    r'|\b(secrets|hmac|defusedxml|bleach|bcrypt|argon2)\b',
    re.IGNORECASE,
)


def _ast_depth_py(tree) -> int:
    if not isinstance(tree, ast.AST):
        return 0
    children = list(ast.iter_child_nodes(tree))
    if not children:
        return 1
    return 1 + max(_ast_depth_py(child) for child in children)


def _ast_depth_brace(code: str) -> int:
    max_depth = 0
    cur = 0
    for ch in code:
        if ch in '{(':
            cur += 1
            max_depth = max(max_depth, cur)
        elif ch in '})':
            cur = max(0, cur - 1)
    return max_depth


def _shannon_entropy(text: str) -> float:
    if not text:
        return 0.0
    freq: dict = {}
    for c in text:
        freq[c] = freq.get(c, 0) + 1
    n = len(text)
    return -sum((f / n) * math.log2(f / n) for f in freq.values())


def _is_java(code: str) -> bool:
    return bool(_PAT_JAVA_DETECT.search(code))


def extraer_features_codigo(codigo: str) -> dict:
    is_java = _is_java(codigo)
    features: dict = {}

    if is_java:
        features["ast_depth"] = _ast_depth_brace(codigo)
        features["ast_node_count"] = len(re.findall(
            r'\b(new|if|for|while|switch|try|catch|return|throw|import|class|'
            r'public|private|protected|void|int|String|boolean|static|final)\b', codigo)
        ) + len(re.findall(r'\w+\s*\(', codigo))
        features["ast_parse_error"] = 0
        features["func_calls_count"] = len(re.findall(r'\w+\s*\(', codigo))
        branch_nodes_jj = len(re.findall(r'\b(if|for|while|switch|case|catch)\b', codigo))
        logical_ops = len(re.findall(r'&&|\|\|', codigo))
        features["cyclomatic_complexity"] = branch_nodes_jj + logical_ops + 1

        features["dangerous_func_calls"] = len(_PAT_DANGEROUS_JJ.findall(codigo))
        features["subprocess_shell"] = 1 if _PAT_CMD_EXEC_JJ.search(codigo) else 0
        features["subprocess_any"] = 1 if re.search(r'\bProcessBuilder\b', codigo, re.IGNORECASE) else 0
        features["os_commands"] = 1 if _PAT_CMD_EXEC_JJ.search(codigo) or re.search(r'\bProcessBuilder\b', codigo, re.IGNORECASE) else 0
        features["sql_raw_concat"] = 1 if _PAT_SQL_CONCAT_JJ.search(codigo) else 0
        features["sql_fstring"] = 1 if _PAT_SQL_CONCAT_JJ.search(codigo) else 0
        features["pickle_usage"] = 1 if _PAT_DESERIALIZATION_JJ.search(codigo) else 0
        features["path_concat"] = 1 if _PAT_PATH_TRAVERSAL_JJ.search(codigo) else 0
        features["weak_hash"] = 1 if _PAT_WEAK_CRYPTO_JJ.search(codigo) else 0
        features["insecure_random"] = 1 if re.search(r'\bnew\s+java\.util\.Random\b', codigo) else 0

        features["sanitization_present"] = 1 if _PAT_SANITIZATION.search(codigo) else 0
        features["sql_parameterized"] = 1 if _PAT_SQL_PARAM.search(codigo) else 0
        features["env_vars_used"] = 1 if _PAT_ENV_VARS.search(codigo) else 0
        features["type_validation"] = 1 if _PAT_TYPE_VALID.search(codigo) else 0
        features["exception_handling"] = 1 if _PAT_EXCEPTIONS.search(codigo) else 0
        features["secure_imports"] = 1 if _PAT_SECURE_IMPORTS.search(codigo) else 0

        features["raise_count"] = len(re.findall(r'\bthrow\b', codigo))
    else:
        try:
            tree = ast.parse(codigo)
            features["ast_depth"] = _ast_depth_py(tree)
            features["ast_node_count"] = sum(1 for _ in ast.walk(tree))
            features["ast_parse_error"] = 0
            features["func_calls_count"] = sum(1 for node in ast.walk(tree) if isinstance(node, ast.Call))
            branch_nodes = (ast.If, ast.For, ast.While, ast.Try, ast.ExceptHandler, ast.With, ast.Assert, ast.comprehension)
            features["cyclomatic_complexity"] = sum(1 for node in ast.walk(tree) if isinstance(node, branch_nodes)) + 1
        except SyntaxError:
            features["ast_depth"] = 0
            features["ast_node_count"] = 0
            features["ast_parse_error"] = 1
            features["func_calls_count"] = 0
            features["cyclomatic_complexity"] = 1

        features["dangerous_func_calls"] = len(_PAT_DANGEROUS_PY.findall(codigo))
        features["subprocess_shell"] = 1 if _PAT_SUBPROCESS_SHELL_PY.search(codigo) else 0
        features["subprocess_any"] = 1 if _PAT_SUBPROCESS_ANY_PY.search(codigo) else 0
        features["os_commands"] = 1 if _PAT_OS_COMMANDS_PY.search(codigo) else 0
        features["sql_raw_concat"] = 1 if _PAT_SQL_RAW_PY.search(codigo) else 0
        features["sql_fstring"] = 1 if _PAT_SQL_FSTRING_PY.search(codigo) else 0
        features["pickle_usage"] = 1 if re.search(r'\bpickle\.(loads|load|Unpickler)\s*\(', codigo, re.IGNORECASE) else 0
        features["path_concat"] = 1 if _PAT_PATH_CONCAT_PY.search(codigo) else 0
        features["weak_hash"] = 1 if _PAT_WEAK_HASH_PY.search(codigo) else 0
        features["insecure_random"] = 1 if re.search(r'random\.(randint|random|choice|shuffle|sample)\s*\(', codigo, re.IGNORECASE) else 0

        features["sanitization_present"] = 1 if _PAT_SANITIZATION.search(codigo) else 0
        features["sql_parameterized"] = 1 if _PAT_SQL_PARAM.search(codigo) else 0
        features["env_vars_used"] = 1 if _PAT_ENV_VARS.search(codigo) else 0
        features["type_validation"] = 1 if _PAT_TYPE_VALID.search(codigo) else 0
        features["exception_handling"] = 1 if _PAT_EXCEPTIONS.search(codigo) else 0
        features["secure_imports"] = 1 if _PAT_SECURE_IMPORTS.search(codigo) else 0

        features["raise_count"] = len(re.findall(r'\braise\b', codigo))

    features["shannon_entropy"] = _shannon_entropy(codigo)
    features["lines_count"] = codigo.count("\n") + 1
    features["string_concat_count"] = len(_PAT_STR_CONCAT.findall(codigo))
    features["security_comments"] = len(_PAT_SEC_COMMENTS.findall(codigo))
    features["secure_advanced"] = 1 if _PAT_SECURE_CRYPTO.search(codigo) else 0

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
        + features.get("secure_advanced", 0) * 2
    )

    return features


class FeatureExtractor:
    PYTHON_DANGEROUS_PATTERNS = {
        "eval": r"\beval\s*\(",
        "exec": r"\bexec\s*\(",
        "os_system": r"\bos\s*\.\s*system\s*\(",
        "subprocess_popen": r"\bsubprocess\s*\.\s*(Popen|call|run|check_output)\s*\(",
        "compile": r"\bcompile\s*\(",
        "pickle": r"\bpickle\s*\.\s*loads?\s*\(",
    }
    JAVA_DANGEROUS_PATTERNS = {
        "runtime_exec": (
            r'Runtime\s*\.\s*(?:getRuntime\s*\(\s*\)\s*\.\s*)?exec\s*\('
            r'|\w+\.\s*(?:exec|start)\s*\('
        ),
        "processbuilder": r"\bProcessBuilder\s*\(",
        "sql_concat": r'["\'](?:SELECT|INSERT|UPDATE|DELETE|DROP|ALTER|CREATE|EXEC|MERGE|CALL)[^"\']*["\']\s*\+',
        "reflection": r"Class\s*\.\s*forName\s*\(",
    }
    SANITIZATION_PATTERNS = {
        "prepared_statement": r"PreparedStatement\b",
        "input_validation": r"(?:validate|check|verify|sanitize|escape)\s*\(",
        "regex_validation": r"matches\s*\(|Pattern\s*\.\s*compile",
        "sql_param": r"\?\s*\)",
    }
    VALIDATION_PATTERNS = {
        "null_check": r"\s+(?:if|!=)\s+.*(?:null|None)\b",
        "type_check": r"(?:isinstance|type)\s*\(\s*\w+\s*,",
        "length_check": r"(?:len|length)\s*\(\s*\w+\s*\)\s*(?:>|<|==)",
        "range_check": r"(?:\d+\s*<\s*\w+|in\s+range)",
    }

    def __init__(self, language: str = "auto"):
        self.language = language
        logger.info(f"FeatureExtractor initialized for language: {language}")

    def _detect_language(self, code: str) -> str:
        if _is_java(code):
            return "java"
        if "import " in code or "from " in code:
            return "python"
        if "def " in code or "class " in code:
            return "python"
        return "python"

    def _count_tokens(self, code: str) -> int:
        tokens = re.findall(r"\w+|[(){}\[\];:,.]|[+\-*/%=<>!&|^~]", code)
        return len(tokens)

    def _calculate_ast_depth(self, code: str, language: str = "python") -> int:
        if language == "python":
            try:
                return _ast_depth_py(ast.parse(code))
            except SyntaxError:
                return 0
        elif language == "java":
            return _ast_depth_brace(code)
        else:
            return code.count("{") // 2

    def _get_ast_depth(self, node: ast.AST, depth: int = 0) -> int:
        if not isinstance(node, ast.AST):
            return depth
        max_depth = depth
        for child in ast.iter_child_nodes(node):
            child_depth = self._get_ast_depth(child, depth + 1)
            max_depth = max(max_depth, child_depth)
        return max_depth

    def _detect_dangerous_functions(self, code: str, language: str) -> tuple[int, dict]:
        count = 0
        details = {}
        patterns = self.PYTHON_DANGEROUS_PATTERNS if language == "python" else self.JAVA_DANGEROUS_PATTERNS
        for func_name, pattern in patterns.items():
            matches = re.findall(pattern, code, re.IGNORECASE | re.MULTILINE)
            if matches:
                count += len(matches)
                details[func_name] = len(matches)
        return count, details

    def _detect_sanitization(self, code: str) -> bool:
        for pattern in self.SANITIZATION_PATTERNS.values():
            if re.search(pattern, code, re.IGNORECASE):
                return True
        return False

    def _detect_input_validation(self, code: str) -> bool:
        for pattern in self.VALIDATION_PATTERNS.values():
            if re.search(pattern, code, re.IGNORECASE):
                return True
        return False

    def _has_try_catch(self, code: str, language: str) -> bool:
        if language == "python":
            return bool(re.search(r"\btry\s*:|except\s+", code))
        elif language == "java":
            return bool(re.search(r"\btry\s*\{|catch\s*\(", code))
        return bool(re.search(r"try|catch|except", code, re.IGNORECASE))

    def extract(self, code: str, language: Optional[str] = None) -> CodeFeatures:
        if not code or not code.strip():
            return CodeFeatures(0, 0, False, False, False, False, False, False, False, False, False, False, 0, 0)

        lang = language or self.language
        if lang == "auto":
            lang = self._detect_language(code)

        token_count = self._count_tokens(code)
        ast_depth = self._calculate_ast_depth(code, lang)
        lines_of_code = len(code.split("\n"))

        dangerous_count, dangerous_details = self._detect_dangerous_functions(code, lang)

        return CodeFeatures(
            token_count=token_count,
            ast_depth=ast_depth,
            has_eval="eval" in dangerous_details,
            has_exec="exec" in dangerous_details,
            has_sql_concat="sql_concat" in dangerous_details,
            has_os_system="os_system" in dangerous_details,
            has_subprocess="subprocess_popen" in dangerous_details,
            has_runtime_exec="runtime_exec" in dangerous_details,
            has_processbuilder="processbuilder" in dangerous_details,
            has_input_validation=self._detect_input_validation(code),
            has_sanitization=self._detect_sanitization(code),
            has_try_catch=self._has_try_catch(code, lang),
            lines_of_code=lines_of_code,
            dangerous_functions_count=dangerous_count,
        )

    def extract_batch(self, code_fragments: list[dict]) -> list[dict]:
        results = []
        for fragment in code_fragments:
            code = fragment.get("code", "")
            file = fragment.get("file", "unknown")
            line = fragment.get("line", 0)
            try:
                features = self.extract(code)
                results.append({"file": file, "line": line, "features": features.to_dict()})
            except Exception as e:
                logger.error(f"Error extracting features from {file}:{line}: {e}")
                results.append({"file": file, "line": line, "error": str(e), "features": None})
        return results


def main():
    logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(name)s - %(levelname)s - %(message)s")

    test_cases = [
        ("SQL Injection (vuln)", 'String q = "SELECT * FROM users WHERE id = " + request.getParameter("id");\nStatement stmt = conn.createStatement();\nResultSet rs = stmt.executeQuery(q);\n'),
        ("SQL Safe", 'String q = "SELECT * FROM users WHERE id = ?";\nPreparedStatement stmt = conn.prepareStatement(q);\nstmt.setInt(1, userId);\n'),
        ("Runtime exec", 'Runtime rt = Runtime.getRuntime();\nProcess p = rt.exec("cmd /c " + input);\n'),
        ("Crypto weak", 'MessageDigest md = MessageDigest.getInstance("MD5");\nmd.digest(data);\n'),
        ("Deserialization", 'FileInputStream fis = new FileInputStream("data.ser");\nObjectInputStream ois = new ObjectInputStream(fis);\nObject obj = ois.readObject();\n'),
    ]
    for name, code in test_cases:
        feats = extraer_features_codigo(code)
        print(f"\n=== {name} ===")
        for k, v in sorted(feats.items()):
            print(f"  {k}: {v}")


if __name__ == "__main__":
    main()
