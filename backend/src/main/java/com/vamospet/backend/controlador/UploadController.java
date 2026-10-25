package com.vamospet.backend.controlador;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/uploads")
@CrossOrigin(origins = "*")
public class UploadController {

    private static final Set<String> EXTENSIONES_PERMITIDAS = Set.of("jpg", "jpeg", "png", "webp", "gif", "pdf");
    private static final long TAMANO_MAXIMO_BYTES = 5L * 1024 * 1024;

    @PostMapping
    public ResponseEntity<?> subirImagen(@RequestParam("archivo") MultipartFile archivo) {
        if (archivo.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No se envió ningún archivo."));
        }
        if (archivo.getSize() > TAMANO_MAXIMO_BYTES) {
            return ResponseEntity.badRequest().body(Map.of("error", "El archivo supera los 5MB permitidos."));
        }

        String nombreOriginal = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "";
        String extension = "";
        int puntoIdx = nombreOriginal.lastIndexOf('.');
        if (puntoIdx >= 0) {
            extension = nombreOriginal.substring(puntoIdx + 1).toLowerCase();
        }
        if (!EXTENSIONES_PERMITIDAS.contains(extension)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Formato no permitido. Usa jpg, png, webp o gif."));
        }

        try {
            Path carpetaUploads = Paths.get("uploads");
            if (!Files.exists(carpetaUploads)) {
                Files.createDirectories(carpetaUploads);
            }

            String nombreArchivo = UUID.randomUUID().toString() + "." + extension;
            Path destino = carpetaUploads.resolve(nombreArchivo);
            archivo.transferTo(destino);

            Map<String, String> respuesta = new HashMap<>();
            respuesta.put("url", "/uploads/" + nombreArchivo);
            return ResponseEntity.ok(respuesta);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "No se pudo guardar la imagen."));
        }
    }
}
