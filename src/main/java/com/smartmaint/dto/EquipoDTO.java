package com.smartmaint.dto;

import com.smartmaint.model.Equipo;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EquipoDTO {
    private Long id;
    private String nombre;
    private String tipo;
    private String ubicacion;
    private String categoria;
    private String descripcion;
    private LocalDateTime createdAt;
    private Long empresaId;

    public static EquipoDTO fromEntity(Equipo equipo) {
        EquipoDTO dto = new EquipoDTO();
        dto.setId(equipo.getId());
        dto.setNombre(equipo.getNombre());
        dto.setTipo(equipo.getTipo());
        dto.setUbicacion(equipo.getUbicacion());
        dto.setCategoria(equipo.getCategoria());
        dto.setDescripcion(equipo.getDescripcion());
        dto.setCreatedAt(equipo.getCreatedAt());
        dto.setEmpresaId(equipo.getEmpresa() != null ? equipo.getEmpresa().getId() : null);
        return dto;
    }
}
