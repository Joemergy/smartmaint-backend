package com.smartmaint.controller;

import com.smartmaint.api.ApiResponses;
import com.smartmaint.dto.EquipoCreateDTO;
import com.smartmaint.dto.EquipoDTO;
import com.smartmaint.service.EquipoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/equipos")
public class EquipoController {

    private final EquipoService equipoService;

    public EquipoController(EquipoService equipoService) {
        this.equipoService = equipoService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<?> crearEquipo(@RequestBody EquipoCreateDTO dto, Authentication authentication) {
        try {
            String correo = authentication.getName();
            EquipoDTO guardado = equipoService.crearEquipo(dto, correo);
            return ApiResponses.created(guardado);
        } catch (IllegalArgumentException iae) {
            return ApiResponses.badRequest(iae.getMessage());
        } catch (Exception e) {
            return ApiResponses.internalError("Error interno al registrar equipo");
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<?> listarEquipos(Authentication authentication) {
        try {
            String correo = authentication.getName();
            List<EquipoDTO> equipos = equipoService.listarEquipos(correo);
            return ApiResponses.ok(equipos);
        } catch (IllegalArgumentException iae) {
            return ApiResponses.unauthorized(iae.getMessage());
        } catch (Exception e) {
            return ApiResponses.internalError("Error interno al listar equipos");
        }
    }
}
