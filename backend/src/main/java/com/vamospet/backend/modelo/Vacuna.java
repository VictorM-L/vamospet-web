package com.vamospet.backend.modelo;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "vacuna")
public class Vacuna {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer mascota_id;
    private String nombre;
    private LocalDate fecha_aplicacion;
    private LocalDate proxima_dosis;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getMascota_id() { return mascota_id; }
    public void setMascota_id(Integer mascota_id) { this.mascota_id = mascota_id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public LocalDate getFecha_aplicacion() { return fecha_aplicacion; }
    public void setFecha_aplicacion(LocalDate fecha_aplicacion) { this.fecha_aplicacion = fecha_aplicacion; }

    public LocalDate getProxima_dosis() { return proxima_dosis; }
    public void setProxima_dosis(LocalDate proxima_dosis) { this.proxima_dosis = proxima_dosis; }
}