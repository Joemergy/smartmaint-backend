package com.smartmaint.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartmaint.exception.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

public final class SecurityExceptionHandlers {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private SecurityExceptionHandlers() {
    }

    public static AuthenticationEntryPoint authenticationEntryPoint() {
        return (HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) ->
            writeError(response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Autenticacion requerida", request.getRequestURI());
    }

    public static AccessDeniedHandler accessDeniedHandler() {
        return (HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex) ->
            writeError(response, HttpStatus.FORBIDDEN, "FORBIDDEN", "No tienes permisos para este recurso", request.getRequestURI());
    }

    private static void writeError(HttpServletResponse response, HttpStatus status, String code, String message, String path) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ApiErrorResponse body = new ApiErrorResponse(
            LocalDateTime.now(),
            status.value(),
            status.getReasonPhrase(),
            code,
            message,
            path,
            Map.of()
        );

        OBJECT_MAPPER.writeValue(response.getWriter(), body);
    }
}
