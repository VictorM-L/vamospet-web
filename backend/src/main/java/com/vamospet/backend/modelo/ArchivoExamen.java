package com.vamospet.backend.modelo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "archivo_examen")
public class ArchivoExamen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer orden_id;
    private String nombre_original;
    private String nombre_almacenado;
    private String tipo_contenido;
    private Integer tamano_bytes;
    private Integer subido_por;
    private LocalDateTime fecha_subida = LocalDateTime.now();

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getOrden_id() { return orden_id; }
    public void setOrden_id(Integer orden_id) { this.orden_id = orden_id; }
    public String getNombre_original() { return nombre_original; }
    public void setNombre_original(String nombre_original) { this.nombre_original = nombre_original; }
    public String getNombre_almacenado() { return nombre_almacenado; }
    public void setNombre_almacenado(String nombre_almacenado) { this.nombre_almacenado = nombre_almacenado; }
    public String getTipo_contenido() { return tipo_contenido; }
    public void setTipo_contenido(String tipo_contenido) { this.tipo_contenido = tipo_contenido; }
    public Integer getTamano_bytes() { return tamano_bytes; }
    public void setTamano_bytes(Integer tamano_bytes) { this.tamano_bytes = tamano_bytes; }
    public Integer getSubido_por() { return subido_por; }
    public void setSubido_por(Integer subido_por) { this.subido_por = subido_por; }
    public LocalDateTime getFecha_subida() { return fecha_subida; }
    public void setFecha_subida(LocalDateTime fecha_subida) { this.fecha_subida = fecha_subida; }
}
