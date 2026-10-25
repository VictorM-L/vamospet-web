package com.vamospet.backend.modelo;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pedido")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer usuario_id;
    private LocalDateTime fecha = LocalDateTime.now();
    private String estado = "Pendiente";
    private String metodo_entrega;
    private BigDecimal total;

    private String canal = "Web";

    public String getCanal() { return canal; }
    public void setCanal(String canal) { this.canal = canal; }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUsuario_id() { return usuario_id; }
    public void setUsuario_id(Integer usuario_id) { this.usuario_id = usuario_id; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getMetodo_entrega() { return metodo_entrega; }
    public void setMetodo_entrega(String metodo_entrega) { this.metodo_entrega = metodo_entrega; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
}