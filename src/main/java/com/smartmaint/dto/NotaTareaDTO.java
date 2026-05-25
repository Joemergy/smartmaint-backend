package com.smartmaint.dto;

import com.smartmaint.model.NotaTarea;
import java.time.LocalDateTime;

public class NotaTareaDTO {

    public Long id;
    public Long tareaId;
    public String autorNombre;
    public String texto;
    public LocalDateTime fechaCreacion;

    public static NotaTareaDTO fromEntity(NotaTarea n) {
        NotaTareaDTO dto = new NotaTareaDTO();
        dto.id = n.getId();
        dto.tareaId = n.getTarea().getId();
        dto.autorNombre = n.getAutorNombre();
        dto.texto = n.getTexto();
        dto.fechaCreacion = n.getFechaCreacion();
        return dto;
    }
}
