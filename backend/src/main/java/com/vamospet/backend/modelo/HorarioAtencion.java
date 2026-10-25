package com.vamospet.backend.modelo;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "horario_atencion")
public class HorarioAtencion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private Integer veterinario_id;
    private Integer dia_semana;
    private LocalTime hora_inicio;
    private LocalTime hora_fin;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getVeterinario_id() { return veterinario_id; }
    public void setVeterinario_id(Integer veterinario_id) { this.veterinario_id = veterinario_id; }
    public Integer getDia_semana() { return dia_semana; }
    public void setDia_semana(Integer dia_semana) { this.dia_semana = dia_semana; }
    public LocalTime getHora_inicio() { return hora_inicio; }
    public void setHora_inicio(LocalTime hora_inicio) { this.hora_inicio = hora_inicio; }
    public LocalTime getHora_fin() { return hora_fin; }
    public void setHora_fin(LocalTime hora_fin) { this.hora_fin = hora_fin; }
}
