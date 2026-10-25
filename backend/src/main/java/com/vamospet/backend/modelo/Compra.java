package com.vamospet.backend.modelo;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "compra")
public class Compra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private Integer proveedor_id;
    private Integer usuario_id;
    private LocalDateTime fecha = LocalDateTime.now();
    private BigDecimal total;
    private String observacion;
    private String estado = "Registrada";

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getProveedor_id() { return proveedor_id; }
    public void setProveedor_id(Integer proveedor_id) { this.proveedor_id = proveedor_id; }
    public Integer getUsuario_id() { return usuario_id; }
    public void setUsuario_id(Integer usuario_id) { this.usuario_id = usuario_id; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
