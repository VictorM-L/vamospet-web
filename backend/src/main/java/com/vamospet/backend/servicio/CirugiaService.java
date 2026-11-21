package com.vamospet.backend.servicio;

import com.vamospet.backend.modelo.Cita;
import com.vamospet.backend.modelo.CirugiaProgramada;
import com.vamospet.backend.modelo.HistorialEvento;
import com.vamospet.backend.modelo.Mascota;
import com.vamospet.backend.modelo.OrdenExamen;
import com.vamospet.backend.modelo.Receta;
import com.vamospet.backend.modelo.Servicio;
import com.vamospet.backend.modelo.Usuario;
import com.vamospet.backend.repositorio.CirugiaProgramadaRepository;
import com.vamospet.backend.repositorio.CitaRepository;
import com.vamospet.backend.repositorio.HistorialEventoRepository;
import com.vamospet.backend.repositorio.MascotaRepository;
import com.vamospet.backend.repositorio.OrdenExamenRepository;
import com.vamospet.backend.repositorio.PagoRepository;
import com.vamospet.backend.repositorio.RecetaRepository;
import com.vamospet.backend.repositorio.ServicioRepository;
import com.vamospet.backend.repositorio.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CirugiaService {

    public static final String POR_CONFIRMAR = "Por_confirmar";
    public static final String PROGRAMADA = "Programada";
    public static final String EN_CURSO = "En_curso";
    public static final String POSTOPERATORIO = "Postoperatorio";
    public static final String REALIZADA = "Realizada";
    public static final String CANCELADA = "Cancelada";
    public static final String INTERNAR = "Internar";
    public static final String ALTA_DIRECTA = "Alta directa";
    public static final String CITA_EN_CURSO = "En curso";
    public static final int MINUTOS_ANTES_PARA_INICIAR = 60;
    public static final List<String> COLUMNAS = List.of(POR_CONFIRMAR, PROGRAMADA, EN_CURSO, POSTOPERATORIO);

    private static final Set<String> TIPOS_LINEA = Set.of("Examen solicitado", "Examen aprobado", "Examen rechazado",
            "Cirugía programada", "Cirugía confirmada", "Cirugía iniciada", "Cirugía realizada", "Cirugía cancelada",
            "Internamiento", "Alta directa", "Alta de internamiento");
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public record Paso(String resultado, String complicaciones, String medicamentos, String decision,
                       LocalDateTime ingreso, LocalDateTime salidaEstimada, String observaciones) {
        public static Paso vacio() {
            return new Paso(null, null, null, null, null, null, null);
        }
    }

    @Autowired private CirugiaProgramadaRepository cirugiaRepositorio;
    @Autowired private CitaRepository citaRepositorio;
    @Autowired private OrdenExamenRepository ordenRepositorio;
    @Autowired private MascotaRepository mascotaRepositorio;
    @Autowired private UsuarioRepository usuarioRepositorio;
    @Autowired private ServicioRepository servicioRepositorio;
    @Autowired private RecetaRepository recetaRepositorio;
    @Autowired private HistorialEventoRepository historialRepositorio;
    @Autowired private PagoRepository pagoRepositorio;
    @Autowired private NotificacionService notificaciones;
    @Autowired private EstanciaService estanciaService;
    @Autowired private CitaService citaService;

    @Transactional
    public CirugiaProgramada siguiente(Integer veterinarioId, Integer cirugiaId, Paso datos) {
        CirugiaProgramada c = obtener(cirugiaId);
        exigirAsignada(c, veterinarioId);
        Paso p = datos == null ? Paso.vacio() : datos;
        return switch (String.valueOf(c.getEstado())) {
            case POR_CONFIRMAR -> confirmar(veterinarioId, c);
            case PROGRAMADA -> iniciar(c);
            case EN_CURSO -> finalizar(veterinarioId, c, p);
            case POSTOPERATORIO -> cerrar(veterinarioId, c, p);
            default -> throw new NegocioException(409, "Esta cirugía ya no tiene pasos pendientes (estado: " + c.getEstado() + ").");
        };
    }

    @Transactional
    public CirugiaProgramada guardarNotas(Integer veterinarioId, Integer cirugiaId, String notas) {
        CirugiaProgramada c = obtener(cirugiaId);
        exigirAsignada(c, veterinarioId);
        if (!EN_CURSO.equals(c.getEstado())) throw new NegocioException(409, "Las notas se registran mientras la cirugía está en curso.");
        String limpias = limpiar(notas);
        if (limpias != null && limpias.length() > 2000) throw new NegocioException(400, "Las notas admiten hasta 2000 caracteres.");
        c.setNotas_intra(limpias);
        return cirugiaRepositorio.save(c);
    }

    private CirugiaProgramada confirmar(Integer veterinarioId, CirugiaProgramada c) {
        Cita cita = c.getCita_id() == null ? null : citaRepositorio.findById(c.getCita_id()).orElse(null);
        if (cita != null && CitaService.PENDIENTE.equals(cita.getEstado())) {
            citaService.confirmar(veterinarioId, cita.getId());
        }
        CirugiaProgramada actual = obtener(c.getId());
        if (POR_CONFIRMAR.equals(actual.getEstado())) {
            actual.setEstado(PROGRAMADA);
            actual = cirugiaRepositorio.save(actual);
            evento(actual, "Cirugía confirmada", actual.getTipo_cirugia() + " confirmada para el " + actual.getFecha_hora().format(FORMATO) + ".", veterinarioId);
        }
        return actual;
    }

    private CirugiaProgramada iniciar(CirugiaProgramada c) {
        LocalDateTime ahora = LocalDateTime.now();
        if (c.getFecha_hora() != null && ahora.isBefore(c.getFecha_hora().minusMinutes(MINUTOS_ANTES_PARA_INICIAR))) {
            throw new NegocioException(409, "La cirugía se puede iniciar desde " + MINUTOS_ANTES_PARA_INICIAR
                    + " minutos antes de la hora programada (" + c.getFecha_hora().format(FORMATO) + ").");
        }
        OrdenExamen orden = c.getOrden_id() == null ? null : ordenRepositorio.findById(c.getOrden_id()).orElse(null);
        if (orden == null || !"Apto_Cirugia".equals(orden.getEstado()) || !ExamenService.vigenteEn(orden, ahora)) {
            throw new NegocioException(409, "No se puede iniciar: el examen prequirúrgico no está apto o ya venció. Solicita uno nuevo.");
        }
        c.setInicio_real(ahora);
        c.setEstado(EN_CURSO);
        c = cirugiaRepositorio.save(c);
        Cita cita = c.getCita_id() == null ? null : citaRepositorio.findById(c.getCita_id()).orElse(null);
        if (cita != null) {
            cita.setEstado(CITA_EN_CURSO);
            cita.setInicio_real(ahora);
            citaRepositorio.save(cita);
        }
        evento(c, "Cirugía iniciada", c.getTipo_cirugia() + " iniciada el " + ahora.format(FORMATO) + ".", c.getVeterinario_id());
        avisarDueno(c, "Cirugía en curso", "La cirugía (" + c.getTipo_cirugia() + ") de %s comenzó. Te avisaremos al terminar.");
        return c;
    }

    private CirugiaProgramada finalizar(Integer veterinarioId, CirugiaProgramada c, Paso p) {
        String resultado = limpiar(p.resultado());
        String complicaciones = limpiar(p.complicaciones());
        String medicamentos = limpiar(p.medicamentos());
        if (resultado == null) throw new NegocioException(400, "Escribe el resultado de la cirugía.");
        if (resultado.length() > 2000 || largo(complicaciones) > 2000 || largo(medicamentos) > 2000) {
            throw new NegocioException(400, "El resultado, las complicaciones y los medicamentos admiten hasta 2000 caracteres cada uno.");
        }
        LocalDateTime ahora = LocalDateTime.now();
        c.setResultado(resultado);
        c.setComplicaciones(complicaciones);
        c.setFin_real(ahora);
        c.setEstado(POSTOPERATORIO);
        c = cirugiaRepositorio.save(c);
        if (medicamentos != null) {
            Receta r = new Receta();
            r.setMascota_id(c.getMascota_id());
            r.setVeterinario_id(veterinarioId);
            r.setContenido(medicamentos);
            r.setEstado("Aprobada");
            r.setAprobada_en(LocalDateTime.now());
            recetaRepositorio.save(r);
        }
        Cita cita = c.getCita_id() == null ? null : citaRepositorio.findById(c.getCita_id()).orElse(null);
        if (cita != null) {
            cita.setEstado(CitaService.ATENDIDA);
            cita.setFin_real(ahora);
            if (cita.getInicio_real() == null) cita.setInicio_real(c.getInicio_real());
            citaRepositorio.save(cita);
            notificaciones.cancelarProgramadas("cita:" + cita.getId());
        }
        StringBuilder texto = new StringBuilder(c.getTipo_cirugia()).append(" realizada el ").append(ahora.format(FORMATO)).append('.')
                .append("\nResultado: ").append(resultado);
        if (complicaciones != null) texto.append("\nComplicaciones: ").append(complicaciones);
        if (medicamentos != null) texto.append("\nMedicamentos e indicaciones: ").append(medicamentos);
        if (c.getNotas_intra() != null) texto.append("\nNotas de la cirugía: ").append(c.getNotas_intra());
        evento(c, "Cirugía realizada", texto.toString(), veterinarioId);
        avisarDueno(c, "Cirugía realizada", "La cirugía (" + c.getTipo_cirugia() + ") de %s terminó. Revisa su historial"
                + (medicamentos != null ? " y la receta con los medicamentos indicados." : "."));
        return c;
    }

    private CirugiaProgramada cerrar(Integer veterinarioId, CirugiaProgramada c, Paso p) {
        String decision = limpiar(p.decision());
        if (INTERNAR.equals(decision)) {
            LocalDateTime ingreso = p.ingreso() == null ? LocalDateTime.now() : p.ingreso();
            estanciaService.internar(veterinarioId, c.getId(), ingreso, p.salidaEstimada(), p.observaciones());
        } else if (ALTA_DIRECTA.equals(decision)) {
            evento(c, "Alta directa", "Alta directa tras " + c.getTipo_cirugia() + ". La mascota vuelve a casa el "
                    + LocalDateTime.now().format(FORMATO) + "." + (limpiar(p.observaciones()) != null ? "\nIndicaciones: " + limpiar(p.observaciones()) : ""), veterinarioId);
            avisarDueno(c, "Alta de cirugía", "%s recibió el alta y puede volver a casa. Sigue las indicaciones de su historial.");
        } else {
            throw new NegocioException(400, "Elige si la mascota se interna o recibe el alta directa.");
        }
        CirugiaProgramada actual = obtener(c.getId());
        actual.setPostoperatorio(decision);
        actual.setEstado(REALIZADA);
        return cirugiaRepositorio.save(actual);
    }

    public List<Map<String, Object>> tablero(Integer usuarioId) {
        Usuario quien = usuarioId == null ? null : usuarioRepositorio.findById(usuarioId).orElse(null);
        if (quien == null) throw new NegocioException(403, "Inicia sesión para ver las cirugías.");
        boolean clinico = Integer.valueOf(AutorizacionService.VETERINARIO).equals(quien.getRol_id())
                || Integer.valueOf(AutorizacionService.ADMIN).equals(quien.getRol_id());
        boolean soloPropias = Integer.valueOf(AutorizacionService.VETERINARIO).equals(quien.getRol_id());
        LocalDateTime desde = LocalDateTime.now().minusDays(7);
        return cirugiaRepositorio.findAll().stream()
                .filter(c -> COLUMNAS.contains(c.getEstado())
                        || (REALIZADA.equals(c.getEstado()) && c.getFin_real() != null && c.getFin_real().isAfter(desde)))
                .filter(c -> !soloPropias || quien.getId().equals(c.getVeterinario_id()))
                .sorted(Comparator.comparing(CirugiaProgramada::getFecha_hora, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(c -> describir(c, clinico))
                .toList();
    }

    public List<Map<String, Object>> lineaTiempo(Integer cirugiaId) {
        CirugiaProgramada c = obtener(cirugiaId);
        OrdenExamen orden = c.getOrden_id() == null ? null : ordenRepositorio.findById(c.getOrden_id()).orElse(null);
        LocalDateTime desde = orden != null && orden.getFecha_solicitud() != null ? orden.getFecha_solicitud().minusMinutes(1)
                : (c.getFecha_creacion() != null ? c.getFecha_creacion().minusMinutes(1) : LocalDateTime.MIN);
        return historialRepositorio.findAll().stream()
                .filter(h -> c.getMascota_id().equals(h.getMascota_id()))
                .filter(h -> TIPOS_LINEA.contains(h.getTipo_evento()))
                .filter(h -> h.getFecha() != null && h.getFecha().isAfter(desde))
                .sorted(Comparator.comparing(HistorialEvento::getFecha))
                .map(h -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("fecha", h.getFecha());
                    m.put("tipo", h.getTipo_evento());
                    m.put("descripcion", h.getDescripcion());
                    return m;
                })
                .toList();
    }

    private Map<String, Object> describir(CirugiaProgramada c, boolean clinico) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("estado", c.getEstado());
        m.put("tipo_cirugia", c.getTipo_cirugia());
        m.put("fecha_hora", c.getFecha_hora());
        m.put("cita_id", c.getCita_id());
        m.put("veterinario_id", c.getVeterinario_id());
        Usuario vet = c.getVeterinario_id() == null ? null : usuarioRepositorio.findById(c.getVeterinario_id()).orElse(null);
        m.put("veterinario", vet != null ? vet.getNombre() : null);
        Mascota mascota = mascotaRepositorio.findById(c.getMascota_id()).orElse(null);
        m.put("mascota_id", c.getMascota_id());
        m.put("mascota", mascota != null ? mascota.getNombre() : null);
        m.put("especie", mascota != null ? mascota.getEspecie() : null);
        Usuario dueno = mascota != null && mascota.getUsuario_id() != null ? usuarioRepositorio.findById(mascota.getUsuario_id()).orElse(null) : null;
        m.put("cliente", dueno != null ? dueno.getNombre() : null);
        m.put("postoperatorio", c.getPostoperatorio());
        boolean cobrada = c.getCita_id() != null && pagoRepositorio.findAll().stream()
                .anyMatch(pg -> c.getCita_id().equals(pg.getCita_id()) && !PagoService.RECHAZADO.equals(pg.getEstado()));
        m.put("cobro", POSTOPERATORIO.equals(c.getEstado()) || REALIZADA.equals(c.getEstado()) ? (cobrada ? "Cobrada" : "Por cobrar") : null);
        if (clinico) {
            Servicio servicio = c.getServicio_id() == null ? null : servicioRepositorio.findById(c.getServicio_id()).orElse(null);
            m.put("duracion_min", AgendaService.duracionDe(servicio));
            m.put("alergias", mascota != null ? mascota.getAlergias() : null);
            m.put("peso_kg", mascota != null ? mascota.getPeso_kg() : null);
            OrdenExamen orden = c.getOrden_id() == null ? null : ordenRepositorio.findById(c.getOrden_id()).orElse(null);
            m.put("examen", orden != null ? orden.getTipo_examen() : null);
            m.put("examen_apto", orden != null && "Apto_Cirugia".equals(orden.getEstado()) && ExamenService.vigenteEn(orden, LocalDateTime.now()));
            m.put("examen_vigente_hasta", orden != null && orden.getFecha_evaluacion() != null
                    ? orden.getFecha_evaluacion().plusDays(ExamenService.DIAS_VIGENCIA).toLocalDate() : null);
            m.put("inicio_real", c.getInicio_real());
            m.put("fin_real", c.getFin_real());
            m.put("notas_intra", c.getNotas_intra());
            m.put("resultado", c.getResultado());
            m.put("complicaciones", c.getComplicaciones());
        }
        return m;
    }

    private CirugiaProgramada obtener(Integer id) {
        return cirugiaRepositorio.findById(id).orElseThrow(() -> new NegocioException(404, "La cirugía no existe."));
    }

    private static void exigirAsignada(CirugiaProgramada c, Integer veterinarioId) {
        if (veterinarioId == null || !veterinarioId.equals(c.getVeterinario_id())) {
            throw new NegocioException(403, "Esta cirugía está asignada a otro veterinario.");
        }
    }

    private void evento(CirugiaProgramada c, String tipo, String descripcion, Integer vetId) {
        HistorialEvento h = new HistorialEvento();
        h.setMascota_id(c.getMascota_id());
        h.setTipo_evento(tipo);
        h.setFecha(LocalDateTime.now());
        h.setDescripcion(descripcion);
        h.setVeterinario_id(vetId);
        historialRepositorio.save(h);
    }

    private void avisarDueno(CirugiaProgramada c, String titulo, String plantilla) {
        Mascota m = mascotaRepositorio.findById(c.getMascota_id()).orElse(null);
        if (m != null && m.getUsuario_id() != null) {
            notificaciones.notificar(m.getUsuario_id(), titulo, String.format(plantilla, m.getNombre()), "Examen");
        }
    }

    private static String limpiar(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static int largo(String s) {
        return s == null ? 0 : s.length();
    }
}
