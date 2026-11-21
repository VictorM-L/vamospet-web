package com.vamospet.backend.servicio;

import com.vamospet.backend.modelo.CodigoVerificacion;
import com.vamospet.backend.repositorio.CodigoVerificacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Service
public class CodigoService {

    public static final String REGISTRO = "Registro";
    public static final String RECUPERACION = "Recuperacion";
    public static final int MAX_INTENTOS = 5;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired
    private CodigoVerificacionRepository repositorio;

    public String generar(Integer usuarioId, String proposito, int minutosVigencia) {
        LocalDateTime ahora = LocalDateTime.now();
        List<CodigoVerificacion> anteriores = repositorio.buscarRecientes(usuarioId, proposito);
        for (CodigoVerificacion anterior : anteriores) {
            if (!Boolean.TRUE.equals(anterior.getUsado())) {
                anterior.setUsado(true);
                repositorio.save(anterior);
            }
        }
        String codigo = String.format("%06d", RANDOM.nextInt(1_000_000));
        CodigoVerificacion nuevo = new CodigoVerificacion();
        nuevo.setUsuario_id(usuarioId);
        nuevo.setProposito(proposito);
        nuevo.setCodigo_hash(hash(usuarioId, codigo));
        nuevo.setCreado(ahora);
        nuevo.setExpira(ahora.plusMinutes(minutosVigencia));
        nuevo.setIntentos(0);
        nuevo.setUsado(false);
        repositorio.save(nuevo);
        return codigo;
    }

    public long enviadosDesde(Integer usuarioId, String proposito, LocalDateTime desde) {
        return repositorio.contarDesde(usuarioId, proposito, desde);
    }

    public Optional<CodigoVerificacion> ultimo(Integer usuarioId, String proposito) {
        return repositorio.buscarRecientes(usuarioId, proposito).stream().findFirst();
    }

    public CodigoVerificacion validar(Integer usuarioId, String proposito, String codigo) {
        CodigoVerificacion c = ultimo(usuarioId, proposito)
                .orElseThrow(() -> new NegocioException(410, "No hay un código vigente. Pide uno nuevo."));
        if (Boolean.TRUE.equals(c.getUsado())) {
            throw new NegocioException(410, "Ese código ya fue usado. Pide uno nuevo.");
        }
        if (c.getExpira() == null || c.getExpira().isBefore(LocalDateTime.now())) {
            throw new NegocioException(410, "El código venció. Pide uno nuevo.");
        }
        int intentos = c.getIntentos() == null ? 0 : c.getIntentos();
        if (intentos >= MAX_INTENTOS) {
            throw new NegocioException(429, "Demasiados intentos. Pide un código nuevo.");
        }
        String limpio = codigo == null ? "" : codigo.trim();
        boolean igual = MessageDigest.isEqual(
                hash(usuarioId, limpio).getBytes(StandardCharsets.UTF_8),
                String.valueOf(c.getCodigo_hash()).getBytes(StandardCharsets.UTF_8));
        if (!igual) {
            intentos++;
            c.setIntentos(intentos);
            repositorio.save(c);
            int quedan = MAX_INTENTOS - intentos;
            if (quedan <= 0) {
                throw new NegocioException(429, "Demasiados intentos. Pide un código nuevo.");
            }
            throw new NegocioException(400, "Código incorrecto. Te queda" + (quedan == 1 ? " 1 intento." : "n " + quedan + " intentos."));
        }
        return c;
    }

    public void marcarUsado(CodigoVerificacion c) {
        c.setUsado(true);
        repositorio.save(c);
    }

    String hash(Integer usuarioId, String codigo) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest((usuarioId + ":" + codigo).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(h);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
