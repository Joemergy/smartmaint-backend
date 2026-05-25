package com.smartmaint.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiter en memoria para el endpoint de login.
 * Protege contra ataques de fuerza bruta sin necesitar Redis ni dependencias externas.
 *
 * Política: máximo 10 intentos fallidos por IP en ventana de 15 minutos.
 * Al superar el límite, la IP queda bloqueada hasta que la ventana expire.
 * Un login exitoso resetea el contador de la IP.
 */
@Service
public class LoginRateLimiterService {

    private static final Logger logger = LoggerFactory.getLogger(LoginRateLimiterService.class);

    private static final int MAX_INTENTOS   = 10;
    private static final long VENTANA_MS    = 15 * 60 * 1000L; // 15 minutos

    // Clave: IP del cliente → lista de timestamps de intentos fallidos
    private final ConcurrentHashMap<String, LinkedList<Long>> intentosFallidos = new ConcurrentHashMap<>();

    /**
     * Verifica si la IP puede intentar login.
     * @return true si está permitido, false si excedió el límite.
     */
    public boolean permitirIntento(String ip) {
        if (ip == null || ip.isBlank()) return true;

        long ahora = System.currentTimeMillis();
        long inicioVentana = ahora - VENTANA_MS;

        LinkedList<Long> intentos = intentosFallidos.computeIfAbsent(ip, k -> new LinkedList<>());

        synchronized (intentos) {
            // Eliminar intentos fuera de la ventana
            intentos.removeIf(t -> t < inicioVentana);

            if (intentos.size() >= MAX_INTENTOS) {
                logger.warn("Rate limit alcanzado para IP: {}", ip);
                return false;
            }
            return true;
        }
    }

    /**
     * Registra un intento fallido desde la IP dada.
     */
    public void registrarFallo(String ip) {
        if (ip == null || ip.isBlank()) return;

        LinkedList<Long> intentos = intentosFallidos.computeIfAbsent(ip, k -> new LinkedList<>());
        synchronized (intentos) {
            intentos.add(System.currentTimeMillis());
        }
    }

    /**
     * Limpia el historial de fallos de una IP tras un login exitoso.
     */
    public void resetear(String ip) {
        if (ip != null) intentosFallidos.remove(ip);
    }

    /**
     * Extrae la IP real del cliente considerando proxies reversos.
     */
    public static String extraerIp(jakarta.servlet.http.HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) return realIp.trim();
        return request.getRemoteAddr();
    }

    /**
     * Devuelve cuántos segundos faltan para que la ventana de bloqueo expire.
     */
    public long segundosRestantesBloqueo(String ip) {
        LinkedList<Long> intentos = intentosFallidos.get(ip);
        if (intentos == null) return 0;
        synchronized (intentos) {
            if (intentos.size() < MAX_INTENTOS) return 0;
            long masAntiguo = intentos.peek() != null ? intentos.peek() : 0;
            long restante = (masAntiguo + VENTANA_MS - System.currentTimeMillis()) / 1000;
            return restante > 0 ? restante : 0;
        }
    }
}
