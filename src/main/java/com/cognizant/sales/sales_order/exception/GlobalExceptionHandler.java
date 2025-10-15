package com.cognizant.sales.sales_order.exception;

import com.cognizant.sales.sales_order.exception.DuplicateResourceException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global exception handler to process exceptions and return a consistent JSON error response.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles AccessDeniedException, which is thrown by Spring Security when a user
     * attempts to access a resource they do not have permission for.
     *
     * @param ex The exception that was thrown.
     * @return A ResponseEntity with HTTP status 403 (Forbidden) and a structured error message.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDeniedException(AccessDeniedException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.FORBIDDEN.value());
        body.put("error", "Forbidden");
        body.put("message", "You do not have the required role to perform this action.");

        return new ResponseEntity<>(body, HttpStatus.FORBIDDEN);
    }

    /**
     * Handles EntityNotFoundException, which is commonly thrown by services when an
     * entity cannot be found in the database.
     *
     * @param ex The exception that was thrown.
     * @return A ResponseEntity with HTTP status 404 (Not Found) and a structured error message.
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Object> handleEntityNotFoundException(EntityNotFoundException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.NOT_FOUND.value());
        body.put("error", "Not Found");
        body.put("message", ex.getMessage());

        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    /**
     * Handles DuplicateResourceException, which is thrown when attempting to create a
     * resource that already exists (e.g., a product with a name that is already in use).
     *
     * @param ex The exception that was thrown.
     * @return A ResponseEntity with HTTP status 409 (Conflict) and a structured error message.
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<Object> handleDuplicateResourceException(DuplicateResourceException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.CONFLICT.value());
        body.put("error", "Conflict");
        body.put("message", ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.CONFLICT);
    }

    /**
     * Handles IllegalStateException, which can be thrown for business logic violations,
     * such as trying to cancel an already cancelled order.
     *
     * @param ex The exception that was thrown.
     * @return A ResponseEntity with HTTP status 409 (Conflict) and a structured error message.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Object> handleIllegalStateException(IllegalStateException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.CONFLICT.value());
        body.put("error", "Conflict");
        body.put("message", ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.CONFLICT);
    }
}