package com.vamospet.backend.modelo;

import jakarta.persistence.*;

@Entity
@Table(name = "viaje_mascota")
public class ViajeMascota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer viaje_id;
    private Integer mascota_id;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getViaje_id() { return viaje_id; }
    public void setViaje_id(Integer viaje_id) { this.viaje_id = viaje_id; }
    public Integer getMascota_id() { return mascota_id; }
    public void setMascota_id(Integer mascota_id) { this.mascota_id = mascota_id; }
}
