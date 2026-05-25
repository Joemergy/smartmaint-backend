package com.smartmaint.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Refresh token almacenado en BD.
 * - El access token dura 15 min (configurable).
 * - El refresh token dura 30 días y permite obtener un nuevo access token
 *   sin que el usuario vuelva a ingresar sus credenciales.
 * - Al hacer logout se marca como revoked=true.
 * - Un @Scheduled limpia los tokens expirados periódicamente.
 */
@Entity
@Table(name = "refresh_tokens", schema = "smartmaint")
@Data
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 512)
    private String token;

    @Column(nullable = false, length = 150)
    private String correo;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private boolean revoked = false;
}
