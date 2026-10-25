package com.vamospet.backend.controlador;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/asistente-ia")
@CrossOrigin(origins = "*")
public class AsistenteIAController {

    private static final Logger log = LoggerFactory.getLogger(AsistenteIAController.class);

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.model}")
    private String modeloPreferido;

    private static final String[] MODELOS_RESPALDO = {
        "gemini-2.5-flash",
        "gemini-3.1-flash-lite",
        "gemini-2.5-flash-lite"
    };

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String INSTRUCCION_SISTEMA =
        "Eres el asistente virtual de Vamos Pet, una clínica veterinaria en Lima, Perú, " +
        "especializada en perros y gatos. Responde de forma breve, cálida y útil sobre: " +
        "servicios de la clínica (consultas, cirugías, laboratorio, grooming, hospedaje), " +
        "cómo agendar una cita, farmacia y accesorios, y consejos generales de cuidado de " +
        "mascotas. Si te preguntan algo médico urgente o grave, recomienda contactar " +
        "directamente a la clínica o acudir a emergencias. No des diagnósticos médicos " +
        "definitivos; sugiere siempre una consulta con el veterinario.";

    @PostMapping
    public ResponseEntity<?> conversar(@RequestBody Map<String, String> body) {
        String mensaje = body.get("mensaje");
        if (mensaje == null || mensaje.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El mensaje está vacío."));
        }

        if (apiKey == null || apiKey.isBlank() || apiKey.equals("TU_API_KEY_DE_GEMINI_AQUI")) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "El asistente de IA aún no está configurado (falta la API key de Gemini en application.properties)."));
        }

        Map<String, Object> requestBody = Map.of(
            "system_instruction", Map.of("parts", List.of(Map.of("text", INSTRUCCION_SISTEMA))),
            "contents", List.of(Map.of("parts", List.of(Map.of("text", mensaje))))
        );

        Set<String> modelosAProbar = new LinkedHashSet<>();
        modelosAProbar.add(modeloPreferido);
        for (String m : MODELOS_RESPALDO) modelosAProbar.add(m);

        final int intentosPorModelo = 2;

        for (String modeloActual : modelosAProbar) {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + modeloActual + ":generateContent";

            for (int intento = 1; intento <= intentosPorModelo; intento++) {
                try {
                    String respuestaCruda = restClient.post()
                            .uri(url)
                            .header("x-goog-api-key", apiKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(requestBody)
                            .retrieve()
                            .body(String.class);

                    JsonNode raiz = objectMapper.readTree(respuestaCruda);
                    String textoRespuesta = raiz
                            .path("candidates").path(0)
                            .path("content").path("parts").path(0)
                            .path("text").asText("No pude generar una respuesta, intenta de nuevo.");

                    return ResponseEntity.ok(Map.of("respuesta", textoRespuesta));

                } catch (RestClientResponseException e) {
                    String cuerpoError = e.getResponseBodyAsString();
                    log.warn("Gemini [{}] respondió {} (intento {}/{}): {}",
                            modeloActual, e.getStatusCode(), intento, intentosPorModelo, cuerpoError);

                    boolean esSaturacionTemporal = e.getStatusCode().value() == 503 || e.getStatusCode().value() == 429;

                    if (esSaturacionTemporal && intento < intentosPorModelo) {
                        try {
                            Thread.sleep(900L * intento);
                        } catch (InterruptedException ignored) {
                            Thread.currentThread().interrupt();
                        }
                        continue;
                    }
                    break;

                } catch (Exception e) {
                    log.error("Error consultando Gemini [{}]", modeloActual, e);
                    break;
                }
            }
        }

        log.error("Ningún modelo de Gemini respondió correctamente. Revisa los avisos anteriores para el detalle de cada error.");
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", "El asistente no está disponible en este momento. Intenta de nuevo en unos segundos 🙏"));
    }
}
