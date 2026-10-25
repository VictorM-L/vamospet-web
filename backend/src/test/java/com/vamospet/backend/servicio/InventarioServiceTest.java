package com.vamospet.backend.servicio;
import com.vamospet.backend.modelo.LoteProducto;
import com.vamospet.backend.modelo.MovimientoStock;
import com.vamospet.backend.modelo.Producto;
import com.vamospet.backend.modelo.Usuario;
import com.vamospet.backend.repositorio.LoteProductoRepository;
import com.vamospet.backend.repositorio.MovimientoStockRepository;
import com.vamospet.backend.repositorio.ProductoRepository;
import com.vamospet.backend.repositorio.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class InventarioServiceTest {
    @Mock private ProductoRepository productoRepositorio;
    @Mock private MovimientoStockRepository movimientoRepositorio;
    @Mock private LoteProductoRepository loteRepositorio;
    @Mock private UsuarioRepository usuarioRepositorio;
    @Mock private NotificacionService notificacionService;
    @InjectMocks private InventarioService inventario;
    @BeforeEach
    void guardarDevuelveLoMismo() {
        lenient().when(productoRepositorio.save(any(Producto.class))).thenAnswer(i -> i.getArgument(0));
        lenient().when(movimientoRepositorio.save(any(MovimientoStock.class))).thenAnswer(i -> i.getArgument(0));
    }
    private Producto producto(int id, int stock, int minimo, String estado) {
        Producto p = new Producto();
        p.setId(id);
        p.setNombre("Producto " + id);
        p.setStock(stock);
        p.setStock_minimo(minimo);
        p.setEstado(estado);
        when(productoRepositorio.findById(id)).thenReturn(Optional.of(p));
        return p;
    }
    @Test
    @DisplayName("Entrada suma al stock y registra el movimiento en el kardex")
    void entradaSumaStock() {
        Producto p = producto(1, 10, 5, "Activo");
        MovimientoStock m = inventario.registrarMovimiento(1, "Entrada", 5, "Compra", 1);
        assertEquals(15, p.getStock());
        assertEquals("Entrada", m.getTipo());
        assertEquals(5, m.getCantidad());
    }
    @Test
    @DisplayName("Salida resta del stock")
    void salidaRestaStock() {
        Producto p = producto(1, 10, 5, "Activo");
        inventario.registrarMovimiento(1, "Salida", 3, "Venta", null);
        assertEquals(7, p.getStock());
    }
    @Test
    @DisplayName("Guava checkState: una salida mayor al stock se rechaza y NO guarda nada")
    void salidaMayorAlStockSeRechaza() {
        Producto p = producto(1, 2, 5, "Activo");
        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> inventario.registrarMovimiento(1, "Salida", 5, "Venta", null));
        assertTrue(ex.getMessage().contains("Stock insuficiente"));
        assertEquals(2, p.getStock());
        verify(productoRepositorio, never()).save(any());
        verify(movimientoRepositorio, never()).save(any());
    }
    @Test
    @DisplayName("Un ajuste negativo que dejaría el stock bajo cero se rechaza")
    void ajusteNegativoExcesivoSeRechaza() {
        producto(1, 3, 5, "Activo");
        assertThrows(IllegalStateException.class,
            () -> inventario.registrarMovimiento(1, "Ajuste", -10, "Conteo", 1));
    }
    @Test
    @DisplayName("Guava checkArgument: cantidad 0 o nula no es válida")
    void cantidadInvalida() {
        assertThrows(IllegalArgumentException.class, () -> inventario.registrarMovimiento(1, "Entrada", 0, "x", 1));
        assertThrows(IllegalArgumentException.class, () -> inventario.registrarMovimiento(1, "Entrada", null, "x", 1));
    }
    @Test
    @DisplayName("Guava checkArgument: un tipo desconocido no es válido")
    void tipoInvalido() {
        assertThrows(IllegalArgumentException.class, () -> inventario.registrarMovimiento(1, "Regalo", 1, "x", 1));
    }
    @Test
    @DisplayName("Si el producto no existe se devuelve null")
    void productoInexistente() {
        when(productoRepositorio.findById(99)).thenReturn(Optional.empty());
        assertNull(inventario.registrarMovimiento(99, "Entrada", 1, "x", 1));
    }
    @Test
    @DisplayName("Al llegar a 0 el producto pasa a 'Agotado'")
    void llegarACeroMarcaAgotado() {
        Producto p = producto(1, 3, 5, "Activo");
        inventario.registrarMovimiento(1, "Salida", 3, "Venta", null);
        assertEquals("Agotado", p.getEstado());
    }
    @Test
    @DisplayName("Una entrada sobre un producto 'Agotado' lo reactiva")
    void entradaReactivaProducto() {
        Producto p = producto(1, 0, 5, "Agotado");
        inventario.registrarMovimiento(1, "Entrada", 10, "Compra", 1);
        assertEquals("Activo", p.getEstado());
    }
    @Test
    @DisplayName("Un producto 'Descontinuado' no cambia de estado aunque llegue a 0")
    void descontinuadoNoCambia() {
        Producto p = producto(1, 2, 5, "Descontinuado");
        inventario.registrarMovimiento(1, "Salida", 2, "Venta", null);
        assertEquals("Descontinuado", p.getEstado());
    }
    @Test
    @DisplayName("Al cruzar el stock mínimo se notifica SOLO a los administradores activos")
    void cruzarUmbralNotificaAdministradores() {
        producto(1, 8, 5, "Activo");
        Usuario admin = usuario(1, 1, true);
        Usuario adminInactivo = usuario(2, 1, false);
        Usuario cliente = usuario(3, 2, true);
        when(usuarioRepositorio.findAll()).thenReturn(List.of(admin, adminInactivo, cliente));
        inventario.registrarMovimiento(1, "Salida", 4, "Venta", null);
        ArgumentCaptor<String> titulo = ArgumentCaptor.forClass(String.class);
        verify(notificacionService, times(1)).notificar(eq(1), titulo.capture(), anyString(), eq("Inventario"));
        assertTrue(titulo.getValue().startsWith("Stock bajo"));
        verifyNoMoreInteractions(notificacionService);
    }
    @Test
    @DisplayName("Si el stock ya estaba por debajo del mínimo NO se vuelve a notificar")
    void noNotificaDosVeces() {
        producto(1, 4, 5, "Activo");
        inventario.registrarMovimiento(1, "Salida", 1, "Venta", null);
        verifyNoInteractions(notificacionService);
    }
    private LoteProducto lote(int id, int productoId, String numero, int stock, LocalDate vence) {
        LoteProducto l = new LoteProducto();
        l.setId(id);
        l.setProducto_id(productoId);
        l.setNumero_lote(numero);
        l.setStock_actual(stock);
        l.setFecha_vencimiento(vence);
        return l;
    }

    @Test
    @DisplayName("Lotes: una salida descuenta primero del lote que vence antes (FEFO)")
    void salidaDescuentaPrimeroElLoteQueVenceAntes() {
        Producto p = producto(1, 10, 2, "Activo");
        LoteProducto tarde = lote(1, 1, "L-TARDE", 6, LocalDate.now().plusDays(200));
        LoteProducto pronto = lote(2, 1, "L-PRONTO", 4, LocalDate.now().plusDays(20));
        when(loteRepositorio.delProducto(1)).thenReturn(new ArrayList<>(List.of(tarde, pronto)));
        inventario.registrarMovimiento(1, "Salida", 6, "Venta", null);
        assertEquals(0, pronto.getStock_actual());
        assertEquals(4, tarde.getStock_actual());
        assertEquals(4, p.getStock());
        assertEquals(LocalDate.now().plusDays(200), p.getFecha_vencimiento(), "el próximo vencimiento pasa al lote que queda");
    }

    @Test
    @DisplayName("Lotes: una venta no puede tomar unidades de un lote vencido")
    void ventaNoTomaLotesVencidos() {
        producto(1, 10, 2, "Activo");
        LoteProducto vencido = lote(1, 1, "L-VENCIDO", 8, LocalDate.now().minusDays(3));
        LoteProducto bueno = lote(2, 1, "L-BUENO", 2, LocalDate.now().plusDays(90));
        when(loteRepositorio.delProducto(1)).thenReturn(new ArrayList<>(List.of(vencido, bueno)));
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> inventario.registrarMovimiento(1, "Salida", 5, "Venta", null));
        assertTrue(ex.getMessage().contains("vencidas"));
        assertEquals(8, vencido.getStock_actual());
        verify(movimientoRepositorio, never()).save(any());
    }

    @Test
    @DisplayName("Lotes: un ajuste negativo (merma) sí puede retirar el lote vencido")
    void ajusteNegativoPuedeRetirarVencidos() {
        Producto p = producto(1, 10, 2, "Activo");
        LoteProducto vencido = lote(1, 1, "L-VENCIDO", 8, LocalDate.now().minusDays(3));
        LoteProducto bueno = lote(2, 1, "L-BUENO", 2, LocalDate.now().plusDays(90));
        when(loteRepositorio.delProducto(1)).thenReturn(new ArrayList<>(List.of(vencido, bueno)));
        inventario.registrarMovimiento(1, "Ajuste", -8, "Merma por vencimiento", 1);
        assertEquals(0, vencido.getStock_actual());
        assertEquals(2, p.getStock());
    }

    @Test
    @DisplayName("Lotes: una entrada con número de lote crea el lote y suma al stock total")
    void entradaCreaLote() {
        Producto p = producto(1, 0, 5, "Agotado");
        LocalDate vence = LocalDate.now().plusDays(120);
        inventario.registrarMovimiento(1, "Entrada", 12, "Compra", 1, "LT-2026-01", vence);
        ArgumentCaptor<LoteProducto> guardado = ArgumentCaptor.forClass(LoteProducto.class);
        verify(loteRepositorio).save(guardado.capture());
        assertEquals("LT-2026-01", guardado.getValue().getNumero_lote());
        assertEquals(12, guardado.getValue().getStock_actual());
        assertEquals(12, p.getStock());
        assertEquals(vence, p.getFecha_vencimiento());
        assertEquals("Activo", p.getEstado());
    }

    @Test
    @DisplayName("Lotes: no se puede ingresar un lote ya vencido")
    void entradaDeLoteVencidoSeRechaza() {
        producto(1, 0, 5, "Agotado");
        assertThrows(IllegalArgumentException.class,
                () -> inventario.registrarMovimiento(1, "Entrada", 5, "Compra", 1, "LT-VIEJO", LocalDate.now().minusDays(1)));
        verify(loteRepositorio, never()).save(any());
    }

    @Test
    @DisplayName("Lotes: un producto antiguo sin lotes recibe un LOTE-INICIAL con su stock actual")
    void productoSinLotesGeneraLoteInicial() {
        producto(1, 7, 2, "Activo");
        inventario.registrarMovimiento(1, "Salida", 2, "Venta", null);
        ArgumentCaptor<LoteProducto> guardado = ArgumentCaptor.forClass(LoteProducto.class);
        verify(loteRepositorio).save(guardado.capture());
        assertEquals("LOTE-INICIAL", guardado.getValue().getNumero_lote());
        assertEquals(5, guardado.getValue().getStock_actual());
    }

    @Test
    @DisplayName("Lotes: el stock vendible descuenta lo que está en lotes vencidos")
    void stockVendibleExcluyeVencidos() {
        Producto p = new Producto();
        p.setId(1);
        p.setStock(10);
        when(loteRepositorio.delProducto(1)).thenReturn(List.of(
                lote(1, 1, "A", 8, LocalDate.now().minusDays(1)), lote(2, 1, "B", 2, LocalDate.now().plusDays(30))));
        assertEquals(2, inventario.stockVendible(p));
    }

    private Usuario usuario(int id, int rol, boolean activo) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setRol_id(rol);
        u.setActivo(activo);
        return u;
    }
}