package com.vamospet.backend.modelo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "orden_examen")
public class OrdenExamen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer mascota_id;
    private Integer cita_id;
    private Integer veterinario_id;
    private String tipo_examen;
    private String cirugia_prevista;
    private String indicaciones;
    private String estado = "Pendiente";
    private LocalDateTime fecha_solicitud = LocalDateTime.now();
    private Integer evaluado_por;
    private LocalDateTime fecha_evaluacion;
    private String observaciones;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getMascota_id() { return mascota_id; }
    public void setMascota_id(Integer mascota_id) { this.mascota_id = mascota_id; }
    public Integer getCita_id() { return cita_id; }
    public void setCita_id(Integer cita_id) { this.cita_id = cita_id; }
    public Integer getVeterinario_id() { return veterinario_id; }
    public void setVeterinario_id(Integer veterinario_id) { this.veterinario_id = veterinario_id; }
    public String getTipo_examen() { return tipo_examen; }
    public void setTipo_examen(String tipo_examen) { this.tipo_examen = tipo_examen; }
    public String getCirugia_prevista() { return cirugia_prevista; }
    public void setCirugia_prevista(String cirugia_prevista) { this.cirugia_prevista = cirugia_prevista; }
    public String getIndicaciones() { return indicaciones; }
    public void setIndicaciones(String indicaciones) { this.indicaciones = indicaciones; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public LocalDateTime getFecha_solicitud() { return fecha_solicitud; }
    public void setFecha_solicitud(LocalDateTime fecha_solicitud) { this.fecha_solicitud = fecha_solicitud; }
    public Integer getEvaluado_por() { return evaluado_por; }
    public void setEvaluado_por(Integer evaluado_por) { this.evaluado_por = evaluado_por; }
    public LocalDateTime getFecha_evaluacion() { return fecha_evaluacion; }
    public void setFecha_evaluacion(LocalDateTime fecha_evaluacion) { this.fecha_evaluacion = fecha_evaluacion; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
}
