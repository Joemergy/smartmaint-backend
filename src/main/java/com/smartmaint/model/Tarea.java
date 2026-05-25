package com.smartmaint.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "tareas", schema = "smartmaint")
@Data
public class Tarea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(length = 20, nullable = false)
    private String estado;

    @NotBlank
    @Column(length = 100, nullable = false)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(columnDefinition = "TEXT")
    private String notaTecnica;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime fechaInicio;

    private LocalDateTime fechaCierre;

    @Column(length = 30)
    private String categoria;

    @NotBlank
    @Column(length = 50, nullable = false)
    private String nombreMaquina;

    @Column(name = "id_maquina")
    private String idMaquina;

    @Column(length = 50)
    private String ubicacion;

    @Column(length = 50)
    private String idColaborador;

    @Column(length = 100)
    private String nombreColaborador;

    @Column(length = 100)
    private String correoColaborador;

    private boolean grupal;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "tarea_archivos", schema = "smartmaint", joinColumns = @JoinColumn(name = "tarea_id"))
    @Column(name = "archivo")
    private List<String> archivos;

    @Column(length = 20)
    private String prioridad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = true)
    @JsonIgnoreProperties({"tareasAsignadas"}) // 👈 rompe el ciclo
    private Usuario usuarioAsignado;

    @Override
    public String toString() {
        return "Tarea{" +
                "id=" + id +
                ", estado='" + estado + '\'' +
                ", titulo='" + titulo + '\'' +
                ", descripcion='" + descripcion + '\'' +
                ", nombreColaborador='" + nombreColaborador + '\'' +
                ", correoColaborador='" + correoColaborador + '\'' +
                ", nombreMaquina='" + nombreMaquina + '\'' +
                ", fechaInicio=" + fechaInicio +
                ", fechaCierre=" + fechaCierre +
                ", prioridad='" + prioridad + '\'' +
                '}';
    }
}
