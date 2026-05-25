package com.smartmaint.util;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Profile("!test")
@Component
public class JwtFilter extends OncePerRequestFilter {

private static final Logger logger = LoggerFactory.getLogger(JwtFilter.class);

private final JwtUtil jwtUtil;

public JwtFilter(JwtUtil jwtUtil) {
    this.jwtUtil = jwtUtil;
}

@Override
protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {

    String path = request.getServletPath();

    logger.debug("JWT Filter evaluando ruta: {}", path);

    return path.equals("/api/auth/login") ||
           path.equals("/api/auth/register") ||
           path.equals("/api/auth/refresh") ||
           path.equals("/api/auth/logout") ||
           path.equals("/api/auth/recuperar-contrasena") ||
           path.equals("/api/empresas") ||
           path.equals("/api/empresas/planes/compra") ||
           path.equals("/api/empresas/activar") ||
           path.startsWith("/api/empresas/validar-id") ||
           path.equals("/error") ||
           path.equals("/api/test-db") ||
           path.startsWith("/api/demo");
}

@Override
protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
) throws ServletException, IOException {

    try {

        final String authHeader = request.getHeader("Authorization");

        // ✅ NO HAY TOKEN → CONTINUAR NORMAL
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {

            logger.debug("No Authorization Bearer token encontrado");

            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        // ✅ TOKEN INVÁLIDO → LIMPIAR CONTEXTO
        if (!jwtUtil.validarToken(token)) {

            logger.debug("JWT inválido o expirado");

            SecurityContextHolder.clearContext();

            filterChain.doFilter(request, response);
            return;
        }

        String correo = jwtUtil.extraerCorreo(token);

        var claims = jwtUtil.getClaims(token);

        List<String> roles = claims.get("roles", List.class);

        logger.debug("JWT válido para usuario {} con roles {}", correo, roles);

        // ✅ SOLO AUTENTICAR SI NO EXISTE AUTENTICACIÓN
        if (
            correo != null &&
            SecurityContextHolder.getContext().getAuthentication() == null
        ) {

            List<SimpleGrantedAuthority> authorities =
                    roles != null
                            ? roles.stream()
                                .map(r -> r == null ? "" : r.trim().toUpperCase())
                                .map(this::normalizeGrantedAuthority)
                                .filter(r -> !r.isBlank())
                                .map(SimpleGrantedAuthority::new)
                                .collect(Collectors.toList())
                            : Collections.emptyList();

            logger.debug("Authorities generadas: {}", authorities);

            User userDetails = new User(
                    correo,
                    "",
                    authorities
            );

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            authorities
                    );

            authToken.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );

            SecurityContextHolder
                    .getContext()
                    .setAuthentication(authToken);

            logger.debug("Usuario autenticado en SecurityContext");
        }

        filterChain.doFilter(request, response);

    } catch (Exception ex) {

        logger.error("Error procesando JWT", ex);

        SecurityContextHolder.clearContext();

        filterChain.doFilter(request, response);
    }
}

private String normalizeGrantedAuthority(String role) {

    if (role == null || role.isBlank()) {
        return "";
    }

    String normalized = role.trim().toUpperCase();

    // ✅ QUITAR ROLE_ SI YA EXISTE
    if (normalized.startsWith("ROLE_")) {
        normalized = normalized.substring(5);
    }

    // ✅ QUITAR UNDERSCORES
    normalized = normalized.replace("_", "");

    // ✅ EJEMPLO:
    // SUPER_ADMIN -> ROLE_SUPERADMIN
    // SUPERADMIN -> ROLE_SUPERADMIN
    // ADMIN -> ROLE_ADMIN
    // USUARIO -> ROLE_USUARIO

    return "ROLE_" + normalized;
}

}
