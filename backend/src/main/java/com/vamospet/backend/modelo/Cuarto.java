package com.vamospet.backend.modelo;

import jakarta.persistence.*;

@Entity
@Table(name = "cuarto")
public class Cuarto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer numero;
    private String nombre;
    private Boolean activo = true;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getNumero() { return numero; }
    public void setNumero(Integer numero) { this.numero = numero; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
