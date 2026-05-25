package com.smartmaint.dto;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class UsuarioUpdateDTO {
    private String nombre;
    @Email
    private String correo;
    private String contrasena;
    private Long rolId;
    private Long empresaId; // solo si quieres mover usuarios entre empresas
}
