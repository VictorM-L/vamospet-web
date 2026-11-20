package com.vamospet.backend.modelo;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity
@Table(name = "cita")
public class Cita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer mascota_id;
    private Integer servicio_id;
    private Integer especialidad_id;
    private Integer veterinario_id;

    private LocalDateTime fecha_hora;
    private String estado = "Pendiente";
    private BigDecimal costo;
    private String enlace_videollamada;
    private String motivo_estado;
    private LocalDateTime cancelacion_solicitada;
    private String motivo_solicitud;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getMascota_id() { return mascota_id; }
    public void setMascota_id(Integer mascota_id) { this.mascota_id = mascota_id; }

    public Integer getServicio_id() { return servicio_id; }
    public void setServicio_id(Integer servicio_id) { this.servicio_id = servicio_id; }

    public Integer getEspecialidad_id() { return especialidad_id; }
    public void setEspecialidad_id(Integer especialidad_id) { this.especialidad_id = especialidad_id; }

    public Integer getVeterinario_id() { return veterinario_id; }
    public void setVeterinario_id(Integer veterinario_id) { this.veterinario_id = veterinario_id; }

    public LocalDateTime getFecha_hora() { return fecha_hora; }
    public void setFecha_hora(LocalDateTime fecha_hora) { this.fecha_hora = fecha_hora; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public BigDecimal getCosto() { return costo; }
    public void setCosto(BigDecimal costo) { this.costo = costo; }

    public String getEnlace_videollamada() { return enlace_videollamada; }
    public void setEnlace_videollamada(String enlace_videollamada) { this.enlace_videollamada = enlace_videollamada; }
    public String getMotivo_estado() { return motivo_estado; }
    public void setMotivo_estado(String motivo_estado) { this.motivo_estado = motivo_estado; }
    public LocalDateTime getCancelacion_solicitada() { return cancelacion_solicitada; }
    public void setCancelacion_solicitada(LocalDateTime cancelacion_solicitada) { this.cancelacion_solicitada = cancelacion_solicitada; }
    public String getMotivo_solicitud() { return motivo_solicitud; }
    public void setMotivo_solicitud(String motivo_solicitud) { this.motivo_solicitud = motivo_solicitud; }

    private LocalDateTime inicio_real;
    public LocalDateTime getInicio_real() { return inicio_real; }
    public void setInicio_real(LocalDateTime inicio_real) { this.inicio_real = inicio_real; }

    private LocalDateTime fin_real;
    public LocalDateTime getFin_real() { return fin_real; }
    public void setFin_real(LocalDateTime fin_real) { this.fin_real = fin_real; }
}
