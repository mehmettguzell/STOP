package com.stop.notification_service.common.exception;

import com.stop.notification_service.common.error.CommonErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.util.Objects;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ProblemDetail> handleDataAccess(DataAccessException ex, HttpServletRequest req) {
        log.error("Database error: {}", ex.getMessage());

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "Veritabani su an kullanilamıyor. Lutfen kisa sure sonra tekrar deneyin.");
        pd.setProperty("code", CommonErrorCode.SERVICE_UNAVAILABLE.getCode());
        pd.setInstance(URI.create(req.getRequestURI()));
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(pd);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("Validation error");

        log.info("Validation error: {}", message);

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message);
        pd.setProperty("code", CommonErrorCode.INVALID_REQUEST.getCode());
        pd.setInstance(URI.create(req.getRequestURI()));
        return ResponseEntity.badRequest().body(pd);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest req) {
        String message = ex.getConstraintViolations().iterator().next().getMessage();
        log.info("Constraint violation: {}", message);

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message);
        pd.setProperty("code", CommonErrorCode.INVALID_REQUEST.getCode());
        pd.setInstance(URI.create(req.getRequestURI()));
        return ResponseEntity.badRequest().body(pd);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        log.warn("Malformed JSON request", ex);

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Gecersiz veri formati.");
        pd.setProperty("code", CommonErrorCode.INVALID_REQUEST.getCode());
        pd.setInstance(URI.create(req.getRequestURI()));
        return ResponseEntity.badRequest().body(pd);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.METHOD_NOT_ALLOWED, "HTTP metodu desteklenmiyor.");
        pd.setProperty("code", CommonErrorCode.INVALID_REQUEST.getCode());
        pd.setInstance(URI.create(req.getRequestURI()));
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(pd);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                String.format("'%s' parametresi icin gecersiz deger.", ex.getName()));
        pd.setProperty("code", CommonErrorCode.INVALID_REQUEST.getCode());
        pd.setInstance(URI.create(req.getRequestURI()));
        return ResponseEntity.badRequest().body(pd);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoResource(NoResourceFoundException ex, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, CommonErrorCode.RESOURCE_NOT_FOUND.getMessage());
        pd.setProperty("code", CommonErrorCode.RESOURCE_NOT_FOUND.getCode());
        pd.setInstance(URI.create(req.getRequestURI()));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        log.warn("Access denied: {}", ex.getMessage());

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, CommonErrorCode.FORBIDDEN.getMessage());
        pd.setProperty("code", CommonErrorCode.FORBIDDEN.getCode());
        pd.setInstance(URI.create(req.getRequestURI()));
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(pd);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthentication(AuthenticationException ex, HttpServletRequest req) {
        log.warn("Authentication failed: {}", ex.getMessage());

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, CommonErrorCode.UNAUTHORIZED.getMessage());
        pd.setProperty("code", CommonErrorCode.UNAUTHORIZED.getCode());
        pd.setInstance(URI.create(req.getRequestURI()));
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(pd);
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ProblemDetail> handleJwt(JwtException ex, HttpServletRequest req) {
        log.warn("JWT exception: {}", ex.getMessage());

        String detail = ex.getMessage() != null && ex.getMessage().toLowerCase().contains("expired")
                ? "Oturum suresi doldu, lutfen tekrar giris yapin."
                : "Gecersiz oturum, lutfen tekrar giris yapin.";

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, detail);
        pd.setProperty("code", CommonErrorCode.UNAUTHORIZED.getCode());
        pd.setInstance(URI.create(req.getRequestURI()));
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(pd);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneral(Exception ex, HttpServletRequest req) {
        log.error("Unexpected system error", ex);

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, CommonErrorCode.INTERNAL_ERROR.getMessage());
        pd.setProperty("code", CommonErrorCode.INTERNAL_ERROR.getCode());
        pd.setInstance(URI.create(req.getRequestURI()));
        return ResponseEntity.internalServerError().body(pd);
    }
}
