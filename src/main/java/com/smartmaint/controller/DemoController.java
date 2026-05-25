package com.smartmaint.controller;

import com.smartmaint.api.ApiResponses;
import com.smartmaint.dto.UsuarioDemoDTO;
import com.smartmaint.service.DemoService;
import com.smartmaint.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/demo")
public class DemoController {

    @Autowired
    private DemoService demoService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @PostMapping("/solicitar")
    public ResponseEntity<?> solicitarDemo(@RequestBody Map<String, String> datos) {
        String nombre = datos.get("nombre");
        String empresa = datos.get("empresa");
        String correo = datos.get("correo");

        if (nombre == null || nombre.isBlank() || empresa == null || empresa.isBlank() || correo == null || correo.isBlank()) {
            return ApiResponses.badRequest("Nombre, empresa y correo son obligatorios.");
        }

        try {
            Map<String, String> credenciales = demoService.generarDemo(nombre, empresa, correo);

            Map<String, String> respuesta = Map.of(
                "message", "Las credenciales demo fueron enviadas al correo solicitado.",
                "destinatario", credenciales.getOrDefault("destinatario", correo),
                "expiraEn", credenciales.getOrDefault("expiraEn", ""),
                "superAdminCorreo", credenciales.getOrDefault("superAdminCorreo", ""),
                "superAdminContrasena", credenciales.getOrDefault("superAdminContrasena", ""),
                "adminCorreo", credenciales.getOrDefault("adminCorreo", ""),
                "adminContrasena", credenciales.getOrDefault("adminContrasena", ""),
                "usuarioCorreo", credenciales.getOrDefault("usuarioCorreo", ""),
                "usuarioContrasena", credenciales.getOrDefault("usuarioContrasena", "")
            );

            return ApiResponses.ok(respuesta);
        } catch (IllegalArgumentException e) {
            return ApiResponses.badRequest(e.getMessage());
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return ApiResponses.internalError("No se pudo enviar el correo demo. Verifica la configuración SMTP (SPRING_MAIL_HOST, SPRING_MAIL_USERNAME y SPRING_MAIL_PASSWORD).");
        } catch (Exception e) {
            e.printStackTrace(); // Log en consola para diagnóstico
            return ApiResponses.internalError("Error interno al generar demo. Intenta nuevamente.");
        }
    }

    // ✅ Endpoint para listar usuarios demo usando DTO
    @GetMapping("/listar")
    public ResponseEntity<?> listarUsuariosDemo() {
        try {
            List<UsuarioDemoDTO> usuariosDemo = usuarioRepository.findByDemoTrue().stream()
                .map(u -> new UsuarioDemoDTO(
                    u.getNombre(),
                    u.getCorreo(),
                    u.getRol().getNombre(),
                    u.getEmpresa().getNombre()
                ))
                .toList();

            return ApiResponses.ok(usuariosDemo);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponses.internalError("Error al listar usuarios demo");
        }
    }
}
