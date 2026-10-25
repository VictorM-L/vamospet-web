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

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
