package com.vamospet.backend.modelo;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "servicio")
public class Servicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String nombre;
    private String descripcion;
    private BigDecimal precio_base;
    private Boolean requiere_pago_previo = false;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public BigDecimal getPrecio_base() { return precio_base; }
    public void setPrecio_base(BigDecimal precio_base) { this.precio_base = precio_base; }

    public Boolean getRequiere_pago_previo() { return requiere_pago_previo; }
    public void setRequiere_pago_previo(Boolean requiere_pago_previo) { this.requiere_pago_previo = requiere_pago_previo; }

    private Boolean activo = true;
    private String tipo = "Cita";

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    private Integer duracion_min;
    public Integer getDuracion_min() { return duracion_min; }
    public void setDuracion_min(Integer duracion_min) { this.duracion_min = duracion_min; }

    private Integer cupo_diario;
    public Integer getCupo_diario() { return cupo_diario; }
    public void setCupo_diario(Integer cupo_diario) { this.cupo_diario = cupo_diario; }
}
