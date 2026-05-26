package com.jva.ERP.exception;

import com.jva.ERP.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * GlobalExceptionHandler — centralised error handling for all REST controllers.
 *
 * Handles:
 *   - Bean validation errors (@Valid) → 400 with per-field error map
 *   - ResourceNotFoundException       → 404
 *   - BusinessException               → 400
 *   - AccessDeniedException           → 403
 *   - Authentication failures         → 401
 *   - Missing / malformed request     → 400
 *   - Method not allowed              → 405
 *   - No handler found                → 404
 *   - All other exceptions            → 500
 *
 * All responses use the standard {@link ApiResponse} envelope:
 *   { "code": int, "message": string, "data": object|null, "timestamp": long }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ── Validation errors ─────────────────────────────────────────────────────

    /**
     * Handles @Valid / @Validated bean validation failures.
     * Returns a 400 with a map of { fieldName: "error message" } pairs.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                        // keep first error per field if there are multiple
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));

        logger.debug("Validation failed: {}", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(HttpStatus.BAD_REQUEST.value(),
                        "Validation failed", fieldErrors));
    }

    // ── Domain exceptions ─────────────────────────────────────────────────────

    /**
     * Handles ResourceNotFoundException → 404 Not Found.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleResourceNotFound(
            ResourceNotFoundException ex, WebRequest request) {
        logger.debug("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(HttpStatus.NOT_FOUND.value(),
                        "Resource not found", ex.getMessage()));
    }

    /**
     * Handles BusinessException → 400 Bad Request.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<?>> handleBusinessException(
            BusinessException ex, WebRequest request) {
        logger.debug("Business rule violation: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(HttpStatus.BAD_REQUEST.value(),
                        "Business logic error", ex.getMessage()));
    }

    // ── Security exceptions ───────────────────────────────────────────────────

    /**
     * Handles Spring Security access denied → 403 Forbidden.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse<>(HttpStatus.FORBIDDEN.value(),
                        "Access denied",
                        "You do not have permission to perform this action"));
    }

    /**
     * Handles bad credentials → 401 Unauthorized.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<?>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse<>(HttpStatus.UNAUTHORIZED.value(),
                        "Authentication failed", "Invalid username or password"));
    }

    /**
     * Handles disabled account → 401 Unauthorized.
     */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<?>> handleDisabled(DisabledException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse<>(HttpStatus.UNAUTHORIZED.value(),
                        "Authentication failed", "Account is disabled"));
    }

    /**
     * Handles locked account → 401 Unauthorized.
     */
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiResponse<?>> handleLocked(LockedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse<>(HttpStatus.UNAUTHORIZED.value(),
                        "Authentication failed", "Account is locked"));
    }

    // ── HTTP / request errors ─────────────────────────────────────────────────

    /**
     * Handles malformed JSON body → 400 Bad Request.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<?>> handleUnreadableMessage(
            HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(HttpStatus.BAD_REQUEST.value(),
                        "Malformed request body",
                        "The request body could not be parsed. Please check your JSON syntax."));
    }

    /**
     * Handles missing required request parameters → 400 Bad Request.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<?>> handleMissingParam(
            MissingServletRequestParameterException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(HttpStatus.BAD_REQUEST.value(),
                        "Missing required parameter",
                        "Required parameter '" + ex.getParameterName() + "' is missing"));
    }

    /**
     * Handles path variable / request parameter type mismatch → 400 Bad Request.
     * E.g. passing "abc" where a Long is expected.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<?>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {
        String detail = String.format(
                "Parameter '%s' should be of type %s",
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(HttpStatus.BAD_REQUEST.value(),
                        "Invalid parameter type", detail));
    }

    /**
     * Handles unsupported HTTP method → 405 Method Not Allowed.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<?>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new ApiResponse<>(HttpStatus.METHOD_NOT_ALLOWED.value(),
                        "HTTP method not allowed",
                        ex.getMethod() + " is not supported for this endpoint"));
    }

    /**
     * Handles no matching handler (404) when spring.mvc.throw-exception-if-no-handler-found=true.
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleNoHandlerFound(NoHandlerFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(HttpStatus.NOT_FOUND.value(),
                        "Endpoint not found",
                        ex.getRequestURL() + " does not exist"));
    }

    // ── Catch-all ─────────────────────────────────────────────────────────────

    /**
     * Catch-all handler for any unhandled exception → 500 Internal Server Error.
     * The exception message is logged but NOT exposed to the client for security.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGenericException(
            Exception ex, WebRequest request) {
        logger.error("Unhandled exception at {}: {}",
                request.getDescription(false), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "An unexpected error occurred",
                        "Please contact support if the problem persists"));
    }
}
