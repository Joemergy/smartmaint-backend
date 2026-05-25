package com.smartmaint.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PlanCompraRequestDTO {

    @NotBlank
    private String nombre;

    @NotBlank
    private String apellido;

    @NotBlank
    private String idInstitucional;

    @NotBlank
    private String tipoDocumento;

    @NotBlank
    private String numeroDocumento;

    @NotBlank
    private String telefonoCelular;

    @Email
    @NotBlank
    private String correoPersonal;

    @Email
    private String correoInstitucional;

    @NotBlank
    private String plan;
}
