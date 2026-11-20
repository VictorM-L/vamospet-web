package com.vamospet.backend.servicio;

import com.vamospet.backend.dto.ConfiguracionRequest;
import com.vamospet.backend.modelo.ConfiguracionClinica;
import com.vamospet.backend.repositorio.ConfiguracionClinicaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ConfiguracionService {

    public static final int ID = 1;

    @Autowired private ConfiguracionClinicaRepository repositorio;
    @Autowired private AutorizacionService autorizacion;

    public ConfiguracionClinica actual() {
        return repositorio.findById(ID).orElseGet(ConfiguracionClinica::new);
    }

    public int horasCancelacion() {
        Integer h = actual().getHoras_cancelacion();
        return h == null || h < 1 ? 24 : h;
    }

    public int aperturaHospedaje() {
        Integer h = actual().getHospedaje_apertura();
        return h == null ? EstanciaService.HORA_APERTURA : h;
    }

    public int cierreHospedaje() {
        Integer h = actual().getHospedaje_cierre();
        return h == null ? EstanciaService.HORA_CIERRE : h;
    }

    public Map<String, Object> publica() {
        ConfiguracionClinica c = actual();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("nombre", c.getNombre());
        m.put("direccion", c.getDireccion());
        m.put("telefono", c.getTelefono());
        m.put("whatsapp", c.getWhatsapp());
        m.put("correo", c.getCorreo());
        m.put("horario_atencion", c.getHorario_atencion());
        m.put("emergencias_24h", c.getEmergencias_24h());
        m.put("hospedaje_apertura", aperturaHospedaje());
        m.put("hospedaje_cierre", cierreHospedaje());
        m.put("horas_cancelacion", horasCancelacion());
        return m;
    }

    @Transactional
    public Map<String, Object> guardar(Integer actorId, ConfiguracionRequest r) {
        if (autorizacion.conRol(actorId, AutorizacionService.ADMIN) == null) {
            throw new NegocioException(403, "Solo el Administrador cambia la configuración de la clínica.");
        }
        if (r.hospedaje_cierre() <= r.hospedaje_apertura()) {
            throw new NegocioException(400, "El cierre del hospedaje debe ser después de la apertura.");
        }
        ConfiguracionClinica c = actual();
        c.setId(ID);
        c.setNombre(r.nombre().trim());
        c.setDireccion(r.direccion().trim());
        c.setTelefono(r.telefono().trim());
        c.setWhatsapp(r.whatsapp().trim());
        c.setCorreo(r.correo().trim());
        c.setHorario_atencion(r.horario_atencion().trim());
        c.setEmergencias_24h(r.emergencias_24h());
        c.setHospedaje_apertura(r.hospedaje_apertura());
        c.setHospedaje_cierre(r.hospedaje_cierre());
        c.setHoras_cancelacion(r.horas_cancelacion());
        c.setActualizado_por(actorId);
        c.setActualizado_en(LocalDateTime.now());
        repositorio.save(c);
        return publica();
    }
}
