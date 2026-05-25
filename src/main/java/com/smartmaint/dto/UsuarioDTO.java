package com.smartmaint.dto;

import com.smartmaint.model.Usuario;
import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class UsuarioDTO {

    private Long id;             // ID del usuario
    private String nombre;       // Nombre completo del usuario
    @Email
    private String correo;       // Correo institucional
    private Long rolId;          // ID del rol asignado
    private Long empresaId;      // ID de la empresa asociada
    private String rol;          // Nombre del rol (ADMIN, USUARIO, etc.)
    private String idColaborador;
    private Boolean activo;
    private String cargo;

    public static UsuarioDTO fromEntity(Usuario usuario) {
        if (usuario == null) return null;

        UsuarioDTO dto = new UsuarioDTO();
        dto.setId(usuario.getId());
        dto.setNombre(usuario.getNombre());
        dto.setCorreo(usuario.getCorreo());
        dto.setRol(usuario.getRol() != null ? usuario.getRol().getNombre() : null);
        dto.setRolId(usuario.getRol() != null ? usuario.getRol().getId() : null);
        dto.setEmpresaId(usuario.getEmpresa() != null ? usuario.getEmpresa().getId() : null);
        dto.setIdColaborador(usuario.getIdColaborador());
        dto.setActivo(usuario.getActivo());
        dto.setCargo(usuario.getCargo());
        return dto;
    }

    @Override
    public String toString() {
        return "UsuarioDTO{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                ", correo='" + correo + '\'' +
                ", rolId=" + rolId +
                ", empresaId=" + empresaId +
                ", rol='" + rol + '\'' +
                '}';
    }
}
