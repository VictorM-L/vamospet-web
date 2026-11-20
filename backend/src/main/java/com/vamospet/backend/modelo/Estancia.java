package com.vamospet.backend.modelo;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "estancia")
public class Estancia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer cuarto_id;
    private Integer mascota_id;
    private Integer usuario_id;
    private String tipo;
    private LocalDateTime ingreso;
    private LocalDateTime salida_estimada;
    private LocalDateTime salida_real;
    private String estado = "Reservada";
    private BigDecimal precio_unitario;
    private BigDecimal total = BigDecimal.ZERO;
    private Integer cirugia_id;
    private String observaciones;
    private Integer creado_por;
    private LocalDateTime fecha_creacion = LocalDateTime.now();

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getCuarto_id() { return cuarto_id; }
    public void setCuarto_id(Integer cuarto_id) { this.cuarto_id = cuarto_id; }
    public Integer getMascota_id() { return mascota_id; }
    public void setMascota_id(Integer mascota_id) { this.mascota_id = mascota_id; }
    public Integer getUsuario_id() { return usuario_id; }
    public void setUsuario_id(Integer usuario_id) { this.usuario_id = usuario_id; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public LocalDateTime getIngreso() { return ingreso; }
    public void setIngreso(LocalDateTime ingreso) { this.ingreso = ingreso; }
    public LocalDateTime getSalida_estimada() { return salida_estimada; }
    public void setSalida_estimada(LocalDateTime salida_estimada) { this.salida_estimada = salida_estimada; }
    public LocalDateTime getSalida_real() { return salida_real; }
    public void setSalida_real(LocalDateTime salida_real) { this.salida_real = salida_real; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public BigDecimal getPrecio_unitario() { return precio_unitario; }
    public void setPrecio_unitario(BigDecimal precio_unitario) { this.precio_unitario = precio_unitario; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public Integer getCirugia_id() { return cirugia_id; }
    public void setCirugia_id(Integer cirugia_id) { this.cirugia_id = cirugia_id; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public Integer getCreado_por() { return creado_por; }
    public void setCreado_por(Integer creado_por) { this.creado_por = creado_por; }
    public LocalDateTime getFecha_creacion() { return fecha_creacion; }
    public void setFecha_creacion(LocalDateTime fecha_creacion) { this.fecha_creacion = fecha_creacion; }

    @Column(length = 500)
    private String cuidados_especiales;
    public String getCuidados_especiales() { return cuidados_especiales; }
    public void setCuidados_especiales(String cuidados_especiales) { this.cuidados_especiales = cuidados_especiales; }
}
