package com.vamospet.backend.servicio;

import com.vamospet.backend.modelo.*;
import com.vamospet.backend.repositorio.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ExamenService {

    public static final int DIAS_VIGENCIA = 30;
    public static final int MAX_ARCHIVOS_POR_ORDEN = 10;
    static final Path CARPETA_ARCHIVOS = Paths.get("uploads_clinicos");

    @Autowired private OrdenExamenRepository ordenRepositorio;
    @Autowired private ArchivoExamenRepository archivoRepositorio;
    @Autowired private CirugiaProgramadaRepository cirugiaRepositorio;
    @Autowired private MascotaRepository mascotaRepositorio;
    @Autowired private UsuarioRepository usuarioRepositorio;
    @Autowired private CitaRepository citaRepositorio;
    @Autowired private ServicioRepository servicioRepositorio;
    @Autowired private HistorialEventoRepository historialRepositorio;
    @Autowired private AutorizacionService autorizacion;
    @Autowired private AgendaService agendaService;
    @Autowired private NotificacionService notificacionService;


    @Transactional
    public OrdenExamen solicitar(Integer usuarioId, Integer mascotaId, Integer citaId, String tipoExamen,
                                 String cirugiaPrevista, String indicaciones) {
        Usuario vet = autorizacion.conRol(usuarioId, AutorizacionService.ADMIN, AutorizacionService.VETERINARIO);
        if (vet == null) throw new NegocioException(403, "Solo un veterinario puede solicitar exámenes.");
        Mascota mascota = mascotaId == null ? null : mascotaRepositorio.findById(mascotaId).orElse(null);
        if (mascota == null) throw new NegocioException(404, "La mascota no existe.");
        String tipo = limpiar(tipoExamen);
        if (tipo == null || tipo.length() > 150) throw new NegocioException(400, "Indica el examen solicitado (máximo 150 caracteres).");
        if (citaId != null && citaRepositorio.findById(citaId).isEmpty()) throw new NegocioException(404, "La cita indicada no existe.");

        OrdenExamen o = new OrdenExamen();
        o.setMascota_id(mascotaId);
        o.setCita_id(citaId);
        o.setVeterinario_id(vet.getId());
        o.setTipo_examen(tipo);
        o.setCirugia_prevista(recortar(limpiar(cirugiaPrevista), 150));
        o.setIndicaciones(recortar(limpiar(indicaciones), 2000));
        o = ordenRepositorio.save(o);

        evento(mascotaId, "Examen solicitado", "Examen solicitado: " + tipo
                + (o.getCirugia_prevista() != null ? " (previo a: " + o.getCirugia_prevista() + ")" : "")
                + ". Solicitado por: " + vet.getNombre(), vet.getId());
        avisarDueno(mascota, "Examen solicitado", "Se solicitó un examen para " + mascota.getNombre() + ": " + tipo
                + ". Puedes subir el resultado (PDF o JPEG) desde el perfil de tu mascota.");
        return o;
    }


    @Transactional
    public ArchivoExamen subirArchivo(Integer usuarioId, Integer ordenId, String nombreOriginal, byte[] contenido) {
        Usuario quien = autorizacion.conRol(usuarioId, AutorizacionService.ADMIN, AutorizacionService.CLIENTE,
                AutorizacionService.VETERINARIO, AutorizacionService.AUXILIAR);
        if (quien == null) throw new NegocioException(403, "No tienes permiso para subir resultados.");
        OrdenExamen orden = ordenId == null ? null : ordenRepositorio.findById(ordenId).orElse(null);
        if (orden == null) throw new NegocioException(404, "La orden de examen no existe.");
        Mascota mascota = mascotaRepositorio.findById(orden.getMascota_id()).orElse(null);
        if (quien.getRol_id() == AutorizacionService.CLIENTE
                && (mascota == null || !quien.getId().equals(mascota.getUsuario_id()))) {
            throw new NegocioException(403, "Solo puedes subir resultados de tus propias mascotas.");
        }
        if ("Apto_Cirugia".equals(orden.getEstado())) {
            throw new NegocioException(409, "Esta orden ya fue aprobada; no admite más archivos.");
        }
        long existentes = archivoRepositorio.findAll().stream().filter(a -> ordenId.equals(a.getOrden_id())).count();
        if (existentes >= MAX_ARCHIVOS_POR_ORDEN) throw new NegocioException(409, "Se alcanzó el máximo de " + MAX_ARCHIVOS_POR_ORDEN + " archivos por orden.");

        ArchivoClinicoValidador.Tipo tipo = ArchivoClinicoValidador.validar(nombreOriginal, contenido);
        String almacenado = UUID.randomUUID() + "." + tipo.extension();
        try {
            Files.createDirectories(CARPETA_ARCHIVOS);
            Files.write(CARPETA_ARCHIVOS.resolve(almacenado), contenido);
        } catch (IOException e) {
            throw new NegocioException(500, "No se pudo guardar el archivo. Intenta de nuevo.");
        }

        ArchivoExamen a = new ArchivoExamen();
        a.setOrden_id(ordenId);
        a.setNombre_original(nombreSeguro(nombreOriginal));
        a.setNombre_almacenado(almacenado);
        a.setTipo_contenido(tipo.contentType());
        a.setTamano_bytes(contenido.length);
        a.setSubido_por(quien.getId());
        a = archivoRepositorio.save(a);

        orden.setEstado("Resultados_Subidos");
        ordenRepositorio.save(orden);

        notificacionService.notificar(orden.getVeterinario_id(), "Resultados de examen listos",
                "Se subieron resultados de \"" + orden.getTipo_examen() + "\" de " + (mascota != null ? mascota.getNombre() : "una mascota")
                        + ". Revísalos para aprobar o rechazar.", "Examen");
        return a;
    }


    @Transactional
    public OrdenExamen evaluar(Integer usuarioId, Integer ordenId, String decision, String observaciones) {
        Usuario quien = autorizacion.conRol(usuarioId, AutorizacionService.ADMIN, AutorizacionService.VETERINARIO);
        if (quien == null) throw new NegocioException(403, "Solo un veterinario autorizado puede evaluar exámenes.");
        OrdenExamen orden = ordenId == null ? null : ordenRepositorio.findById(ordenId).orElse(null);
        if (orden == null) throw new NegocioException(404, "La orden de examen no existe.");
        if (quien.getRol_id() == AutorizacionService.VETERINARIO && !quien.getId().equals(orden.getVeterinario_id())) {
            throw new NegocioException(403, "Solo el veterinario que solicitó el examen (o el administrador) puede evaluarlo.");
        }
        if (!"Apto_Cirugia".equals(decision) && !"Rechazado".equals(decision)) {
            throw new NegocioException(400, "La decisión debe ser Apto_Cirugia o Rechazado.");
        }
        if (!"Resultados_Subidos".equals(orden.getEstado())) {
            throw new NegocioException(409, "Solo se evalúan órdenes con resultados subidos (estado actual: " + orden.getEstado() + ").");
        }
        String obs = recortar(limpiar(observaciones), 2000);
        if ("Rechazado".equals(decision) && obs == null) {
            throw new NegocioException(400, "Explica el motivo del rechazo para que se puedan repetir los resultados.");
        }

        orden.setEstado(decision);
        orden.setEvaluado_por(quien.getId());
        orden.setFecha_evaluacion(LocalDateTime.now());
        orden.setObservaciones(obs);
        orden = ordenRepositorio.save(orden);

        Mascota mascota = mascotaRepositorio.findById(orden.getMascota_id()).orElse(null);
        boolean apto = "Apto_Cirugia".equals(decision);
        evento(orden.getMascota_id(), apto ? "Examen aprobado" : "Examen rechazado",
                orden.getTipo_examen() + ": " + (apto ? "apto para cirugía (vigente " + DIAS_VIGENCIA + " días)" : "rechazado")
                        + (obs != null ? ". " + obs : "") + ". Evaluó: " + quien.getNombre(), quien.getId());
        if (mascota != null) {
            avisarDueno(mascota, apto ? "Examen aprobado" : "Examen rechazado",
                    apto ? mascota.getNombre() + " está apto para la cirugía. La clínica coordinará la fecha."
                         : "El examen \"" + orden.getTipo_examen() + "\" de " + mascota.getNombre() + " fue rechazado: " + obs + " Puedes subir un nuevo resultado.");
        }
        return orden;
    }


    public static boolean vigenteEn(OrdenExamen o, LocalDateTime fechaCirugia) {
        return o.getFecha_evaluacion() != null
                && !fechaCirugia.isBefore(o.getFecha_evaluacion())
                && !fechaCirugia.isAfter(o.getFecha_evaluacion().plusDays(DIAS_VIGENCIA));
    }

    public Optional<OrdenExamen> aprobacionVigente(Integer mascotaId, LocalDateTime fechaCirugia) {
        Set<Integer> usadas = new HashSet<>();
        cirugiaRepositorio.findAll().stream()
                .filter(c -> !"Cancelada".equals(c.getEstado()))
                .forEach(c -> usadas.add(c.getOrden_id()));
        return ordenRepositorio.findAll().stream()
                .filter(o -> mascotaId != null && mascotaId.equals(o.getMascota_id()))
                .filter(o -> "Apto_Cirugia".equals(o.getEstado()))
                .filter(o -> !usadas.contains(o.getId()))
                .filter(o -> vigenteEn(o, fechaCirugia))
                .max(Comparator.comparing(OrdenExamen::getFecha_evaluacion));
    }

    public static boolean esServicioQuirurgico(Servicio s) {
        if (s == null || s.getNombre() == null) return false;
        String n = Normalizer.normalize(s.getNombre(), Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        return n.contains("cirug");
    }

    public static final String MENSAJE_SIN_APROBACION =
            "No se puede programar la cirugía: la mascota no tiene un examen prequirúrgico aprobado y vigente "
                    + "(máximo " + DIAS_VIGENCIA + " días antes de la cirugía). Solicita y aprueba un examen primero.";

    @Transactional
    public CirugiaProgramada agendarCirugia(Integer usuarioId, Integer ordenId, LocalDateTime fechaHora, String tipoCirugia) {
        Usuario quien = autorizacion.conRol(usuarioId, AutorizacionService.ADMIN, AutorizacionService.VETERINARIO);
        if (quien == null) throw new NegocioException(403, "Solo un veterinario o el administrador pueden programar cirugías.");
        OrdenExamen orden = ordenId == null ? null : ordenRepositorio.findById(ordenId).orElse(null);
        if (orden == null) throw new NegocioException(404, "La orden de examen no existe.");
        if (fechaHora == null || !fechaHora.isAfter(LocalDateTime.now())) throw new NegocioException(400, "La fecha de la cirugía debe ser futura.");
        if (quien.getRol_id() == AutorizacionService.VETERINARIO && !quien.getId().equals(orden.getVeterinario_id())) {
            throw new NegocioException(403, "Solo el veterinario de la orden (o el administrador) puede programar la cirugía.");
        }
        if (!"Apto_Cirugia".equals(orden.getEstado())) throw new NegocioException(409, MENSAJE_SIN_APROBACION);
        if (!vigenteEn(orden, fechaHora)) {
            throw new NegocioException(409, "La aprobación del examen venció o aún no rige para esa fecha (vigencia: " + DIAS_VIGENCIA
                    + " días desde la evaluación del " + orden.getFecha_evaluacion().toLocalDate() + "). Solicita un examen nuevo.");
        }
        boolean yaUsada = cirugiaRepositorio.findAll().stream()
                .anyMatch(c -> ordenId.equals(c.getOrden_id()) && !"Cancelada".equals(c.getEstado()));
        if (yaUsada) throw new NegocioException(409, "Esta aprobación ya se usó para una cirugía programada.");

        Integer vetId = orden.getVeterinario_id();
        if (!agendaService.tieneHorario(vetId) || !agendaService.franjaLibre(vetId, fechaHora)) {
            throw new NegocioException(409, "Esa franja no está disponible para el veterinario. Elige otra.");
        }
        String tipo = recortar(limpiar(tipoCirugia), 150);
        if (tipo == null) tipo = orden.getCirugia_prevista();
        if (tipo == null) throw new NegocioException(400, "Indica el tipo de cirugía.");

        Cita cita = new Cita();
        cita.setMascota_id(orden.getMascota_id());
        cita.setVeterinario_id(vetId);
        cita.setFecha_hora(fechaHora);
        cita.setEstado("Confirmada");
        cita.setCosto(BigDecimal.ZERO);
        try {
            cita = citaRepositorio.saveAndFlush(cita);
        } catch (DataIntegrityViolationException e) {
            throw new NegocioException(409, "Esa franja acaba de ser ocupada. Elige otra.");
        }

        CirugiaProgramada c = new CirugiaProgramada();
        c.setOrden_id(ordenId);
        c.setCita_id(cita.getId());
        c.setMascota_id(orden.getMascota_id());
        c.setVeterinario_id(vetId);
        c.setTipo_cirugia(tipo);
        c.setFecha_hora(fechaHora);
        c.setCreado_por(quien.getId());
        c = cirugiaRepositorio.save(c);

        Mascota mascota = mascotaRepositorio.findById(orden.getMascota_id()).orElse(null);
        evento(orden.getMascota_id(), "Cirugía programada", tipo + " programada para " + fechaHora.toLocalDate() + " "
                + fechaHora.toLocalTime().withSecond(0).withNano(0) + ". Registró: " + quien.getNombre(), quien.getId());
        if (mascota != null) {
            avisarDueno(mascota, "Cirugía programada", "La cirugía (" + tipo + ") de " + mascota.getNombre() + " quedó programada para el "
                    + fechaHora.toLocalDate() + " a las " + fechaHora.toLocalTime().withSecond(0).withNano(0) + ".");
        }
        return c;
    }

    @Transactional
    public void exigirYRegistrarCirugiaDeCita(Cita cita, Servicio servicio) {
        Optional<OrdenExamen> orden = aprobacionVigente(cita.getMascota_id(), cita.getFecha_hora());
        if (orden.isEmpty()) throw new NegocioException(409, MENSAJE_SIN_APROBACION);
        CirugiaProgramada c = new CirugiaProgramada();
        c.setOrden_id(orden.get().getId());
        c.setCita_id(cita.getId());
        c.setMascota_id(cita.getMascota_id());
        c.setVeterinario_id(cita.getVeterinario_id() != null ? cita.getVeterinario_id() : orden.get().getVeterinario_id());
        c.setTipo_cirugia(servicio.getNombre());
        c.setFecha_hora(cita.getFecha_hora());
        c.setCreado_por(orden.get().getVeterinario_id());
        cirugiaRepositorio.save(c);
    }


    public List<Map<String, Object>> listar(Integer usuarioId, Integer mascotaId, Set<String> estados) {
        Usuario quien = autorizacion.conRol(usuarioId, AutorizacionService.ADMIN, AutorizacionService.CLIENTE,
                AutorizacionService.VETERINARIO, AutorizacionService.AUXILIAR);
        if (quien == null) throw new NegocioException(403, "No tienes permiso para ver exámenes.");
        boolean interno = quien.getRol_id() != AutorizacionService.CLIENTE;
        Set<Integer> misMascotas = new HashSet<>();
        if (!interno) mascotaRepositorio.buscarPorDueno(usuarioId).forEach(m -> misMascotas.add(m.getId()));

        return ordenRepositorio.findAll().stream()
                .filter(o -> mascotaId == null || mascotaId.equals(o.getMascota_id()))
                .filter(o -> estados == null || estados.isEmpty() || estados.contains(o.getEstado()))
                .filter(o -> interno || misMascotas.contains(o.getMascota_id()))
                .sorted(Comparator.comparing(OrdenExamen::getFecha_solicitud).reversed())
                .map(this::describir)
                .toList();
    }

    public List<CirugiaProgramada> cirugias(Integer usuarioId, Integer mascotaId) {
        Usuario quien = autorizacion.conRol(usuarioId, AutorizacionService.ADMIN, AutorizacionService.CLIENTE,
                AutorizacionService.VETERINARIO, AutorizacionService.AUXILIAR);
        if (quien == null) throw new NegocioException(403, "No tienes permiso para ver cirugías.");
        boolean interno = quien.getRol_id() != AutorizacionService.CLIENTE;
        Set<Integer> misMascotas = new HashSet<>();
        if (!interno) mascotaRepositorio.buscarPorDueno(usuarioId).forEach(m -> misMascotas.add(m.getId()));
        return cirugiaRepositorio.findAll().stream()
                .filter(c -> mascotaId == null || mascotaId.equals(c.getMascota_id()))
                .filter(c -> interno || misMascotas.contains(c.getMascota_id()))
                .sorted(Comparator.comparing(CirugiaProgramada::getFecha_hora))
                .toList();
    }

    public record Descarga(ArchivoExamen archivo, byte[] contenido) {}

    public Descarga descargar(Integer usuarioId, Integer archivoId) {
        Usuario quien = autorizacion.conRol(usuarioId, AutorizacionService.ADMIN, AutorizacionService.CLIENTE,
                AutorizacionService.VETERINARIO, AutorizacionService.AUXILIAR);
        if (quien == null) throw new NegocioException(403, "No tienes permiso para ver este archivo.");
        ArchivoExamen a = archivoId == null ? null : archivoRepositorio.findById(archivoId).orElse(null);
        if (a == null) throw new NegocioException(404, "El archivo no existe.");
        if (quien.getRol_id() == AutorizacionService.CLIENTE) {
            OrdenExamen o = ordenRepositorio.findById(a.getOrden_id()).orElse(null);
            Mascota m = o == null ? null : mascotaRepositorio.findById(o.getMascota_id()).orElse(null);
            if (m == null || !quien.getId().equals(m.getUsuario_id())) throw new NegocioException(403, "Este archivo no pertenece a tus mascotas.");
        }
        try {
            return new Descarga(a, Files.readAllBytes(CARPETA_ARCHIVOS.resolve(a.getNombre_almacenado())));
        } catch (IOException e) {
            throw new NegocioException(404, "El archivo ya no está disponible en el servidor.");
        }
    }

    private Map<String, Object> describir(OrdenExamen o) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", o.getId());
        m.put("mascota_id", o.getMascota_id());
        Mascota mascota = mascotaRepositorio.findById(o.getMascota_id()).orElse(null);
        m.put("mascota_nombre", mascota != null ? mascota.getNombre() : null);
        Usuario dueno = mascota != null && mascota.getUsuario_id() != null ? usuarioRepositorio.findById(mascota.getUsuario_id()).orElse(null) : null;
        m.put("cliente_nombre", dueno != null ? dueno.getNombre() : null);
        m.put("tipo_examen", o.getTipo_examen());
        m.put("cirugia_prevista", o.getCirugia_prevista());
        m.put("indicaciones", o.getIndicaciones());
        m.put("estado", o.getEstado());
        m.put("fecha_solicitud", o.getFecha_solicitud());
        m.put("fecha_evaluacion", o.getFecha_evaluacion());
        m.put("observaciones", o.getObservaciones());
        Usuario vet = usuarioRepositorio.findById(o.getVeterinario_id()).orElse(null);
        m.put("veterinario_id", o.getVeterinario_id());
        m.put("veterinario_nombre", vet != null ? vet.getNombre() : null);
        boolean vigente = "Apto_Cirugia".equals(o.getEstado()) && o.getFecha_evaluacion() != null
                && !LocalDateTime.now().isAfter(o.getFecha_evaluacion().plusDays(DIAS_VIGENCIA));
        m.put("vigente", vigente);
        m.put("vigente_hasta", o.getFecha_evaluacion() != null ? o.getFecha_evaluacion().plusDays(DIAS_VIGENCIA).toLocalDate() : null);
        List<Map<String, Object>> archivos = new ArrayList<>();
        archivoRepositorio.findAll().stream().filter(a -> o.getId().equals(a.getOrden_id())).forEach(a -> {
            Map<String, Object> f = new LinkedHashMap<>();
            f.put("id", a.getId());
            f.put("nombre", a.getNombre_original());
            f.put("tipo", a.getTipo_contenido());
            f.put("tamano_bytes", a.getTamano_bytes());
            f.put("fecha_subida", a.getFecha_subida());
            archivos.add(f);
        });
        m.put("archivos", archivos);
        return m;
    }


    private void evento(Integer mascotaId, String tipo, String descripcion, Integer vetId) {
        HistorialEvento e = new HistorialEvento();
        e.setMascota_id(mascotaId);
        e.setTipo_evento(tipo);
        e.setFecha(LocalDateTime.now());
        e.setDescripcion(descripcion);
        e.setVeterinario_id(vetId);
        historialRepositorio.save(e);
    }

    private void avisarDueno(Mascota m, String titulo, String mensaje) {
        if (m.getUsuario_id() != null) notificacionService.notificar(m.getUsuario_id(), titulo, mensaje, "Examen");
    }

    private static String limpiar(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String recortar(String s, int max) {
        return s == null ? null : (s.length() > max ? s.substring(0, max) : s);
    }

    private static String nombreSeguro(String nombre) {
        String n = nombre == null ? "archivo" : nombre.replace('\\', '/');
        n = n.substring(n.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "").trim();
        if (n.isEmpty()) n = "archivo";
        return n.length() > 255 ? n.substring(n.length() - 255) : n;
    }
}
