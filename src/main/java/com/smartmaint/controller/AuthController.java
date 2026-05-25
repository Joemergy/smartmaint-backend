package com.smartmaint.controller;

import com.smartmaint.api.ApiResponses;
import com.smartmaint.dto.ChangePasswordRequest;
import com.smartmaint.dto.LoginRequest;
import com.smartmaint.dto.RegisterRequest;
import com.smartmaint.model.Empresa;
import com.smartmaint.model.RefreshToken;
import com.smartmaint.model.Rol;
import com.smartmaint.model.Usuario;
import com.smartmaint.repository.EmpresaRepository;
import com.smartmaint.repository.RolRepository;
import com.smartmaint.service.AuthService;
import com.smartmaint.service.LoginRateLimiterService;
import com.smartmaint.service.RecuperacionEmailService;
import com.smartmaint.service.RefreshTokenService;
import com.smartmaint.service.UsuarioService;
import com.smartmaint.util.InputSanitizer;
import com.smartmaint.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;

    @Autowired
    private UsuarioService usuarioService;

    // ✅ AHORA ES OPCIONAL
    @Autowired
    private ObjectProvider<RecuperacionEmailService> recuperacionEmailServiceProvider;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private LoginRateLimiterService rateLimiter;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private EmpresaRepository empresaRepository;

    /**
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest,
                                   HttpServletRequest httpRequest) {

        String ip = LoginRateLimiterService.extraerIp(httpRequest);

        if (!rateLimiter.permitirIntento(ip)) {
            long restante = rateLimiter.segundosRestantesBloqueo(ip);

            return ApiResponses.error(
                    org.springframework.http.HttpStatus.TOO_MANY_REQUESTS,
                    "RATE_LIMIT_EXCEEDED",
                    "Demasiados intentos fallidos. Espera " + restante + " segundos.",
                    Map.of("segundosRestantes", restante)
            );
        }

        String correo = InputSanitizer.normalizeEmail(loginRequest.getCorreo());
        String contrasena = Optional.ofNullable(loginRequest.getContrasena()).orElse("");
        String rolSolicitado = jwtUtil.normalizeRole(
                InputSanitizer.normalizeText(loginRequest.getRol())
        );

        if (correo.isEmpty() || contrasena.isEmpty()) {
            return ApiResponses.badRequest("Correo y contraseña son obligatorios");
        }

        try {

            Usuario usuario = authService.validarCredenciales(correo, contrasena);

            if (usuario == null) {
                rateLimiter.registrarFallo(ip);
                return ApiResponses.unauthorized("Credenciales inválidas");
            }

            if (Boolean.TRUE.equals(usuario.getDemo())
                    && usuario.getExpiraEn() != null
                    && LocalDateTime.now().isAfter(usuario.getExpiraEn())) {

                rateLimiter.registrarFallo(ip);

                return ApiResponses.error(
                        org.springframework.http.HttpStatus.FORBIDDEN,
                        "DEMO_EXPIRED",
                        "Tu demo ha expirado",
                        Map.of("expirado", true)
                );
            }

            if (usuario.getActivo() != null && !usuario.getActivo()) {
                rateLimiter.registrarFallo(ip);
                return ApiResponses.forbidden("Usuario inactivo");
            }

            Rol rol = usuario.getRol();
            Empresa empresa = usuario.getEmpresa();

            if (rol == null || rol.getNombre() == null) {
                return ApiResponses.internalError("Usuario sin rol");
            }

            String rolNormalizado = jwtUtil.normalizeRole(rol.getNombre());

            if (!rolSolicitado.isEmpty()
                    && !rolNormalizado.equalsIgnoreCase(rolSolicitado)) {

                rateLimiter.registrarFallo(ip);

                return ApiResponses.forbidden(
                        "El rol seleccionado no coincide con tus credenciales"
                );
            }

            if (empresa == null) {
                return ApiResponses.internalError("Usuario sin empresa");
            }

            rateLimiter.resetear(ip);

            String accessToken =
                    jwtUtil.generarTokenConRol(usuario.getCorreo(), rolNormalizado);

            RefreshToken refreshToken =
                    refreshTokenService.crear(usuario.getCorreo());

            Map<String, Object> payload =
                    buildAuthPayload(usuario, accessToken, rolNormalizado);

            payload.put("refreshToken", refreshToken.getToken());

            return ApiResponses.ok(payload);

        } catch (Exception e) {

            logger.error("Error en login: {}", e.getMessage());

            return ApiResponses.internalError(e.getMessage());
        }
    }

    /**
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {

        try {

            String correoNormalizado =
                    InputSanitizer.normalizeEmail(request.getCorreo());

            Rol rol = rolRepository.findById(request.getRolId())
                    .orElseThrow(() -> new RuntimeException("Rol no encontrado"));

            Empresa empresa = empresaRepository.findById(request.getEmpresaId())
                    .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));

            Usuario usuario = new Usuario();

            usuario.setNombre(
                    InputSanitizer.normalizeText(request.getNombre(), 50)
            );

            usuario.setCorreo(correoNormalizado);
            usuario.setContrasena(request.getContrasena());
            usuario.setRol(rol);
            usuario.setEmpresa(empresa);
            usuario.setIdColaborador("REG-" + correoNormalizado.hashCode());
            usuario.setCargo("No especificado");
            usuario.setArea("No especificada");
            usuario.setTelefono("0000000000");
            usuario.setDebeCambiarContrasena(false);

            Usuario usuarioRegistrado =
                    authService.registrarUsuario(usuario);

            return ApiResponses.ok(
                    buildRegisterPayload(usuarioRegistrado, rol, empresa)
            );

        } catch (Exception e) {

            logger.error("Error en registro: {}", e.getMessage());

            return ApiResponses.internalError(e.getMessage());
        }
    }

    /**
     * GET /api/auth/me
     */
    @GetMapping("/me")
    public ResponseEntity<?> me(
            @RequestHeader("Authorization") String authHeader
    ) {

        try {

            String correo =
                    jwtUtil.extraerCorreoDesdeToken(authHeader);

            Usuario usuario =
                    authService.buscarPorCorreo(correo);

            if (usuario == null) {
                return ApiResponses.notFound("Usuario no encontrado");
            }

            String rol =
                    usuario.getRol() != null
                            ? usuario.getRol().getNombre().trim().toUpperCase()
                            : null;

            return ApiResponses.ok(
                    buildAuthPayload(usuario, null, rol)
            );

        } catch (Exception e) {

            logger.error("Error en /me: {}", e.getMessage());

            return ApiResponses.internalError(e.getMessage());
        }
    }

    /**
     * CAMBIAR CONTRASEÑA
     */
    @PutMapping("/cambiar-contrasena-inicial")
    public ResponseEntity<?> cambiarContrasenaInicial(
            @Valid @RequestBody ChangePasswordRequest request,
            @RequestHeader("Authorization") String authHeader
    ) {

        try {

            String correo =
                    jwtUtil.extraerCorreoDesdeToken(authHeader);

            usuarioService.cambiarContrasenaInicial(
                    correo,
                    request.getContrasenaActual(),
                    request.getNuevaContrasena()
            );

            return ApiResponses.message(
                    org.springframework.http.HttpStatus.OK,
                    "message",
                    "Contraseña actualizada"
            );

        } catch (Exception e) {

            return ApiResponses.badRequest(e.getMessage());
        }
    }

    /**
     * REFRESH TOKEN
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {

        String tokenStr =
                body.getOrDefault("refreshToken", "").trim();

        if (tokenStr.isBlank()) {
            return ApiResponses.badRequest(
                    "El refreshToken es obligatorio"
            );
        }

        return refreshTokenService.validar(tokenStr)
                .map(rt -> {

                    RefreshToken nuevo =
                            refreshTokenService.rotar(rt);

                    Usuario usuario =
                            authService.buscarPorCorreo(rt.getCorreo());

                    if (usuario == null) {
                        return ApiResponses.unauthorized(
                                "Usuario no encontrado"
                        );
                    }

                    String rol =
                            usuario.getRol() != null
                                    ? usuario.getRol().getNombre().trim().toUpperCase()
                                    : "USUARIO";

                    String nuevoAccessToken =
                            jwtUtil.generarTokenConRol(rt.getCorreo(), rol);

                    return ApiResponses.ok(Map.of(
                            "token", nuevoAccessToken,
                            "refreshToken", nuevo.getToken()
                    ));
                })
                .orElseGet(() ->
                        ApiResponses.unauthorized(
                                "Refresh token inválido o expirado"
                        )
                );
    }

    /**
     * LOGOUT
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestBody Map<String, String> body
    ) {

        String tokenStr =
                body.getOrDefault("refreshToken", "").trim();

        if (!tokenStr.isBlank()) {
            refreshTokenService.revocarUno(tokenStr);
        }

        return ApiResponses.message(
                org.springframework.http.HttpStatus.OK,
                "message",
                "Sesión cerrada correctamente"
        );
    }

    /**
     * LOGOUT ALL
     */
    @PostMapping("/logout-all")
    public ResponseEntity<?> logoutAll(
            @RequestHeader("Authorization") String authHeader
    ) {

        try {

            String correo =
                    jwtUtil.extraerCorreoDesdeToken(authHeader);

            refreshTokenService.revocarTodos(correo);

            return ApiResponses.message(
                    org.springframework.http.HttpStatus.OK,
                    "message",
                    "Sesión cerrada en todos los dispositivos"
            );

        } catch (Exception e) {

            return ApiResponses.badRequest(
                    "No se pudo cerrar sesión"
            );
        }
    }

    /**
     * RECUPERAR CONTRASEÑA
     */
    @PostMapping("/recuperar-contrasena")
    public ResponseEntity<?> recuperarContrasena(
            @RequestBody Map<String, String> body
    ) {

        String correo = InputSanitizer.normalizeEmail(
                body.getOrDefault("correo", "")
        );

        if (correo.isBlank()) {
            return ApiResponses.badRequest(
                    "El correo es obligatorio"
            );
        }

        try {

            String[] resultado =
                    usuarioService.recuperarContrasena(correo);

            // ✅ OPCIONAL
            RecuperacionEmailService emailService =
                    recuperacionEmailServiceProvider.getIfAvailable();

            if (emailService != null) {

                try {

                    emailService.enviarContrasenaTemporal(
                            correo,
                            resultado[1],
                            resultado[0]
                    );

                } catch (Exception emailEx) {

                    logger.warn(
                            "No se pudo enviar email: {}",
                            emailEx.getMessage()
                    );
                }
            }

        } catch (RuntimeException ignored) {
        }

        return ApiResponses.message(
                org.springframework.http.HttpStatus.OK,
                "message",
                "Si el correo está registrado, recibirás una contraseña temporal"
        );
    }

    private Map<String, Object> buildAuthPayload(
            Usuario usuario,
            String token,
            String rolNormalizado
    ) {

        Empresa empresa = usuario.getEmpresa();
        Rol rol = usuario.getRol();

        Map<String, Object> respuesta = new HashMap<>();

        if (token != null) {
            respuesta.put("token", token);
        }

        respuesta.put("idUsuario", usuario.getId());
        respuesta.put("nombre", usuario.getNombre());
        respuesta.put("correo", usuario.getCorreo());
        respuesta.put("idColaborador", usuario.getIdColaborador());
        respuesta.put("telefono", usuario.getTelefono());
        respuesta.put("cargo", usuario.getCargo());
        respuesta.put("area", usuario.getArea());
        respuesta.put("rol", rolNormalizado);
        respuesta.put("rolId", rol != null ? rol.getId() : null);
        respuesta.put("idEmpresa", empresa != null ? empresa.getId() : null);
        respuesta.put("empresa", empresa != null ? empresa.getNombre() : null);
        respuesta.put("plan", empresa != null ? empresa.getPlan() : null);
        respuesta.put("demo", usuario.getDemo());
        respuesta.put("expiraEn", usuario.getExpiraEn());
        respuesta.put(
                "requiereCambioContrasena",
                usuario.getDebeCambiarContrasena()
        );

        return respuesta;
    }

    private Map<String, Object> buildRegisterPayload(
            Usuario usuario,
            Rol rol,
            Empresa empresa
    ) {

        Map<String, Object> respuesta = new HashMap<>();

        respuesta.put("idUsuario", usuario.getId());
        respuesta.put("nombre", usuario.getNombre());
        respuesta.put("correo", usuario.getCorreo());
        respuesta.put("rol", rol.getNombre());
        respuesta.put("idEmpresa", empresa.getId());

        return respuesta;
    }
}