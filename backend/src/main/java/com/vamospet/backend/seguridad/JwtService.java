package com.vamospet.backend.seguridad;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class JwtService {

    public static final int MINIMO_SECRETO = 32;
    private static final String CABECERA = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
    private static final String CABECERA_CODIFICADA = codificar(CABECERA.getBytes(StandardCharsets.UTF_8));
    private static final Pattern CAMPO = Pattern.compile("\"(sub|jti|iat|exp|ini)\":\"?([A-Za-z0-9-]+)\"?");

    private final byte[] secreto;
    private final Clock reloj;
    private final Map<String, Instant> revocados = new ConcurrentHashMap<>();

    @Autowired
    public JwtService(@Value("${jwt.secreto:}") String secreto) {
        this(secreto, Clock.systemUTC());
    }

    public JwtService(String secreto, Clock reloj) {
        if (secreto == null || secreto.getBytes(StandardCharsets.UTF_8).length < MINIMO_SECRETO) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos " + MINIMO_SECRETO + " caracteres. Defínelo en backend/.env.properties.");
        }
        this.secreto = secreto.getBytes(StandardCharsets.UTF_8);
        this.reloj = reloj;
    }

    public record Token(Integer usuarioId, String jti, Instant emitido, Instant expira, Instant inicio) {
    }

    public Instant ahora() {
        return Instant.ofEpochSecond(reloj.instant().getEpochSecond());
    }

    public String emitir(Integer usuarioId, Instant inicio, Instant expira) {
        Instant iat = ahora();
        String cuerpo = "{\"sub\":\"" + usuarioId + "\",\"jti\":\"" + UUID.randomUUID() + "\",\"iat\":" + iat.getEpochSecond()
                + ",\"exp\":" + expira.getEpochSecond() + ",\"ini\":" + inicio.getEpochSecond() + "}";
        String base = CABECERA_CODIFICADA + "." + codificar(cuerpo.getBytes(StandardCharsets.UTF_8));
        return base + "." + codificar(firmar(base));
    }

    public String emitir(Integer usuarioId, Duration vigencia) {
        Instant ahora = ahora();
        return emitir(usuarioId, ahora, ahora.plus(vigencia));
    }

    public Token validar(String token) {
        if (token == null) return null;
        String[] partes = token.split("\\.", -1);
        if (partes.length != 3 || !CABECERA_CODIFICADA.equals(partes[0])) return null;
        byte[] firma;
        String cuerpo;
        try {
            firma = Base64.getUrlDecoder().decode(partes[2]);
            cuerpo = new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return null;
        }
        if (!MessageDigest.isEqual(firmar(partes[0] + "." + partes[1]), firma)) return null;
        Map<String, String> campos = new ConcurrentHashMap<>();
        Matcher m = CAMPO.matcher(cuerpo);
        while (m.find()) campos.put(m.group(1), m.group(2));
        if (campos.size() != 5) return null;
        try {
            Token t = new Token(Integer.valueOf(campos.get("sub")), campos.get("jti"),
                    Instant.ofEpochSecond(Long.parseLong(campos.get("iat"))),
                    Instant.ofEpochSecond(Long.parseLong(campos.get("exp"))),
                    Instant.ofEpochSecond(Long.parseLong(campos.get("ini"))));
            if (!ahora().isBefore(t.expira()) || revocados.containsKey(t.jti())) return null;
            return t;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public void revocar(Token token) {
        if (token == null) return;
        Instant ahora = ahora();
        revocados.entrySet().removeIf(e -> !ahora.isBefore(e.getValue()));
        revocados.put(token.jti(), token.expira());
    }

    private byte[] firmar(String datos) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secreto, "HmacSHA256"));
            return mac.doFinal(datos.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo firmar el token.", e);
        }
    }

    private static String codificar(byte[] datos) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(datos);
    }
}
