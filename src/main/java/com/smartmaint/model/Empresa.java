package com.smartmaint.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "empresas", schema = "smartmaint")
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(nullable = false, unique = true, length = 100)
    private String correo;

    @Column(nullable = false, length = 50)
    private String sector;

    // 🔹 Ahora usamos el enum PlanEmpresa
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PlanEmpresa plan;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private boolean activa = false;

    @Column(unique = true, length = 255)
    private String token;

    // 🔹 Ignoramos usuarios en la serialización JSON
    @OneToMany(mappedBy = "empresa", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Usuario> usuarios;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }

    public PlanEmpresa getPlan() { return plan; }
    public void setPlan(PlanEmpresa plan) { this.plan = plan; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public List<Usuario> getUsuarios() { return usuarios; }
    public void setUsuarios(List<Usuario> usuarios) { this.usuarios = usuarios; }

    @Override
    public String toString() {
        return "Empresa{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                ", correo='" + correo + '\'' +
                ", sector='" + sector + '\'' +
                ", plan=" + plan +
                ", activa=" + activa +
                ", token='" + token + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
