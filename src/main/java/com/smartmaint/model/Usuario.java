package com.smartmaint.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "usuarios", schema = "smartmaint")
@Data
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 50)
    private String nombre;

    @NotBlank
    @Email
    @Column(nullable = false, unique = true, length = 50)
    private String correo;

    @NotBlank
    @Column(nullable = false, length = 255)
    private String contrasena;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "id_colaborador", nullable = false, unique = true, length = 50)
    private String idColaborador;

    @Column(name = "cargo", nullable = false, length = 100)
    private String cargo;

    @Column(name = "area", nullable = false, length = 100)
    private String area;

    @Column(name = "telefono", nullable = false, length = 30)
    private String telefono;

    @Column(name = "fecha_ingreso")
    private LocalDate fechaIngreso;

    @Column(name = "direccion", length = 255)
    private String direccion;

    @Column(name = "foto_perfil", columnDefinition = "TEXT")
    private String fotoPerfil;

    @Column(name = "debe_cambiar_contrasena", nullable = false)
    private Boolean debeCambiarContrasena = false;

    @Column(name = "demo")
    private Boolean demo = false;

    @Column(name = "expira_en")
    private LocalDateTime expiraEn;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "usuarioAsignado", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Tarea> tareasAsignadas;

    @PrePersist
    protected void onCreate() {
        if (this.activo == null) {
            this.activo = true;
        }
        if (this.demo == null) {
            this.demo = false;
        }
        if (this.debeCambiarContrasena == null) {
            this.debeCambiarContrasena = false;
        }
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        if (this.activo == null) {
            this.activo = true;
        }
        if (this.demo == null) {
            this.demo = false;
        }
        if (this.debeCambiarContrasena == null) {
            this.debeCambiarContrasena = false;
        }
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "Usuario{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                ", correo='" + correo + '\'' +
                ", rol=" + (rol != null ? rol.getNombre() : null) +
                ", empresa=" + (empresa != null ? empresa.getNombre() : null) +
                ", activo=" + activo +
                ", demo=" + demo +
                ", expiraEn=" + expiraEn +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
