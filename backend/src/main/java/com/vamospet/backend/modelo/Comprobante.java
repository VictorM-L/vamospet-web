package com.vamospet.backend.modelo;

import jakarta.persistence.*;

@Entity
@Table(name = "comprobante")
public class Comprobante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer pago_id;
    private String tipo;
    private String numero;
    private String archivo;
    private String codigo_verificacion;

    public String getCodigo_verificacion() { return codigo_verificacion; }
    public void setCodigo_verificacion(String codigo_verificacion) { this.codigo_verificacion = codigo_verificacion; }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getPago_id() { return pago_id; }
    public void setPago_id(Integer pago_id) { this.pago_id = pago_id; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public String getArchivo() { return archivo; }
    public void setArchivo(String archivo) { this.archivo = archivo; }
}