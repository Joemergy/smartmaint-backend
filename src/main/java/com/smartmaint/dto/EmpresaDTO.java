package com.smartmaint.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import com.smartmaint.model.Empresa;

public class EmpresaDTO {

    // 🔹 Campos para registro (POST)
    private String idEmpresa;       // Nombre institucional de la empresa

    @NotBlank(message = "El sector es obligatorio")
    @Size(max = 50, message = "El sector no puede superar 50 caracteres")
    private String sector;

    @NotBlank(message = "El correo del administrador es obligatorio")
    @Email(message = "El correo debe tener un formato válido")
    private String correoAdmin;

    @NotBlank(message = "La contraseña es obligatoria")
    private String contrasena;

    @NotBlank(message = "El plan es obligatorio")
    private String plan;

    // 🔹 Campos para listado (GET)
    private Long id;
    private String nombre;
    private String correo;
    private boolean activa;
    private String token;
    private LocalDateTime createdAt;

    public EmpresaDTO() {}

    // Mapper estático desde entidad (para GET)
    public static EmpresaDTO fromEntity(Empresa emp) {
        if (emp == null) return null;
        EmpresaDTO dto = new EmpresaDTO();
        dto.setId(emp.getId());               // ID autogenerado en BD
        dto.setNombre(emp.getNombre());       // Nombre institucional
        dto.setIdEmpresa(emp.getNombre());    // 🔹 Mapear también a idEmpresa
        dto.setCorreo(emp.getCorreo());
        dto.setSector(emp.getSector());
        dto.setPlan(emp.getPlan().name());
        dto.setActiva(emp.isActiva());
        dto.setToken(emp.getToken());
        dto.setCreatedAt(emp.getCreatedAt());
        return dto;
    }

    // Getters y setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getIdEmpresa() { return idEmpresa; }
    public void setIdEmpresa(String idEmpresa) { this.idEmpresa = idEmpresa; }
    public String getCorreoAdmin() { return correoAdmin; }
    public void setCorreoAdmin(String correoAdmin) { this.correoAdmin = correoAdmin; }
    public String getContrasena() { return contrasena; }
    public void setContrasena(String contrasena) { this.contrasena = contrasena; }
    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }
    public String getPlan() { return plan; }
    public void setPlan(String plan) { this.plan = plan; }

    @Override
    public String toString() {
        return "EmpresaDTO{" +
                "idEmpresa='" + idEmpresa + '\'' +
                ", sector='" + sector + '\'' +
                ", correoAdmin='" + correoAdmin + '\'' +
                ", plan='" + plan + '\'' +
                ", id=" + id +
                ", nombre='" + nombre + '\'' +
                ", correo='" + correo + '\'' +
                ", activa=" + activa +
                ", token='" + token + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
