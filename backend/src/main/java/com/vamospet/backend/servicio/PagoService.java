package com.vamospet.backend.servicio;

import com.vamospet.backend.modelo.*;
import com.vamospet.backend.repositorio.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class PagoService {

    private static final Logger log = LoggerFactory.getLogger(PagoService.class);

    public static final String TARJETA = "Tarjeta";
    public static final String YAPE = "Yape";
    public static final String EFECTIVO = "Efectivo";
    public static final String PAGADO = "Pagado";
    public static final String POR_CONFIRMAR = "Por_confirmar";
    public static final String RECHAZADO = "Rechazado";

    private static final Pattern CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern ULTIMOS4 = Pattern.compile("^\\d{4}$");
    private static final Pattern OPERACION = Pattern.compile("^[A-Za-z0-9]{6,20}$");

    public record DatosPago(String metodo, String correoTarjeta, String ultimos4, String operacionYape) {}

    public record Resultado(Pago pago, Comprobante comprobante) {}

    @Value("${app.frontend.url:http://localhost:8080/index.html}")
    private String frontendUrl;

    @Autowired private PagoRepository pagoRepositorio;
    @Autowired private ComprobanteRepository comprobanteRepositorio;
    @Autowired private CitaRepository citaRepositorio;
    @Autowired private ServicioRepository servicioRepositorio;
    @Autowired private CitaCostoAdicionalRepository costoRepositorio;
    @Autowired private MascotaRepository mascotaRepositorio;
    @Autowired private PedidoRepository pedidoRepositorio;
    @Autowired private PedidoDetalleRepository pedidoDetalleRepositorio;
    @Autowired private EstanciaRepository estanciaRepositorio;
    @Autowired private ViajeSolicitudRepository viajeRepositorio;
    @Autowired private UsuarioRepository usuarioRepositorio;
    @Autowired private AutorizacionService autorizacion;
    @Autowired private NotificacionService notificacionService;
    @Autowired private ComprobantePdfService comprobantePdfService;

    @Transactional
    public synchronized Resultado cobrar(Integer actorId, String tipoReferencia, Integer referenciaId, DatosPago datos) {
        Usuario actor = autorizacion.conRol(actorId, AutorizacionService.ADMIN, AutorizacionService.CLIENTE,
                AutorizacionService.VETERINARIO, AutorizacionService.AUXILIAR);
        if (actor == null) throw new NegocioException(403, "Inicia sesión para registrar un pago.");
        boolean personal = esPersonalDeCobro(actor);
        if (datos == null) throw new NegocioException(400, "Indica el método de pago.");
        String metodo = normalizarMetodo(datos.metodo());
        if (referenciaId == null || tipoReferencia == null) throw new NegocioException(400, "Falta indicar qué se está pagando.");

        Integer duenoId;
        BigDecimal monto;
        Pago pago = new Pago();
        switch (tipoReferencia) {
            case "cita" -> {
                Cita cita = citaRepositorio.findById(referenciaId).orElse(null);
                if (cita == null) throw new NegocioException(404, "La cita no existe.");
                if (!"Atendida".equals(cita.getEstado())) throw new NegocioException(409, "Solo se pagan citas Atendidas.");
                Mascota m = cita.getMascota_id() == null ? null : mascotaRepositorio.findById(cita.getMascota_id()).orElse(null);
                if (m == null || m.getUsuario_id() == null) throw new NegocioException(409, "La cita no tiene un cliente asociado.");
                duenoId = m.getUsuario_id();
                monto = totalCita(cita);
                pago.setCita_id(referenciaId);
            }
            case "pedido" -> {
                Pedido pedido = pedidoRepositorio.findById(referenciaId).orElse(null);
                if (pedido == null) throw new NegocioException(404, "El pedido no existe.");
                duenoId = pedido.getUsuario_id();
                monto = totalPedido(pedido);
                pago.setPedido_id(referenciaId);
            }
            case "estancia" -> {
                Estancia e = estanciaRepositorio.findById(referenciaId).orElse(null);
                if (e == null) throw new NegocioException(404, "La reserva no existe.");
                if ("Cancelada".equals(e.getEstado())) throw new NegocioException(409, "La reserva está cancelada.");
                if ("Internamiento".equals(e.getTipo()) && !"Finalizada".equals(e.getEstado())) {
                    throw new NegocioException(409, "El internamiento se paga cuando la mascota recibe el alta.");
                }
                duenoId = e.getUsuario_id();
                monto = e.getTotal();
                pago.setEstancia_id(referenciaId);
            }
            case "viaje" -> {
                ViajeSolicitud v = viajeRepositorio.findById(referenciaId).orElse(null);
                if (v == null) throw new NegocioException(404, "La solicitud de viaje no existe.");
                if ("Cancelada".equals(v.getEstado())) throw new NegocioException(409, "La solicitud está cancelada.");
                duenoId = v.getUsuario_id();
                monto = v.getTotal();
                pago.setViaje_id(referenciaId);
            }
            default -> throw new NegocioException(400, "Tipo de pago no válido.");
        }
        if (duenoId == null) throw new NegocioException(409, "No hay un cliente asociado a este pago.");
        if (!personal && !actor.getId().equals(duenoId)) throw new NegocioException(403, "Solo puedes pagar tus propias cuentas.");
        if (monto == null || monto.signum() <= 0) throw new NegocioException(409, "El monto a pagar no es válido.");
        if (tienePagoVigente(pago)) throw new NegocioException(409, "Esto ya tiene un pago registrado.");

        String estado = PAGADO;
        switch (metodo) {
            case EFECTIVO -> {
                if (!personal) throw new NegocioException(400, "El pago en efectivo solo se registra en mostrador con el personal.");
            }
            case TARJETA -> {
                String correo = datos.correoTarjeta() == null ? "" : datos.correoTarjeta().trim();
                if (correo.length() > 150 || !CORREO.matcher(correo).matches()) {
                    throw new NegocioException(400, "Escribe el correo asociado a la tarjeta (se enviará tu comprobante ahí).");
                }
                String ultimos = datos.ultimos4() == null ? "" : datos.ultimos4().trim();
                if (!ULTIMOS4.matcher(ultimos).matches()) throw new NegocioException(400, "Los datos de la tarjeta no son válidos.");
                pago.setCorreo_tarjeta(correo);
                pago.setTarjeta_ultimos4(ultimos);
            }
            default -> {
                String op = datos.operacionYape() == null ? "" : datos.operacionYape().trim();
                if (!OPERACION.matcher(op).matches()) {
                    throw new NegocioException(400, "Escribe el número de operación de tu Yape (6 a 20 letras o números).");
                }
                pago.setOperacion_yape(op);
                estado = personal ? PAGADO : POR_CONFIRMAR;
            }
        }

        pago.setUsuario_id(duenoId);
        pago.setMonto(monto.setScale(2, RoundingMode.HALF_UP));
        pago.setMetodo(metodo);
        pago.setEstado(estado);
        pago.setFecha(LocalDateTime.now());
        pago.setId_registrado_por(actor.getId());
        pago = pagoRepositorio.save(pago);

        Comprobante comprobante = null;
        if (PAGADO.equals(estado)) {
            comprobante = emitirComprobante(pago);
            efectosDePagoAprobado(pago);
            avisarPagoAprobado(pago, comprobante);
        } else {
            avanzarViajeSiCorresponde(pago, "Pago_por_confirmar");
            avisarAlPersonal(pago);
            notificacionService.notificar(duenoId, "Pago por confirmar",
                    "Recibimos tu pago por Yape (operación " + pago.getOperacion_yape() + "). Lo confirmaremos en breve y te enviaremos tu comprobante.", "Pago");
        }
        return new Resultado(pago, comprobante);
    }

    @Transactional
    public synchronized Resultado resolverYape(Integer staffId, Integer pagoId, boolean aprobar, String motivo) {
        Usuario staff = autorizacion.conRol(staffId, AutorizacionService.ADMIN, AutorizacionService.AUXILIAR);
        if (staff == null) throw new NegocioException(403, "Solo el administrador o el auxiliar pueden confirmar pagos.");
        Pago pago = pagoId == null ? null : pagoRepositorio.findById(pagoId).orElse(null);
        if (pago == null) throw new NegocioException(404, "El pago no existe.");
        if (!POR_CONFIRMAR.equals(pago.getEstado())) {
            throw new NegocioException(409, "Este pago ya fue resuelto (estado: " + pago.getEstado() + ").");
        }
        if (aprobar) {
            pago.setEstado(PAGADO);
            pago = pagoRepositorio.save(pago);
            Comprobante c = emitirComprobante(pago);
            efectosDePagoAprobado(pago);
            avisarPagoAprobado(pago, c);
            return new Resultado(pago, c);
        }
        String razon = motivo == null || motivo.isBlank() ? "No pudimos verificar la operación." : motivo.trim();
        pago.setEstado(RECHAZADO);
        pago = pagoRepositorio.save(pago);
        cancelarReferencia(pago);
        if (pago.getUsuario_id() != null) {
            notificacionService.notificar(pago.getUsuario_id(), "Pago rechazado",
                    "Tu pago por Yape de S/ " + pago.getMonto() + " fue rechazado: " + razon + " Puedes volver a intentarlo o escribirnos.", "Pago");
        }
        return new Resultado(pago, null);
    }

    public BigDecimal totalCita(Cita cita) {
        BigDecimal base = cita.getCosto();
        if (base == null && cita.getServicio_id() != null) {
            Servicio s = servicioRepositorio.findById(cita.getServicio_id()).orElse(null);
            if (s != null) base = s.getPrecio_base();
        }
        if (base == null) base = BigDecimal.ZERO;
        BigDecimal extras = costoRepositorio.findAll().stream()
                .filter(c -> cita.getId().equals(c.getCita_id()))
                .map(CitaCostoAdicional::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return base.add(extras);
    }

    public BigDecimal totalPedido(Pedido pedido) {
        BigDecimal suma = pedidoDetalleRepositorio.findAll().stream()
                .filter(d -> pedido.getId().equals(d.getPedido_id()) && d.getPrecio_unitario() != null && d.getCantidad() != null)
                .map(d -> d.getPrecio_unitario().multiply(BigDecimal.valueOf(d.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return suma.signum() > 0 ? suma : pedido.getTotal();
    }

    public boolean tienePagoVigente(Pago ref) {
        return pagoRepositorio.findAll().stream().anyMatch(p -> !RECHAZADO.equals(p.getEstado()) && mismaReferencia(p, ref));
    }

    public static boolean mismaReferencia(Pago a, Pago b) {
        return (a.getCita_id() != null && a.getCita_id().equals(b.getCita_id()))
                || (a.getPedido_id() != null && a.getPedido_id().equals(b.getPedido_id()))
                || (a.getEstancia_id() != null && a.getEstancia_id().equals(b.getEstancia_id()))
                || (a.getViaje_id() != null && a.getViaje_id().equals(b.getViaje_id()));
    }

    public static String normalizarMetodo(String metodo) {
        String m = metodo == null ? "" : metodo.trim().toLowerCase();
        return switch (m) {
            case "tarjeta" -> TARJETA;
            case "yape" -> YAPE;
            case "efectivo" -> EFECTIVO;
            default -> throw new NegocioException(400, "Elige un método de pago: Tarjeta, Yape o Efectivo.");
        };
    }

    public static boolean esPersonalDeCobro(Usuario u) {
        return u.getRol_id() != null && (u.getRol_id() == AutorizacionService.ADMIN || u.getRol_id() == AutorizacionService.AUXILIAR);
    }

    private Comprobante emitirComprobante(Pago pago) {
        String base;
        if (pago.getCita_id() != null) base = "B001-C" + pago.getCita_id();
        else if (pago.getPedido_id() != null) base = "B001-" + pago.getPedido_id();
        else if (pago.getEstancia_id() != null) base = "B001-E" + pago.getEstancia_id();
        else base = "B001-V" + pago.getViaje_id();
        final String propuesto = base;
        boolean usado = comprobanteRepositorio.findAll().stream().anyMatch(c -> propuesto.equals(c.getNumero()));
        Comprobante c = new Comprobante();
        c.setPago_id(pago.getId());
        c.setTipo("Boleta");
        c.setNumero(usado ? base + "-" + pago.getId() : base);
        c.setCodigo_verificacion(UUID.randomUUID().toString());
        return comprobanteRepositorio.save(c);
    }

    private void efectosDePagoAprobado(Pago pago) {
        avanzarViajeSiCorresponde(pago, "Registrada");
        if (pago.getPedido_id() != null) {
            pedidoRepositorio.findById(pago.getPedido_id()).ifPresent(p -> {
                if (VentaService.MOSTRADOR.equals(p.getCanal()) && "Pendiente".equals(p.getEstado())) {
                    p.setEstado("Entregado");
                    pedidoRepositorio.save(p);
                }
            });
        }
    }

    private void avanzarViajeSiCorresponde(Pago pago, String estado) {
        if (pago.getViaje_id() == null) return;
        viajeRepositorio.findById(pago.getViaje_id()).ifPresent(v -> {
            if ("Pago_por_confirmar".equals(v.getEstado()) || "Registrada".equals(v.getEstado()) || v.getEstado() == null) {
                v.setEstado(estado);
                viajeRepositorio.save(v);
            }
        });
    }

    private void cancelarReferencia(Pago pago) {
        if (pago.getViaje_id() != null) {
            viajeRepositorio.findById(pago.getViaje_id()).ifPresent(v -> {
                v.setEstado("Cancelada");
                viajeRepositorio.save(v);
            });
        }
        if (pago.getEstancia_id() != null) {
            estanciaRepositorio.findById(pago.getEstancia_id()).ifPresent(e -> {
                if ("Hospedaje".equals(e.getTipo()) && "Reservada".equals(e.getEstado())) {
                    e.setEstado("Cancelada");
                    estanciaRepositorio.save(e);
                }
            });
        }
    }

    private void avisarPagoAprobado(Pago pago, Comprobante comprobante) {
        String concepto = describirConcepto(pago);
        if (pago.getUsuario_id() != null) {
            notificacionService.notificar(pago.getUsuario_id(), "Pago confirmado",
                    "Recibimos tu pago de S/ " + pago.getMonto() + " por " + concepto + ". Tu comprobante es el " + comprobante.getNumero() + ".", "Pago");
        }
        if (PagoService.TARJETA.equals(pago.getMetodo()) && pago.getCorreo_tarjeta() != null) {
            String texto = "Gracias por tu pago en Vamos Pet.\n\nConcepto: " + concepto + "\nMonto: S/ " + pago.getMonto()
                    + "\nMétodo: Tarjeta terminada en " + pago.getTarjeta_ultimos4()
                    + "\nComprobante: " + comprobante.getNumero()
                    + "\nVerifícalo aquí: " + frontendUrl + "?verificar=" + comprobante.getCodigo_verificacion();
            enviarComprobantePorCorreo(pago.getCorreo_tarjeta(), comprobante, texto);
        }
    }

    void enviarComprobantePorCorreo(String destino, Comprobante comprobante, String texto) {
        byte[] pdf = null;
        try {
            pdf = comprobantePdfService.generar(comprobante);
        } catch (Exception e) {
            log.warn("No se pudo generar el PDF del comprobante {}: {}", comprobante.getNumero(), e.getMessage());
        }
        String asunto = "Vamos Pet — Comprobante " + comprobante.getNumero();
        if (pdf != null && pdf.length > 0) {
            notificacionService.enviarCorreo(destino, asunto, texto + "\n\nAdjuntamos tu comprobante en PDF.", "comprobante-" + comprobante.getNumero() + ".pdf", pdf);
        } else {
            notificacionService.enviarCorreoDirecto(destino, "Comprobante " + comprobante.getNumero(), texto);
        }
    }

    private void avisarAlPersonal(Pago pago) {
        String aviso = "Pago por Yape de S/ " + pago.getMonto() + " por " + describirConcepto(pago)
                + " (operación " + pago.getOperacion_yape() + "). Confírmalo en Finanzas.";
        usuarioRepositorio.findAll().stream()
                .filter(u -> Boolean.TRUE.equals(u.getActivo()) && u.getRol_id() != null
                        && (u.getRol_id() == AutorizacionService.ADMIN || u.getRol_id() == AutorizacionService.AUXILIAR))
                .forEach(u -> notificacionService.notificar(u.getId(), "Pago Yape por confirmar", aviso, "Pago"));
    }

    public String describirConcepto(Pago pago) {
        if (pago.getCita_id() != null) return "la cita #" + pago.getCita_id();
        if (pago.getPedido_id() != null) return "el pedido #" + pago.getPedido_id();
        if (pago.getEstancia_id() != null) {
            Estancia e = estanciaRepositorio.findById(pago.getEstancia_id()).orElse(null);
            return (e != null && "Internamiento".equals(e.getTipo()) ? "el internamiento #" : "la reserva de hospedaje #") + pago.getEstancia_id();
        }
        return "tu solicitud de viaje #" + pago.getViaje_id();
    }

    public static final Set<String> METODOS = Set.of(TARJETA, YAPE, EFECTIVO);
}
