package com.vamospet.backend.modelo;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "receta")
public class Receta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer mascota_id;
    private Integer veterinario_id;
    
    private LocalDateTime fecha = LocalDateTime.now();
    
    private String contenido;
    private String archivo_url;
    private String estado = "Pendiente"; 
    private Boolean usada = false;
    private LocalDateTime usada_en;
    private LocalDateTime aprobada_en;
    private Integer pedido_id;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getMascota_id() { return mascota_id; }
    public void setMascota_id(Integer mascota_id) { this.mascota_id = mascota_id; }

    public Integer getVeterinario_id() { return veterinario_id; }
    public void setVeterinario_id(Integer veterinario_id) { this.veterinario_id = veterinario_id; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }

    public String getArchivo_url() { return archivo_url; }
    public void setArchivo_url(String archivo_url) { this.archivo_url = archivo_url; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public Boolean getUsada() { return usada; }
    public void setUsada(Boolean usada) { this.usada = usada; }
    public LocalDateTime getUsada_en() { return usada_en; }
    public void setUsada_en(LocalDateTime usada_en) { this.usada_en = usada_en; }
    public LocalDateTime getAprobada_en() { return aprobada_en; }
    public void setAprobada_en(LocalDateTime aprobada_en) { this.aprobada_en = aprobada_en; }
    public Integer getPedido_id() { return pedido_id; }
    public void setPedido_id(Integer pedido_id) { this.pedido_id = pedido_id; }
}
