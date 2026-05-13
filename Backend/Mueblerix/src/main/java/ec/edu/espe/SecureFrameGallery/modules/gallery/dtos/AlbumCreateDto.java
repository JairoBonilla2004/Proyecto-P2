package ec.edu.espe.SecureFrameGallery.modules.gallery.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;


@Data
public class AlbumCreateDto {

    @NotBlank(message = "El título es obligatorio")
    @Size(min = 1, max = 100, message = "El título debe tener entre 1 y 100 caracteres")
    private String title;

    @Size(max = 500, message = "La descripción no puede superar 500 caracteres")
    private String description;

    private boolean isPublic = true;
}
