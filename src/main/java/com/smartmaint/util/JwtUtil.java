package com.smartmaint.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.security.Key;

@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret:SmartmaintClaveUltraSeguraJWT2025++}")
    private String secretKey;

    @Value("${jwt.expiration:86400000}") // 24h por defecto
    private long expirationTime;

    private Key key;

    @PostConstruct
    public void initKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("❌ Clave JWT demasiado corta: mínimo 32 bytes requeridos para HS256");
        }
        key = Keys.hmacShaKeyFor(keyBytes);
        logger.info("Clave JWT inicializada correctamente");
    }

    // ✅ Generar token con rol incluido
    public String generarTokenConRol(String correo, String rol) {
        if (correo == null || correo.isBlank()) {
            throw new IllegalArgumentException("❌ El correo no puede ser nulo o vacío");
        }
        if (rol == null || rol.isBlank()) {
            throw new IllegalArgumentException("❌ El rol no puede ser nulo o vacío");
        }
        String rolNormalizado = normalizeRole(rol);
        return Jwts.builder()
                .setSubject(correo)
                .claim("roles", List.of("ROLE_" + rolNormalizado))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "";
        }
        String normalized = role.trim().toUpperCase();
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring(5);
        }
        normalized = normalized.replaceAll("_", "");
        if (normalized.isBlank()) {
            return "";
        }
        if (normalized.equals("SUPERADMIN") || normalized.equals("ADMIN") || normalized.equals("USUARIO")) {
            return normalized;
        }
        return normalized;
    }

    // Extraer correo (subject) del token
    public String extraerCorreo(String token) {
        return getClaims(token).getSubject();
    }

    // Extraer correo desde token con prefijo Bearer
    public String extraerCorreoDesdeToken(String token) {
        if (token == null || token.isBlank()) return null;
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return extraerCorreo(token);
    }

    // Validar token (vigencia y firma)
    public boolean validarToken(String token) {
        try {
            Claims claims = getClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            logger.debug("Token inválido: {}", e.getMessage());
            return false;
        }
    }

    /** Tiempo de vida del access token en milisegundos. */
    public long getExpirationTime() {
        return expirationTime;
    }

    // Obtener claims completos
    public Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
