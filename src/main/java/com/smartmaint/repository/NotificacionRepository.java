package com.smartmaint.repository;

import com.smartmaint.model.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
    List<Notificacion> findByUsuario_IdOrderByFechaDesc(Long usuarioId);
    long countByUsuario_IdAndLeidoFalse(Long usuarioId);
}
