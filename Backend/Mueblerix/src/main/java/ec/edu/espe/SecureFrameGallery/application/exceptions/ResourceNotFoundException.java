package ec.edu.espe.SecureFrameGallery.application.exceptions;


public class ResourceNotFoundException extends BusinessException {
    
    public ResourceNotFoundException(String message) {
        super(message, "RESOURCE_NOT_FOUND", org.springframework.http.HttpStatus.NOT_FOUND);
    }

}
