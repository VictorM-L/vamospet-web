package com.vamospet.backend.modelo;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "viaje_solicitud")
public class ViajeSolicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer usuario_id;
    private LocalDate fecha_viaje;
    private String destino;
    private String documento_nombre;
    private String documento_almacenado;
    private String documento_tipo;
    private Integer cantidad;
    private BigDecimal total;
    private String estado = "Registrada";
    private String observaciones;
    private Integer revisado_por;
    private LocalDateTime fecha_solicitud = LocalDateTime.now();

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getUsuario_id() { return usuario_id; }
    public void setUsuario_id(Integer usuario_id) { this.usuario_id = usuario_id; }
    public LocalDate getFecha_viaje() { return fecha_viaje; }
    public void setFecha_viaje(LocalDate fecha_viaje) { this.fecha_viaje = fecha_viaje; }
    public String getDestino() { return destino; }
    public void setDestino(String destino) { this.destino = destino; }
    public String getDocumento_nombre() { return documento_nombre; }
    public void setDocumento_nombre(String documento_nombre) { this.documento_nombre = documento_nombre; }
    public String getDocumento_almacenado() { return documento_almacenado; }
    public void setDocumento_almacenado(String documento_almacenado) { this.documento_almacenado = documento_almacenado; }
    public String getDocumento_tipo() { return documento_tipo; }
    public void setDocumento_tipo(String documento_tipo) { this.documento_tipo = documento_tipo; }
    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public Integer getRevisado_por() { return revisado_por; }
    public void setRevisado_por(Integer revisado_por) { this.revisado_por = revisado_por; }
    public LocalDateTime getFecha_solicitud() { return fecha_solicitud; }
    public void setFecha_solicitud(LocalDateTime fecha_solicitud) { this.fecha_solicitud = fecha_solicitud; }
}
