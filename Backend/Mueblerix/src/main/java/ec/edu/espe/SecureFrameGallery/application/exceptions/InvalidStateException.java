package ec.edu.espe.SecureFrameGallery.application.exceptions;


public class InvalidStateException extends BusinessException {
    
    public InvalidStateException(String message) {
        super(message, "INVALID_STATE");
    }

}
