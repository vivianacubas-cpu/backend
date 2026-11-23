package pe.edu.vallegrande.app.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import pe.edu.vallegrande.app.service.exception.DuplicateResourceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Global exception handler for REST controllers.
 * Centralizes HTTP error responses without hiding exceptions from server logs.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /** Handles Bean Validation errors (@Valid) — returns 400 with field-level messages. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validationError(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());
        String summary = String.join(" | ", errors);
        log.debug("Validation failed: {}", summary);
        return ResponseEntity.badRequest().body(Map.of(
                "error", summary,
                "fields", errors
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", safeMessage(ex, "The submitted data is not valid.")));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> unreadableBody(HttpMessageNotReadableException ex) {
        log.debug("Invalid JSON body", ex);
        return ResponseEntity.badRequest().body(Map.of("error", "The submitted JSON is not valid or contains an incorrect data type."));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, Object>> notFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", safeMessage(ex, "The requested resource was not found.")));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<Map<String, Object>> duplicate(DuplicateResourceException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", safeMessage(ex, "The record already exists.")));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> conflict(DataIntegrityViolationException ex) {
        // SQL Server may reject unique names, foreign keys, or required columns.
        // Internal details are not exposed to the client; the stack trace remains in the backend log.
        log.warn("Database integrity conflict", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "The record could not be saved because it violates a database constraint. Check for duplicates and required relationships."));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> runtimeError(RuntimeException ex) {
        log.error("Runtime error", ex);
        String message = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> internalError(Exception ex) {
        log.error("Internal error processing REST request", ex);
        // Include root cause to assist diagnosis from Swagger/frontend
        String message = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
        String cause = ex.getCause() != null && ex.getCause().getMessage() != null
                ? ex.getCause().getMessage()
                : null;
        if (cause != null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", message, "cause", cause));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", message));
    }

    private String safeMessage(Exception ex, String fallback) {
        return ex.getMessage() == null || ex.getMessage().isBlank() ? fallback : ex.getMessage();
    }
}
