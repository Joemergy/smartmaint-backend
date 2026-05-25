package com.smartmaint.dto;

import lombok.Data;

@Data
public class EquipoCreateDTO {
    private String nombre;     // obligatorio
    private String tipo;       // opcional
    private String ubicacion;  // opcional
    private String categoria;  // opcional
    private String descripcion; // opcional
}
