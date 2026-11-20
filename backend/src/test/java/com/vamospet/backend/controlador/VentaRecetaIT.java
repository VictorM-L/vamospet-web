package com.vamospet.backend.controlador;

import com.vamospet.backend.modelo.Categoria;
import com.vamospet.backend.modelo.Mascota;
import com.vamospet.backend.modelo.Producto;
import com.vamospet.backend.modelo.Receta;
import com.vamospet.backend.modelo.Usuario;
import com.vamospet.backend.repositorio.CategoriaRepository;
import com.vamospet.backend.repositorio.MascotaRepository;
import com.vamospet.backend.repositorio.MovimientoStockRepository;
import com.vamospet.backend.repositorio.PedidoDetalleRepository;
import com.vamospet.backend.repositorio.PedidoRepository;
import com.vamospet.backend.repositorio.ProductoRepository;
import com.vamospet.backend.repositorio.RecetaRepository;
import com.vamospet.backend.repositorio.UsuarioRepository;
import com.vamospet.backend.servicio.NotificacionService;
import com.vamospet.backend.servicio.VentaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.vamospet.backend.ConToken.como;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VentaRecetaIT {


    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private VentaService ventaService;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private MascotaRepository mascotas;
    @Autowired private ProductoRepository productos;
    @Autowired private CategoriaRepository categorias;
    @Autowired private RecetaRepository recetas;
    @Autowired private PedidoRepository pedidos;
    @Autowired private PedidoDetalleRepository detalles;
    @Autowired private MovimientoStockRepository movimientos;
    @MockitoBean private NotificacionService notificacionService;

    private Usuario admin, vet, aux, cliente;
    private Mascota mascota;
    private Producto antibiotico, shampoo;
    private Categoria categoria;

    @BeforeEach
    void datos() {
        detalles.deleteAll();
        recetas.deleteAll();
        pedidos.deleteAll();
        movimientos.deleteAll();
        productos.deleteAll();
        categorias.deleteAll();
        mascotas.deleteAll();
        usuarios.deleteAll();
        admin = guardar("Admin", "64000001", 1);
        vet = guardar("Lucia Vet", "64000002", 3);
        aux = guardar("Rosa Aux", "64000003", 4);
        cliente = guardar("Ana Torres", "64000004", 2);
        Mascota m = new Mascota();
        m.setUsuario_id(cliente.getId());
        m.setNombre("Luna");
        m.setEspecie("Gato");
        mascota = mascotas.save(m);
        Categoria c = new Categoria();
        c.setNombre("Antibióticos");
        c.setTipo("Farmacia");
        categoria = categorias.save(c);
        antibiotico = producto("Amoxicilina", "45.00", true);
        shampoo = producto("Shampoo", "20.00", false);
    }

    @AfterEach
    void quitarClaves() {
        jdbc.execute("ALTER TABLE producto DROP CONSTRAINT IF EXISTS producto_categoria_id_fkey");
        jdbc.execute("ALTER TABLE notificacion DROP CONSTRAINT IF EXISTS notificacion_usuario_id_fkey");
        jdbc.execute("ALTER TABLE movimiento_stock DROP CONSTRAINT IF EXISTS movimiento_stock_usuario_id_fkey");
    }

    private Usuario guardar(String nombre, String dni, int rol) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setDni(dni);
        u.setCorreo(dni + "@venta.test");
        u.setContrasena("Clave2026");
        u.setRol_id(rol);
        u.setActivo(true);
        return usuarios.save(u);
    }

    private Producto producto(String nombre, String precio, boolean receta) {
        Producto p = new Producto();
        p.setNombre(nombre);
        p.setPrecio(new BigDecimal(precio));
        p.setStock(10);
        p.setRequiere_receta(receta);
        p.setTipo("Farmacia");
        p.setCategoria_id(categoria.getId());
        return productos.save(p);
    }

    private Receta recetaAprobada(LocalDateTime aprobada) {
        Receta r = new Receta();
        r.setMascota_id(mascota.getId());
        r.setVeterinario_id(vet.getId());
        r.setContenido("Amoxicilina 250 mg cada 12 h");
        r.setEstado("Aprobada");
        r.setFecha(aprobada);
        r.setAprobada_en(aprobada);
        return recetas.save(r);
    }

    private String venta(Integer clienteId, Integer receta, Producto p, int cantidad) {
        return "{" + (clienteId != null ? "\"cliente_id\":" + clienteId + "," : "") + "\"items\":[{\"producto_id\":" + p.getId()
                + ",\"cantidad\":" + cantidad + "}]" + (receta != null ? ",\"receta_id\":" + receta : "") + "}";
    }

    @Test
    @DisplayName("Web: sin receta se rechaza; con receta vigente se vende una sola vez y queda anotada")
    void ventaWebConReceta() throws Exception {
        mvc.perform(post("/api/pedidos").with(como(cliente.getId())).contentType(MediaType.APPLICATION_JSON).content(venta(null, null, antibiotico, 1)))
                .andExpect(status().isConflict());
        Receta r = recetaAprobada(LocalDateTime.now().minusDays(3));
        mvc.perform(get("/api/recetas/vigentes/cliente/" + cliente.getId()).with(como(cliente.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(r.getId())).andExpect(jsonPath("$[0].mascota").value("Luna"));
        mvc.perform(post("/api/pedidos").with(como(cliente.getId())).contentType(MediaType.APPLICATION_JSON)
                        .content(venta(null, r.getId(), antibiotico, 2).replace("\"cantidad\":2}", "\"cantidad\":2,\"precio_unitario\":1}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pedido.total").value(90.0))
                .andExpect(jsonPath("$.detalle[0].receta_id").value(r.getId()));
        Receta usada = recetas.findById(r.getId()).orElseThrow();
        assertTrue(usada.getUsada());
        assertNotNull(usada.getUsada_en());
        assertEquals(8, productos.findById(antibiotico.getId()).orElseThrow().getStock());
        mvc.perform(post("/api/pedidos").with(como(cliente.getId())).contentType(MediaType.APPLICATION_JSON).content(venta(null, r.getId(), antibiotico, 1)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("Esa receta ya se usó en otra compra."));
        mvc.perform(get("/api/recetas/vigentes/cliente/" + cliente.getId()).with(como(cliente.getId())))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Una receta de hace más de 30 días no sirve; el Admin no puede vender")
    void vencidaYAdmin() throws Exception {
        Receta r = recetaAprobada(LocalDateTime.now().minusDays(31));
        mvc.perform(post("/api/pedidos").with(como(cliente.getId())).contentType(MediaType.APPLICATION_JSON).content(venta(null, r.getId(), antibiotico, 1)))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/pedidos").with(como(admin.getId())).contentType(MediaType.APPLICATION_JSON).content(venta(cliente.getId(), r.getId(), antibiotico, 1)))
                .andExpect(status().isForbidden());
        assertEquals(0, pedidos.count());
    }

    @Test
    @DisplayName("Mostrador: el Auxiliar sube la receta en papel, el veterinario la aprueba y recién se puede vender")
    void mostradorConRecetaEnPapel() throws Exception {
        mvc.perform(post("/api/recetas").with(como(aux.getId())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mascota_id\":" + mascota.getId() + ",\"archivo_url\":\"http://localhost:8080/uploads/receta.jpg\",\"estado\":\"Aprobada\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("Pendiente"));
        Integer recetaId = recetas.findAll().get(0).getId();
        mvc.perform(post("/api/pedidos").with(como(aux.getId())).contentType(MediaType.APPLICATION_JSON).content(venta(cliente.getId(), recetaId, antibiotico, 1)))
                .andExpect(status().isConflict());
        mvc.perform(patch("/api/recetas/" + recetaId + "/estado").with(como(vet.getId())).contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"Aprobada\"}"))
                .andExpect(status().isOk());
        assertNotNull(recetas.findById(recetaId).orElseThrow().getAprobada_en());
        mvc.perform(post("/api/pedidos").with(como(aux.getId())).contentType(MediaType.APPLICATION_JSON).content(venta(cliente.getId(), recetaId, antibiotico, 1)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pedido.canal").value("Mostrador"))
                .andExpect(jsonPath("$.pedido.usuario_id").value(cliente.getId()));
        mvc.perform(patch("/api/recetas/" + recetaId + "/estado").with(como(vet.getId())).contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"Rechazada\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Mostrador: si se cierra el cobro, la venta se anula, devuelve stock y libera la receta; cobrada pasa a Entregado y ya no se anula")
    void anularVentaSinCobrar() throws Exception {
        Receta r = recetaAprobada(LocalDateTime.now().minusDays(2));
        mvc.perform(post("/api/pedidos").with(como(aux.getId())).contentType(MediaType.APPLICATION_JSON).content(venta(cliente.getId(), r.getId(), antibiotico, 2)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pedido.estado").value("Pendiente"));
        Integer pedidoId = pedidos.findAll().get(0).getId();
        assertEquals(8, productos.findById(antibiotico.getId()).orElseThrow().getStock());
        assertTrue(recetas.findById(r.getId()).orElseThrow().getUsada());
        mvc.perform(patch("/api/pedidos/" + pedidoId + "/anular").with(como(cliente.getId()))).andExpect(status().isForbidden());
        mvc.perform(patch("/api/pedidos/" + pedidoId + "/anular").with(como(aux.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("Cancelado"));
        assertEquals(10, productos.findById(antibiotico.getId()).orElseThrow().getStock());
        Receta libre = recetas.findById(r.getId()).orElseThrow();
        assertFalse(libre.getUsada());
        assertNull(libre.getPedido_id());
        mvc.perform(patch("/api/pedidos/" + pedidoId + "/anular").with(como(aux.getId()))).andExpect(status().isConflict());

        mvc.perform(post("/api/pedidos").with(como(aux.getId())).contentType(MediaType.APPLICATION_JSON).content(venta(cliente.getId(), r.getId(), antibiotico, 1)))
                .andExpect(status().isOk());
        Integer segundo = pedidos.findAll().stream().mapToInt(p -> p.getId()).max().orElseThrow();
        mvc.perform(post("/api/pagos/registrar").with(como(aux.getId())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"referencia\":\"pedido\",\"referencia_id\":" + segundo + ",\"metodo\":\"Efectivo\"}"))
                .andExpect(status().isOk());
        assertEquals("Entregado", pedidos.findById(segundo).orElseThrow().getEstado());
        mvc.perform(patch("/api/pedidos/" + segundo + "/anular").with(como(aux.getId()))).andExpect(status().isConflict());
        assertEquals(9, productos.findById(antibiotico.getId()).orElseThrow().getStock());
    }

    @Test
    @DisplayName("Pasar un pedido a Cancelado por estado devuelve el stock una sola vez, libera la receta y no se reabre")
    void cancelarPorEstado() throws Exception {
        Receta r = recetaAprobada(LocalDateTime.now().minusDays(1));
        mvc.perform(post("/api/pedidos").with(como(cliente.getId())).contentType(MediaType.APPLICATION_JSON).content(venta(null, r.getId(), antibiotico, 2)))
                .andExpect(status().isOk());
        Integer pedidoId = pedidos.findAll().get(0).getId();
        assertEquals(8, productos.findById(antibiotico.getId()).orElseThrow().getStock());
        String cancelar = "{\"estado\":\"Cancelado\"}";
        mvc.perform(patch("/api/pedidos/" + pedidoId + "/estado").with(como(aux.getId())).contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"Listo para recoger\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("Listo para recoger"));
        mvc.perform(patch("/api/pedidos/" + pedidoId + "/estado").with(como(aux.getId())).contentType(MediaType.APPLICATION_JSON).content(cancelar))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("Cancelado"));
        assertEquals(10, productos.findById(antibiotico.getId()).orElseThrow().getStock());
        Receta libre = recetas.findById(r.getId()).orElseThrow();
        assertFalse(libre.getUsada());
        assertNull(libre.getPedido_id());
        mvc.perform(patch("/api/pedidos/" + pedidoId + "/estado").with(como(aux.getId())).contentType(MediaType.APPLICATION_JSON).content(cancelar))
                .andExpect(status().isOk());
        assertEquals(10, productos.findById(antibiotico.getId()).orElseThrow().getStock());
        mvc.perform(patch("/api/pedidos/" + pedidoId + "/estado").with(como(aux.getId())).contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"Pendiente\"}"))
                .andExpect(status().isConflict());
        mvc.perform(patch("/api/pedidos/" + pedidoId + "/estado").with(como(aux.getId())).contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"Perdido\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/pedidos/999999/estado").with(como(aux.getId())).contentType(MediaType.APPLICATION_JSON).content(cancelar))
                .andExpect(status().isNotFound());
        assertEquals("Cancelado", pedidos.findById(pedidoId).orElseThrow().getEstado());
    }

    @Test
    @DisplayName("Cancelar por estado un pedido cobrado o Entregado responde 409 y pide usar una devolución")
    void cancelarCobradoOEntregado() throws Exception {
        String cancelar = "{\"estado\":\"Cancelado\"}";
        mvc.perform(post("/api/pedidos").with(como(aux.getId())).contentType(MediaType.APPLICATION_JSON).content(venta(cliente.getId(), null, shampoo, 2)))
                .andExpect(status().isOk());
        Integer cobrado = pedidos.findAll().get(0).getId();
        mvc.perform(post("/api/pagos/registrar").with(como(aux.getId())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"referencia\":\"pedido\",\"referencia_id\":" + cobrado + ",\"metodo\":\"Efectivo\"}"))
                .andExpect(status().isOk());
        assertEquals("Entregado", pedidos.findById(cobrado).orElseThrow().getEstado());
        mvc.perform(patch("/api/pedidos/" + cobrado + "/estado").with(como(aux.getId())).contentType(MediaType.APPLICATION_JSON).content(cancelar))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("Para este pedido usa una devolución"));
        assertEquals(8, productos.findById(shampoo.getId()).orElseThrow().getStock());

        mvc.perform(post("/api/pedidos").with(como(cliente.getId())).contentType(MediaType.APPLICATION_JSON).content(venta(null, null, shampoo, 1)))
                .andExpect(status().isOk());
        Integer web = pedidos.findAll().stream().mapToInt(p -> p.getId()).max().orElseThrow();
        mvc.perform(patch("/api/pedidos/" + web + "/estado").with(como(aux.getId())).contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"Entregado\"}"))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/pedidos/" + web + "/estado").with(como(aux.getId())).contentType(MediaType.APPLICATION_JSON).content(cancelar))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("Para este pedido usa una devolución"));
        assertEquals(7, productos.findById(shampoo.getId()).orElseThrow().getStock());
        assertEquals("Entregado", pedidos.findById(web).orElseThrow().getEstado());
    }

    @Test
    @DisplayName("Dos ventas simultáneas con la misma receta: solo una pasa")
    void ventasSimultaneas() throws Exception {
        Receta r = recetaAprobada(LocalDateTime.now().minusDays(1));
        VentaService.Pedir pedir = new VentaService.Pedir(null, List.of(new VentaService.Item(antibiotico.getId(), 1)), r.getId(), null);
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        List<Future<Boolean>> resultados = new ArrayList<>();
        Callable<Boolean> intento = () -> {
            salida.await();
            try {
                ventaService.vender(cliente.getId(), pedir);
                return true;
            } catch (RuntimeException e) {
                return false;
            }
        };
        resultados.add(hilos.submit(intento));
        resultados.add(hilos.submit(intento));
        salida.countDown();
        int exitos = 0;
        for (Future<Boolean> f : resultados) if (f.get()) exitos++;
        hilos.shutdown();
        assertEquals(1, exitos);
        assertEquals(1, pedidos.count());
        assertEquals(9, productos.findById(antibiotico.getId()).orElseThrow().getStock());
    }

    @Test
    @DisplayName("Borrar una categoría con productos devuelve 409 con mensaje claro")
    void categoriaConProductos() throws Exception {
        mvc.perform(delete("/api/categorias/" + categoria.getId()).with(como(aux.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("productos activos")));
    }

    @Test
    @DisplayName("Con las claves foráneas nuevas, un producto con categoría inexistente o una notificación huérfana se rechazan")
    void clavesForaneas() throws Exception {
        jdbc.update("DELETE FROM notificacion WHERE usuario_id IS NOT NULL AND usuario_id NOT IN (SELECT id FROM usuario)");
        jdbc.update("DELETE FROM movimiento_stock WHERE usuario_id IS NOT NULL AND usuario_id NOT IN (SELECT id FROM usuario)");
        jdbc.execute("ALTER TABLE producto ADD CONSTRAINT producto_categoria_id_fkey FOREIGN KEY (categoria_id) REFERENCES categoria_producto(id)");
        jdbc.execute("ALTER TABLE notificacion ADD CONSTRAINT notificacion_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES usuario(id)");
        jdbc.execute("ALTER TABLE movimiento_stock ADD CONSTRAINT movimiento_stock_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES usuario(id)");
        mvc.perform(post("/api/productos").with(como(aux.getId())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Huérfano\",\"precio\":10,\"stock\":0,\"categoria_id\":999999}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("relacionado")));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> jdbc.update("INSERT INTO notificacion (usuario_id, titulo, enviado) VALUES (999999, 'x', true)"));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> jdbc.update("INSERT INTO movimiento_stock (producto_id, tipo, cantidad, usuario_id) VALUES (?, 'Salida', 1, 999999)", shampoo.getId()));
        mvc.perform(post("/api/pedidos").with(como(cliente.getId())).contentType(MediaType.APPLICATION_JSON).content(venta(null, null, shampoo, 1)))
                .andExpect(status().isOk());
    }
}
