package com.smartmaint.service;

import com.smartmaint.model.RefreshToken;
import com.smartmaint.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Gestiona el ciclo de vida de los refresh tokens:
 *  - Creación al hacer login
 *  - Validación al renovar el access token
 *  - Rotación: el token viejo se revoca y se genera uno nuevo
 *  - Revocación al hacer logout (single device o todos)
 *  - Limpieza periódica de tokens expirados/revocados
 */
@Service
public class RefreshTokenService {

    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenService.class);

    @Value("${jwt.refresh.expiration:2592000000}") // 30 días en ms
    private long refreshExpirationMs;

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /** Crea y persiste un nuevo refresh token para el correo dado. */
    @Transactional
    public RefreshToken crear(String correo) {
        RefreshToken rt = new RefreshToken();
        rt.setToken(UUID.randomUUID().toString());
        rt.setCorreo(correo.toLowerCase().trim());
        rt.setExpiresAt(LocalDateTime.now().plusNanos(refreshExpirationMs * 1_000_000L));
        rt.setRevoked(false);
        return refreshTokenRepository.save(rt);
    }

    /**
     * Valida el token: debe existir, no estar revocado y no haber expirado.
     * @return Optional vacío si es inválido.
     */
    public Optional<RefreshToken> validar(String token) {
        return refreshTokenRepository.findByToken(token)
                .filter(rt -> !rt.isRevoked())
                .filter(rt -> rt.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    /**
     * Rota el refresh token: revoca el anterior y emite uno nuevo.
     * Esto limita el impacto si un token robado intenta usarse por segunda vez.
     */
    @Transactional
    public RefreshToken rotar(RefreshToken viejo) {
        viejo.setRevoked(true);
        refreshTokenRepository.save(viejo);
        return crear(viejo.getCorreo());
    }

    /** Revoca todos los refresh tokens activos del usuario (logout total). */
    @Transactional
    public void revocarTodos(String correo) {
        refreshTokenRepository.revokeAllByCorreo(correo.toLowerCase().trim());
        logger.info("Refresh tokens revocados para: {}", correo);
    }

    /** Revoca un token específico (logout de un solo dispositivo). */
    @Transactional
    public void revocarUno(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
    }

    /**
     * Limpieza programada: elimina tokens expirados o revocados.
     * Se ejecuta cada 6 horas.
     */
    @Scheduled(fixedRate = 6 * 60 * 60 * 1000L)
    @Transactional
    public void limpiarTokensExpirados() {
        refreshTokenRepository.deleteExpiredAndRevoked(LocalDateTime.now());
        logger.debug("Limpieza de refresh tokens expirados/revocados completada");
    }
}
