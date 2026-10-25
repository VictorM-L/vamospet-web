package com.vamospet.backend.modelo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "contacto_emergencia")
public class ContactoEmergencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer mascota_id;
    private String codigo_emitido;
    private String nombre_acompanante;
    private String telefono;
    private String motivo;
    private LocalDateTime fecha_ingreso = LocalDateTime.now();
    private Integer registrado_por;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getMascota_id() { return mascota_id; }
    public void setMascota_id(Integer mascota_id) { this.mascota_id = mascota_id; }
    public String getCodigo_emitido() { return codigo_emitido; }
    public void setCodigo_emitido(String codigo_emitido) { this.codigo_emitido = codigo_emitido; }
    public String getNombre_acompanante() { return nombre_acompanante; }
    public void setNombre_acompanante(String nombre_acompanante) { this.nombre_acompanante = nombre_acompanante; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public LocalDateTime getFecha_ingreso() { return fecha_ingreso; }
    public void setFecha_ingreso(LocalDateTime fecha_ingreso) { this.fecha_ingreso = fecha_ingreso; }
    public Integer getRegistrado_por() { return registrado_por; }
    public void setRegistrado_por(Integer registrado_por) { this.registrado_por = registrado_por; }
}
