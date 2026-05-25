package com.smartmaint.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateEstadoRequest {

    @NotBlank(message = "El estado es obligatorio")
    @Size(max = 20, message = "El estado no puede superar 20 caracteres")
    private String estado;

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
