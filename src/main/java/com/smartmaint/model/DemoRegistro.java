package com.smartmaint.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "demo_registros")
public class DemoRegistro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombreSolicitante;
    private String empresaSolicitada;
    private String ipSolicitante;
    private LocalDateTime fechaSolicitud;

    public DemoRegistro() {}

    public DemoRegistro(String nombreSolicitante, String empresaSolicitada, String ipSolicitante, LocalDateTime fechaSolicitud) {
        this.nombreSolicitante = nombreSolicitante;
        this.empresaSolicitada = empresaSolicitada;
        this.ipSolicitante = ipSolicitante;
        this.fechaSolicitud = fechaSolicitud;
    }

    public Long getId() { return id; }
    public String getNombreSolicitante() { return nombreSolicitante; }
    public String getEmpresaSolicitada() { return empresaSolicitada; }
    public String getIpSolicitante() { return ipSolicitante; }
    public LocalDateTime getFechaSolicitud() { return fechaSolicitud; }
}
