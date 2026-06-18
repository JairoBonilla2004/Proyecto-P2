package ec.edu.espe.SecureFrameGallery.application.security;

public class AplicacionDelCaos {

    // 1. Credenciales hardcodeadas
    private static final String ADMIN = "admin";
    private static final String PASSWORD = "123456";

    public static void main(String[] args) {

        System.out.println("=== SISTEMA HIPER INSEGURO ===");

        String usuario = "admin";
        String password = "' OR '1'='1";

        // 2. SQL Injection (simulada)
        String sql = "SELECT * FROM usuarios WHERE usuario='"
                + usuario
                + "' AND password='"
                + password
                + "'";

        System.out.println("\n[SQL GENERADO]");
        System.out.println(sql);

        // 3. Contraseñas en texto plano
        System.out.println("\n[DEBUG]");
        System.out.println("Password real del admin: " + PASSWORD);

        // 4. Autenticación absurda
        if (usuario.equals(ADMIN) || password.contains("OR")) {
            System.out.println("Acceso concedido.");
        }

        // 5. Información sensible expuesta
        System.out.println("\n[CONFIGURACION]");
        System.out.println("Servidor: localhost");
        System.out.println("Base: produccion");
        System.out.println("Usuario BD: root");
        System.out.println("Password BD: root123");

        // 6. XSS reflejado (simulado)
        String comentario = "<script>alert('Hackeado');</script>";

        System.out.println("\n[COMENTARIO USUARIO]");
        System.out.println(comentario);

        // 7. Path Traversal (simulado)
        String archivo = "../../../Windows/System32/config";

        System.out.println("\n[ABRIENDO ARCHIVO]");
        System.out.println(archivo);

        // 8. Generación de token insegura
        int token = (int) (Math.random() * 1000);

        System.out.println("\n[TOKEN]");
        System.out.println(token);

        // 9. Datos sensibles registrados
        System.out.println("\n[LOG]");
        System.out.println(
                "Usuario="
                        + usuario
                        + " Password="
                        + password);

        // 10. Sin validación de entrada
        String correo = "no_es_un_correo";

        System.out.println("\n[CORREO]");
        System.out.println(correo);

        // 11. Rol otorgado por variable modificable
        String rol = "ADMIN";

        if ("ADMIN".equals(rol)) {
            System.out.println(
                    "\nPermisos totales otorgados.");
        }

        // 12. Manejo de errores inexistente
        System.out.println("\nTodo funciona perfectamente...");
    }
}