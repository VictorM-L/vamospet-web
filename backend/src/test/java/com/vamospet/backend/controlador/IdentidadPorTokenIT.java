package com.vamospet.backend.controlador;

import com.vamospet.backend.modelo.Usuario;
import com.vamospet.backend.permisos.Rol;
import com.vamospet.backend.repositorio.UsuarioRepository;
import com.vamospet.backend.servicio.NotificacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.vamospet.backend.ConToken.como;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IdentidadPorTokenIT {

    @Autowired private MockMvc mvc;
    @Autowired private UsuarioRepository usuarios;
    @MockitoBean private NotificacionService notificacionService;

    private final Map<Rol, Usuario> porRol = new EnumMap<>(Rol.class);

    @BeforeEach
    void datos() {
        usuarios.deleteAll();
        int dni = 62000001;
        for (Rol rol : Rol.values()) {
            Usuario u = new Usuario();
            u.setNombre("Persona " + rol.name());
            u.setDni(String.valueOf(dni++));
            u.setCorreo(rol.name().toLowerCase() + "@identidad.test");
            u.setContrasena("Clave2026");
            u.setTelefono("987654321");
            u.setRol_id(rol.getId());
            u.setActivo(true);
            porRol.put(rol, usuarios.save(u));
        }
    }

    private static Map<Rol, Integer> esperado(int admin, int cliente, int veterinario, int auxiliar) {
        Map<Rol, Integer> m = new EnumMap<>(Rol.class);
        m.put(Rol.ADMIN, admin);
        m.put(Rol.CLIENTE, cliente);
        m.put(Rol.VETERINARIO, veterinario);
        m.put(Rol.AUXILIAR, auxiliar);
        return m;
    }

    @Test
    @DisplayName("Cada endpoint que antes leía usuarioId de la URL responde según el rol del token, sin enviar usuarioId")
    void identidadDelTokenPorRol() throws Exception {
        String dniCliente = porRol.get(Rol.CLIENTE).getDni();
        Map<String, Map<Rol, Integer>> casos = new LinkedHashMap<>();
        casos.put("/api/usuarios", esperado(200, 403, 200, 200));
        casos.put("/api/usuarios/buscar-dni/" + dniCliente, esperado(200, 403, 403, 200));
        casos.put("/api/cirugias/tablero", esperado(200, 403, 200, 200));
        casos.put("/api/lotes/por-vencer", esperado(200, 403, 403, 200));
        casos.put("/api/lotes/producto/999999", esperado(200, 403, 403, 200));
        casos.put("/api/veterinario/examenes/pendientes", esperado(200, 403, 200, 403));
        casos.put("/api/examenes", esperado(200, 200, 200, 200));
        casos.put("/api/cirugias", esperado(200, 200, 200, 200));
        casos.put("/api/pagos/pendientes", esperado(403, 200, 403, 403));
        casos.put("/api/pagos/resumen", esperado(200, 403, 403, 200));
        casos.put("/api/auxiliar/emergencia/pendientes", esperado(200, 403, 200, 200));
        casos.put("/api/estancias", esperado(200, 200, 200, 200));
        casos.put("/api/estancias/cuartos", esperado(200, 403, 200, 200));
        casos.put("/api/viajes", esperado(200, 200, 403, 200));
        casos.put("/api/reportes/formal/ingresos", esperado(200, 403, 403, 403));
        List<String> fallos = new ArrayList<>();
        for (Map.Entry<String, Map<Rol, Integer>> caso : casos.entrySet()) {
            for (Map.Entry<Rol, Integer> e : caso.getValue().entrySet()) {
                int estado = mvc.perform(get(caso.getKey()).with(como(porRol.get(e.getKey()).getId())))
                        .andReturn().getResponse().getStatus();
                if (estado != e.getValue()) fallos.add(e.getKey() + " GET " + caso.getKey() + " -> " + estado + " (se esperaba " + e.getValue() + ")");
            }
        }
        assertTrue(fallos.isEmpty(), String.join("\n", fallos));
    }

    @Test
    @DisplayName("El Admin abre la lista de usuarios (pantalla Personal) solo con su token")
    void adminListaUsuarios() throws Exception {
        mvc.perform(get("/api/usuarios").with(como(porRol.get(Rol.ADMIN).getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(Rol.values().length));
    }

    @Test
    @DisplayName("Editar el perfil propio y enviar un restablecimiento funcionan sin usuarioId en la URL")
    void escriturasSinParametro() throws Exception {
        Usuario cliente = porRol.get(Rol.CLIENTE);
        String cuerpo = "{\"nombre\":\"Ana Torres\",\"telefono\":\"999888777\",\"direccion\":\"Av. Larco 100\"}";
        mvc.perform(put("/api/usuarios/" + cliente.getId() + "/perfil").with(como(cliente.getId()))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isOk()).andExpect(jsonPath("$.telefono").value("999888777"));
        mvc.perform(put("/api/usuarios/" + cliente.getId() + "/perfil").with(como(porRol.get(Rol.AUXILIAR).getId()))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isForbidden());
        when(notificacionService.enviarCorreoDirecto(anyString(), anyString(), anyString())).thenReturn(true);
        Usuario vet = porRol.get(Rol.VETERINARIO);
        mvc.perform(post("/api/usuarios/personal/" + vet.getId() + "/restablecimiento").with(como(porRol.get(Rol.ADMIN).getId())))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Un usuarioId en la URL no puede suplantar al dueño del token")
    void parametroNoSuplanta() throws Exception {
        Usuario cliente = porRol.get(Rol.CLIENTE);
        Usuario admin = porRol.get(Rol.ADMIN);
        mvc.perform(get("/api/usuarios").with(como(cliente.getId())).param("usuarioId", admin.getId().toString()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/reportes/formal/ingresos").with(como(cliente.getId())))
                .andExpect(status().isForbidden());
    }
}
