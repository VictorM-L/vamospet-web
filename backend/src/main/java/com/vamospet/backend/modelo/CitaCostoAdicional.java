package com.vamospet.backend.modelo;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "cita_costo_adicional")
public class CitaCostoAdicional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private Integer cita_id;
    private String concepto;
    private BigDecimal monto;
    private Integer agregado_por;
    private LocalDateTime fecha = LocalDateTime.now();

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getCita_id() { return cita_id; }
    public void setCita_id(Integer cita_id) { this.cita_id = cita_id; }
    public String getConcepto() { return concepto; }
    public void setConcepto(String concepto) { this.concepto = concepto; }
    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }
    public Integer getAgregado_por() { return agregado_por; }
    public void setAgregado_por(Integer agregado_por) { this.agregado_por = agregado_por; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
