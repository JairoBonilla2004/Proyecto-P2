package ec.edu.espe.SecureFrameGallery.application.exceptions;

import org.springframework.http.HttpStatus;


public class BusinessException extends RuntimeException {
    private final HttpStatus httpStatus;
    private final String errorCode;

    public BusinessException(String message, String errorCode) {
        this(message, errorCode, HttpStatus.CONFLICT);
    }

    public BusinessException(String message, String errorCode, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
