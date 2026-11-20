package com.vamospet.backend.servicio;

import com.vamospet.backend.modelo.*;
import com.vamospet.backend.repositorio.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;

@Service
public class ViajeService {

    public static final String SERVICIO_BASE = "Viaja con tu mascota";
    public static final String SERVICIO_ADICIONAL = "Viaja con tu mascota - mascota adicional";
    public static final int MAXIMO_MASCOTAS = 6;
    static final Path CARPETA = Paths.get("uploads_clinicos");
    public static final String ACEPTADO = "Aceptado";
    public static final String OBSERVADO = "Observado";
    public static final String PENDIENTE = "Pendiente";
    public static final Map<String, String> REQUISITOS;

    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("certificado_salud", "Certificado de salud");
        m.put("microchip", "Constancia de microchip");
        m.put("vacunas", "Carné de vacunas");
        REQUISITOS = Collections.unmodifiableMap(m);
    }

    public record Archivo(String nombre, byte[] contenido) {}

    @Autowired private ViajeSolicitudRepository viajeRepositorio;
    @Autowired private ViajeMascotaRepository viajeMascotaRepositorio;
    @Autowired private MascotaRepository mascotaRepositorio;
    @Autowired private UsuarioRepository usuarioRepositorio;
    @Autowired private ServicioRepository servicioRepositorio;
    @Autowired private PagoRepository pagoRepositorio;
    @Autowired private ComprobanteRepository comprobanteRepositorio;
    @Autowired private AutorizacionService autorizacion;
    @Autowired private PagoService pagoService;
    @Autowired private NotificacionService notificacionService;
    @Autowired private ViajeDocumentoRepository documentoRepositorio;

    public static BigDecimal calcularTotal(BigDecimal base, BigDecimal adicional, int cantidad) {
        return base.add(adicional.multiply(BigDecimal.valueOf(Math.max(0, cantidad - 1))));
    }

    public List<Map<String, String>> requisitos() {
        List<Map<String, String>> lista = new ArrayList<>();
        REQUISITOS.forEach((clave, etiqueta) -> lista.add(Map.of("clave", clave, "etiqueta", etiqueta)));
        return lista;
    }

    public Map<String, Object> tarifa() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("base", precio(SERVICIO_BASE));
        r.put("adicional", precio(SERVICIO_ADICIONAL));
        r.put("maximo_mascotas", MAXIMO_MASCOTAS);
        return r;
    }

    @Transactional
    public synchronized Map<String, Object> solicitar(Integer actorId, List<Integer> mascotaIds, LocalDate fechaViaje, String destino,
                                                      Map<String, Archivo> documentos, PagoService.DatosPago datosPago) {
        Usuario actor = autorizacion.conRol(actorId, AutorizacionService.CLIENTE);
        if (actor == null) throw new NegocioException(403, "Inicia sesión como cliente para solicitar un viaje.");
        if (mascotaIds == null || mascotaIds.isEmpty()) throw new NegocioException(400, "Elige al menos una mascota para viajar.");
        Set<Integer> unicas = new LinkedHashSet<>(mascotaIds);
        if (unicas.size() > MAXIMO_MASCOTAS) throw new NegocioException(400, "Puedes llevar hasta " + MAXIMO_MASCOTAS + " mascotas por viaje.");
        if (fechaViaje == null || fechaViaje.isBefore(LocalDate.now())) throw new NegocioException(400, "Indica una fecha de viaje que no haya pasado.");
        String dest = destino == null ? "" : destino.trim();
        if (dest.length() < 2 || dest.length() > 150) throw new NegocioException(400, "Indica el destino del viaje.");
        Map<String, Archivo> recibidos = presentes(documentos);
        List<String> faltan = REQUISITOS.keySet().stream().filter(k -> !recibidos.containsKey(k)).map(REQUISITOS::get).toList();
        if (!faltan.isEmpty()) throw new NegocioException(400, "Falta adjuntar: " + String.join(", ", faltan) + ".");
        for (Integer id : unicas) {
            Mascota m = id == null ? null : mascotaRepositorio.findById(id).orElse(null);
            if (m == null || !actor.getId().equals(m.getUsuario_id())) throw new NegocioException(403, "Solo puedes elegir tus propias mascotas.");
        }
        BigDecimal base = precio(SERVICIO_BASE);
        BigDecimal adicional = precio(SERVICIO_ADICIONAL);
        if (base.signum() <= 0) throw new NegocioException(409, "La tarifa de viaje aún no está configurada.");

        Map<String, ArchivoClinicoValidador.Tipo> tipos = new LinkedHashMap<>();
        for (Map.Entry<String, Archivo> e : recibidos.entrySet()) {
            tipos.put(e.getKey(), validar(e.getKey(), e.getValue()));
        }
        List<String> guardados = new ArrayList<>();
        try {
            ViajeSolicitud v = new ViajeSolicitud();
            v.setUsuario_id(actor.getId());
            v.setFecha_viaje(fechaViaje);
            v.setDestino(dest);
            v.setCantidad(unicas.size());
            v.setTotal(calcularTotal(base, adicional, unicas.size()));
            v.setEstado("Pago_por_confirmar");
            v = viajeRepositorio.save(v);
            for (Integer id : unicas) {
                ViajeMascota vm = new ViajeMascota();
                vm.setViaje_id(v.getId());
                vm.setMascota_id(id);
                viajeMascotaRepositorio.save(vm);
            }
            for (Map.Entry<String, Archivo> e : recibidos.entrySet()) {
                guardados.add(guardar(v.getId(), e.getKey(), e.getValue(), tipos.get(e.getKey()), actor.getId()));
            }
            PagoService.Resultado r = pagoService.cobrar(actor.getId(), "viaje", v.getId(), datosPago);
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("viaje", describir(viajeRepositorio.findById(v.getId()).orElse(v)));
            resp.put("pago", r.pago());
            resp.put("comprobante", r.comprobante());
            return resp;
        } catch (RuntimeException e) {
            borrar(guardados);
            throw e;
        }
    }

    @Transactional
    public synchronized Map<String, Object> reenviarDocumentos(Integer clienteId, Integer viajeId, Map<String, Archivo> documentos) {
        Usuario actor = autorizacion.conRol(clienteId, AutorizacionService.CLIENTE);
        if (actor == null) throw new NegocioException(403, "Inicia sesión como cliente para enviar documentos.");
        ViajeSolicitud v = obtener(viajeId);
        if (!actor.getId().equals(v.getUsuario_id())) throw new NegocioException(403, "Esta solicitud de viaje no es tuya.");
        if (!"Rechazada".equals(v.getEstado())) throw new NegocioException(409, "Solo se reenvían documentos de una solicitud rechazada (estado actual: " + v.getEstado() + ").");
        Map<String, Archivo> recibidos = presentes(documentos);
        if (recibidos.isEmpty()) throw new NegocioException(400, "Adjunta al menos un documento corregido.");
        Map<String, ViajeDocumento> vigentes = vigentes(v.getId());
        Set<String> porCorregir = new LinkedHashSet<>();
        for (String k : REQUISITOS.keySet()) {
            ViajeDocumento d = vigentes.get(k);
            if (d == null || OBSERVADO.equals(d.getEstado())) porCorregir.add(k);
        }
        for (String k : recibidos.keySet()) {
            if (!porCorregir.contains(k)) throw new NegocioException(400, "El documento \"" + REQUISITOS.get(k) + "\" no fue observado; no hace falta reenviarlo.");
        }
        List<String> faltan = porCorregir.stream().filter(k -> !recibidos.containsKey(k)).map(REQUISITOS::get).toList();
        if (!faltan.isEmpty()) throw new NegocioException(400, "Falta reenviar: " + String.join(", ", faltan) + ".");
        Map<String, ArchivoClinicoValidador.Tipo> tipos = new LinkedHashMap<>();
        for (Map.Entry<String, Archivo> e : recibidos.entrySet()) {
            tipos.put(e.getKey(), validar(e.getKey(), e.getValue()));
        }
        List<String> guardados = new ArrayList<>();
        try {
            for (Map.Entry<String, Archivo> e : recibidos.entrySet()) {
                ViajeDocumento anterior = vigentes.get(e.getKey());
                if (anterior != null) {
                    anterior.setVigente(false);
                    documentoRepositorio.save(anterior);
                }
                guardados.add(guardar(v.getId(), e.getKey(), e.getValue(), tipos.get(e.getKey()), actor.getId()));
            }
            v.setEstado("Registrada");
            v.setRevisado_por(null);
            v = viajeRepositorio.save(v);
            return describir(v);
        } catch (RuntimeException e) {
            borrar(guardados);
            throw e;
        }
    }

    @Transactional
    public synchronized ViajeSolicitud revisar(Integer usuarioId, Integer viajeId, String decision, String observaciones) {
        return revisar(usuarioId, viajeId, decision, observaciones, List.of());
    }

    @Transactional
    public synchronized ViajeSolicitud revisar(Integer usuarioId, Integer viajeId, String decision, String observaciones, List<String> observados) {
        Usuario quien = autorizacion.conRol(usuarioId, AutorizacionService.ADMIN, AutorizacionService.VETERINARIO, AutorizacionService.AUXILIAR);
        if (quien == null) throw new NegocioException(403, "No tienes permiso para revisar solicitudes de viaje.");
        ViajeSolicitud v = obtener(viajeId);
        if (!"Aprobada".equals(decision) && !"Rechazada".equals(decision)) throw new NegocioException(400, "La decisión debe ser Aprobada o Rechazada.");
        if (!"Registrada".equals(v.getEstado())) throw new NegocioException(409, "Solo se revisa una solicitud con pago confirmado (estado actual: " + v.getEstado() + ").");
        String obs = observaciones == null ? null : observaciones.trim();
        if ("Rechazada".equals(decision) && (obs == null || obs.length() < 3)) throw new NegocioException(400, "Indica el motivo del rechazo.");
        if (obs != null && obs.length() > 1000) throw new NegocioException(400, "Las observaciones no pueden superar los 1000 caracteres.");
        Map<String, ViajeDocumento> vigentes = vigentes(v.getId());
        Set<String> marcados = new LinkedHashSet<>();
        if (observados != null) observados.stream().filter(Objects::nonNull).map(String::trim).filter(t -> !t.isEmpty()).forEach(marcados::add);
        for (String k : marcados) {
            if (!REQUISITOS.containsKey(k)) throw new NegocioException(400, "El requisito " + k + " no existe.");
        }
        if ("Aprobada".equals(decision) && !marcados.isEmpty()) throw new NegocioException(400, "Para aprobar no debe quedar ningún documento observado.");
        if ("Aprobada".equals(decision) && !vigentes.isEmpty() && !vigentes.keySet().containsAll(REQUISITOS.keySet())) {
            throw new NegocioException(409, "Faltan documentos por recibir; no se puede aprobar todavía.");
        }
        if ("Rechazada".equals(decision) && !vigentes.isEmpty() && marcados.isEmpty()) {
            throw new NegocioException(400, "Marca qué documento está mal para que el cliente sepa qué corregir.");
        }
        for (Map.Entry<String, ViajeDocumento> e : vigentes.entrySet()) {
            e.getValue().setEstado(marcados.contains(e.getKey()) ? OBSERVADO : ACEPTADO);
            documentoRepositorio.save(e.getValue());
        }
        v.setEstado(decision);
        v.setObservaciones(obs == null || obs.isEmpty() ? null : obs);
        v.setRevisado_por(quien.getId());
        v = viajeRepositorio.save(v);
        notificacionService.notificar(v.getUsuario_id(), "Solicitud de viaje " + ("Aprobada".equals(decision) ? "aprobada" : "rechazada"),
                "Tu solicitud de viaje #" + v.getId() + " a " + v.getDestino() + " fue " + ("Aprobada".equals(decision) ? "aprobada." : "rechazada. Motivo: " + obs
                        + (marcados.isEmpty() ? "" : ". Corrige: " + String.join(", ", marcados.stream().map(REQUISITOS::get).toList()) + ".")),
                "Viaje");
        return v;
    }

    public List<Map<String, Object>> listar(Integer usuarioId) {
        Usuario quien = autorizacion.conRol(usuarioId, AutorizacionService.ADMIN, AutorizacionService.CLIENTE,
                AutorizacionService.VETERINARIO, AutorizacionService.AUXILIAR);
        if (quien == null) throw new NegocioException(403, "Inicia sesión para ver las solicitudes de viaje.");
        boolean interno = quien.getRol_id() != AutorizacionService.CLIENTE;
        List<Map<String, Object>> lista = new ArrayList<>();
        viajeRepositorio.findAll().stream()
                .filter(v -> interno || quien.getId().equals(v.getUsuario_id()))
                .sorted(Comparator.comparing(ViajeSolicitud::getFecha_solicitud).reversed())
                .forEach(v -> lista.add(describir(v)));
        return lista;
    }

    public Object[] documento(Integer usuarioId, Integer viajeId) {
        Usuario quien = autorizacion.conRol(usuarioId, AutorizacionService.ADMIN, AutorizacionService.CLIENTE,
                AutorizacionService.VETERINARIO, AutorizacionService.AUXILIAR);
        if (quien == null) throw new NegocioException(403, "No tienes permiso para ver este documento.");
        ViajeSolicitud v = obtener(viajeId);
        if (quien.getRol_id() == AutorizacionService.CLIENTE && !quien.getId().equals(v.getUsuario_id())) {
            throw new NegocioException(403, "Este documento no es tuyo.");
        }
        if (v.getDocumento_almacenado() == null) throw new NegocioException(404, "Esta solicitud guarda un documento por requisito.");
        try {
            byte[] datos = Files.readAllBytes(CARPETA.resolve(v.getDocumento_almacenado()).normalize());
            return new Object[]{datos, v.getDocumento_tipo(), v.getDocumento_nombre()};
        } catch (IOException e) {
            throw new NegocioException(404, "El documento ya no está disponible.");
        }
    }

    public Object[] documentoRequisito(Integer usuarioId, Integer viajeId, Integer documentoId) {
        Usuario quien = autorizacion.conRol(usuarioId, AutorizacionService.ADMIN, AutorizacionService.CLIENTE, AutorizacionService.AUXILIAR);
        if (quien == null) throw new NegocioException(403, "No tienes permiso para ver este documento.");
        ViajeSolicitud v = obtener(viajeId);
        if (quien.getRol_id() == AutorizacionService.CLIENTE && !quien.getId().equals(v.getUsuario_id())) {
            throw new NegocioException(403, "Este documento no es tuyo.");
        }
        ViajeDocumento d = documentoId == null ? null : documentoRepositorio.findById(documentoId).orElse(null);
        if (d == null || !viajeId.equals(d.getViaje_id())) throw new NegocioException(404, "El documento no existe.");
        try {
            byte[] datos = Files.readAllBytes(CARPETA.resolve(d.getNombre_almacenado()).normalize());
            return new Object[]{datos, d.getTipo_contenido(), d.getNombre_original()};
        } catch (IOException e) {
            throw new NegocioException(404, "El documento ya no está disponible.");
        }
    }

    private Map<String, ViajeDocumento> vigentes(Integer viajeId) {
        Map<String, ViajeDocumento> m = new LinkedHashMap<>();
        for (ViajeDocumento d : documentoRepositorio.deViaje(viajeId)) {
            if (Boolean.TRUE.equals(d.getVigente())) m.putIfAbsent(d.getRequisito(), d);
        }
        return m;
    }

    private static Map<String, Archivo> presentes(Map<String, Archivo> documentos) {
        Map<String, Archivo> r = new LinkedHashMap<>();
        if (documentos == null) return r;
        for (String k : REQUISITOS.keySet()) {
            Archivo a = documentos.get(k);
            if (a != null && a.contenido() != null && a.contenido().length > 0) r.put(k, a);
        }
        return r;
    }

    private static ArchivoClinicoValidador.Tipo validar(String requisito, Archivo a) {
        try {
            return ArchivoClinicoValidador.validar(a.nombre(), a.contenido());
        } catch (NegocioException e) {
            throw new NegocioException(e.getEstado(), REQUISITOS.get(requisito) + ": " + e.getMessage());
        }
    }

    private String guardar(Integer viajeId, String requisito, Archivo a, ArchivoClinicoValidador.Tipo tipo, Integer subidoPor) {
        String almacenado = UUID.randomUUID() + "." + tipo.extension();
        try {
            Files.createDirectories(CARPETA);
            Files.write(CARPETA.resolve(almacenado), a.contenido());
        } catch (IOException e) {
            throw new NegocioException(500, "No se pudo guardar el documento. Intenta de nuevo.");
        }
        ViajeDocumento d = new ViajeDocumento();
        d.setViaje_id(viajeId);
        d.setRequisito(requisito);
        d.setNombre_original(nombreSeguro(a.nombre()));
        d.setNombre_almacenado(almacenado);
        d.setTipo_contenido(tipo.contentType());
        d.setTamano_bytes(a.contenido().length);
        d.setVigente(true);
        d.setEstado(PENDIENTE);
        d.setSubido_por(subidoPor);
        documentoRepositorio.save(d);
        return almacenado;
    }

    private static void borrar(List<String> almacenados) {
        for (String n : almacenados) {
            try { Files.deleteIfExists(CARPETA.resolve(n)); } catch (IOException ignorado) { }
        }
    }

    private Map<String, Object> describir(ViajeSolicitud v) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", v.getId());
        m.put("estado", v.getEstado());
        m.put("fecha_viaje", v.getFecha_viaje());
        m.put("destino", v.getDestino());
        m.put("cantidad", v.getCantidad());
        m.put("total", v.getTotal());
        m.put("documento_nombre", v.getDocumento_nombre());
        m.put("documento_unico", v.getDocumento_almacenado() != null);
        List<Map<String, Object>> docs = new ArrayList<>();
        Map<String, ViajeDocumento> vig = vigentes(v.getId());
        for (Map.Entry<String, String> r : REQUISITOS.entrySet()) {
            ViajeDocumento d = vig.get(r.getKey());
            Map<String, Object> x = new LinkedHashMap<>();
            x.put("requisito", r.getKey());
            x.put("etiqueta", r.getValue());
            x.put("id", d != null ? d.getId() : null);
            x.put("nombre", d != null ? d.getNombre_original() : null);
            x.put("estado", d != null ? d.getEstado() : null);
            x.put("subido_en", d != null ? d.getSubido_en() : null);
            docs.add(x);
        }
        m.put("documentos", docs);
        m.put("observaciones", v.getObservaciones());
        m.put("fecha_solicitud", v.getFecha_solicitud());
        Usuario c = v.getUsuario_id() == null ? null : usuarioRepositorio.findById(v.getUsuario_id()).orElse(null);
        m.put("cliente", c != null ? c.getNombre() : null);
        List<String> nombres = new ArrayList<>();
        viajeMascotaRepositorio.findAll().stream().filter(x -> v.getId().equals(x.getViaje_id())).forEach(x ->
                mascotaRepositorio.findById(x.getMascota_id()).ifPresent(mm -> nombres.add(mm.getNombre())));
        m.put("mascotas", nombres);
        Pago pago = pagoRepositorio.findAll().stream()
                .filter(p -> v.getId().equals(p.getViaje_id()) && !PagoService.RECHAZADO.equals(p.getEstado())).findFirst().orElse(null);
        m.put("pago_estado", pago != null ? pago.getEstado() : null);
        Comprobante comp = pago == null ? null : comprobanteRepositorio.findAll().stream()
                .filter(x -> pago.getId().equals(x.getPago_id())).findFirst().orElse(null);
        m.put("comprobante_id", comp != null ? comp.getId() : null);
        return m;
    }

    private ViajeSolicitud obtener(Integer id) {
        ViajeSolicitud v = id == null ? null : viajeRepositorio.findById(id).orElse(null);
        if (v == null) throw new NegocioException(404, "La solicitud de viaje no existe.");
        return v;
    }

    private BigDecimal precio(String nombre) {
        return servicioRepositorio.findAll().stream()
                .filter(s -> s.getNombre() != null && s.getNombre().equalsIgnoreCase(nombre) && !Boolean.FALSE.equals(s.getActivo()) && s.getPrecio_base() != null)
                .map(Servicio::getPrecio_base).findFirst().orElse(BigDecimal.ZERO);
    }

    private static String nombreSeguro(String n) {
        String s = n == null ? "documento" : n.replaceAll("[\\\\/\\r\\n]", "_").trim();
        if (s.isEmpty()) s = "documento";
        return s.length() > 150 ? s.substring(s.length() - 150) : s;
    }
}
