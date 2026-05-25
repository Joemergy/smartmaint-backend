package com.smartmaint.service;

import com.smartmaint.dto.UsuarioCreateDTO;
import com.smartmaint.dto.UsuarioDTO;
import com.smartmaint.dto.UsuarioUpdateDTO;
import com.smartmaint.model.Empresa;
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
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class UsuarioService {

    private static final Pattern PASSWORD_RULE = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$");

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final EmpresaRepository empresaRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          RolRepository rolRepository,
                          EmpresaRepository empresaRepository,
                          BCryptPasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.empresaRepository = empresaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UsuarioDTO crearEnEmpresaDelAdmin(UsuarioCreateDTO dto, String correoAdmin) {
        Usuario admin = usuarioRepository.findByCorreo(correoAdmin.trim().toLowerCase())
                .orElseThrow(() -> new RuntimeException("Admin no encontrado"));

        if (admin.getEmpresa() == null)
            throw new RuntimeException("Admin sin empresa asignada");

        String correoNormalizado = InputSanitizer.normalizeEmail(dto.getCorreo());
        usuarioRepository.findByCorreo(correoNormalizado)
                .ifPresent(u -> { throw new RuntimeException("Correo ya en uso"); });

        String idColaboradorNormalizado = InputSanitizer.normalizeText(dto.getIdColaborador(), 50);
        usuarioRepository.findByIdColaborador(idColaboradorNormalizado)
            .ifPresent(u -> { throw new RuntimeException("ID de colaborador ya en uso"); });

        Rol rolSolicitado = rolRepository.findById(dto.getRolId())
            .orElseThrow(() -> new RuntimeException("Rol no encontrado"));

        String rolSolicitante = admin.getRol() != null && admin.getRol().getNombre() != null
            ? admin.getRol().getNombre().trim().toUpperCase()
            : "";
        String nombreRolSolicitado = rolSolicitado.getNombre() != null
            ? rolSolicitado.getNombre().trim().toUpperCase()
            : "";

        if ("ADMIN".equals(rolSolicitante) && !"USUARIO".equals(nombreRolSolicitado)) {
            throw new RuntimeException("Un ADMIN solo puede crear usuarios estándar");
        }
        if ("SUPERADMIN".equals(rolSolicitante)
            && !("USUARIO".equals(nombreRolSolicitado) || "ADMIN".equals(nombreRolSolicitado))) {
            throw new RuntimeException("Un SUPERADMIN solo puede crear ADMIN o USUARIO");
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(InputSanitizer.normalizeText(dto.getNombre(), 50));
        usuario.setCorreo(correoNormalizado);
        usuario.setContrasena(passwordEncoder.encode(dto.getContrasena())); // ✅ encriptado
        usuario.setRol(rolSolicitado);
        usuario.setEmpresa(admin.getEmpresa());
        usuario.setIdColaborador(idColaboradorNormalizado);
        usuario.setActivo(dto.getActivo());
        usuario.setCargo(InputSanitizer.normalizeText(dto.getCargo(), 100));
        usuario.setArea(InputSanitizer.normalizeText(dto.getArea(), 100));
        usuario.setTelefono(InputSanitizer.normalizeText(dto.getTelefono(), 30));
        usuario.setFechaIngreso(dto.getFechaIngreso());
        usuario.setDireccion(dto.getDireccion() != null ? InputSanitizer.normalizeText(dto.getDireccion(), 255) : null);
        usuario.setFotoPerfil(dto.getFotoPerfil());
        usuario.setDebeCambiarContrasena(true);

        return UsuarioDTO.fromEntity(usuarioRepository.save(usuario));
    }

    @Transactional
    public void cambiarContrasenaInicial(String correo, String contrasenaActual, String nuevaContrasena) {
        Usuario usuario = usuarioRepository.findByCorreo(InputSanitizer.normalizeEmail(correo))
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (contrasenaActual == null || nuevaContrasena == null ||
                contrasenaActual.isBlank() || nuevaContrasena.isBlank()) {
            throw new RuntimeException("Debes completar la contraseña actual y la nueva contraseña");
        }

        if (!passwordEncoder.matches(contrasenaActual, usuario.getContrasena())) {
            throw new RuntimeException("La contraseña actual es incorrecta");
        }

        if (contrasenaActual.equals(nuevaContrasena)) {
            throw new RuntimeException("La nueva contraseña debe ser diferente a la actual");
        }

        if (!PASSWORD_RULE.matcher(nuevaContrasena).matches()) {
            throw new RuntimeException("La nueva contraseña debe tener mínimo 8 caracteres e incluir letras, números y un carácter especial");
        }

        usuario.setContrasena(passwordEncoder.encode(nuevaContrasena));
        usuario.setDebeCambiarContrasena(false);
        usuarioRepository.save(usuario);
    }

    @Transactional
    public UsuarioDTO actualizar(Long id, UsuarioUpdateDTO dto) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (dto.getNombre() != null) usuario.setNombre(dto.getNombre());

        if (dto.getCorreo() != null) {
            String correoNormalizado = InputSanitizer.normalizeEmail(dto.getCorreo());
            usuarioRepository.findByCorreo(correoNormalizado)
                    .filter(u -> !u.getId().equals(id))
                    .ifPresent(u -> { throw new RuntimeException("Correo ya en uso"); });
            usuario.setCorreo(correoNormalizado);
        }

        if (dto.getContrasena() != null)
            usuario.setContrasena(passwordEncoder.encode(dto.getContrasena())); // ✅ encriptado

        if (dto.getRolId() != null)
            usuario.setRol(rolRepository.findById(dto.getRolId())
                    .orElseThrow(() -> new RuntimeException("Rol no encontrado")));

        if (dto.getEmpresaId() != null)
            usuario.setEmpresa(empresaRepository.findById(dto.getEmpresaId())
                    .orElseThrow(() -> new RuntimeException("Empresa no encontrada")));

        return UsuarioDTO.fromEntity(usuarioRepository.save(usuario));
    }

    public List<UsuarioDTO> listarDeEmpresaDelAdmin(String correoAdmin) {
        Usuario admin = usuarioRepository.findByCorreo(InputSanitizer.normalizeEmail(correoAdmin))
                .orElseThrow(() -> new RuntimeException("Admin no encontrado"));
        if (admin.getEmpresa() == null)
            throw new RuntimeException("Admin sin empresa asignada");

        // Solo listar colaboradores (rol USUARIO) en orden de creación.
        return usuarioRepository.findByEmpresaIdAndRolNombreIgnoreCaseOrderByCreatedAtAsc(
                admin.getEmpresa().getId(),
                "USUARIO"
            )
                .stream().map(UsuarioDTO::fromEntity).collect(Collectors.toList());
    }

    @Transactional
    public void eliminar(Long id) { usuarioRepository.deleteById(id); }

    @Transactional
    public void eliminarVarios(List<Long> ids) { usuarioRepository.deleteAllById(ids); }

    /**
     * Guarda una contraseña temporal en la BD dentro de esta transacción
     * y devuelve [temporal, nombre] para que el llamador envíe el correo
     * DESPUÉS de que la transacción confirme.
     */
    @Transactional
    public String[] recuperarContrasena(String correo) {
        String correoNorm = InputSanitizer.normalizeEmail(correo);
        Usuario usuario = usuarioRepository.findByCorreo(correoNorm)
                .orElseThrow(() -> new RuntimeException("No existe una cuenta con ese correo"));

        String temporal = generarContrasenaTemporal();
        usuario.setContrasena(passwordEncoder.encode(temporal));
        usuario.setDebeCambiarContrasena(true);
        usuarioRepository.save(usuario);
        return new String[]{ temporal, usuario.getNombre() };
    }

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789!@#$";
    private static final SecureRandom RANDOM = new SecureRandom();

    private String generarContrasenaTemporal() {
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        return sb.toString();
    }
}