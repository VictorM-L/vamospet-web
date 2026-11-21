package com.vamospet.backend.controlador;

import com.vamospet.backend.permisos.UsuarioActual;
import com.vamospet.backend.modelo.Pago;
import com.vamospet.backend.repositorio.PagoRepository;
import com.vamospet.backend.servicio.FinanzasService;
import com.vamospet.backend.servicio.NegocioException;
import com.vamospet.backend.servicio.PagoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pagos")
@CrossOrigin(origins = "*")
public class PagoController {

    @Autowired private PagoRepository repositorio;
    @Autowired private PagoService pagoService;
    @Autowired private FinanzasService finanzasService;

    @GetMapping
    public List<Pago> listar() { return repositorio.findAll(); }

    @GetMapping("/usuario/{usuarioId}")
    public List<Pago> listarPorUsuario(@PathVariable Integer usuarioId) {
        return repositorio.findAll().stream()
                .filter(p -> p.getUsuario_id() != null && p.getUsuario_id().equals(usuarioId))
                .toList();
    }

    @PostMapping
    public Pago guardar(@RequestBody Pago pago) {
        pago.setId(null);
        pago.setEstado(PagoService.PAGADO);
        return repositorio.save(pago);
    }

    @PostMapping("/registrar")
    public ResponseEntity<?> registrar(@RequestBody Map<String, Object> b) {
        PagoService.Resultado r = pagoService.cobrar(entero(b, "usuario_id"), texto(b, "referencia"), entero(b, "referencia_id"), datos(b));
        return ResponseEntity.ok(respuesta(r));
    }

    @PatchMapping("/{id}/yape")
    public ResponseEntity<?> resolverYape(@PathVariable Integer id, @RequestBody Map<String, Object> b) {
        boolean aprobar = Boolean.parseBoolean(String.valueOf(b.get("aprobar")));
        return ResponseEntity.ok(respuesta(pagoService.resolverYape(entero(b, "usuario_id"), id, aprobar, texto(b, "motivo"))));
    }

    @GetMapping("/pendientes")
    public ResponseEntity<?> pendientes() {
        Integer usuarioId = UsuarioActual.id();
        return ResponseEntity.ok(finanzasService.pendientesDeCliente(usuarioId));
    }

    @GetMapping("/resumen")
    public ResponseEntity<?> resumen() {
        Integer usuarioId = UsuarioActual.id();
        return ResponseEntity.ok(finanzasService.resumen(usuarioId));
    }

    static PagoService.DatosPago datos(Map<String, Object> b) {
        return new PagoService.DatosPago(texto(b, "metodo"), texto(b, "correo_tarjeta"), texto(b, "tarjeta_ultimos4"), texto(b, "operacion_yape"));
    }

    private static Map<String, Object> respuesta(PagoService.Resultado r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("pago", r.pago());
        m.put("comprobante", r.comprobante());
        return m;
    }

    private static String texto(Map<String, Object> b, String k) {
        return b.get(k) == null ? null : b.get(k).toString();
    }

    private static Integer entero(Map<String, Object> b, String k) {
        try {
            return b.get(k) == null || b.get(k).toString().isBlank() ? null : Integer.valueOf(b.get(k).toString());
        } catch (NumberFormatException e) {
            throw new NegocioException(400, "El campo " + k + " no es válido.");
        }
    }
}
