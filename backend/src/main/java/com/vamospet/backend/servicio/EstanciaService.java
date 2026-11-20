package com.vamospet.backend.servicio;

import com.vamospet.backend.modelo.*;
import com.vamospet.backend.repositorio.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class EstanciaService {

    public static final int HORA_APERTURA = 8;
    public static final int HORA_CIERRE = 20;
    public static final int MAXIMO_NOCHES = 60;
    public static final String SERVICIO_HOSPEDAJE = "Hospedaje";
    public static final String SERVICIO_INTERNAMIENTO = "Internamiento";
    public static final String SERVICIO_GUARDERIA = "Guardería por día";
    public static final String HOSPEDAJE = "Hospedaje";
    public static final String GUARDERIA = "Guarderia";
    public static final int MAXIMO_DIAS_GUARDERIA = 30;
    public static final int CUPO_GUARDERIA_POR_DEFECTO = 8;

    @Autowired private EstanciaRepository estanciaRepositorio;
    @Autowired private CuartoRepository cuartoRepositorio;
    @Autowired private MascotaRepository mascotaRepositorio;
    @Autowired private UsuarioRepository usuarioRepositorio;
    @Autowired private ServicioRepository servicioRepositorio;
    @Autowired private CirugiaProgramadaRepository cirugiaRepositorio;
    @Autowired private HistorialEventoRepository historialRepositorio;
    @Autowired private PagoRepository pagoRepositorio;
    @Autowired private ComprobanteRepository comprobanteRepositorio;
    @Autowired private AutorizacionService autorizacion;
    @Autowired private PagoService pagoService;
    @Autowired private NotificacionService notificacionService;
    @Autowired private ConfiguracionService configuracion;

    public static long unidades(LocalDateTime desde, LocalDateTime hasta) {
        long minutos = Duration.between(desde, hasta).toMinutes();
        return Math.max(1, (long) Math.ceil(minutos / 1440.0));
    }

    public static long dias(LocalDateTime desde, LocalDateTime hasta) {
        return ChronoUnit.DAYS.between(desde.toLocalDate(), hasta.toLocalDate()) + 1;
    }

    public static boolean solapa(LocalDateTime aIni, LocalDateTime aFin, LocalDateTime bIni, LocalDateTime bFin) {
        return aIni.isBefore(bFin) && bIni.isBefore(aFin);
    }

    public static boolean dentroDeHorario(LocalDateTime f, int apertura, int cierre) {
        int minutos = f.getHour() * 60 + f.getMinute();
        return minutos >= apertura * 60 && minutos <= cierre * 60;
    }

    private LocalDateTime finEfectivo(Estancia e, LocalDateTime ahora) {
        if ("Finalizada".equals(e.getEstado()) && e.getSalida_real() != null) return e.getSalida_real();
        if ("En_curso".equals(e.getEstado()) && e.getSalida_estimada().isBefore(ahora)) return ahora.plusMinutes(1);
        return e.getSalida_estimada();
    }

    public List<Integer> cuartosLibres(LocalDateTime ingreso, LocalDateTime salida, Integer ignorarEstancia) {
        LocalDateTime ahora = LocalDateTime.now();
        List<Estancia> activas = estanciaRepositorio.findAll().stream()
                .filter(e -> !"Cancelada".equals(e.getEstado()) && !e.getId().equals(ignorarEstancia))
                .filter(e -> e.getCuarto_id() != null)
                .toList();
        List<Integer> libres = new ArrayList<>();
        cuartoRepositorio.findAll().stream()
                .filter(c -> !Boolean.FALSE.equals(c.getActivo()))
                .sorted(Comparator.comparing(Cuarto::getNumero))
                .forEach(c -> {
                    boolean ocupado = activas.stream().anyMatch(e -> e.getCuarto_id().equals(c.getId())
                            && solapa(ingreso, salida, e.getIngreso(), finEfectivo(e, ahora)));
                    if (!ocupado) libres.add(c.getId());
                });
        return libres;
    }

    public int cupoGuarderia() {
        return servicioGuarderia().map(sv -> sv.getCupo_diario() == null ? CUPO_GUARDERIA_POR_DEFECTO : sv.getCupo_diario())
                .orElse(CUPO_GUARDERIA_POR_DEFECTO);
    }

    public long ocupadosGuarderia(LocalDate dia) {
        LocalDateTime ahora = LocalDateTime.now();
        return estanciaRepositorio.findAll().stream()
                .filter(e -> GUARDERIA.equals(e.getTipo()) && !"Cancelada".equals(e.getEstado()))
                .filter(e -> !dia.isBefore(e.getIngreso().toLocalDate()) && !dia.isAfter(finEfectivo(e, ahora).toLocalDate()))
                .count();
    }

    public LocalDate primerDiaSinCupo(LocalDateTime ingreso, LocalDateTime salida) {
        int cupo = cupoGuarderia();
        for (LocalDate d = ingreso.toLocalDate(); !d.isAfter(salida.toLocalDate()); d = d.plusDays(1)) {
            if (ocupadosGuarderia(d) >= cupo) return d;
        }
        return null;
    }

    public Map<String, Object> disponibilidad(LocalDateTime ingreso, LocalDateTime salida, String modalidad) {
        if (!GUARDERIA.equals(modalidad)) return disponibilidad(ingreso, salida);
        if (ingreso == null || salida == null || !salida.isAfter(ingreso)) throw new NegocioException(400, "Elige el primer y el último día de guardería.");
        long d = dias(ingreso, salida);
        BigDecimal precio = precioServicio(SERVICIO_GUARDERIA);
        LocalDate sinCupo = primerDiaSinCupo(ingreso, salida);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("modalidad", GUARDERIA);
        r.put("dias", d);
        r.put("cupo_diario", cupoGuarderia());
        r.put("hay_cupo", sinCupo == null);
        r.put("dia_sin_cupo", sinCupo);
        r.put("precio_dia", precio);
        r.put("total", precio.multiply(BigDecimal.valueOf(d)));
        return r;
    }

    public Map<String, Object> disponibilidad(LocalDateTime ingreso, LocalDateTime salida) {
        if (ingreso == null || salida == null || !salida.isAfter(ingreso)) throw new NegocioException(400, "Elige una fecha de ingreso y una de salida válidas.");
        List<Integer> libres = cuartosLibres(ingreso, salida, null);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total_cuartos", cuartoRepositorio.findAll().stream().filter(c -> !Boolean.FALSE.equals(c.getActivo())).count());
        r.put("cuartos_libres", libres.size());
        r.put("noches", unidades(ingreso, salida));
        BigDecimal precio = precioServicio(SERVICIO_HOSPEDAJE);
        r.put("precio_noche", precio);
        r.put("total", precio.multiply(BigDecimal.valueOf(unidades(ingreso, salida))));
        return r;
    }

    public Map<String, Object> configuracion() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("hora_apertura", configuracion.aperturaHospedaje());
        r.put("hora_cierre", configuracion.cierreHospedaje());
        r.put("precio_noche", precioServicio(SERVICIO_HOSPEDAJE));
        r.put("precio_dia_internamiento", precioServicio(SERVICIO_INTERNAMIENTO));
        r.put("cuartos", cuartoRepositorio.findAll().stream().filter(c -> !Boolean.FALSE.equals(c.getActivo())).count());
        BigDecimal precioGuarderia = precioServicio(SERVICIO_GUARDERIA);
        r.put("precio_dia_guarderia", precioGuarderia);
        r.put("guarderia_disponible", precioGuarderia.signum() > 0);
        r.put("cupo_guarderia", cupoGuarderia());
        r.put("maximo_noches", MAXIMO_NOCHES);
        r.put("maximo_dias_guarderia", MAXIMO_DIAS_GUARDERIA);
        return r;
    }

    @Transactional
    public synchronized Map<String, Object> reservarHospedaje(Integer actorId, Integer mascotaId, LocalDateTime ingreso,
                                                              LocalDateTime salida, PagoService.DatosPago datosPago) {
        return reservar(actorId, HOSPEDAJE, mascotaId, ingreso, salida, null, datosPago);
    }

    @Transactional
    public synchronized Map<String, Object> reservar(Integer actorId, String modalidad, Integer mascotaId, LocalDateTime ingreso,
                                                     LocalDateTime salida, String cuidadosEspeciales, PagoService.DatosPago datosPago) {
        boolean guarderia = GUARDERIA.equals(modalidad);
        if (!guarderia && modalidad != null && !HOSPEDAJE.equals(modalidad)) throw new NegocioException(400, "Elige hospedaje por noche o guardería por día.");
        String cuidados = limpiar(cuidadosEspeciales);
        if (cuidados != null && cuidados.length() > 500) throw new NegocioException(400, "Los cuidados especiales admiten hasta 500 caracteres.");
        Usuario actor = autorizacion.conRol(actorId, AutorizacionService.CLIENTE, AutorizacionService.ADMIN, AutorizacionService.AUXILIAR);
        if (actor == null) throw new NegocioException(403, "Inicia sesión para reservar un hospedaje.");
        Mascota mascota = mascotaId == null ? null : mascotaRepositorio.findById(mascotaId).orElse(null);
        if (mascota == null) throw new NegocioException(404, "Elige la mascota que se hospedará.");
        if (mascota.getUsuario_id() == null) throw new NegocioException(409, "La mascota debe estar vinculada a un cliente para hospedarse.");
        boolean personal = PagoService.esPersonalDeCobro(actor);
        if (!personal && !actor.getId().equals(mascota.getUsuario_id())) throw new NegocioException(403, "Solo puedes reservar para tus propias mascotas.");
        if (ingreso == null || salida == null) throw new NegocioException(400, "Indica la fecha y hora de ingreso y de salida.");
        if (ingreso.isBefore(LocalDateTime.now().minusMinutes(5))) throw new NegocioException(400, "La fecha de ingreso ya pasó.");
        if (!salida.isAfter(ingreso)) throw new NegocioException(400, "La salida debe ser posterior al ingreso.");
        int apertura = configuracion.aperturaHospedaje(), cierre = configuracion.cierreHospedaje();
        if (!dentroDeHorario(ingreso, apertura, cierre) || !dentroDeHorario(salida, apertura, cierre)) {
            throw new NegocioException(400, "Los ingresos y salidas se atienden todos los días de " + apertura + ":00 a " + cierre + ":00.");
        }
        long noches = guarderia ? dias(ingreso, salida) : unidades(ingreso, salida);
        if (!guarderia && noches > MAXIMO_NOCHES) throw new NegocioException(400, "La estadía máxima es de " + MAXIMO_NOCHES + " noches.");
        if (guarderia && noches > MAXIMO_DIAS_GUARDERIA) throw new NegocioException(400, "La guardería se reserva por " + MAXIMO_DIAS_GUARDERIA + " días como máximo.");
        LocalDateTime ahora = LocalDateTime.now();
        boolean mascotaOcupada = estanciaRepositorio.findAll().stream().anyMatch(e -> e.getMascota_id().equals(mascotaId)
                && !"Cancelada".equals(e.getEstado()) && solapa(ingreso, salida, e.getIngreso(), finEfectivo(e, ahora)));
        if (mascotaOcupada) throw new NegocioException(409, "Esta mascota ya tiene una estadía o internamiento en esas fechas.");

        Integer cuarto = null;
        BigDecimal precio;
        if (guarderia) {
            precio = precioServicio(SERVICIO_GUARDERIA);
            if (precio.signum() <= 0) throw new NegocioException(409, "La guardería aún no está disponible.");
            LocalDate sinCupo = primerDiaSinCupo(ingreso, salida);
            if (sinCupo != null) throw new NegocioException(409, "La guardería ya no tiene cupo el " + sinCupo + ". Prueba con otras fechas.");
        } else {
            List<Integer> libres = cuartosLibres(ingreso, salida, null);
            if (libres.isEmpty()) throw new NegocioException(409, "No hay cuartos disponibles para esas fechas. Prueba con otras.");
            cuarto = libres.get(0);
            precio = precioServicio(SERVICIO_HOSPEDAJE);
            if (precio.signum() <= 0) throw new NegocioException(409, "El precio del hospedaje aún no está configurado.");
        }

        Estancia e = new Estancia();
        e.setCuarto_id(cuarto);
        e.setMascota_id(mascotaId);
        e.setUsuario_id(mascota.getUsuario_id());
        e.setTipo(guarderia ? GUARDERIA : HOSPEDAJE);
        e.setCuidados_especiales(cuidados);
        e.setIngreso(ingreso);
        e.setSalida_estimada(salida);
        e.setEstado("Reservada");
        e.setPrecio_unitario(precio);
        e.setTotal(precio.multiply(BigDecimal.valueOf(noches)));
        e.setCreado_por(actor.getId());
        e = estanciaRepositorio.save(e);

        PagoService.Resultado r = pagoService.cobrar(actor.getId(), "estancia", e.getId(), datosPago);
        notificacionService.notificar(mascota.getUsuario_id(), guarderia ? "Guardería reservada" : "Hospedaje reservado",
                (guarderia ? "Reservamos guardería para " : "Reservamos un cuarto para ") + mascota.getNombre() + " del "
                        + ingreso.toLocalDate() + " " + ingreso.toLocalTime() + " al " + salida.toLocalDate() + " " + salida.toLocalTime() + ".", "Hospedaje");
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("estancia", e);
        resp.put("pago", r.pago());
        resp.put("comprobante", r.comprobante());
        return resp;
    }

    @Transactional
    public synchronized Estancia internar(Integer usuarioId, Integer cirugiaId, LocalDateTime ingreso, LocalDateTime salidaEstimada, String observaciones) {
        Usuario quien = autorizacion.conRol(usuarioId, AutorizacionService.ADMIN, AutorizacionService.VETERINARIO);
        if (quien == null) throw new NegocioException(403, "Solo un veterinario o el administrador pueden internar a una mascota.");
        CirugiaProgramada cirugia = cirugiaId == null ? null : cirugiaRepositorio.findById(cirugiaId).orElse(null);
        if (cirugia == null) throw new NegocioException(404, "La cirugía no existe.");
        if (quien.getRol_id() == AutorizacionService.VETERINARIO && !quien.getId().equals(cirugia.getVeterinario_id())) {
            throw new NegocioException(403, "Esta cirugía está asignada a otro veterinario.");
        }
        if (!"Postoperatorio".equals(cirugia.getEstado())) throw new NegocioException(409, "Solo se interna a una mascota que está en postoperatorio.");
        if (ingreso == null || salidaEstimada == null) throw new NegocioException(400, "Indica el día y la hora de ingreso y la salida estimada.");
        if (!salidaEstimada.isAfter(ingreso)) throw new NegocioException(400, "La salida estimada debe ser posterior al ingreso.");
        if (ingreso.isBefore(LocalDateTime.now().minusDays(1))) throw new NegocioException(400, "El ingreso no puede ser de hace más de un día.");
        boolean yaInternada = estanciaRepositorio.findAll().stream()
                .anyMatch(e -> cirugiaId.equals(e.getCirugia_id()) && !"Cancelada".equals(e.getEstado()));
        if (yaInternada) throw new NegocioException(409, "Esta cirugía ya tiene un internamiento registrado.");
        Mascota mascota = mascotaRepositorio.findById(cirugia.getMascota_id()).orElse(null);
        if (mascota == null) throw new NegocioException(404, "La mascota no existe.");
        LocalDateTime ahora = LocalDateTime.now();
        boolean mascotaOcupada = estanciaRepositorio.findAll().stream().anyMatch(e -> e.getMascota_id().equals(mascota.getId())
                && !"Cancelada".equals(e.getEstado()) && solapa(ingreso, salidaEstimada, e.getIngreso(), finEfectivo(e, ahora)));
        if (mascotaOcupada) throw new NegocioException(409, "Esta mascota ya tiene una estadía registrada en esas fechas.");
        List<Integer> libres = cuartosLibres(ingreso, salidaEstimada, null);
        if (libres.isEmpty()) throw new NegocioException(409, "No hay cuartos libres para internar en ese horario.");
        BigDecimal precio = precioServicio(SERVICIO_INTERNAMIENTO);
        if (precio.signum() <= 0) throw new NegocioException(409, "El precio del internamiento aún no está configurado.");

        Estancia e = new Estancia();
        e.setCuarto_id(libres.get(0));
        e.setMascota_id(mascota.getId());
        e.setUsuario_id(mascota.getUsuario_id());
        e.setTipo("Internamiento");
        e.setIngreso(ingreso);
        e.setSalida_estimada(salidaEstimada);
        e.setEstado(ingreso.isAfter(ahora) ? "Reservada" : "En_curso");
        e.setPrecio_unitario(precio);
        e.setTotal(BigDecimal.ZERO);
        e.setCirugia_id(cirugiaId);
        e.setObservaciones(limpiar(observaciones));
        e.setCreado_por(quien.getId());
        e = estanciaRepositorio.save(e);

        registrarEvento(mascota.getId(), "Internamiento", "Internamiento tras " + cirugia.getTipo_cirugia() + " desde el "
                + ingreso.toLocalDate() + " " + ingreso.toLocalTime().withSecond(0).withNano(0) + ". Salida estimada: "
                + salidaEstimada.toLocalDate() + " " + salidaEstimada.toLocalTime().withSecond(0).withNano(0) + ".", quien.getId());
        if (mascota.getUsuario_id() != null) {
            notificacionService.notificar(mascota.getUsuario_id(), "Tu mascota fue internada",
                    mascota.getNombre() + " queda en observación en la clínica tras su cirugía. Te avisaremos cuando reciba el alta.", "Hospedaje");
        }
        return e;
    }

    @Transactional
    public synchronized Estancia registrarIngreso(Integer usuarioId, Integer estanciaId) {
        Usuario quien = autorizacion.conRol(usuarioId, AutorizacionService.ADMIN, AutorizacionService.AUXILIAR, AutorizacionService.VETERINARIO);
        if (quien == null) throw new NegocioException(403, "No tienes permiso para registrar ingresos.");
        Estancia e = obtener(estanciaId);
        if (!"Reservada".equals(e.getEstado())) throw new NegocioException(409, "Solo se registra el ingreso de una reserva (estado actual: " + e.getEstado() + ").");
        if (!"Internamiento".equals(e.getTipo())) {
            boolean rechazado = pagoRepositorio.findAll().stream().noneMatch(p -> estanciaId.equals(p.getEstancia_id()) && !PagoService.RECHAZADO.equals(p.getEstado()));
            if (rechazado) throw new NegocioException(409, "La reserva no tiene un pago vigente.");
        }
        e.setEstado("En_curso");
        return estanciaRepositorio.save(e);
    }

    @Transactional
    public synchronized Estancia finalizar(Integer usuarioId, Integer estanciaId) {
        Usuario quien = autorizacion.conRol(usuarioId, AutorizacionService.ADMIN, AutorizacionService.AUXILIAR, AutorizacionService.VETERINARIO);
        if (quien == null) throw new NegocioException(403, "No tienes permiso para registrar salidas.");
        Estancia e = obtener(estanciaId);
        boolean internamiento = "Internamiento".equals(e.getTipo());
        if (internamiento && quien.getRol_id() == AutorizacionService.AUXILIAR) {
            throw new NegocioException(403, "El alta de un internamiento la da el veterinario.");
        }
        if (!internamiento && quien.getRol_id() == AutorizacionService.VETERINARIO) {
            throw new NegocioException(403, "La salida de un hospedaje la registra el personal de recepción.");
        }
        if (!"En_curso".equals(e.getEstado())) throw new NegocioException(409, "Solo se finaliza una estadía en curso (estado actual: " + e.getEstado() + ").");
        LocalDateTime ahora = LocalDateTime.now();
        e.setSalida_real(ahora);
        e.setEstado("Finalizada");
        if (internamiento) e.setTotal(e.getPrecio_unitario().multiply(BigDecimal.valueOf(unidades(e.getIngreso(), ahora))));
        e = estanciaRepositorio.save(e);
        Mascota mascota = mascotaRepositorio.findById(e.getMascota_id()).orElse(null);
        if (internamiento) {
            registrarEvento(e.getMascota_id(), "Alta de internamiento", "Alta del internamiento el " + ahora.toLocalDate() + " "
                    + ahora.toLocalTime().withSecond(0).withNano(0) + ". Total: S/ " + e.getTotal() + ".", quien.getId());
            if (mascota != null && mascota.getUsuario_id() != null) {
                notificacionService.notificar(mascota.getUsuario_id(), "Alta de internamiento",
                        mascota.getNombre() + " recibió el alta. El total del internamiento es S/ " + e.getTotal() + ". Puedes pagarlo en Mis pagos.", "Hospedaje");
            }
        } else if (mascota != null && mascota.getUsuario_id() != null) {
            notificacionService.notificar(mascota.getUsuario_id(), "Salida de hospedaje",
                    "Registramos la salida de " + mascota.getNombre() + ". ¡Gracias por confiar en Vamos Pet!", "Hospedaje");
        }
        return e;
    }

    @Transactional
    public synchronized Estancia cancelar(Integer usuarioId, Integer estanciaId) {
        Usuario quien = autorizacion.conRol(usuarioId, AutorizacionService.ADMIN, AutorizacionService.AUXILIAR);
        if (quien == null) throw new NegocioException(403, "Solo el administrador o el auxiliar pueden cancelar reservas.");
        Estancia e = obtener(estanciaId);
        if (!"Reservada".equals(e.getEstado())) throw new NegocioException(409, "Solo se cancela una reserva que aún no ingresó (estado actual: " + e.getEstado() + ").");
        e.setEstado("Cancelada");
        e = estanciaRepositorio.save(e);
        if (e.getUsuario_id() != null) {
            notificacionService.notificar(e.getUsuario_id(), "Reserva cancelada",
                    "Tu reserva #" + e.getId() + " fue cancelada. Si ya pagaste, la clínica coordinará contigo la devolución.", "Hospedaje");
        }
        return e;
    }

    public List<Map<String, Object>> listar(Integer usuarioId) {
        Usuario quien = autorizacion.conRol(usuarioId, AutorizacionService.ADMIN, AutorizacionService.CLIENTE,
                AutorizacionService.VETERINARIO, AutorizacionService.AUXILIAR);
        if (quien == null) throw new NegocioException(403, "Inicia sesión para ver las estadías.");
        boolean interno = quien.getRol_id() != AutorizacionService.CLIENTE;
        List<Map<String, Object>> lista = new ArrayList<>();
        estanciaRepositorio.findAll().stream()
                .filter(e -> interno || quien.getId().equals(e.getUsuario_id()))
                .sorted(Comparator.comparing(Estancia::getIngreso).reversed())
                .forEach(e -> lista.add(describir(e)));
        return lista;
    }

    public List<Map<String, Object>> estadoCuartos(Integer usuarioId) {
        Usuario quien = autorizacion.conRol(usuarioId, AutorizacionService.ADMIN, AutorizacionService.AUXILIAR, AutorizacionService.VETERINARIO);
        if (quien == null) throw new NegocioException(403, "No tienes permiso para ver los cuartos.");
        LocalDateTime ahora = LocalDateTime.now();
        List<Estancia> activas = estanciaRepositorio.findAll().stream().filter(e -> !"Cancelada".equals(e.getEstado())).toList();
        List<Map<String, Object>> cuartos = new ArrayList<>();
        cuartoRepositorio.findAll().stream().sorted(Comparator.comparing(Cuarto::getNumero)).forEach(c -> {
            Estancia actual = activas.stream().filter(e -> c.getId().equals(e.getCuarto_id())
                    && "En_curso".equals(e.getEstado())).findFirst().orElse(null);
            Estancia proxima = activas.stream().filter(e -> c.getId().equals(e.getCuarto_id())
                    && "Reservada".equals(e.getEstado()) && e.getSalida_estimada().isAfter(ahora))
                    .min(Comparator.comparing(Estancia::getIngreso)).orElse(null);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("nombre", c.getNombre());
            m.put("ocupado", actual != null);
            m.put("mascota", actual != null ? nombreMascota(actual.getMascota_id()) : null);
            m.put("tipo", actual != null ? actual.getTipo() : null);
            m.put("proxima_reserva", proxima != null ? proxima.getIngreso() : null);
            cuartos.add(m);
        });
        return cuartos;
    }

    private Map<String, Object> describir(Estancia e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId());
        m.put("tipo", e.getTipo());
        m.put("estado", e.getEstado());
        m.put("mascota_id", e.getMascota_id());
        m.put("cirugia_id", e.getCirugia_id());
        m.put("mascota", nombreMascota(e.getMascota_id()));
        Usuario c = e.getUsuario_id() == null ? null : usuarioRepositorio.findById(e.getUsuario_id()).orElse(null);
        m.put("cliente", c != null ? c.getNombre() : null);
        Cuarto cuarto = e.getCuarto_id() == null ? null : cuartoRepositorio.findById(e.getCuarto_id()).orElse(null);
        m.put("cuarto", cuarto != null ? cuarto.getNombre() : null);
        m.put("cuidados_especiales", e.getCuidados_especiales());
        m.put("ingreso", e.getIngreso());
        m.put("salida_estimada", e.getSalida_estimada());
        m.put("salida_real", e.getSalida_real());
        m.put("precio_unitario", e.getPrecio_unitario());
        m.put("total", e.getTotal());
        m.put("observaciones", e.getObservaciones());
        Pago pago = pagoRepositorio.findAll().stream()
                .filter(p -> e.getId().equals(p.getEstancia_id()) && !PagoService.RECHAZADO.equals(p.getEstado()))
                .findFirst().orElse(null);
        m.put("pago_estado", pago != null ? pago.getEstado() : null);
        Comprobante comp = pago == null ? null : comprobanteRepositorio.findAll().stream()
                .filter(x -> pago.getId().equals(x.getPago_id())).findFirst().orElse(null);
        m.put("comprobante_id", comp != null ? comp.getId() : null);
        return m;
    }

    private String nombreMascota(Integer id) {
        Mascota m = id == null ? null : mascotaRepositorio.findById(id).orElse(null);
        return m != null ? m.getNombre() : null;
    }

    private Estancia obtener(Integer id) {
        Estancia e = id == null ? null : estanciaRepositorio.findById(id).orElse(null);
        if (e == null) throw new NegocioException(404, "La estadía no existe.");
        return e;
    }

    private Optional<Servicio> servicioGuarderia() {
        return servicioRepositorio.findAll().stream()
                .filter(sv -> sv.getNombre() != null && sv.getNombre().equalsIgnoreCase(SERVICIO_GUARDERIA)).findFirst();
    }

    private BigDecimal precioServicio(String nombre) {
        return servicioRepositorio.findAll().stream()
                .filter(s -> s.getNombre() != null && s.getNombre().equalsIgnoreCase(nombre) && !Boolean.FALSE.equals(s.getActivo()) && s.getPrecio_base() != null)
                .map(Servicio::getPrecio_base).findFirst().orElse(BigDecimal.ZERO);
    }

    private void registrarEvento(Integer mascotaId, String tipo, String descripcion, Integer vetId) {
        HistorialEvento h = new HistorialEvento();
        h.setMascota_id(mascotaId);
        h.setTipo_evento(tipo);
        h.setFecha(LocalDateTime.now());
        h.setDescripcion(descripcion);
        h.setVeterinario_id(vetId);
        historialRepositorio.save(h);
    }

    private static String limpiar(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : (t.length() > 2000 ? t.substring(0, 2000) : t);
    }
}
