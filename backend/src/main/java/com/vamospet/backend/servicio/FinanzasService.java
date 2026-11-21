package com.vamospet.backend.servicio;

import com.vamospet.backend.modelo.*;
import com.vamospet.backend.repositorio.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class FinanzasService {

    @Autowired private CitaRepository citaRepositorio;
    @Autowired private MascotaRepository mascotaRepositorio;
    @Autowired private UsuarioRepository usuarioRepositorio;
    @Autowired private ServicioRepository servicioRepositorio;
    @Autowired private EstanciaRepository estanciaRepositorio;
    @Autowired private PagoRepository pagoRepositorio;
    @Autowired private PagoService pagoService;
    @Autowired private AutorizacionService autorizacion;

    public Map<String, Object> resumen(Integer usuarioId) {
        Usuario quien = autorizacion.conRol(usuarioId, AutorizacionService.ADMIN, AutorizacionService.AUXILIAR);
        if (quien == null) throw new NegocioException(403, "Solo el administrador o el auxiliar pueden ver las finanzas.");
        List<Map<String, Object>> cuentas = cuentasPorCobrar(null);
        BigDecimal total = cuentas.stream().map(c -> (BigDecimal) c.get("total")).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Map<String, Object>> yape = new ArrayList<>();
        for (Pago p : pagoRepositorio.findAll()) {
            if (!PagoService.POR_CONFIRMAR.equals(p.getEstado())) continue;
            Usuario c = p.getUsuario_id() == null ? null : usuarioRepositorio.findById(p.getUsuario_id()).orElse(null);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("pago_id", p.getId());
            m.put("monto", p.getMonto());
            m.put("operacion", p.getOperacion_yape());
            m.put("cliente", c != null ? c.getNombre() : null);
            m.put("concepto", pagoService.describirConcepto(p));
            m.put("fecha", p.getFecha());
            yape.add(m);
        }
        yape.sort(Comparator.comparing(m -> String.valueOf(m.get("fecha"))));
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("por_cobrar_total", total);
        r.put("por_cobrar", cuentas);
        r.put("yape_pendientes", yape);
        return r;
    }

    public List<Map<String, Object>> pendientesDeCliente(Integer usuarioId) {
        Usuario quien = autorizacion.conRol(usuarioId, AutorizacionService.CLIENTE);
        if (quien == null) throw new NegocioException(403, "Inicia sesión como cliente para ver tus pagos pendientes.");
        return cuentasPorCobrar(quien.getId());
    }

    private List<Map<String, Object>> cuentasPorCobrar(Integer duenoFiltro) {
        List<Pago> pagos = pagoRepositorio.findAll();
        Set<Integer> citasConPago = new HashSet<>();
        Set<Integer> estanciasConPago = new HashSet<>();
        for (Pago p : pagos) {
            if (PagoService.RECHAZADO.equals(p.getEstado())) continue;
            if (p.getCita_id() != null) citasConPago.add(p.getCita_id());
            if (p.getEstancia_id() != null) estanciasConPago.add(p.getEstancia_id());
        }
        List<Map<String, Object>> lista = new ArrayList<>();
        for (Cita c : citaRepositorio.findAll()) {
            if (!"Atendida".equals(c.getEstado()) || citasConPago.contains(c.getId())) continue;
            Mascota m = c.getMascota_id() == null ? null : mascotaRepositorio.findById(c.getMascota_id()).orElse(null);
            if (m == null || m.getUsuario_id() == null) continue;
            if (duenoFiltro != null && !duenoFiltro.equals(m.getUsuario_id())) continue;
            Usuario d = usuarioRepositorio.findById(m.getUsuario_id()).orElse(null);
            Servicio s = c.getServicio_id() == null ? null : servicioRepositorio.findById(c.getServicio_id()).orElse(null);
            Map<String, Object> f = new LinkedHashMap<>();
            f.put("tipo", "cita");
            f.put("referencia_id", c.getId());
            f.put("concepto", "Cita #" + c.getId() + (s != null ? " — " + s.getNombre() : ""));
            f.put("mascota", m.getNombre());
            f.put("cliente", d != null ? d.getNombre() : null);
            f.put("total", pagoService.totalCita(c));
            f.put("fecha", c.getFecha_hora());
            lista.add(f);
        }
        for (Estancia e : estanciaRepositorio.findAll()) {
            if (!"Internamiento".equals(e.getTipo()) || !"Finalizada".equals(e.getEstado()) || estanciasConPago.contains(e.getId())) continue;
            if (e.getUsuario_id() == null) continue;
            if (duenoFiltro != null && !duenoFiltro.equals(e.getUsuario_id())) continue;
            Mascota m = mascotaRepositorio.findById(e.getMascota_id()).orElse(null);
            Usuario d = usuarioRepositorio.findById(e.getUsuario_id()).orElse(null);
            Map<String, Object> f = new LinkedHashMap<>();
            f.put("tipo", "estancia");
            f.put("referencia_id", e.getId());
            f.put("concepto", "Internamiento #" + e.getId());
            f.put("mascota", m != null ? m.getNombre() : null);
            f.put("cliente", d != null ? d.getNombre() : null);
            f.put("total", e.getTotal());
            f.put("fecha", e.getSalida_real());
            lista.add(f);
        }
        lista.sort(Comparator.comparing(f -> String.valueOf(f.get("fecha"))));
        return lista;
    }
}
