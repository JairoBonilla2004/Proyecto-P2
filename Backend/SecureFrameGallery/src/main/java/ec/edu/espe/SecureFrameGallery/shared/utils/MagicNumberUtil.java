package ec.edu.espe.SecureFrameGallery.shared.utils;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;

/**
 * Validación de tipos de archivo mediante números mágicos (magic bytes).
 *
 * RNF-Seguridad: Nunca confiar en la extensión ni en el Content-Type del cliente.
 * Se deben inspeccionar los primeros bytes del archivo para determinar su tipo real.
 *
 * Tipos soportados: JPEG, PNG, GIF, WEBP.
 */
@Component
public class MagicNumberUtil {

    private static final Map<String, byte[]> MAGIC_BYTES = Map.of(
        "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
        "image/png",  new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A},
        "image/gif",  new byte[]{0x47, 0x49, 0x46, 0x38},
        "image/webp", new byte[]{0x52, 0x49, 0x46, 0x46}  // RIFF (validación completa abajo)
    );

    private static final byte[] WEBP_FOURCC = new byte[]{0x57, 0x45, 0x42, 0x50}; // "WEBP"

    /**
     * Verifica que los bytes iniciales del archivo corresponden a un tipo de imagen válido.
     *
     * @param fileBytes contenido del archivo
     * @return el MIME type detectado ("image/jpeg", "image/png", etc.)
     * @throws IllegalArgumentException si el archivo no es una imagen válida
     */
    public String detectAndValidate(byte[] fileBytes) {
        if (fileBytes == null || fileBytes.length < 12) {
            throw new IllegalArgumentException("El archivo está vacío o es demasiado pequeño");
        }

        for (Map.Entry<String, byte[]> entry : MAGIC_BYTES.entrySet()) {
            String mime = entry.getKey();
            byte[] magic = entry.getValue();

            // Comparar solo los primeros N bytes según la firma del tipo
            if (!startsWith(fileBytes, magic)) {
                continue;
            }

            // WEBP es un contenedor RIFF: validar también el FOURCC "WEBP" en offset 8.
            if ("image/webp".equals(mime) && !isWebp(fileBytes)) {
                continue;
            }

            return mime;
        }

        throw new IllegalArgumentException(
            "Tipo de archivo no permitido. Solo se aceptan: JPEG, PNG, GIF, WEBP"
        );
    }

    /**
     * Verifica si los bytes del archivo comienzan con la secuencia magic dada.
     */
    private boolean startsWith(byte[] fileBytes, byte[] magic) {
        if (fileBytes.length < magic.length) return false;
        return Arrays.equals(
            Arrays.copyOfRange(fileBytes, 0, magic.length),
            magic
        );
    }

    private boolean isWebp(byte[] fileBytes) {
        if (fileBytes == null || fileBytes.length < 12) {
            return false;
        }
        // RIFF....WEBP
        return startsWith(fileBytes, MAGIC_BYTES.get("image/webp"))
            && Arrays.equals(Arrays.copyOfRange(fileBytes, 8, 12), WEBP_FOURCC);
    }
}
