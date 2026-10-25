package com.vamospet.backend.modelo;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pago")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer usuario_id;
    private Integer cita_id;
    private Integer pedido_id;
    private BigDecimal monto;
    private LocalDateTime fecha = LocalDateTime.now();
    private String metodo;

    private Integer id_registrado_por;

    public Integer getId_registrado_por() { return id_registrado_por; }
    public void setId_registrado_por(Integer id_registrado_por) { this.id_registrado_por = id_registrado_por; }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUsuario_id() { return usuario_id; }
    public void setUsuario_id(Integer usuario_id) { this.usuario_id = usuario_id; }

    public Integer getCita_id() { return cita_id; }
    public void setCita_id(Integer cita_id) { this.cita_id = cita_id; }

    public Integer getPedido_id() { return pedido_id; }
    public void setPedido_id(Integer pedido_id) { this.pedido_id = pedido_id; }

    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public String getMetodo() { return metodo; }
    public void setMetodo(String metodo) { this.metodo = metodo; }
}