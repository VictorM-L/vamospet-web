package com.vamospet.backend.modelo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "codigo_verificacion")
public class CodigoVerificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer usuario_id;
    private String codigo_hash;
    private LocalDateTime expira;
    private Integer intentos = 0;
    private Boolean usado = false;
    private LocalDateTime creado = LocalDateTime.now();

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getUsuario_id() { return usuario_id; }
    public void setUsuario_id(Integer usuario_id) { this.usuario_id = usuario_id; }
    public String getCodigo_hash() { return codigo_hash; }
    public void setCodigo_hash(String codigo_hash) { this.codigo_hash = codigo_hash; }
    public LocalDateTime getExpira() { return expira; }
    public void setExpira(LocalDateTime expira) { this.expira = expira; }
    public Integer getIntentos() { return intentos; }
    public void setIntentos(Integer intentos) { this.intentos = intentos; }
    public Boolean getUsado() { return usado; }
    public void setUsado(Boolean usado) { this.usado = usado; }
    public LocalDateTime getCreado() { return creado; }
    public void setCreado(LocalDateTime creado) { this.creado = creado; }
}
