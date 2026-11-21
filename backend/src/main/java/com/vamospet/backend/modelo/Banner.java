package com.vamospet.backend.modelo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "banner")
public class Banner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String seccion;
    private String imagen_pc;
    private String imagen_movil;
    private Integer orden = 1;
    private Boolean activo = true;
    private LocalDateTime creado_en = LocalDateTime.now();

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getSeccion() { return seccion; }
    public void setSeccion(String seccion) { this.seccion = seccion; }
    public String getImagen_pc() { return imagen_pc; }
    public void setImagen_pc(String imagen_pc) { this.imagen_pc = imagen_pc; }
    public String getImagen_movil() { return imagen_movil; }
    public void setImagen_movil(String imagen_movil) { this.imagen_movil = imagen_movil; }
    public Integer getOrden() { return orden; }
    public void setOrden(Integer orden) { this.orden = orden; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
    public LocalDateTime getCreado_en() { return creado_en; }
    public void setCreado_en(LocalDateTime creado_en) { this.creado_en = creado_en; }
}
