package com.smartmaint.service;

import com.smartmaint.model.Usuario;
import com.smartmaint.repository.UsuarioRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DemoCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(DemoCleanupService.class);

    private final UsuarioRepository usuarioRepository;

    public DemoCleanupService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Ejecuta cada día a las 3:00 AM y deshabilita usuarios demo expirados.
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void eliminarUsuariosDemoExpirados() {
        List<Usuario> expirados = usuarioRepository.findByDemoTrueAndExpiraEnBefore(LocalDateTime.now());

        if (!expirados.isEmpty()) {
            int deshabilitados = 0;
            for (Usuario usuario : expirados) {
                if (usuario.getActivo() == null || usuario.getActivo()) {
                    usuario.setActivo(false);
                    deshabilitados++;
                }
            }

            if (deshabilitados > 0) {
                usuarioRepository.saveAll(expirados);
            }

            logger.info("Usuarios demo expirados revisados: {}. Deshabilitados en esta ejecución: {}", expirados.size(), deshabilitados);
        } else {
            logger.info("No se encontraron usuarios demo expirados en esta ejecución");
        }
    }
}
