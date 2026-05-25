package com.smartmaint.dto;

import com.smartmaint.model.Tarea;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TareaDTO {

    private Long id;

    @NotBlank(message = "El estado es obligatorio")
    private String estado;

    @NotBlank(message = "El título es obligatorio")
    private String titulo;

    @NotBlank(message = "La descripción es obligatoria")
    private String descripcion;

    private String notaTecnica;

    @NotNull(message = "La fecha de inicio es obligatoria")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaInicio;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaCierre;

    private String categoria;

    @NotBlank(message = "El nombre de la máquina es obligatorio")
    private String nombreMaquina;

    private String idMaquina;

    private String ubicacion;
    private String nombreColaborador;
    private String idColaborador;
    private String correoColaborador;

    private boolean grupal;
    private String observaciones;

    private List<String> archivos;
    private String prioridad;

    public static TareaDTO fromEntity(Tarea tarea) {
        if (tarea == null) return null;

        TareaDTO dto = new TareaDTO();
        dto.setId(tarea.getId());
        dto.setEstado(tarea.getEstado());
        dto.setTitulo(tarea.getTitulo());
        dto.setDescripcion(tarea.getDescripcion());
        dto.setNotaTecnica(tarea.getNotaTecnica());
        dto.setFechaInicio(tarea.getFechaInicio());
        dto.setFechaCierre(tarea.getFechaCierre());
        dto.setCategoria(tarea.getCategoria());
        dto.setNombreMaquina(tarea.getNombreMaquina());
        dto.setIdMaquina(tarea.getIdMaquina());
        dto.setUbicacion(tarea.getUbicacion());
        dto.setNombreColaborador(tarea.getNombreColaborador());
        dto.setIdColaborador(tarea.getIdColaborador());
        dto.setCorreoColaborador(tarea.getCorreoColaborador());
        dto.setGrupal(tarea.isGrupal());
        dto.setObservaciones(tarea.getObservaciones());

        // Materializamos la colección para evitar LazyInitializationException
        dto.setArchivos(
            tarea.getArchivos() != null ? List.copyOf(tarea.getArchivos()) : List.of()
        );

        dto.setPrioridad(tarea.getPrioridad());
        return dto;
    }

    public Tarea toEntity() {
        Tarea tarea = new Tarea();
        tarea.setId(this.id);
        tarea.setEstado(this.estado);
        tarea.setTitulo(this.titulo);
        tarea.setDescripcion(this.descripcion);
        tarea.setNotaTecnica(this.notaTecnica);
        tarea.setFechaInicio(this.fechaInicio);
        tarea.setFechaCierre(this.fechaCierre);
        tarea.setCategoria(this.categoria);
        tarea.setNombreMaquina(this.nombreMaquina);
        tarea.setIdMaquina(this.idMaquina);
        tarea.setUbicacion(this.ubicacion);
        tarea.setNombreColaborador(this.nombreColaborador);
        tarea.setIdColaborador(this.idColaborador);
        tarea.setCorreoColaborador(this.correoColaborador);
        tarea.setGrupal(this.grupal);
        tarea.setObservaciones(this.observaciones);
        tarea.setArchivos(this.archivos);
        tarea.setPrioridad(this.prioridad);
        return tarea;
    }
}
