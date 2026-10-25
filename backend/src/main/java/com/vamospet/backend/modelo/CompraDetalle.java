package com.vamospet.backend.modelo;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "compra_detalle")
public class CompraDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private Integer compra_id;
    private Integer producto_id;
    private Integer cantidad;
    private BigDecimal costo_unitario;
    private String numero_lote;
    private java.time.LocalDate fecha_vencimiento;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getCompra_id() { return compra_id; }
    public void setCompra_id(Integer compra_id) { this.compra_id = compra_id; }
    public Integer getProducto_id() { return producto_id; }
    public void setProducto_id(Integer producto_id) { this.producto_id = producto_id; }
    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
    public BigDecimal getCosto_unitario() { return costo_unitario; }
    public void setCosto_unitario(BigDecimal costo_unitario) { this.costo_unitario = costo_unitario; }
    public String getNumero_lote() { return numero_lote; }
    public void setNumero_lote(String numero_lote) { this.numero_lote = numero_lote; }
    public java.time.LocalDate getFecha_vencimiento() { return fecha_vencimiento; }
    public void setFecha_vencimiento(java.time.LocalDate fecha_vencimiento) { this.fecha_vencimiento = fecha_vencimiento; }
}
