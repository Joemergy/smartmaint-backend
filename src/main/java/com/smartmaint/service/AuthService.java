package com.smartmaint.service;

import com.smartmaint.model.Usuario;
import com.smartmaint.repository.UsuarioRepository;
import com.smartmaint.util.InputSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private static final Pattern BCRYPT_PATTERN =
            Pattern.compile("^\\$2[aby]?\\$\\d{2}\\$[./A-Za-z0-9]{53}$");

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    // Validar credenciales de acceso
    public Usuario validarCredenciales(String correo, String contrasena) {
        if (correo == null || contrasena == null || correo.isBlank() || contrasena.isBlank()) {
            logger.warn("Credenciales incompletas. correo='{}' passwordNull={}", correo, contrasena == null);
            return null;
        }

        String correoNormalizado = InputSanitizer.normalizeEmail(correo);
        Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreo(correoNormalizado);

        if (usuarioOpt.isEmpty()) {
            logger.warn("Usuario no encontrado: {}", correoNormalizado);
            return null;
        }

        Usuario usuario = usuarioOpt.get();

        if (usuario.getContrasena() == null || usuario.getContrasena().isBlank()) {
            logger.warn("Usuario sin contraseña registrada: {}", usuario.getCorreo());
            return null;
        }

        if (!esHashBCrypt(usuario.getContrasena())) {
            logger.warn("Credencial legacy detectada para {}. Se validará contra texto plano y se migrará a BCrypt si coincide.", usuario.getCorreo());
            return validarCredencialLegacy(usuario, contrasena);
        }

        String raw = contrasena;
        String rawTrim = InputSanitizer.normalizeText(contrasena);
        String hash = usuario.getContrasena();
        String hashTrim = hash.trim();

        boolean coincide = passwordEncoder.matches(raw, hash);
        boolean coincideRawTrim = passwordEncoder.matches(rawTrim, hash);
        boolean coincideHashTrim = passwordEncoder.matches(raw, hashTrim);
        boolean coincideTrimTrim = passwordEncoder.matches(rawTrim, hashTrim);

        logger.debug("🔎 Login debug {} -> raw='{}' len={} bytes={} trimLen={} sameAfterTrim={}",
                usuario.getCorreo(), raw, raw.length(), raw.getBytes().length, rawTrim.length(), raw.equals(rawTrim));
        logger.debug("🔎 Login debug {} -> storedHash='{}' len={} trimLen={} bcryptPatternOk={} hashChangedByTrim={}",
                usuario.getCorreo(), hash, hash.length(), hashTrim.length(), esHashBCrypt(hash), !hash.equals(hashTrim));
        logger.debug("🔎 Login debug {} -> matches(raw,hash)={} matches(trimRaw,hash)={} matches(raw,trimHash)={} matches(trimRaw,trimHash)={}",
                usuario.getCorreo(), coincide, coincideRawTrim, coincideHashTrim, coincideTrimTrim);

        if (!coincide) {
            logger.warn("Contraseña incorrecta para: {}", usuario.getCorreo());
            return null;
        }

        logger.info("Acceso válido: {} | ID: {} | Rol: {}",
                usuario.getCorreo(), usuario.getId(), (usuario.getRol() != null ? usuario.getRol().getNombre() : "N/A"));
        return usuario;
    }

    // Registrar nuevo usuario con contraseña encriptada
    public Usuario registrarUsuario(Usuario usuario) {
        if (usuario == null || usuario.getCorreo() == null || usuario.getContrasena() == null) {
            logger.warn("Datos de usuario incompletos para registro");
            return null;
        }

        usuario.setCorreo(InputSanitizer.normalizeEmail(usuario.getCorreo()));
        if (!esHashBCrypt(usuario.getContrasena())) {
            usuario.setContrasena(passwordEncoder.encode(usuario.getContrasena()));
        }
        usuario.setActivo(true);
        if (usuario.getIdColaborador() == null || usuario.getIdColaborador().isBlank()) {
            usuario.setIdColaborador("AUTO-" + System.currentTimeMillis());
        }
        if (usuario.getCargo() == null || usuario.getCargo().isBlank()) {
            usuario.setCargo("No especificado");
        }
        if (usuario.getArea() == null || usuario.getArea().isBlank()) {
            usuario.setArea("No especificada");
        }
        if (usuario.getTelefono() == null || usuario.getTelefono().isBlank()) {
            usuario.setTelefono("0000000000");
        }
        if (usuario.getDebeCambiarContrasena() == null) {
            usuario.setDebeCambiarContrasena(false);
        }

        logger.info("Registrando usuario: {}", usuario.getCorreo());
        return usuarioRepository.save(usuario);
    }

    // Buscar usuario por correo (para /api/auth/me)
    public Usuario buscarPorCorreo(String correo) {
        if (correo == null || correo.isBlank()) {
            return null;
        }
        return usuarioRepository.findByCorreo(InputSanitizer.normalizeEmail(correo)).orElse(null);
    }

    private boolean esHashBCrypt(String valor) {
        return valor != null && BCRYPT_PATTERN.matcher(valor).matches();
    }

    private Usuario validarCredencialLegacy(Usuario usuario, String contrasena) {
        String almacenada = usuario.getContrasena();
        String ingresada = contrasena != null ? contrasena : "";

        boolean coincideExacta = almacenada.equals(ingresada);
        boolean coincideTrim = almacenada.trim().equals(ingresada.trim());

        if (!coincideExacta && !coincideTrim) {
            logger.warn("Contraseña legacy incorrecta para: {}", usuario.getCorreo());
            return null;
        }

        usuario.setContrasena(passwordEncoder.encode(ingresada.trim()));
        usuarioRepository.save(usuario);
        logger.info("Contraseña legacy migrada a BCrypt para: {}", usuario.getCorreo());
        return usuario;
    }

    private String safeHash(String hash) {
        if (hash == null) return "null";
        if (hash.length() <= 14) return hash;
        return hash.substring(0, 7) + "..." + hash.substring(hash.length() - 7);
    }
}
