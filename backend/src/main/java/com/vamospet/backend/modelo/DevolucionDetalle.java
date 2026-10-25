package com.vamospet.backend.modelo;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "devolucion_detalle")
public class DevolucionDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private Integer devolucion_id;
    private Integer producto_id;
    private Integer cantidad;
    private BigDecimal precio_unitario;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getDevolucion_id() { return devolucion_id; }
    public void setDevolucion_id(Integer devolucion_id) { this.devolucion_id = devolucion_id; }
    public Integer getProducto_id() { return producto_id; }
    public void setProducto_id(Integer producto_id) { this.producto_id = producto_id; }
    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
    public BigDecimal getPrecio_unitario() { return precio_unitario; }
    public void setPrecio_unitario(BigDecimal precio_unitario) { this.precio_unitario = precio_unitario; }
}
