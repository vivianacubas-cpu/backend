package pe.edu.vallegrande.app.service.exception;

/** Indicates that a resource attempts to duplicate a unique business value. */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
