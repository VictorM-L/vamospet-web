package com.vamospet.backend.modelo;

import jakarta.persistence.*;

@Entity
@Table(name = "producto_imagen")
public class ProductoImagen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer producto_id;
    private String url;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getProducto_id() { return producto_id; }
    public void setProducto_id(Integer producto_id) { this.producto_id = producto_id; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    private Boolean activo = true;

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
