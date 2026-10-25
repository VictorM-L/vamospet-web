package com.vamospet.backend.modelo;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "devolucion")
public class Devolucion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private Integer pedido_id;
    private Integer usuario_id;
    private LocalDateTime fecha = LocalDateTime.now();
    private String motivo;
    private BigDecimal total_reembolso;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getPedido_id() { return pedido_id; }
    public void setPedido_id(Integer pedido_id) { this.pedido_id = pedido_id; }
    public Integer getUsuario_id() { return usuario_id; }
    public void setUsuario_id(Integer usuario_id) { this.usuario_id = usuario_id; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public BigDecimal getTotal_reembolso() { return total_reembolso; }
    public void setTotal_reembolso(BigDecimal total_reembolso) { this.total_reembolso = total_reembolso; }
}
