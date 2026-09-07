package FinanceManangementSystem.demo.Exceptions;

import FinanceManangementSystem.demo.Payloads.ResponseDTO.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Validation Exceptions
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();

        for(FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        ErrorResponse resp = new ErrorResponse(
                "VALIDATION_ERROR",
                "Validation failed",
                errors
        );

        return new ResponseEntity<>(resp, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParams(org.springframework.web.bind.MissingServletRequestParameterException ex) {
        ErrorResponse resp = new ErrorResponse(
                "MISSING_PARAMETER",
                "Required parameter '" + ex.getParameterName() + "' is missing."
        );
        return new ResponseEntity<>(resp, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(org.springframework.web.servlet.resource.NoResourceFoundException ex) {
        ErrorResponse resp = new ErrorResponse(
                "RESOURCE_NOT_FOUND",
                "The requested resource was not found: " + ex.getResourcePath()
        );
        return new ResponseEntity<>(resp, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(org.springframework.web.HttpRequestMethodNotSupportedException ex) {
        log.warn("Method not supported: {}", ex.getMessage());
        ErrorResponse resp = new ErrorResponse(
                "METHOD_NOT_ALLOWED",
                ex.getMessage()
        );
        return new ResponseEntity<>(resp, HttpStatus.METHOD_NOT_ALLOWED);
    }

    /**
     * Handles all typed application exceptions (ResourceNotFoundException, DuplicateResourceException, etc.)
     * Each exception carries its own HTTP status code, error code, and message.
     * This handler is more specific than the catch-all Exception.class handler below.
     */
    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ErrorResponse> handleApplicationException(ApplicationException ex) {
        ErrorResponse resp = new ErrorResponse(
                ex.getErrorCode(),
                ex.getMessage()
        );

        return ResponseEntity
                .status(ex.getHttpStatus())
                .body(resp);
    }

    /**
     * Handles InvalidRefreshToken exceptions.
     * Kept separate from ApplicationException handler for backward compatibility.
     */
    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRefreshToken(InvalidRefreshTokenException ex){
        ErrorResponse resp = new ErrorResponse(
                "INVALID_REFRESH_TOKEN",
                ex.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(resp);
    }

    @ExceptionHandler(MalformedJwtException.class)
    public ResponseEntity<ErrorResponse> handleMalformedJwt(MalformedJwtException ex) {
        ErrorResponse resp = new ErrorResponse(
                "INVALID_TOKEN",
                "Token is invalid or malformed"
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(resp);
    }

    @ExceptionHandler(SignatureException.class)
    public ResponseEntity<ErrorResponse> handleSignatureException(SignatureException ex) {
        ErrorResponse resp = new ErrorResponse(
                "INVALID_TOKEN_SIGNATURE",
                "Token signature is invalid"
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(resp);
    }

    /**
     * Handles RefreshTokenExpired exceptions.
     * Kept separate from ApplicationException handler for backward compatibility.
     */
    @ExceptionHandler(RefreshTokenExpiredException.class)
    public ResponseEntity<ErrorResponse> handleRefreshTokenExpired(RefreshTokenExpiredException ex) {

        ErrorResponse resp = new ErrorResponse(
                "REFRESH_TOKEN_EXPIRED",
                ex.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(resp);
    }

    /**
     * Handles all Spring Security authentication failures (bad credentials, disabled/locked
     * accounts, etc.) that occur during login. This is an expected, routine failure case —
     * not a bug — so it must NOT be logged as ERROR or leak to the generic exception handler.
     *
     * Covers: BadCredentialsException, DisabledException, LockedException,
     * AccountExpiredException, and any other AuthenticationException subclass.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
        // WARN — not ERROR: wrong password is routine user error, not an application defect
        log.warn("Authentication failed: {}", ex.getMessage());

        ErrorResponse resp = new ErrorResponse(
                "INVALID_CREDENTIALS",
                "Incorrect email or password. Please check your details and try again."
        );

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(resp);
    }

    /**
     * Handles a malformed path variable — e.g. a non-UUID string where a UUID publicId is
     * expected (GET /customer/abc123). This is a client input error, not a server bug.
     */
    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex) {
        log.warn("Type mismatch on parameter '{}': {}", ex.getName(), ex.getMessage());

        ErrorResponse resp = new ErrorResponse(
                "INVALID_PARAMETER",
                "Invalid value for parameter '" + ex.getName() + "'"
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp);
    }

    /**
     * Handles malformed JSON request bodies, including an enum value that doesn't match any
     * constant (e.g. "unit": "LB" when only G/KG/TON are valid). This is a client input error.
     */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(org.springframework.http.converter.HttpMessageNotReadableException ex) {
        log.warn("Malformed request body: {}", ex.getMessage());

        ErrorResponse resp = new ErrorResponse(
                "MALFORMED_REQUEST",
                "Request body is malformed or contains an invalid value"
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp);
    }

    /**
     * Handles DB-level unique-constraint violations (e.g. duplicate document number).
     * These should not be exposed to the client as 500s — surface them as 409 Conflict.
     */
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(org.springframework.dao.DataIntegrityViolationException ex) {
        log.error("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());

        ErrorResponse resp = new ErrorResponse(
                "CONFLICT",
                "Request could not be completed due to a data conflict"
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(resp);
    }

    /**
     * Handles all unexpected/unhandled exceptions that are not caught by more specific handlers.
     * Spring exception handler matching is by most-specific-first, so this handler will only
     * catch truly unexpected errors (NPEs, bugs, etc.) that are NOT instances of ApplicationException,
     * MethodArgumentNotValidException, InvalidRefreshTokenException, RefreshTokenExpiredException,
     * or AuthenticationException.
     *
     * NOTE: In production, unexpected errors should be logged with full stack trace at ERROR level
     * but the client response should NOT leak internal implementation details.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        // Log the unexpected exception with full stack trace for debugging
        log.error("Unhandled exception encountered", ex);

        ErrorResponse resp = new ErrorResponse(
                "INTERNAL_ERROR",
                "An unexpected error occurred"
        );

        return new ResponseEntity<>(
                resp,
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}
