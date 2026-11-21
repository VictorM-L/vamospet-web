package com.vamospet.backend.modelo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "mensaje_contacto")
public class MensajeContacto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String nombre;
    private String correo;
    private String telefono;
    private String asunto;
    @Column(length = 2000)
    private String mensaje;
    private String estado = "Nuevo";
    @Column(length = 2000)
    private String respuesta;
    private Integer respondido_por;
    private String ip_origen;
    private LocalDateTime creado_en = LocalDateTime.now();
    private LocalDateTime respondido_en;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getAsunto() { return asunto; }
    public void setAsunto(String asunto) { this.asunto = asunto; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getRespuesta() { return respuesta; }
    public void setRespuesta(String respuesta) { this.respuesta = respuesta; }
    public Integer getRespondido_por() { return respondido_por; }
    public void setRespondido_por(Integer respondido_por) { this.respondido_por = respondido_por; }
    public String getIp_origen() { return ip_origen; }
    public void setIp_origen(String ip_origen) { this.ip_origen = ip_origen; }
    public LocalDateTime getCreado_en() { return creado_en; }
    public void setCreado_en(LocalDateTime creado_en) { this.creado_en = creado_en; }
    public LocalDateTime getRespondido_en() { return respondido_en; }
    public void setRespondido_en(LocalDateTime respondido_en) { this.respondido_en = respondido_en; }
}
