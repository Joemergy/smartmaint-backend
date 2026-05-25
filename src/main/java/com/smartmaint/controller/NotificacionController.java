package com.smartmaint.controller;

import com.smartmaint.api.ApiResponses;
import com.smartmaint.dto.NotificacionDTO;
import com.smartmaint.service.NotificacionService;
import com.smartmaint.util.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final NotificacionService notificacionService;
    private final JwtUtil jwtUtil;

    public NotificacionController(NotificacionService notificacionService, JwtUtil jwtUtil) {
        this.notificacionService = notificacionService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/mias")
    public ResponseEntity<?> listarMisNotificaciones(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        try {
            if (authorizationHeader == null || authorizationHeader.isBlank()) {
                return ApiResponses.unauthorized("Authorization header ausente");
            }
            String token = authorizationHeader.startsWith("Bearer ") ? authorizationHeader.substring(7) : authorizationHeader;
            String correo = jwtUtil.extraerCorreoDesdeToken(token);
            if (correo == null || correo.isBlank()) {
                return ApiResponses.unauthorized("Token inválido o sin correo");
            }

            List<NotificacionDTO> notificaciones = notificacionService.listarPorCorreoUsuario(correo);
            long pendientes = notificacionService.contarNoLeidasPorCorreoUsuario(correo);

            return ApiResponses.ok(Map.of(
                    "items", notificaciones,
                    "pendientes", pendientes
            ));
        } catch (Exception e) {
            return ApiResponses.internalError(e.getMessage());
        }
    }

    @PutMapping("/{id}/leer")
    public ResponseEntity<?> marcarComoLeida(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        try {
            if (authorizationHeader == null || authorizationHeader.isBlank()) {
                return ApiResponses.unauthorized("Authorization header ausente");
            }
            String token = authorizationHeader.startsWith("Bearer ") ? authorizationHeader.substring(7) : authorizationHeader;
            String correo = jwtUtil.extraerCorreoDesdeToken(token);
            if (correo == null || correo.isBlank()) {
                return ApiResponses.unauthorized("Token inválido o sin correo");
            }

            NotificacionDTO dto = notificacionService.marcarComoLeida(id, correo);
            return ApiResponses.ok(dto);
        } catch (RuntimeException e) {
            return ApiResponses.badRequest(e.getMessage());
        } catch (Exception e) {
            return ApiResponses.internalError(e.getMessage());
        }
    }
}
