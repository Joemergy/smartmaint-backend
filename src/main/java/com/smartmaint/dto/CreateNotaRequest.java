package com.smartmaint.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateNotaRequest {

    @NotBlank(message = "El texto de la nota es obligatorio")
    @Size(max = 4000, message = "El texto de la nota no puede superar 4000 caracteres")
    private String texto;

    @Size(max = 150, message = "El nombre del autor no puede superar 150 caracteres")
    private String autorNombre;

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public String getAutorNombre() {
        return autorNombre;
    }

    public void setAutorNombre(String autorNombre) {
        this.autorNombre = autorNombre;
    }
}
