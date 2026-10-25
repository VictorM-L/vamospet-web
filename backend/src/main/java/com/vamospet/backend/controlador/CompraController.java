package com.vamospet.backend.controlador;

import com.vamospet.backend.modelo.Compra;
import com.vamospet.backend.modelo.CompraDetalle;
import com.vamospet.backend.modelo.Producto;
import com.vamospet.backend.modelo.Proveedor;
import com.vamospet.backend.modelo.Usuario;
import com.vamospet.backend.repositorio.*;
import com.vamospet.backend.servicio.InventarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/compras")
@CrossOrigin(origins = "*")
public class CompraController {

    @Autowired private CompraRepository compraRepositorio;
    @Autowired private CompraDetalleRepository detalleRepositorio;
    @Autowired private ProveedorRepository proveedorRepositorio;
    @Autowired private ProductoRepository productoRepositorio;
    @Autowired private UsuarioRepository usuarioRepositorio;
    @Autowired private InventarioService inventarioService;

    @GetMapping
    public List<Compra> listar() {
        return compraRepositorio.findAll().stream()
                .sorted((a, b) -> b.getFecha().compareTo(a.getFecha())).toList();
    }

    @GetMapping("/{id}/detalle")
    public List<CompraDetalle> detalle(@PathVariable Integer id) {
        return detalleRepositorio.findAll().stream().filter(d -> id.equals(d.getCompra_id())).toList();
    }

    @PostMapping
    @Transactional
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> registrar(@RequestBody Map<String, Object> body) {
        try {
            Integer proveedorId = entero(body.get("proveedor_id"));
            Integer usuarioId = entero(body.get("usuario_id"));
            List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");

            Usuario admin = usuarioId == null ? null : usuarioRepositorio.findById(usuarioId).orElse(null);
            if (admin == null || !Integer.valueOf(1).equals(admin.getRol_id())) {
                return error(HttpStatus.FORBIDDEN, "Solo el Administrador puede registrar compras.");
            }
            Proveedor proveedor = proveedorId == null ? null : proveedorRepositorio.findById(proveedorId).orElse(null);
            if (proveedor == null || Boolean.FALSE.equals(proveedor.getActivo())) {
                return error(HttpStatus.BAD_REQUEST, "Elige un proveedor válido.");
            }
            if (items == null || items.isEmpty()) {
                return error(HttpStatus.BAD_REQUEST, "Agrega al menos un producto a la compra.");
            }

            List<CompraDetalle> lineas = new ArrayList<>();
            BigDecimal total = BigDecimal.ZERO;
            for (Map<String, Object> it : items) {
                Integer productoId = entero(it.get("producto_id"));
                Integer cantidad = entero(it.get("cantidad"));
                BigDecimal costo = it.get("costo_unitario") == null ? null : new BigDecimal(it.get("costo_unitario").toString());
                if (productoId == null || productoRepositorio.findById(productoId).isEmpty()) {
                    return error(HttpStatus.BAD_REQUEST, "Hay un producto que no existe.");
                }
                if (cantidad == null || cantidad <= 0) return error(HttpStatus.BAD_REQUEST, "Las cantidades deben ser mayores a 0.");
                if (costo == null || costo.signum() < 0) return error(HttpStatus.BAD_REQUEST, "Cada producto necesita un costo unitario válido.");
                CompraDetalle d = new CompraDetalle();
                d.setProducto_id(productoId);
                d.setCantidad(cantidad);
                d.setCosto_unitario(costo);
                String lote = it.get("numero_lote") == null ? null : it.get("numero_lote").toString().trim();
                if (lote != null && lote.isEmpty()) lote = null;
                if (lote != null && lote.length() > 60) return error(HttpStatus.BAD_REQUEST, "El número de lote admite hasta 60 caracteres.");
                java.time.LocalDate vence = null;
                String venceTexto = it.get("fecha_vencimiento") == null ? "" : it.get("fecha_vencimiento").toString().trim();
                if (!venceTexto.isEmpty()) {
                    try { vence = java.time.LocalDate.parse(venceTexto); }
                    catch (java.time.format.DateTimeParseException e) { return error(HttpStatus.BAD_REQUEST, "Fecha de vencimiento inválida."); }
                    if (vence.isBefore(java.time.LocalDate.now())) return error(HttpStatus.BAD_REQUEST, "No se puede comprar un lote ya vencido.");
                }
                d.setNumero_lote(lote);
                d.setFecha_vencimiento(vence);
                lineas.add(d);
                total = total.add(costo.multiply(BigDecimal.valueOf(cantidad)));
            }

            Compra compra = new Compra();
            compra.setProveedor_id(proveedorId);
            compra.setUsuario_id(usuarioId);
            compra.setTotal(total);
            compra.setObservacion(body.get("observacion") != null ? body.get("observacion").toString() : null);
            compra = compraRepositorio.save(compra);

            for (CompraDetalle d : lineas) {
                d.setCompra_id(compra.getId());
                detalleRepositorio.save(d);
                inventarioService.registrarMovimiento(d.getProducto_id(), "Entrada", d.getCantidad(),
                        "Compra #" + compra.getId() + " — " + proveedor.getRazon_social(), usuarioId,
                        d.getNumero_lote(), d.getFecha_vencimiento());
                Producto prod = productoRepositorio.findById(d.getProducto_id()).orElse(null);
                if (prod != null) {
                    prod.setPrecio_costo(d.getCosto_unitario());
                    prod.setProveedor(proveedor.getRazon_social());
                    productoRepositorio.save(prod);
                }
            }
            return ResponseEntity.ok(compra);
        } catch (ClassCastException | NumberFormatException e) {
            return error(HttpStatus.BAD_REQUEST, "Datos de compra inválidos.");
        }
    }

    private Integer entero(Object o) {
        return o == null ? null : Integer.valueOf(o.toString());
    }

    private ResponseEntity<?> error(HttpStatus s, String msg) {
        return ResponseEntity.status(s).body(Map.of("error", msg));
    }
}
