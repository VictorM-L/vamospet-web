package com.vamospet.backend.modelo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cirugia_programada")
public class CirugiaProgramada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer orden_id;
    private Integer cita_id;
    private Integer mascota_id;
    private Integer veterinario_id;
    private String tipo_cirugia;
    private LocalDateTime fecha_hora;
    private String estado = "Programada";
    private Integer creado_por;
    private LocalDateTime fecha_creacion = LocalDateTime.now();

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getOrden_id() { return orden_id; }
    public void setOrden_id(Integer orden_id) { this.orden_id = orden_id; }
    public Integer getCita_id() { return cita_id; }
    public void setCita_id(Integer cita_id) { this.cita_id = cita_id; }
    public Integer getMascota_id() { return mascota_id; }
    public void setMascota_id(Integer mascota_id) { this.mascota_id = mascota_id; }
    public Integer getVeterinario_id() { return veterinario_id; }
    public void setVeterinario_id(Integer veterinario_id) { this.veterinario_id = veterinario_id; }
    public String getTipo_cirugia() { return tipo_cirugia; }
    public void setTipo_cirugia(String tipo_cirugia) { this.tipo_cirugia = tipo_cirugia; }
    public LocalDateTime getFecha_hora() { return fecha_hora; }
    public void setFecha_hora(LocalDateTime fecha_hora) { this.fecha_hora = fecha_hora; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Integer getCreado_por() { return creado_por; }
    public void setCreado_por(Integer creado_por) { this.creado_por = creado_por; }
    public LocalDateTime getFecha_creacion() { return fecha_creacion; }
    public void setFecha_creacion(LocalDateTime fecha_creacion) { this.fecha_creacion = fecha_creacion; }

    private Integer servicio_id;
    public Integer getServicio_id() { return servicio_id; }
    public void setServicio_id(Integer servicio_id) { this.servicio_id = servicio_id; }

    private LocalDateTime inicio_real;
    public LocalDateTime getInicio_real() { return inicio_real; }
    public void setInicio_real(LocalDateTime inicio_real) { this.inicio_real = inicio_real; }

    private LocalDateTime fin_real;
    public LocalDateTime getFin_real() { return fin_real; }
    public void setFin_real(LocalDateTime fin_real) { this.fin_real = fin_real; }

    @Column(length = 2000)
    private String resultado;
    public String getResultado() { return resultado; }
    public void setResultado(String resultado) { this.resultado = resultado; }

    @Column(length = 2000)
    private String complicaciones;
    public String getComplicaciones() { return complicaciones; }
    public void setComplicaciones(String complicaciones) { this.complicaciones = complicaciones; }

    @Column(length = 2000)
    private String notas_intra;
    public String getNotas_intra() { return notas_intra; }
    public void setNotas_intra(String notas_intra) { this.notas_intra = notas_intra; }

    private String postoperatorio;
    public String getPostoperatorio() { return postoperatorio; }
    public void setPostoperatorio(String postoperatorio) { this.postoperatorio = postoperatorio; }
}
