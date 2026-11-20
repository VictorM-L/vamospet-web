package com.vamospet.backend.controlador;

import com.vamospet.backend.modelo.Cita;
import com.vamospet.backend.modelo.Pedido;
import com.vamospet.backend.modelo.PedidoDetalle;
import com.vamospet.backend.modelo.Producto;
import com.vamospet.backend.modelo.Servicio;
import com.vamospet.backend.modelo.Usuario;
import com.vamospet.backend.repositorio.CitaRepository;
import com.vamospet.backend.repositorio.PedidoDetalleRepository;
import com.vamospet.backend.repositorio.PedidoRepository;
import com.vamospet.backend.repositorio.ProductoRepository;
import com.vamospet.backend.repositorio.ServicioRepository;
import com.vamospet.backend.repositorio.UsuarioRepository;
import com.vamospet.backend.servicio.NotificacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.vamospet.backend.ConToken.como;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BorradosSegurosIT {


    @Autowired private MockMvc mvc;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private ServicioRepository servicios;
    @Autowired private CitaRepository citas;
    @Autowired private ProductoRepository productos;
    @Autowired private PedidoRepository pedidos;
    @Autowired private PedidoDetalleRepository detalles;
    @MockitoBean private NotificacionService notificacionService;

    private Usuario admin, vet, aux;

    @BeforeEach
    void datos() {
        detalles.deleteAll();
        pedidos.deleteAll();
        citas.deleteAll();
        servicios.deleteAll();
        usuarios.deleteAll();
        admin = guardar("Admin", "65000001", 1);
        vet = guardar("Lucia Vet", "65000002", 3);
        aux = guardar("Rosa Aux", "65000003", 4);
    }

    private Usuario guardar(String nombre, String dni, int rol) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setDni(dni);
        u.setCorreo(dni + "@borrado.test");
        u.setContrasena("Clave2026");
        u.setRol_id(rol);
        u.setActivo(true);
        return usuarios.save(u);
    }

    private Cita citaFutura(Integer servicioId, Integer vetId, String estado) {
        Cita c = new Cita();
        c.setServicio_id(servicioId);
        c.setVeterinario_id(vetId);
        c.setFecha_hora(LocalDateTime.now().plusDays(3));
        c.setEstado(estado);
        c.setCosto(new BigDecimal("60.00"));
        return citas.save(c);
    }

    @Test
    @DisplayName("Un servicio con citas por atender no se da de baja (409); sin ellas, sí")
    void servicioConCitas() throws Exception {
        Servicio s = new Servicio();
        s.setNombre("Consulta");
        s.setPrecio_base(new BigDecimal("60.00"));
        s.setActivo(true);
        s = servicios.save(s);
        Cita c = citaFutura(s.getId(), vet.getId(), "Confirmada");
        mvc.perform(delete("/api/servicios/" + s.getId()).with(como(admin.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("cita(s) por atender")));
        c.setEstado("Cancelada");
        citas.save(c);
        mvc.perform(delete("/api/servicios/" + s.getId()).with(como(admin.getId()))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Un veterinario con citas por atender no se da de baja (409)")
    void veterinarioConCitas() throws Exception {
        citaFutura(null, vet.getId(), "Pendiente");
        mvc.perform(patch("/api/usuarios/" + vet.getId() + "/baja").with(como(admin.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("No puedo atender")));
        mvc.perform(patch("/api/usuarios/" + aux.getId() + "/baja").with(como(admin.getId()))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Un producto en un pedido por entregar no se da de baja (409)")
    void productoEnPedidoAbierto() throws Exception {
        Producto p = new Producto();
        p.setNombre("Collar");
        p.setPrecio(new BigDecimal("25.00"));
        p.setStock(3);
        p = productos.save(p);
        Pedido pe = new Pedido();
        pe.setEstado("Listo para recoger");
        pe.setMetodo_entrega("Recojo en tienda");
        pe.setTotal(new BigDecimal("25.00"));
        pe = pedidos.save(pe);
        PedidoDetalle d = new PedidoDetalle();
        d.setPedido_id(pe.getId());
        d.setProducto_id(p.getId());
        d.setCantidad(1);
        d.setPrecio_unitario(new BigDecimal("25.00"));
        detalles.save(d);
        mvc.perform(delete("/api/productos/" + p.getId()).with(como(aux.getId()))).andExpect(status().isConflict());
        pe.setEstado("Entregado");
        pedidos.save(pe);
        mvc.perform(delete("/api/productos/" + p.getId()).with(como(aux.getId()))).andExpect(status().isOk());
    }
}
