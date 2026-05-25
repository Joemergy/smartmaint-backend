package com.smartmaint.service;

import com.smartmaint.model.Empresa;
import com.smartmaint.model.PlanEmpresa;
import com.smartmaint.model.Rol;
import com.smartmaint.model.Usuario;
import com.smartmaint.model.DemoRegistro;
import com.smartmaint.repository.EmpresaRepository;
import com.smartmaint.repository.RolRepository;
import com.smartmaint.repository.UsuarioRepository;
import com.smartmaint.repository.DemoRegistroRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class DemoService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private DemoRegistroRepository demoRegistroRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private DemoEmailService demoEmailService;

    @Value("${app.demo.request.cooldown.hours:0}")
    private long demoRequestCooldownHours;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    /**
     * Genera una empresa y usuarios demo por 1 semana.
     */
    @Transactional
    public Map<String, String> generarDemo(String nombre, String empresaNombre, String correoSolicitante) {
        String nombreNormalizado = nombre == null ? "" : nombre.trim();
        String empresaNormalizada = empresaNombre == null ? "" : empresaNombre.trim();
        String correoDestino = correoSolicitante == null ? "" : correoSolicitante.trim().toLowerCase();

        if (nombreNormalizado.isBlank() || empresaNormalizada.isBlank() || correoDestino.isBlank()) {
            throw new IllegalArgumentException("Nombre, empresa y correo son obligatorios.");
        }
        if (!EMAIL_PATTERN.matcher(correoDestino).matches()) {
            throw new IllegalArgumentException("El correo de solicitud debe tener un formato válido.");
        }

        String ip = obtenerIpActual();
        LocalDateTime ahora = LocalDateTime.now();

        // 1) Limitar solicitudes por IP solo si el cooldown está configurado (>0)
        if (demoRequestCooldownHours > 0) {
            boolean ipRepetida = demoRegistroRepository
                    .existsByIpSolicitanteAndFechaSolicitudAfter(ip, ahora.minusHours(demoRequestCooldownHours));
            if (ipRepetida) {
                throw new IllegalArgumentException(
                        "Ya se solicitó una demo recientemente desde esta IP. Intenta más tarde."
                );
            }
        }

        // 2) Evitar duplicados por nombre de empresa (sin importar mayusculas/minusculas)
        empresaRepository.findByNombreIgnoreCase(empresaNormalizada).ifPresent(empresaExistente -> {
            if (empresaExistente.getPlan() == PlanEmpresa.DEMO) {
                throw new IllegalArgumentException("Ya existe una empresa demo con ese nombre.");
            }
            throw new IllegalArgumentException("Ya existe una empresa registrada con ese nombre. Usa otro nombre para la demo.");
        });

        String correoEmpresaDemo = "demo@" + empresaNormalizada.toLowerCase().replaceAll("\\s+", "") + ".com";
        if (empresaRepository.findByCorreo(correoEmpresaDemo).isPresent()) {
            throw new IllegalArgumentException("Ya existe una demo registrada para ese nombre de empresa.");
        }

        LocalDateTime expiracion = ahora.plusWeeks(1);

        // 3) Crear empresa demo
        Empresa empresa = new Empresa();
        empresa.setNombre(empresaNormalizada);
        empresa.setCorreo(correoEmpresaDemo);
        empresa.setSector("Demo");
        empresa.setPlan(PlanEmpresa.DEMO);
        empresa.setActiva(false); // la demo puede estar inactiva hasta activación si así lo decides
        empresa = empresaRepository.save(empresa);

            // 4) Roles necesarios
            Rol rolSuperAdmin = rolRepository.findByNombre("SUPERADMIN")
                .orElseThrow(() -> new RuntimeException("Rol SUPERADMIN no encontrado"));
            Rol rolAdmin = rolRepository.findByNombre("ADMIN")
                .orElseThrow(() -> new RuntimeException("Rol ADMIN no encontrado"));
            Rol rolUsuario = rolRepository.findByNombre("USUARIO")
                .orElseThrow(() -> new RuntimeException("Rol USUARIO no encontrado"));

        // 5) Crear credenciales demo únicas
        String correoSuperAdmin = "demo_superadmin_" + System.currentTimeMillis() + "@smartmaint.com";
        String correoAdmin = "demo_admin_" + System.currentTimeMillis() + "@smartmaint.com";
        String correoUsuario = "demo_usuario_" + System.currentTimeMillis() + "@smartmaint.com";

        Usuario superAdmin = new Usuario();
        superAdmin.setNombre(nombreNormalizado + " (SuperAdmin Demo)");
        superAdmin.setCorreo(correoSuperAdmin);
        superAdmin.setContrasena(passwordEncoder.encode("demo1234"));
        superAdmin.setRol(rolSuperAdmin);
        superAdmin.setEmpresa(empresa);
        superAdmin.setDemo(true);
        superAdmin.setExpiraEn(expiracion);
        superAdmin.setIdColaborador("DEMO-SUPERADMIN-" + System.currentTimeMillis());
        superAdmin.setCargo("Owner");
        superAdmin.setArea("Dirección");
        superAdmin.setTelefono("0000000000");
        superAdmin.setDebeCambiarContrasena(false);

        Usuario admin = new Usuario();
        admin.setNombre(nombreNormalizado + " (Admin Demo)");
        admin.setCorreo(correoAdmin);
        admin.setContrasena(passwordEncoder.encode("demo1234"));
        admin.setRol(rolAdmin);
        admin.setEmpresa(empresa);
        admin.setDemo(true);
        admin.setExpiraEn(expiracion);
        admin.setIdColaborador("DEMO-ADMIN-" + System.currentTimeMillis());
        admin.setCargo("Administrador");
        admin.setArea("IT");
        admin.setTelefono("0000000000");
        admin.setDebeCambiarContrasena(false);

        Usuario usuario = new Usuario();
        usuario.setNombre(nombreNormalizado + " (Usuario Demo)");
        usuario.setCorreo(correoUsuario);
        usuario.setContrasena(passwordEncoder.encode("demo1234"));
        usuario.setRol(rolUsuario);
        usuario.setEmpresa(empresa);
        usuario.setDemo(true);
        usuario.setExpiraEn(expiracion);
        usuario.setIdColaborador("DEMO-USER-" + System.currentTimeMillis());
        usuario.setCargo("Técnico");
        usuario.setArea("Operaciones");
        usuario.setTelefono("0000000000");
        usuario.setDebeCambiarContrasena(false);

        usuarioRepository.save(superAdmin);
        usuarioRepository.save(admin);
        usuarioRepository.save(usuario);

        // 6) Registrar auditoría de solicitud demo
        DemoRegistro registro = new DemoRegistro(nombre, empresaNombre, ip, ahora);
        demoRegistroRepository.save(registro);

            demoEmailService.enviarCredencialesDemo(
                correoDestino,
                nombreNormalizado,
                empresaNormalizada,
                correoSuperAdmin,
                "demo1234",
                correoAdmin,
                "demo1234",
                correoUsuario,
                "demo1234",
                expiracion
            );

        // 7) Respuesta
        return Map.of(
                "destinatario", correoDestino,
            "expiraEn", expiracion.toString(),
            "superAdminCorreo", correoSuperAdmin,
            "superAdminContrasena", "demo1234",
            "adminCorreo", correoAdmin,
            "adminContrasena", "demo1234",
            "usuarioCorreo", correoUsuario,
            "usuarioContrasena", "demo1234"
        );
    }

    private String obtenerIpActual() {
        String ip = request.getHeader("X-Forwarded-For");
        return (ip != null && !ip.isBlank()) ? ip.split(",")[0].trim() : request.getRemoteAddr();
    }
}
