package com.yowyob.erp.shared.domain.exception;

import com.yowyob.erp.shared.infrastructure.dto.ApiResponseWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the application.
 * Intercepts specific exceptions and returns standardized English error
 * responses.
 * 
 * @author ALD
 * @date 30.09.25
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handles BusinessException.
     * 
     * @param ex the business exception
     * @return bad request response with error message
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponseWrapper<Object>> handleBusinessException(BusinessException ex) {
        log.error("Business error: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponseWrapper.error(ex.getMessage()));
    }

    /**
     * Handles ResourceNotFoundException.
     * 
     * @param ex the resource not found exception
     * @return not found response with error message
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponseWrapper<Object>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.error("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponseWrapper.error(ex.getMessage()));
    }

    /**
     * Handles OrganizationException.
     * 
     * @param ex the organization exception
     * @return forbidden response with error message
     */
    @ExceptionHandler(OrganizationException.class)
    public ResponseEntity<ApiResponseWrapper<Object>> handleOrganizationException(OrganizationException ex) {
        log.error("Organization error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponseWrapper.error(ex.getMessage()));
    }

    /**
     * Handles validation exceptions (MethodArgumentNotValidException).
     * 
     * @param ex the validation exception
     * @return bad request response with detailed validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseWrapper<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));
        return validationErrorResponse(errors);
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ApiResponseWrapper<Map<String, String>>> handleWebExchangeBindException(
            WebExchangeBindException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));
        return validationErrorResponse(errors);
    }

    private ResponseEntity<ApiResponseWrapper<Map<String, String>>> validationErrorResponse(
            Map<String, String> errors) {
        log.error("Validation errors: {}", errors);
        return ResponseEntity.badRequest()
                .body(ApiResponseWrapper.error("Validation failed", errors));
    }

    /**
     * Handles IllegalArgumentException.
     * 
     * @param ex the illegal argument exception
     * @return bad request response with error message
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponseWrapper<Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("Illegal argument: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponseWrapper.error(ex.getMessage()));
    }

    /**
     * Handles IllegalStateException.
     * 
     * @param ex the illegal state exception
     * @return conflict or bad request response with error message
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponseWrapper<Object>> handleIllegalStateException(IllegalStateException ex) {
        log.error("Illegal state: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponseWrapper.error(ex.getMessage(), 409));
    }

    /**
     * Handles DataIntegrityViolationException.
     * 
     * @param ex the data integrity violation exception
     * @return conflict response with error message
     */
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponseWrapper<Object>> handleDataIntegrityViolationException(
            org.springframework.dao.DataIntegrityViolationException ex) {
        log.error("Data integrity violation: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponseWrapper.error("Database error: " + ex.getRootCause().getMessage()));
    }

    /**
     * Handles authorization denials from method security (@PreAuthorize) and the
     * security filter chain. Without this, an AccessDeniedException falls through to
     * the generic handler and returns 500 instead of 403. Covers Spring Security 6's
     * AuthorizationDeniedException, which extends AccessDeniedException.
     *
     * @param ex the access denied exception
     * @return forbidden response
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiResponseWrapper<Object>> handleAccessDeniedException(
            org.springframework.security.access.AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponseWrapper.error("You do not have permission to perform this action"));
    }

    /**
     * Handles any other unexpected exceptions.
     *
     * @param ex the exception
     * @return internal server error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseWrapper<Object>> handleGenericException(Exception ex) {
        log.error("Internal server error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseWrapper.error("An unexpected error occurred on the server"));
    }
    /**
     * Handles NoResourceFoundException (404).
     * 
     * @param ex the no resource found exception
     * @return not found response
     */
    @ExceptionHandler(org.springframework.web.reactive.resource.NoResourceFoundException.class)
    public ResponseEntity<ApiResponseWrapper<Object>> handleNoResourceFoundException(
            org.springframework.web.reactive.resource.NoResourceFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponseWrapper.error("Resource not found"));
    }
}
