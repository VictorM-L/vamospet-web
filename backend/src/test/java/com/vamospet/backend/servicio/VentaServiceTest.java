package com.vamospet.backend.servicio;

import com.vamospet.backend.modelo.Mascota;
import com.vamospet.backend.modelo.Pedido;
import com.vamospet.backend.modelo.PedidoDetalle;
import com.vamospet.backend.modelo.Producto;
import com.vamospet.backend.modelo.Receta;
import com.vamospet.backend.modelo.Usuario;
import com.vamospet.backend.repositorio.MascotaRepository;
import com.vamospet.backend.repositorio.PedidoDetalleRepository;
import com.vamospet.backend.repositorio.PedidoRepository;
import com.vamospet.backend.repositorio.ProductoRepository;
import com.vamospet.backend.repositorio.RecetaRepository;
import com.vamospet.backend.repositorio.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VentaServiceTest {

    private static final int CLIENTE = 20, AUX = 19, MASCOTA = 5, RECETA = 70;

    @Mock private PedidoRepository pedidoRepositorio;
    @Mock private PedidoDetalleRepository detalleRepositorio;
    @Mock private ProductoRepository productoRepositorio;
    @Mock private RecetaRepository recetaRepositorio;
    @Mock private MascotaRepository mascotaRepositorio;
    @Mock private UsuarioRepository usuarioRepositorio;
    @Mock private AutorizacionService autorizacion;
    @Mock private InventarioService inventarioService;
    @Mock private com.vamospet.backend.repositorio.PagoRepository pagoRepositorio;
    @Mock private com.vamospet.backend.repositorio.DevolucionRepository devolucionRepositorio;
    @Mock private com.vamospet.backend.repositorio.DevolucionDetalleRepository devolucionDetalleRepositorio;
    @InjectMocks private VentaService servicio;

    private Usuario usuario(int id, int rol) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setRol_id(rol);
        return u;
    }

    private void comoCliente() {
        when(autorizacion.conRol(CLIENTE, 2, 4)).thenReturn(usuario(CLIENTE, 2));
    }

    private Producto producto(int id, String precio, boolean receta) {
        Producto p = new Producto();
        p.setId(id);
        p.setNombre(receta ? "Antibiótico" : "Shampoo");
        p.setPrecio(new BigDecimal(precio));
        p.setRequiere_receta(receta);
        p.setActivo(true);
        p.setEstado("Activo");
        when(productoRepositorio.findById(id)).thenReturn(Optional.of(p));
        lenient().when(inventarioService.stockVendible(p)).thenReturn(10);
        return p;
    }

    private Receta receta(String estado, LocalDateTime aprobada, boolean usada, int mascota) {
        Receta r = new Receta();
        r.setId(RECETA);
        r.setMascota_id(mascota);
        r.setEstado(estado);
        r.setFecha(aprobada);
        r.setAprobada_en(aprobada);
        r.setUsada(usada);
        when(recetaRepositorio.findById(RECETA)).thenReturn(Optional.of(r));
        return r;
    }

    private void mascota(int id, int dueno) {
        Mascota m = new Mascota();
        m.setId(id);
        m.setUsuario_id(dueno);
        lenient().when(mascotaRepositorio.findById(id)).thenReturn(Optional.of(m));
    }

    private VentaService.Pedir pedido(Integer receta, VentaService.Item... items) {
        return new VentaService.Pedir(null, List.of(items), receta, null);
    }

    private void guardaPedido() {
        when(pedidoRepositorio.save(any(Pedido.class))).thenAnswer(i -> { Pedido p = i.getArgument(0); if (p.getId() == null) p.setId(300); return p; });
    }

    private void guarda() {
        guardaPedido();
        when(detalleRepositorio.save(any(PedidoDetalle.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    @DisplayName("Sin receta no se vende un producto que la exige")
    void sinReceta() {
        comoCliente();
        producto(1, "45.00", true);
        NegocioException ex = assertThrows(NegocioException.class, () -> servicio.vender(CLIENTE, pedido(null, new VentaService.Item(1, 1))));
        assertEquals(409, ex.getEstado());
        assertTrue(ex.getMessage().contains("Antibiótico"));
        verifyNoInteractions(pedidoRepositorio);
    }

    @Test
    @DisplayName("Una receta vencida (más de 30 días desde su aprobación) se rechaza")
    void recetaVencida() {
        comoCliente();
        producto(1, "45.00", true);
        mascota(MASCOTA, CLIENTE);
        receta("Aprobada", LocalDateTime.now().minusDays(31), false, MASCOTA);
        NegocioException ex = assertThrows(NegocioException.class, () -> servicio.vender(CLIENTE, pedido(RECETA, new VentaService.Item(1, 1))));
        assertEquals(409, ex.getEstado());
        assertTrue(ex.getMessage().contains("venció"));
        verifyNoInteractions(pedidoRepositorio);
    }

    @Test
    @DisplayName("Una receta ya usada se rechaza")
    void recetaUsada() {
        comoCliente();
        producto(1, "45.00", true);
        mascota(MASCOTA, CLIENTE);
        receta("Aprobada", LocalDateTime.now().minusDays(2), true, MASCOTA);
        assertTrue(assertThrows(NegocioException.class, () -> servicio.vender(CLIENTE, pedido(RECETA, new VentaService.Item(1, 1))))
                .getMessage().contains("ya se usó"));
    }

    @Test
    @DisplayName("La receta de la mascota de otra persona se rechaza")
    void recetaDeOtraMascota() {
        comoCliente();
        producto(1, "45.00", true);
        mascota(9, 77);
        receta("Aprobada", LocalDateTime.now().minusDays(2), false, 9);
        assertTrue(assertThrows(NegocioException.class, () -> servicio.vender(CLIENTE, pedido(RECETA, new VentaService.Item(1, 1))))
                .getMessage().contains("no es de una mascota de este cliente"));
    }

    @Test
    @DisplayName("Una receta pendiente o rechazada no autoriza la venta")
    void recetaNoAprobada() {
        comoCliente();
        producto(1, "45.00", true);
        mascota(MASCOTA, CLIENTE);
        receta("Pendiente", LocalDateTime.now(), false, MASCOTA);
        assertTrue(assertThrows(NegocioException.class, () -> servicio.vender(CLIENTE, pedido(RECETA, new VentaService.Item(1, 1))))
                .getMessage().contains("no está aprobada"));
    }

    @Test
    @DisplayName("Con receta vigente: precios desde la base, receta marcada como usada y anotada solo en la línea que la exige")
    void ventaConReceta() {
        comoCliente();
        producto(1, "45.00", true);
        producto(2, "20.00", false);
        mascota(MASCOTA, CLIENTE);
        receta("Aprobada", LocalDateTime.now().minusDays(29), false, MASCOTA);
        guarda();
        when(recetaRepositorio.marcarUsada(eq(RECETA), eq(300), any())).thenReturn(1);
        VentaService.Resultado r = servicio.vender(CLIENTE, pedido(RECETA, new VentaService.Item(1, 2), new VentaService.Item(2, 1), new VentaService.Item(1, 1)));
        assertEquals(0, new BigDecimal("155.00").compareTo(r.pedido().getTotal()));
        assertEquals("Web", r.pedido().getCanal());
        assertEquals(CLIENTE, r.pedido().getUsuario_id());
        assertEquals(2, r.detalle().size());
        assertEquals(3, r.detalle().get(0).getCantidad());
        assertEquals(RECETA, r.detalle().get(0).getReceta_id());
        assertNull(r.detalle().get(1).getReceta_id());
        verify(inventarioService).registrarMovimiento(eq(1), eq("Salida"), eq(3), anyString(), eq(CLIENTE));
    }

    private Pedido pedidoMostrador(String estado) {
        Pedido p = new Pedido();
        p.setId(301);
        p.setCanal("Mostrador");
        p.setEstado(estado);
        when(pedidoRepositorio.findById(301)).thenReturn(Optional.of(p));
        return p;
    }

    private PedidoDetalle linea(int producto, int cantidad) {
        PedidoDetalle d = new PedidoDetalle();
        d.setPedido_id(301);
        d.setProducto_id(producto);
        d.setCantidad(cantidad);
        return d;
    }

    @Test
    @DisplayName("La venta de mostrador queda Pendiente hasta que se cobre")
    void mostradorPendienteHastaCobrar() {
        when(autorizacion.conRol(AUX, 2, 4)).thenReturn(usuario(AUX, 4));
        when(usuarioRepositorio.findById(CLIENTE)).thenReturn(Optional.of(usuario(CLIENTE, 2)));
        producto(2, "20.00", false);
        guarda();
        VentaService.Resultado r = servicio.vender(AUX, new VentaService.Pedir(CLIENTE, List.of(new VentaService.Item(2, 1)), null, null));
        assertEquals("Mostrador", r.pedido().getCanal());
        assertEquals("Pendiente", r.pedido().getEstado());
    }

    @Test
    @DisplayName("Anular una venta de mostrador sin cobrar devuelve el stock, libera la receta y la deja Cancelada")
    void anularSinCobrar() {
        when(autorizacion.conRol(AUX, 4)).thenReturn(usuario(AUX, 4));
        pedidoMostrador("Pendiente");
        when(pagoRepositorio.findAll()).thenReturn(List.of());
        PedidoDetalle otro = linea(9, 1);
        otro.setPedido_id(999);
        when(detalleRepositorio.findAll()).thenReturn(List.of(linea(1, 2), linea(2, 1), otro));
        guardaPedido();
        Pedido r = servicio.anularVentaMostrador(AUX, 301);
        assertEquals("Cancelado", r.getEstado());
        verify(inventarioService).registrarMovimiento(eq(1), eq("Entrada"), eq(2), anyString(), eq(AUX));
        verify(inventarioService).registrarMovimiento(eq(2), eq("Entrada"), eq(1), anyString(), eq(AUX));
        verify(inventarioService, never()).registrarMovimiento(eq(9), anyString(), anyInt(), anyString(), any());
        verify(recetaRepositorio).liberarDePedido(301);
    }

    @Test
    @DisplayName("Cancelar un pedido por cambio de estado devuelve solo lo que no se devolvió antes y libera la receta")
    void cancelarDescuentaDevoluciones() {
        pedidoMostrador("Listo para recoger").setCanal("Web");
        when(pagoRepositorio.findAll()).thenReturn(List.of());
        when(detalleRepositorio.findAll()).thenReturn(List.of(linea(1, 3), linea(2, 1)));
        com.vamospet.backend.modelo.Devolucion dv = new com.vamospet.backend.modelo.Devolucion();
        dv.setId(40);
        dv.setPedido_id(301);
        com.vamospet.backend.modelo.Devolucion ajena = new com.vamospet.backend.modelo.Devolucion();
        ajena.setId(41);
        ajena.setPedido_id(999);
        when(devolucionRepositorio.findAll()).thenReturn(List.of(dv, ajena));
        when(devolucionDetalleRepositorio.findAll()).thenReturn(List.of(devuelto(40, 1, 2), devuelto(40, 2, 1), devuelto(41, 1, 3)));
        guardaPedido();
        Pedido r = servicio.cancelarPedido(AUX, 301);
        assertEquals("Cancelado", r.getEstado());
        verify(inventarioService).registrarMovimiento(eq(1), eq("Entrada"), eq(1), anyString(), eq(AUX));
        verify(inventarioService, never()).registrarMovimiento(eq(2), anyString(), anyInt(), anyString(), any());
        verify(recetaRepositorio).liberarDePedido(301);
    }

    @Test
    @DisplayName("Cancelar dos veces no devuelve el stock dos veces; un pedido inexistente da 404")
    void cancelarDosVeces() {
        pedidoMostrador("Cancelado");
        assertEquals("Cancelado", servicio.cancelarPedido(AUX, 301).getEstado());
        verifyNoInteractions(inventarioService);
        verify(pedidoRepositorio, never()).save(any());
        when(pedidoRepositorio.findById(302)).thenReturn(Optional.empty());
        assertEquals(404, assertThrows(NegocioException.class, () -> servicio.cancelarPedido(AUX, 302)).getEstado());
    }

    @Test
    @DisplayName("Un pedido Entregado o con pago registrado no se cancela: se pide usar una devolución")
    void cancelarConPagoOEntregado() {
        pedidoMostrador("Entregado").setCanal("Web");
        NegocioException e = assertThrows(NegocioException.class, () -> servicio.cancelarPedido(AUX, 301));
        assertEquals(409, e.getEstado());
        assertEquals("Para este pedido usa una devolución", e.getMessage());
        pedidoMostrador("Listo para recoger").setCanal("Web");
        com.vamospet.backend.modelo.Pago pago = new com.vamospet.backend.modelo.Pago();
        pago.setPedido_id(301);
        pago.setEstado("Pendiente");
        when(pagoRepositorio.findAll()).thenReturn(List.of(pago));
        assertEquals("Para este pedido usa una devolución", assertThrows(NegocioException.class, () -> servicio.cancelarPedido(AUX, 301)).getMessage());
        pago.setEstado("Rechazado");
        when(detalleRepositorio.findAll()).thenReturn(List.of(linea(1, 1)));
        guardaPedido();
        assertEquals("Cancelado", servicio.cancelarPedido(AUX, 301).getEstado());
        verify(inventarioService).registrarMovimiento(eq(1), eq("Entrada"), eq(1), anyString(), eq(AUX));
        verify(inventarioService, times(1)).registrarMovimiento(anyInt(), anyString(), anyInt(), anyString(), any());
    }

    private com.vamospet.backend.modelo.DevolucionDetalle devuelto(int devolucion, int producto, int cantidad) {
        com.vamospet.backend.modelo.DevolucionDetalle d = new com.vamospet.backend.modelo.DevolucionDetalle();
        d.setDevolucion_id(devolucion);
        d.setProducto_id(producto);
        d.setCantidad(cantidad);
        return d;
    }

    @Test
    @DisplayName("No se anula una venta cobrada, una venta web ni una ya cancelada; solo el Auxiliar anula")
    void anularRechazos() {
        when(autorizacion.conRol(1, 4)).thenReturn(null);
        assertEquals(403, assertThrows(NegocioException.class, () -> servicio.anularVentaMostrador(1, 301)).getEstado());
        when(autorizacion.conRol(AUX, 4)).thenReturn(usuario(AUX, 4));
        pedidoMostrador("Pendiente");
        com.vamospet.backend.modelo.Pago pago = new com.vamospet.backend.modelo.Pago();
        pago.setPedido_id(301);
        pago.setEstado("Pagado");
        when(pagoRepositorio.findAll()).thenReturn(List.of(pago));
        assertEquals(409, assertThrows(NegocioException.class, () -> servicio.anularVentaMostrador(AUX, 301)).getEstado());
        pedidoMostrador("Cancelado");
        assertEquals(409, assertThrows(NegocioException.class, () -> servicio.anularVentaMostrador(AUX, 301)).getEstado());
        pedidoMostrador("Pendiente").setCanal("Web");
        assertEquals(409, assertThrows(NegocioException.class, () -> servicio.anularVentaMostrador(AUX, 301)).getEstado());
        verifyNoInteractions(inventarioService);
        verify(recetaRepositorio, never()).liberarDePedido(anyInt());
    }

    @Test
    @DisplayName("Si otra venta usó la receta al mismo tiempo, esta se rechaza (la marca es atómica)")
    void recetaUsadaEnParalelo() {
        comoCliente();
        producto(1, "45.00", true);
        mascota(MASCOTA, CLIENTE);
        receta("Aprobada", LocalDateTime.now().minusDays(1), false, MASCOTA);
        guardaPedido();
        when(recetaRepositorio.marcarUsada(eq(RECETA), eq(300), any())).thenReturn(0);
        assertEquals(409, assertThrows(NegocioException.class, () -> servicio.vender(CLIENTE, pedido(RECETA, new VentaService.Item(1, 1)))).getEstado());
        verify(detalleRepositorio, never()).save(any());
        verify(inventarioService, never()).registrarMovimiento(anyInt(), anyString(), anyInt(), anyString(), any());
    }

    @Test
    @DisplayName("Sin stock suficiente no se crea el pedido")
    void sinStock() {
        comoCliente();
        Producto p = producto(2, "20.00", false);
        when(inventarioService.stockVendible(p)).thenReturn(1);
        assertTrue(assertThrows(NegocioException.class, () -> servicio.vender(CLIENTE, pedido(null, new VentaService.Item(2, 3))))
                .getMessage().contains("quedan 1"));
        verifyNoInteractions(pedidoRepositorio);
    }

    @Test
    @DisplayName("El Admin no vende (ni puede saltarse la receta); el mostrador exige elegir un cliente")
    void rolesYCliente() {
        when(autorizacion.conRol(1, 2, 4)).thenReturn(null);
        assertEquals(403, assertThrows(NegocioException.class, () -> servicio.vender(1, pedido(null, new VentaService.Item(2, 1)))).getEstado());
        when(autorizacion.conRol(AUX, 2, 4)).thenReturn(usuario(AUX, 4));
        assertEquals(400, assertThrows(NegocioException.class, () -> servicio.vender(AUX, pedido(null, new VentaService.Item(2, 1)))).getEstado());
        when(usuarioRepositorio.findById(18)).thenReturn(Optional.of(usuario(18, 3)));
        assertEquals(400, assertThrows(NegocioException.class,
                () -> servicio.vender(AUX, new VentaService.Pedir(18, List.of(new VentaService.Item(2, 1)), null, null))).getEstado());
        verifyNoInteractions(pedidoRepositorio);
    }

    @Test
    @DisplayName("Los 30 días de vigencia cuentan desde la aprobación, incluido el día 30")
    void vigencia() {
        Receta r = new Receta();
        LocalDateTime aprobada = LocalDateTime.of(2026, 11, 1, 10, 0);
        r.setFecha(aprobada.minusDays(10));
        r.setAprobada_en(aprobada);
        assertTrue(VentaService.vigente(r, aprobada.plusDays(30)));
        assertFalse(VentaService.vigente(r, aprobada.plusDays(30).plusMinutes(1)));
        r.setAprobada_en(null);
        assertFalse(VentaService.vigente(r, aprobada.plusDays(25)));
    }
}
