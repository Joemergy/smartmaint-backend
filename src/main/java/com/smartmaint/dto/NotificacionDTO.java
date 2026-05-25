package com.smartmaint.dto;

import com.smartmaint.model.Notificacion;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificacionDTO {
    private Long id;
    private String mensaje;
    private LocalDateTime fecha;
    private Boolean leido;

    public static NotificacionDTO fromEntity(Notificacion notificacion) {
        NotificacionDTO dto = new NotificacionDTO();
        dto.setId(notificacion.getId());
        dto.setMensaje(notificacion.getMensaje());
        dto.setFecha(notificacion.getFecha());
        dto.setLeido(notificacion.getLeido());
        return dto;
    }
}
