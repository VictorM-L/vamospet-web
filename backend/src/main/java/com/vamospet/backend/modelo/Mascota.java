package com.vamospet.backend.modelo;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDate;
@Entity
@Table(name = "mascota")
public class Mascota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer usuario_id; 
    
    private String nombre;
    private String especie;
    private String raza;
    private String sexo;
    private LocalDate fecha_nacimiento;
    private String foto;

    private String codigo_unico_temporal;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUsuario_id() { return usuario_id; }
    public void setUsuario_id(Integer usuario_id) { this.usuario_id = usuario_id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEspecie() { return especie; }
    public void setEspecie(String especie) { this.especie = especie; }

    public String getRaza() { return raza; }
    public void setRaza(String raza) { this.raza = raza; }

    public String getSexo() { return sexo; }
    public void setSexo(String sexo) { this.sexo = sexo; }

    public LocalDate getFecha_nacimiento() { return fecha_nacimiento; }
    public void setFecha_nacimiento(LocalDate fecha_nacimiento) { this.fecha_nacimiento = fecha_nacimiento; }

    public String getFoto() { return foto; }
    public void setFoto(String foto) { this.foto = foto; }

    @JsonIgnore
    public String getCodigo_unico_temporal() { return codigo_unico_temporal; }
    @JsonIgnore
    public void setCodigo_unico_temporal(String codigo_unico_temporal) { this.codigo_unico_temporal = codigo_unico_temporal; }
}
