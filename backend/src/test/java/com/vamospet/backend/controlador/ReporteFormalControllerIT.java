package com.vamospet.backend.controlador;
import com.vamospet.backend.modelo.Usuario;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReporteFormalControllerIT {
    @Autowired private MockMvc mvc;
    @Autowired private UsuarioRepository usuarios;
    @MockitoBean private NotificacionService notificacionService;
    private Integer adminId;
    private Integer clienteId;
    @BeforeEach
    void datos() {
        usuarios.deleteAll();
        adminId = guardar("Admin IT", "20000001", "admin@it.test", 1).getId();
        clienteId = guardar("Cliente IT", "20000002", "cli@it.test", 2).getId();
    }
    private Usuario guardar(String nombre, String dni, String correo, int rol) {
        Usuario u = new Usuario();
        u.setNombre(nombre); u.setDni(dni); u.setCorreo(correo); u.setContrasena("Demo1234");
        u.setRol_id(rol); u.setActivo(true);
        return usuarios.save(u);
    }
    @Test @DisplayName("Un administrador descarga el Excel del reporte de ingresos")
    void adminDescargaExcel() throws Exception {
        mvc.perform(get("/api/reportes/formal/ingresos/xlsx").param("usuarioId", adminId.toString()))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("reporte-ingresos.xlsx")));
    }
    @Test @DisplayName("Un cliente NO puede pedir reportes (403)")
    void clienteNoPuede() throws Exception {
        mvc.perform(get("/api/reportes/formal/ingresos/xlsx").param("usuarioId", clienteId.toString()))
        .andExpect(status().isForbidden());
    }
    @Test @DisplayName("Un tipo de reporte inexistente devuelve 404")
    void tipoDesconocido() throws Exception {
        mvc.perform(get("/api/reportes/formal/inventado/xlsx").param("usuarioId", adminId.toString()))
        .andExpect(status().isNotFound());
    }
    @Test @DisplayName("GET /api/usuarios sin identificarse devuelve 403")
    void listarUsuariosSinPermiso() throws Exception {
        mvc.perform(get("/api/usuarios")).andExpect(status().isForbidden());
    }
}
