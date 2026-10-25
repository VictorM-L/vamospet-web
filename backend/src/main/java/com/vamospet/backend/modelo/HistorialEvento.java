package com.vamospet.backend.modelo;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "historial_evento")
public class HistorialEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer mascota_id;
    private String tipo_evento;
    
    private LocalDateTime fecha = LocalDateTime.now();
    
    private String descripcion;
    private Integer veterinario_id;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getMascota_id() { return mascota_id; }
    public void setMascota_id(Integer mascota_id) { this.mascota_id = mascota_id; }

    public String getTipo_evento() { return tipo_evento; }
    public void setTipo_evento(String tipo_evento) { this.tipo_evento = tipo_evento; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Integer getVeterinario_id() { return veterinario_id; }
    public void setVeterinario_id(Integer veterinario_id) { this.veterinario_id = veterinario_id; }
}