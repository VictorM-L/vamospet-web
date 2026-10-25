package com.vamospet.backend.servicio;

import com.vamospet.backend.modelo.*;
import com.vamospet.backend.repositorio.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class EmergenciaService {

    public static final String PREFIJO = "VET-PET-";
    private static final Pattern NUMERO = Pattern.compile("^" + PREFIJO + "(\\d+)$");
    private static final Pattern TELEFONO = Pattern.compile("^\\+?\\d{7,15}$");

    @Autowired private MascotaRepository mascotaRepositorio;
    @Autowired private ContactoEmergenciaRepository contactoRepositorio;
    @Autowired private HistorialEventoRepository historialRepositorio;
    @Autowired private AutorizacionService autorizacion;

    public record DatosEmergencia(String nombreAcompanante, String telefono, String mascotaNombre, String especie,
                                  String raza, String sexo, String motivo) {}

    @Transactional
    public Mascota registrar(Integer usuarioId, DatosEmergencia d) {
        Usuario quien = autorizacion.conRol(usuarioId, AutorizacionService.ADMIN, AutorizacionService.VETERINARIO, AutorizacionService.AUXILIAR);
        if (quien == null) throw new NegocioException(403, "Solo el personal de la clínica puede registrar un ingreso por emergencia.");

        String acompanante = limpiar(d.nombreAcompanante());
        String telefono = d.telefono() == null ? null : d.telefono().replaceAll("[\\s-]", "");
        String nombre = limpiar(d.mascotaNombre());
        String especie = limpiar(d.especie());
        if (acompanante == null || acompanante.length() > 120) throw new NegocioException(400, "Indica el nombre del acompañante (máximo 120 caracteres).");
        if (telefono == null || !TELEFONO.matcher(telefono).matches()) throw new NegocioException(400, "El teléfono del acompañante no es válido (solo números, 7 a 15 dígitos).");
        if (nombre == null || nombre.length() > 100) throw new NegocioException(400, "Indica el nombre de la mascota (o un nombre provisional).");
        if (especie == null || especie.length() > 50) throw new NegocioException(400, "Indica la especie de la mascota.");
        String motivo = limpiar(d.motivo());
        if (motivo != null && motivo.length() > 300) motivo = motivo.substring(0, 300);

        Mascota guardada = null;
        for (int intento = 0; intento < 5 && guardada == null; intento++) {
            String codigo = siguienteCodigo();
            Mascota m = new Mascota();
            m.setUsuario_id(null);
            m.setNombre(nombre);
            m.setEspecie(especie);
            m.setRaza(recortar(limpiar(d.raza()), 50));
            m.setSexo(recortar(limpiar(d.sexo()), 20));
            m.setCodigo_unico_temporal(codigo);
            try {
                guardada = mascotaRepositorio.saveAndFlush(m);
                ContactoEmergencia c = new ContactoEmergencia();
                c.setMascota_id(guardada.getId());
                c.setCodigo_emitido(codigo);
                c.setNombre_acompanante(acompanante);
                c.setTelefono(telefono);
                c.setMotivo(motivo);
                c.setRegistrado_por(quien.getId());
                contactoRepositorio.saveAndFlush(c);
            } catch (DataIntegrityViolationException ex) {
                throw new NegocioException(409, "Se registró otro ingreso al mismo tiempo. Intenta de nuevo.");
            }
        }
        if (guardada == null) throw new NegocioException(409, "No se pudo generar el código de emergencia. Intenta de nuevo.");

        HistorialEvento e = new HistorialEvento();
        e.setMascota_id(guardada.getId());
        e.setTipo_evento("Ingreso por emergencia");
        e.setFecha(LocalDateTime.now());
        e.setDescripcion("Ingreso de emergencia" + (motivo != null ? ": " + motivo : "") + ". Acompañante: " + acompanante
                + ". Registró: " + quien.getNombre());
        e.setVeterinario_id(quien.getRol_id() == AutorizacionService.VETERINARIO ? quien.getId() : null);
        historialRepositorio.save(e);
        return guardada;
    }

    @Transactional
    public Mascota vincular(Integer usuarioId, String codigo) {
        Usuario cliente = autorizacion.conRol(usuarioId, AutorizacionService.CLIENTE);
        if (cliente == null) throw new NegocioException(403, "Solo un cliente puede vincular una mascota a su cuenta.");
        String c = limpiar(codigo);
        if (c == null) throw new NegocioException(400, "Escribe el código de tu ticket (ej. VET-PET-1045).");
        String normal = c.toUpperCase(Locale.ROOT);
        Mascota m = mascotaRepositorio.findAll().stream()
                .filter(x -> normal.equals(x.getCodigo_unico_temporal()))
                .findFirst().orElse(null);
        if (m == null || m.getUsuario_id() != null) {
            throw new NegocioException(404, "El código no existe o ya fue utilizado. Revisa tu ticket o consulta en recepción.");
        }
        m.setUsuario_id(cliente.getId());
        m.setCodigo_unico_temporal(null);
        Mascota guardada = mascotaRepositorio.save(m);

        HistorialEvento e = new HistorialEvento();
        e.setMascota_id(guardada.getId());
        e.setTipo_evento("Vinculación a cuenta");
        e.setFecha(LocalDateTime.now());
        e.setDescripcion("La mascota fue vinculada a la cuenta de " + cliente.getNombre() + " mediante su código de emergencia.");
        historialRepositorio.save(e);
        return guardada;
    }

    public List<Map<String, Object>> sinVincular(Integer usuarioId) {
        if (autorizacion.conRol(usuarioId, AutorizacionService.ADMIN, AutorizacionService.VETERINARIO, AutorizacionService.AUXILIAR) == null) {
            throw new NegocioException(403, "No tienes permiso para ver los ingresos por emergencia.");
        }
        Map<Integer, ContactoEmergencia> contactos = new HashMap<>();
        contactoRepositorio.findAll().forEach(c -> contactos.put(c.getMascota_id(), c));
        List<Map<String, Object>> salida = new ArrayList<>();
        mascotaRepositorio.findAll().stream()
                .filter(m -> m.getUsuario_id() == null && m.getCodigo_unico_temporal() != null)
                .sorted(Comparator.comparing(Mascota::getId).reversed())
                .forEach(m -> {
                    ContactoEmergencia c = contactos.get(m.getId());
                    Map<String, Object> f = new LinkedHashMap<>();
                    f.put("mascota_id", m.getId());
                    f.put("codigo", m.getCodigo_unico_temporal());
                    f.put("mascota_nombre", m.getNombre());
                    f.put("especie", m.getEspecie());
                    f.put("acompanante", c != null ? c.getNombre_acompanante() : null);
                    f.put("telefono", c != null ? c.getTelefono() : null);
                    f.put("motivo", c != null ? c.getMotivo() : null);
                    f.put("fecha_ingreso", c != null ? c.getFecha_ingreso() : null);
                    salida.add(f);
                });
        return salida;
    }

    public Map<String, Object> datosTicket(Integer usuarioId, Integer mascotaId) {
        if (autorizacion.conRol(usuarioId, AutorizacionService.ADMIN, AutorizacionService.VETERINARIO, AutorizacionService.AUXILIAR) == null) {
            throw new NegocioException(403, "No tienes permiso para emitir tickets de emergencia.");
        }
        Mascota m = mascotaId == null ? null : mascotaRepositorio.findById(mascotaId).orElse(null);
        if (m == null) throw new NegocioException(404, "La mascota no existe.");
        if (m.getCodigo_unico_temporal() == null) throw new NegocioException(409, "Esta mascota ya fue vinculada a un dueño; su código dejó de ser válido.");
        ContactoEmergencia c = contactoRepositorio.findAll().stream().filter(x -> mascotaId.equals(x.getMascota_id())).findFirst().orElse(null);
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("codigo", m.getCodigo_unico_temporal());
        d.put("mascota", m);
        d.put("contacto", c);
        return d;
    }

    private String siguienteCodigo() {
        int max = 1000;
        for (ContactoEmergencia c : contactoRepositorio.findAll()) {
            if (c.getCodigo_emitido() == null) continue;
            Matcher mt = NUMERO.matcher(c.getCodigo_emitido());
            if (mt.matches()) max = Math.max(max, Integer.parseInt(mt.group(1)));
        }
        return PREFIJO + (max + 1);
    }

    private static String limpiar(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String recortar(String s, int max) {
        return s == null ? null : (s.length() > max ? s.substring(0, max) : s);
    }
}
