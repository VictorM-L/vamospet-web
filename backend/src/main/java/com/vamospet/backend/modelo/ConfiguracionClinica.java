package com.vamospet.backend.modelo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "configuracion_clinica")
public class ConfiguracionClinica {

    @Id
    private Integer id = 1;
    private String nombre = "";
    private String direccion = "";
    private String telefono = "";
    private String whatsapp = "";
    private String correo = "";
    private String horario_atencion = "";
    private Boolean emergencias_24h = true;
    private Integer hospedaje_apertura = 8;
    private Integer hospedaje_cierre = 20;
    private Integer horas_cancelacion = 24;
    private Integer actualizado_por;
    private LocalDateTime actualizado_en = LocalDateTime.now();

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getWhatsapp() { return whatsapp; }
    public void setWhatsapp(String whatsapp) { this.whatsapp = whatsapp; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getHorario_atencion() { return horario_atencion; }
    public void setHorario_atencion(String horario_atencion) { this.horario_atencion = horario_atencion; }
    public Boolean getEmergencias_24h() { return emergencias_24h; }
    public void setEmergencias_24h(Boolean emergencias_24h) { this.emergencias_24h = emergencias_24h; }
    public Integer getHospedaje_apertura() { return hospedaje_apertura; }
    public void setHospedaje_apertura(Integer hospedaje_apertura) { this.hospedaje_apertura = hospedaje_apertura; }
    public Integer getHospedaje_cierre() { return hospedaje_cierre; }
    public void setHospedaje_cierre(Integer hospedaje_cierre) { this.hospedaje_cierre = hospedaje_cierre; }
    public Integer getHoras_cancelacion() { return horas_cancelacion; }
    public void setHoras_cancelacion(Integer horas_cancelacion) { this.horas_cancelacion = horas_cancelacion; }
    public Integer getActualizado_por() { return actualizado_por; }
    public void setActualizado_por(Integer actualizado_por) { this.actualizado_por = actualizado_por; }
    public LocalDateTime getActualizado_en() { return actualizado_en; }
    public void setActualizado_en(LocalDateTime actualizado_en) { this.actualizado_en = actualizado_en; }
}
