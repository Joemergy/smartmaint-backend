package com.smartmaint.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UsuarioCreateDTO {

    @NotBlank
    private String nombre;

    @NotBlank
    @Email
    private String correo;

    @NotBlank
    private String contrasena;

    @NotNull
    private Long rolId;

    @NotBlank
    private String idColaborador;

    @NotNull
    private Boolean activo;

    @NotBlank
    private String cargo;

    @NotBlank
    private String area;

    @NotBlank
    private String telefono;

    @NotNull
    private LocalDate fechaIngreso;

    private String direccion;

    @NotBlank
    private String fotoPerfil;
}
