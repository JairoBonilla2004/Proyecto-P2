package ec.edu.espe.SecureFrameGallery.modules.auth.dtos;

import jakarta.validation.constraints.*;
import lombok.Data;


@Data
public class RegisterRequest {

    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(min = 3, max = 50, message = "El username debe tener entre 3 y 50 caracteres")
    @Pattern(
        regexp = "^[a-zA-Z0-9_.-]+$",
        message = "El username solo puede contener letras, números, guiones y puntos"
    )
    private String username;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "Formato de correo inválido")
    @Size(max = 150, message = "El correo no puede superar 150 caracteres")
    private String email;


    @NotBlank(message = "La contraseña es obligatoria")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
        message = "La contraseña debe tener mínimo 8 caracteres, una mayúscula, una minúscula, un número y un carácter especial"
    )
    private String password;
}
