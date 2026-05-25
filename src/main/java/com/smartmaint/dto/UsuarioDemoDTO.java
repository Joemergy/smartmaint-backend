package com.smartmaint.dto;

public class UsuarioDemoDTO {
    private String nombre;
    private String correo;
    private String rol;
    private String empresa;

    public UsuarioDemoDTO(String nombre, String correo, String rol, String empresa) {
        this.nombre = nombre;
        this.correo = correo;
        this.rol = rol;
        this.empresa = empresa;
    }

    public String getNombre() { return nombre; }
    public String getCorreo() { return correo; }
    public String getRol() { return rol; }
    public String getEmpresa() { return empresa; }
}
