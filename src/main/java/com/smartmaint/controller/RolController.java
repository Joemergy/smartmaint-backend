package com.smartmaint.controller;

import com.smartmaint.model.Rol;
import com.smartmaint.service.RolService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/roles")
public class RolController {

    private final RolService rolService;

    public RolController(RolService rolService) {
        this.rolService = rolService;
    }

    /**
     * GET /api/roles
     * Listar todos los roles disponibles
     */
    @GetMapping
    public ResponseEntity<List<Rol>> listarRoles() {
        List<Rol> roles = rolService.listarTodos();
        return ResponseEntity.ok(roles);
    }

    /**
     * GET /api/roles/{nombre}
     * Obtener rol por nombre
     */
    @GetMapping("/{nombre}")
    public ResponseEntity<?> obtenerRolPorNombre(@PathVariable String nombre) {
        try {
            Rol rol = rolService.obtenerPorNombre(nombre);
            return ResponseEntity.ok(rol);
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.status(404).body(Map.of("error", iae.getMessage()));
        }
    }

    /**
     * POST /api/roles
     * Crear un nuevo rol
     */
    @PostMapping
    public ResponseEntity<?> crearRol(@RequestBody Rol rol) {
        try {
            Rol creado = rolService.crearRol(rol);
            return ResponseEntity.status(201).body(creado);
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.badRequest().body(Map.of("error", iae.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error interno al crear rol"));
        }
    }

    /**
     * DELETE /api/roles/{id}
     * Eliminar rol por ID
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarRol(@PathVariable Long id) {
        try {
            rolService.eliminarRol(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error interno al eliminar rol"));
        }
    }
}
