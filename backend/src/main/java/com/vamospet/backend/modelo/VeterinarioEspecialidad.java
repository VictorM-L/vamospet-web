package com.vamospet.backend.modelo;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "veterinario_especialidad")
public class VeterinarioEspecialidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private Integer veterinario_id;
    private Integer especialidad_id;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getVeterinario_id() { return veterinario_id; }
    public void setVeterinario_id(Integer veterinario_id) { this.veterinario_id = veterinario_id; }
    public Integer getEspecialidad_id() { return especialidad_id; }
    public void setEspecialidad_id(Integer especialidad_id) { this.especialidad_id = especialidad_id; }
}
