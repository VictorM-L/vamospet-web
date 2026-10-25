package com.vamospet.backend.modelo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notificacion")
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer usuario_id;
    private String titulo;
    private String mensaje;
    private String tipo;
    private Boolean leida = false;
    private LocalDateTime fecha = LocalDateTime.now();

    private LocalDateTime fecha_programada;
    private Boolean enviado = true;
    private LocalDateTime fecha_envio;
    private String referencia;

    public LocalDateTime getFecha_programada() { return fecha_programada; }
    public void setFecha_programada(LocalDateTime fecha_programada) { this.fecha_programada = fecha_programada; }
    public Boolean getEnviado() { return enviado; }
    public void setEnviado(Boolean enviado) { this.enviado = enviado; }
    public LocalDateTime getFecha_envio() { return fecha_envio; }
    public void setFecha_envio(LocalDateTime fecha_envio) { this.fecha_envio = fecha_envio; }
    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUsuario_id() { return usuario_id; }
    public void setUsuario_id(Integer usuario_id) { this.usuario_id = usuario_id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public Boolean getLeida() { return leida; }
    public void setLeida(Boolean leida) { this.leida = leida; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
