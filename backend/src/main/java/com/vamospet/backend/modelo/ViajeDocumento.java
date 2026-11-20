package com.vamospet.backend.modelo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "viaje_documento")
public class ViajeDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private Integer viaje_id;
    private String requisito;
    private String nombre_original;
    private String nombre_almacenado;
    private String tipo_contenido;
    private Integer tamano_bytes;
    private Boolean vigente = true;
    private String estado = "Pendiente";
    private Integer subido_por;
    private LocalDateTime subido_en = LocalDateTime.now();

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getViaje_id() { return viaje_id; }
    public void setViaje_id(Integer viaje_id) { this.viaje_id = viaje_id; }
    public String getRequisito() { return requisito; }
    public void setRequisito(String requisito) { this.requisito = requisito; }
    public String getNombre_original() { return nombre_original; }
    public void setNombre_original(String nombre_original) { this.nombre_original = nombre_original; }
    public String getNombre_almacenado() { return nombre_almacenado; }
    public void setNombre_almacenado(String nombre_almacenado) { this.nombre_almacenado = nombre_almacenado; }
    public String getTipo_contenido() { return tipo_contenido; }
    public void setTipo_contenido(String tipo_contenido) { this.tipo_contenido = tipo_contenido; }
    public Integer getTamano_bytes() { return tamano_bytes; }
    public void setTamano_bytes(Integer tamano_bytes) { this.tamano_bytes = tamano_bytes; }
    public Boolean getVigente() { return vigente; }
    public void setVigente(Boolean vigente) { this.vigente = vigente; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Integer getSubido_por() { return subido_por; }
    public void setSubido_por(Integer subido_por) { this.subido_por = subido_por; }
    public LocalDateTime getSubido_en() { return subido_en; }
    public void setSubido_en(LocalDateTime subido_en) { this.subido_en = subido_en; }
}
