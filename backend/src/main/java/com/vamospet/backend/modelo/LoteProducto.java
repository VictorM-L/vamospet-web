package com.vamospet.backend.modelo;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "lote_producto")
public class LoteProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer producto_id;
    private String numero_lote;
    private LocalDate fecha_vencimiento;
    private Integer stock_actual = 0;
    private LocalDateTime fecha_ingreso = LocalDateTime.now();

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getProducto_id() { return producto_id; }
    public void setProducto_id(Integer producto_id) { this.producto_id = producto_id; }
    public String getNumero_lote() { return numero_lote; }
    public void setNumero_lote(String numero_lote) { this.numero_lote = numero_lote; }
    public LocalDate getFecha_vencimiento() { return fecha_vencimiento; }
    public void setFecha_vencimiento(LocalDate fecha_vencimiento) { this.fecha_vencimiento = fecha_vencimiento; }
    public Integer getStock_actual() { return stock_actual; }
    public void setStock_actual(Integer stock_actual) { this.stock_actual = stock_actual; }
    public LocalDateTime getFecha_ingreso() { return fecha_ingreso; }
    public void setFecha_ingreso(LocalDateTime fecha_ingreso) { this.fecha_ingreso = fecha_ingreso; }
}
