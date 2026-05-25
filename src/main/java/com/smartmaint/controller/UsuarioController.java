package com.smartmaint.controller;

import com.smartmaint.dto.UsuarioCreateDTO;
import com.smartmaint.dto.UsuarioDTO;
import com.smartmaint.dto.UsuarioUpdateDTO;
import com.smartmaint.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // POST /api/usuarios → Crear usuario dentro de la empresa del ADMIN autenticado
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<UsuarioDTO> crearUsuario(@RequestBody UsuarioCreateDTO dto, Principal principal) {
        String correoAdmin = principal.getName();
        return ResponseEntity.ok(usuarioService.crearEnEmpresaDelAdmin(dto, correoAdmin));
    }

    // PUT /api/usuarios/{id} → Actualizar usuario (parcial)
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<UsuarioDTO> actualizarUsuario(@PathVariable Long id, @RequestBody UsuarioUpdateDTO dto) {
        return ResponseEntity.ok(usuarioService.actualizar(id, dto));
    }

    // GET /api/usuarios → Listar todos los usuarios de la empresa del ADMIN
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<List<UsuarioDTO>> listarUsuarios(Principal principal) {
        String correoAdmin = principal.getName();
        return ResponseEntity.ok(usuarioService.listarDeEmpresaDelAdmin(correoAdmin));
    }

    // DELETE /api/usuarios/{id} → Eliminar usuario por ID (de la empresa del ADMIN)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<Void> eliminarUsuario(@PathVariable Long id) {
        usuarioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // DELETE /api/usuarios → Eliminar varios usuarios a la vez
    @DeleteMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<Void> eliminarUsuarios(@RequestBody List<Long> ids) {
        usuarioService.eliminarVarios(ids);
        return ResponseEntity.noContent().build();
    }
}
