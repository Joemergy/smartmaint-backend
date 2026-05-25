package com.smartmaint.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException e, HttpServletRequest request) {
        logger.warn("⚠️ Argumento inválido: {}", e.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "Argumento inválido", "BAD_REQUEST", e, request, null);
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<?> handleNullPointer(NullPointerException e, HttpServletRequest request) {
        logger.error("❌ NullPointerException: {}", e.getMessage(), e);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Referencia nula", "NULL_REFERENCE", e, request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        logger.warn("⚠️ Error de validación: {}", ex.getMessage());
        Map<String, String> erroresPorCampo = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        fe -> fe.getField(),
                        fe -> fe.getDefaultMessage(),
                        (existing, replacement) -> existing // si hay duplicados, conservar el primero
                ));

                ApiErrorResponse respuesta = new ApiErrorResponse(
                    LocalDateTime.now(),
                    HttpStatus.BAD_REQUEST.value(),
                    "Validación de entrada",
                    "VALIDATION_ERROR",
                    "Uno o más campos no cumplen las reglas de validación.",
                    request.getRequestURI(),
                    erroresPorCampo
                );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(respuesta);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<?> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        logger.warn("⚠️ Error de restriccion: {}", ex.getMessage());
        Map<String, String> errores = ex.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                        cv -> cv.getPropertyPath().toString(),
                        cv -> cv.getMessage(),
                        (existing, replacement) -> existing
                ));
        return buildResponse(HttpStatus.BAD_REQUEST, "Validación de entrada", "VALIDATION_ERROR", ex, request, errores);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        logger.warn("⚠️ JSON invalido: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "JSON inválido", "MALFORMED_JSON", ex, request, null);
    }

    @ExceptionHandler(Exception.class)
                public ResponseEntity<?> handleException(Exception e, HttpServletRequest request) {
        String tipo = e.getClass().getSimpleName();
        logger.error("💥 Excepción global: {}", tipo, e);

        try {
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                logger.warn("⚠️ Transacción activa detectada; comprobar rollback si aplica.");
            } else {
                logger.info("ℹ️ No hay transacción activa.");
            }
        } catch (Exception ex) {
            logger.info("ℹ️ Error al verificar estado de transacción: {}", ex.getMessage());
        }

        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno en el servidor", "INTERNAL_ERROR", e, request, null);
    }

    private ResponseEntity<?> buildResponse(
        HttpStatus status,
        String error,
        String code,
        Exception e,
        HttpServletRequest request,
        Map<String, ?> details
    ) {
        ApiErrorResponse respuesta = new ApiErrorResponse(
                LocalDateTime.now(),
                status.value(),
                error,
                code,
                e.getMessage(),
                request != null ? request.getRequestURI() : "",
                details
        );
        return ResponseEntity.status(status).body(respuesta);
    }
}
