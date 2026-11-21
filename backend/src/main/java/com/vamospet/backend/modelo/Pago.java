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
    private String estado = "Pagado";
    private String correo_tarjeta;
    private String tarjeta_ultimos4;
    private String operacion_yape;
    private Integer estancia_id;
    private Integer viaje_id;

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

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getCorreo_tarjeta() { return correo_tarjeta; }
    public void setCorreo_tarjeta(String correo_tarjeta) { this.correo_tarjeta = correo_tarjeta; }
    public String getTarjeta_ultimos4() { return tarjeta_ultimos4; }
    public void setTarjeta_ultimos4(String tarjeta_ultimos4) { this.tarjeta_ultimos4 = tarjeta_ultimos4; }
    public String getOperacion_yape() { return operacion_yape; }
    public void setOperacion_yape(String operacion_yape) { this.operacion_yape = operacion_yape; }
    public Integer getEstancia_id() { return estancia_id; }
    public void setEstancia_id(Integer estancia_id) { this.estancia_id = estancia_id; }
    public Integer getViaje_id() { return viaje_id; }
    public void setViaje_id(Integer viaje_id) { this.viaje_id = viaje_id; }
}
