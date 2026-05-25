package com.smartmaint.api;

import com.smartmaint.exception.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.Map;

public final class ApiResponses {

    private ApiResponses() {
    }

    public static <T> ResponseEntity<T> ok(T body) {
        return ResponseEntity.ok(body);
    }

    public static <T> ResponseEntity<T> created(T body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    public static ResponseEntity<Map<String, String>> message(HttpStatus status, String key, String message) {
        return ResponseEntity.status(status).body(Map.of(key, message));
    }

    public static ResponseEntity<ApiErrorResponse> badRequest(String message) {
        return error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message, null);
    }

    public static ResponseEntity<ApiErrorResponse> unauthorized(String message) {
        return error(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message, null);
    }

    public static ResponseEntity<ApiErrorResponse> forbidden(String message) {
        return error(HttpStatus.FORBIDDEN, "FORBIDDEN", message, null);
    }

    public static ResponseEntity<ApiErrorResponse> notFound(String message) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", message, null);
    }

    public static ResponseEntity<ApiErrorResponse> internalError(String message) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", message, null);
    }

    public static ResponseEntity<ApiErrorResponse> error(
            HttpStatus status,
            String code,
            String message,
            Map<String, ?> details
    ) {
        ApiErrorResponse body = new ApiErrorResponse(
                LocalDateTime.now(),
                status.value(),
            status.getReasonPhrase(),
                code,
                message,
                currentPath(),
                details
        );
        return ResponseEntity.status(status).body(body);
    }

    private static String currentPath() {
        try {
            return ServletUriComponentsBuilder.fromCurrentRequestUri().build().getPath();
        } catch (Exception ignored) {
            return "";
        }
    }
}