package com.vamospet.backend.modelo;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "producto")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String nombre;
    private String descripcion;
    private String sku;

    private BigDecimal precio;
    private BigDecimal precio_costo;

    private Integer stock = 0;
    private Integer stock_minimo = 5;

    private Boolean requiere_receta = false;
    private String tipo;
    private String marca;
    private String imagen;
    private String unidad_presentacion;
    private String proveedor;

    private LocalDate fecha_vencimiento;

    private String estado = "Activo";

    private Integer categoria_id;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }

    public BigDecimal getPrecio_costo() { return precio_costo; }
    public void setPrecio_costo(BigDecimal precio_costo) { this.precio_costo = precio_costo; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public Integer getStock_minimo() { return stock_minimo; }
    public void setStock_minimo(Integer stock_minimo) { this.stock_minimo = stock_minimo; }

    public Boolean getRequiere_receta() { return requiere_receta; }
    public void setRequiere_receta(Boolean requiere_receta) { this.requiere_receta = requiere_receta; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public String getImagen() { return imagen; }
    public void setImagen(String imagen) { this.imagen = imagen; }

    public String getUnidad_presentacion() { return unidad_presentacion; }
    public void setUnidad_presentacion(String unidad_presentacion) { this.unidad_presentacion = unidad_presentacion; }

    public String getProveedor() { return proveedor; }
    public void setProveedor(String proveedor) { this.proveedor = proveedor; }

    public LocalDate getFecha_vencimiento() { return fecha_vencimiento; }
    public void setFecha_vencimiento(LocalDate fecha_vencimiento) { this.fecha_vencimiento = fecha_vencimiento; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public Integer getCategoria_id() { return categoria_id; }
    public void setCategoria_id(Integer categoria_id) { this.categoria_id = categoria_id; }

    private Boolean activo = true;

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
