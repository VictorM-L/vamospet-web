package com.vamospet.backend.seguridad;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IntentosLoginService {

    public static final int MAXIMO_FALLOS = 5;
    public static final Duration BLOQUEO = Duration.ofMinutes(15);
    private static final int LIMPIAR_DESDE = 1000;

    private record Estado(int fallos, Instant primerFallo, Instant bloqueadoHasta) {
    }

    private final Clock reloj;
    private final Map<String, Estado> estados = new ConcurrentHashMap<>();

    @Autowired
    public IntentosLoginService() {
        this(Clock.systemUTC());
    }

    public IntentosLoginService(Clock reloj) {
        this.reloj = reloj;
    }

    private static String clave(String correo) {
        return correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);
    }

    public Duration bloqueoRestante(String correo) {
        Estado e = estados.get(clave(correo));
        if (e == null || e.bloqueadoHasta() == null) return null;
        Duration resta = Duration.between(reloj.instant(), e.bloqueadoHasta());
        if (resta.isNegative() || resta.isZero()) {
            estados.remove(clave(correo));
            return null;
        }
        return resta;
    }

    public synchronized int registrarFallo(String correo) {
        Instant ahora = reloj.instant();
        if (estados.size() > LIMPIAR_DESDE) {
            estados.entrySet().removeIf(x -> vencido(x.getValue(), ahora));
        }
        String k = clave(correo);
        Estado e = estados.get(k);
        if (e == null || vencido(e, ahora)) e = new Estado(0, ahora, null);
        int fallos = e.fallos() + 1;
        Instant hasta = fallos >= MAXIMO_FALLOS ? ahora.plus(BLOQUEO) : null;
        estados.put(k, new Estado(fallos, e.primerFallo(), hasta));
        return Math.max(0, MAXIMO_FALLOS - fallos);
    }

    public void limpiar(String correo) {
        estados.remove(clave(correo));
    }

    private static boolean vencido(Estado e, Instant ahora) {
        if (e.bloqueadoHasta() != null) return !ahora.isBefore(e.bloqueadoHasta());
        return !ahora.isBefore(e.primerFallo().plus(BLOQUEO));
    }

    public static String mensajeBloqueo(Duration resta) {
        long minutos = Math.max(1, (resta.getSeconds() + 59) / 60);
        return "Por seguridad bloqueamos el ingreso con este correo durante " + BLOQUEO.toMinutes()
                + " minutos tras " + MAXIMO_FALLOS + " intentos fallidos. Vuelve a intentarlo en " + minutos
                + (minutos == 1 ? " minuto" : " minutos") + " o recupera tu contraseña.";
    }

    public static String mensajeFallo(int quedan) {
        if (quedan > 2) return "Correo o contraseña incorrectos.";
        return "Correo o contraseña incorrectos. Te " + (quedan == 1 ? "queda 1 intento" : "quedan " + quedan + " intentos")
                + " antes de un bloqueo de " + BLOQUEO.toMinutes() + " minutos.";
    }
}
