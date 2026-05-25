package com.smartmaint.service;

import com.smartmaint.dto.EmpresaDTO;
import com.smartmaint.dto.PlanCompraRequestDTO;
import com.smartmaint.model.Empresa;
import com.smartmaint.model.PlanEmpresa;
import com.smartmaint.model.Rol;
import com.smartmaint.model.Usuario;
import com.smartmaint.repository.EmpresaRepository;
import com.smartmaint.repository.RolRepository;
import com.smartmaint.repository.UsuarioRepository;
import com.smartmaint.util.InputSanitizer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class EmpresaService {

    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final PurchaseEmailService purchaseEmailService;

    public EmpresaService(EmpresaRepository empresaRepository,
                          UsuarioRepository usuarioRepository,
                          RolRepository rolRepository,
                          BCryptPasswordEncoder passwordEncoder,
                          PurchaseEmailService purchaseEmailService) {
        this.empresaRepository = empresaRepository;
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.purchaseEmailService = purchaseEmailService;
    }

    /**
     * Registrar empresa y crear primer ADMIN
     */
    public Map<String, Object> registrarEmpresa(EmpresaDTO dto) {
        Empresa empresa = new Empresa();
        empresa.setNombre(dto.getIdEmpresa());
        empresa.setCorreo(dto.getCorreoAdmin().toLowerCase());
        empresa.setSector(dto.getSector());

        // Validar plan permitido
        String planStr = dto.getPlan().toUpperCase();
        PlanEmpresa plan;
        try {
            plan = PlanEmpresa.valueOf(planStr);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Plan inválido. Solo se permite MENSUAL o ANUAL.");
        }
        if (plan == PlanEmpresa.DEMO) {
            throw new IllegalArgumentException("No se puede registrar empresa con plan DEMO desde aquí.");
        }
        empresa.setPlan(plan);
        empresa.setActiva(false);

        // Generar token único
        String token = UUID.randomUUID().toString();
        empresa.setToken(token);

        empresaRepository.saveAndFlush(empresa);

        Rol rolAdmin = rolRepository.findByNombre("ADMIN")
                .orElseThrow(() -> new RuntimeException("Rol ADMIN no encontrado"));

        Usuario admin = new Usuario();
        admin.setNombre("Administrador " + empresa.getNombre());
        admin.setCorreo(dto.getCorreoAdmin().toLowerCase());
        admin.setContrasena(passwordEncoder.encode(dto.getContrasena()));
        admin.setEmpresa(empresa);
        admin.setRol(rolAdmin);
        admin.setActivo(true);
        usuarioRepository.saveAndFlush(admin);

        return Map.of(
                "mensaje", "Empresa y administrador registrados con éxito",
                "idEmpresa", empresa.getId(),
                "empresa", empresa.getNombre(),
                "correoAdmin", admin.getCorreo(),
                "plan", empresa.getPlan().name(),
                "token", token
        );
    }

    /**
     * Activar empresa por token
     */
    public boolean activarEmpresa(String token) {
        Optional<Empresa> empresaOpt = empresaRepository.findByToken(token);
        if (empresaOpt.isPresent()) {
            Empresa empresa = empresaOpt.get();
            empresa.setActiva(true);
            empresaRepository.saveAndFlush(empresa);
            return true;
        }
        return false;
    }

    /**
     * Listar todas las empresas como DTO
     */
    public List<EmpresaDTO> listarEmpresas() {
        return empresaRepository.findAll()
                .stream()
                .map(EmpresaDTO::fromEntity)
                .toList();
    }

    /**
     * Validar si un nombre institucional está disponible
     */
    public boolean nombreDisponible(String idEmpresa) {
        return !empresaRepository.existsByNombre(idEmpresa);
    }

    @Transactional
    public Map<String, Object> registrarCompra(PlanCompraRequestDTO dto) {
        String nombre = InputSanitizer.normalizeText(dto.getNombre(), 50);
        String apellido = InputSanitizer.normalizeText(dto.getApellido(), 50);
        String idColaborador = InputSanitizer.normalizeText(dto.getIdInstitucional(), 50).toUpperCase();
        String tipoDocumento = InputSanitizer.normalizeText(dto.getTipoDocumento(), 50);
        String numeroDocumento = InputSanitizer.normalizeText(dto.getNumeroDocumento(), 60);
        String telefono = InputSanitizer.normalizeText(dto.getTelefonoCelular(), 30);
        String correoPersonal = InputSanitizer.normalizeEmail(dto.getCorreoPersonal());
        String correoInstitucional = InputSanitizer.normalizeEmail(dto.getCorreoInstitucional());

        if (nombre.isBlank() || apellido.isBlank() || idColaborador.isBlank() ||
            tipoDocumento.isBlank() || numeroDocumento.isBlank() || telefono.isBlank() || correoPersonal.isBlank()) {
            throw new IllegalArgumentException("Nombre, apellido, ID institucional, documento, teléfono y correo personal son obligatorios.");
        }

        PlanEmpresa plan = resolverPlan(dto.getPlan());
        if (plan == PlanEmpresa.DEMO) {
            throw new IllegalArgumentException("No se puede comprar plan DEMO.");
        }

        String correoAcceso = correoInstitucional.isBlank() ? correoPersonal : correoInstitucional;
        usuarioRepository.findByCorreo(correoAcceso).ifPresent(u -> {
            throw new IllegalArgumentException("El correo de acceso ya está registrado. Usa otro correo para la compra.");
        });

        usuarioRepository.findByIdColaborador(idColaborador).ifPresent(u -> {
            throw new IllegalArgumentException("El ID institucional ya está en uso. Elige otro.");
        });

        if (empresaRepository.existsByNombre(idColaborador)) {
            throw new IllegalArgumentException("Ya existe una empresa con ese ID institucional.");
        }
        if (empresaRepository.findByCorreo(correoAcceso).isPresent()) {
            throw new IllegalArgumentException("Ya existe una empresa registrada con ese correo.");
        }

        Empresa empresa = new Empresa();
        empresa.setNombre(idColaborador);
        empresa.setCorreo(correoAcceso);
        empresa.setSector("Institucional");
        empresa.setPlan(plan);
        empresa.setActiva(true);
        empresa.setToken(UUID.randomUUID().toString());
        empresa = empresaRepository.saveAndFlush(empresa);

        Rol rolSuperAdmin = rolRepository.findByNombre("SUPERADMIN")
                .orElseThrow(() -> new RuntimeException("Rol SUPERADMIN no encontrado"));

        String nombreCompleto = (nombre + " " + apellido).trim();
        String contrasenaTemporal = generarContrasenaTemporal();

        Usuario superAdmin = new Usuario();
        superAdmin.setNombre(nombreCompleto);
        superAdmin.setCorreo(correoAcceso);
        superAdmin.setContrasena(passwordEncoder.encode(contrasenaTemporal));
        superAdmin.setRol(rolSuperAdmin);
        superAdmin.setEmpresa(empresa);
        superAdmin.setActivo(true);
        superAdmin.setDemo(false);
        superAdmin.setExpiraEn(null);
        superAdmin.setIdColaborador(idColaborador);
        superAdmin.setCargo("Superadministrador");
        superAdmin.setArea("Administración");
        superAdmin.setTelefono(telefono);
        superAdmin.setDireccion((tipoDocumento + ": " + numeroDocumento).trim());
        superAdmin.setDebeCambiarContrasena(true);

        Usuario usuarioGuardado = usuarioRepository.saveAndFlush(superAdmin);

        Set<String> destinatarios = new LinkedHashSet<>();
        if (!correoPersonal.isBlank()) {
            destinatarios.add(correoPersonal);
        }
        if (!correoInstitucional.isBlank()) {
            destinatarios.add(correoInstitucional);
        }

        purchaseEmailService.enviarResumenCompraYCredenciales(
                destinatarios,
                nombreCompleto,
                idColaborador,
                plan,
                correoAcceso,
                contrasenaTemporal
        );

        return Map.of(
                "mensaje", "Compra registrada. Las credenciales fueron enviadas por correo.",
                "empresaId", empresa.getId(),
                "plan", plan.name(),
                "correoAcceso", correoAcceso,
                "destinatarios", destinatarios,
                "idUsuario", usuarioGuardado.getId(),
                "debeCambiarContrasena", true
        );
    }

    private PlanEmpresa resolverPlan(String plan) {
        String planNormalizado = InputSanitizer.normalizeText(plan).toUpperCase();
        try {
            return PlanEmpresa.valueOf(planNormalizado);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Plan inválido. Solo se permite MENSUAL o ANUAL.");
        }
    }

    private String generarContrasenaTemporal() {
        final String upper = "ABCDEFGHJKLMNPQRSTUVWXYZ";
        final String lower = "abcdefghijkmnopqrstuvwxyz";
        final String digits = "23456789";
        final String symbols = "@#$%*!?";
        final String all = upper + lower + digits + symbols;
        final SecureRandom random = new SecureRandom();

        StringBuilder password = new StringBuilder();
        password.append(upper.charAt(random.nextInt(upper.length())));
        password.append(lower.charAt(random.nextInt(lower.length())));
        password.append(digits.charAt(random.nextInt(digits.length())));
        password.append(symbols.charAt(random.nextInt(symbols.length())));

        while (password.length() < 12) {
            password.append(all.charAt(random.nextInt(all.length())));
        }

        return password.toString();
    }
}
