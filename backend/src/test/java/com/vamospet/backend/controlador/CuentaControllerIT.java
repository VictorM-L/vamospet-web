package com.vamospet.backend.controlador;

import com.vamospet.backend.modelo.Usuario;
import com.vamospet.backend.repositorio.CodigoVerificacionRepository;
import com.vamospet.backend.repositorio.UsuarioRepository;
import com.vamospet.backend.servicio.NotificacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.vamospet.backend.ConToken.como;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CuentaControllerIT {

    @Autowired private MockMvc mvc;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private CodigoVerificacionRepository codigos;
    @MockitoBean private NotificacionService notificacionService;
    @Autowired private PasswordEncoder encoder;

    private Usuario admin;
    private Usuario cliente;

    @BeforeEach
    void datos() {
        codigos.deleteAll();
        usuarios.deleteAll();
        admin = guardar("Admin Prueba", "30000001", "admin@it.test", 1, "Admin2026");
        cliente = guardar("Rosa Prueba", "30000002", "rosa@it.test", 2, "Rosa2026");
    }

    private Usuario guardar(String nombre, String dni, String correo, int rol, String clave) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setDni(dni);
        u.setCorreo(correo);
        u.setContrasena(encoder.encode(clave));
        u.setTelefono("987654321");
        u.setRol_id(rol);
        u.setActivo(true);
        return usuarios.save(u);
    }

    private String registro(String nombre, String dni, String correo, String telefono, String clave) {
        return "{\"nombre\":\"" + nombre + "\",\"dni\":\"" + dni + "\",\"correo\":\"" + correo
                + "\",\"telefono\":\"" + telefono + "\",\"contrasena\":\"" + clave + "\"}";
    }

    private String login(String correo, String clave) {
        return "{\"correo\":\"" + correo + "\",\"contrasena\":\"" + clave + "\"}";
    }

    @Test
    @DisplayName("Registro con DNI con letras devuelve 400 e indica el campo")
    void registroDniInvalido() throws Exception {
        mvc.perform(post("/api/usuarios/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(registro("Luis Soto", "1234567A", "luis@it.test", "987654321", "Clave2026")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.dni").exists());
        assertNull(usuarios.findByDni("1234567A"));
    }

    @Test
    @DisplayName("Registro con teléfono, nombre y contraseña inválidos devuelve 400 con cada campo")
    void registroVariosErrores() throws Exception {
        mvc.perform(post("/api/usuarios/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(registro("Luis 123", "40000001", "luis@it.test", "812345678", "corta")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nombre").exists())
                .andExpect(jsonPath("$.campos.telefono").exists())
                .andExpect(jsonPath("$.campos.contrasena").exists());
    }

    @Test
    @DisplayName("Registro válido crea el cliente con su teléfono")
    void registroValido() throws Exception {
        mvc.perform(post("/api/usuarios/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(registro("Luis Soto", "40000002", "luis@it.test", "912345678", "Clave2026")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol_id").value(2))
                .andExpect(jsonPath("$.contrasena").doesNotExist());
        Usuario u = usuarios.findByDni("40000002");
        assertEquals("912345678", u.getTelefono());
        assertTrue(u.getContrasena().startsWith("$2"));
        assertNotEquals("Clave2026", u.getContrasena());
        mvc.perform(post("/api/usuarios/login").contentType(MediaType.APPLICATION_JSON).content(login("luis@it.test", "Clave2026")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.contrasena").doesNotExist());
        mvc.perform(post("/api/usuarios/login").contentType(MediaType.APPLICATION_JSON).content(login("luis@it.test", "Clave2027")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("El login entrega un token que abre las operaciones del usuario; una clave en texto plano ya no entra")
    void loginEntregaToken() throws Exception {
        String cuerpo = mvc.perform(post("/api/usuarios/login").contentType(MediaType.APPLICATION_JSON).content(login("admin@it.test", "Admin2026")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(admin.getId()))
                .andExpect(jsonPath("$.rol_id").value(1))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.token_expira").isNumber())
                .andExpect(jsonPath("$.contrasena").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        Matcher t = Pattern.compile("\"token\":\"([^\"]+)\"").matcher(cuerpo);
        assertTrue(t.find());
        mvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + t.group(1))).andExpect(status().isOk());
        mvc.perform(get("/api/usuarios")).andExpect(status().isUnauthorized());

        Usuario plano = usuarios.findById(cliente.getId()).orElseThrow();
        plano.setContrasena("Rosa2026");
        usuarios.save(plano);
        mvc.perform(post("/api/usuarios/login").contentType(MediaType.APPLICATION_JSON).content(login("rosa@it.test", "Rosa2026")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("El registro aplica la misma regla de contraseña que muestra la página")
    void registroReglaContrasena() throws Exception {
        mvc.perform(post("/api/usuarios/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(registro("Eva Paz", "40000007", "eva@it.test", "912345678", "ClaveSinNum")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.contrasena").value("La contraseña debe tener entre 8 y 64 caracteres, al menos una letra y al menos un número."));
        mvc.perform(post("/api/usuarios/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(registro("Eva Paz", "40000007", "eva@it.test", "912345678", "clave 2026")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Registro con correo duplicado devuelve 409")
    void registroCorreoDuplicado() throws Exception {
        mvc.perform(post("/api/usuarios/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(registro("Otra Rosa", "40000003", "ROSA@it.test", "912345678", "Clave2026")))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Registro con DNI que ya tiene cuenta devuelve 409")
    void registroDniDuplicado() throws Exception {
        mvc.perform(post("/api/usuarios/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(registro("Otra Rosa", "30000002", "nueva@it.test", "912345678", "Clave2026")))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("La consulta en vivo de duplicados solo responde existe true o false")
    void consultaExiste() throws Exception {
        mvc.perform(get("/api/usuarios/existe").param("dni", "30000002"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.existe").value(true));
        mvc.perform(get("/api/usuarios/existe").param("dni", "49999999"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.existe").value(false));
        mvc.perform(get("/api/usuarios/existe").param("correo", "Rosa@IT.test"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.existe").value(true))
                .andExpect(jsonPath("$.nombre").doesNotExist());
        mvc.perform(get("/api/usuarios/existe")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Flujo completo: pedir código, verificarlo, cambiar la contraseña y entrar con la nueva")
    void flujoCompletoRecuperacion() throws Exception {
        when(notificacionService.enviarCorreoDirecto(anyString(), anyString(), anyString())).thenReturn(true);

        mvc.perform(post("/api/usuarios/recuperar/solicitar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"rosa@it.test\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").exists());

        ArgumentCaptor<String> texto = ArgumentCaptor.forClass(String.class);
        verify(notificacionService).enviarCorreoDirecto(eq("rosa@it.test"), anyString(), texto.capture());
        Matcher m = Pattern.compile("Tu código es: (\\d{6})").matcher(texto.getValue());
        assertTrue(m.find());
        String codigo = m.group(1);

        mvc.perform(post("/api/usuarios/recuperar/verificar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"rosa@it.test\",\"codigo\":\"" + codigo + "\"}"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/usuarios/recuperar/restablecer").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"rosa@it.test\",\"codigo\":\"" + codigo + "\",\"contrasena\":\"debil\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/usuarios/recuperar/restablecer").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"rosa@it.test\",\"codigo\":\"" + codigo + "\",\"contrasena\":\"SoloLetrasRosa\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.contrasena").value("La contraseña debe tener entre 8 y 64 caracteres, al menos una letra y al menos un número."));

        mvc.perform(post("/api/usuarios/recuperar/restablecer").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"rosa@it.test\",\"codigo\":\"" + codigo + "\",\"contrasena\":\"NuevaRosa9\"}"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/usuarios/login").contentType(MediaType.APPLICATION_JSON).content(login("rosa@it.test", "NuevaRosa9")))
                .andExpect(status().isOk());
        assertTrue(usuarios.findById(cliente.getId()).orElseThrow().getContrasena().startsWith("$2"));
        mvc.perform(post("/api/usuarios/login").contentType(MediaType.APPLICATION_JSON).content(login("rosa@it.test", "Rosa2026")))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/usuarios/recuperar/restablecer").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"rosa@it.test\",\"codigo\":\"" + codigo + "\",\"contrasena\":\"OtraRosa9\"}"))
                .andExpect(status().isGone());
    }

    @Test
    @DisplayName("Pedir recuperación para un correo inexistente responde igual que para uno real")
    void respuestaGenerica() throws Exception {
        String real = mvc.perform(post("/api/usuarios/recuperar/solicitar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"rosa@it.test\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String falso = mvc.perform(post("/api/usuarios/recuperar/solicitar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"nadie@it.test\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertEquals(real, falso);
        verify(notificacionService, never()).enviarCorreoDirecto(eq("nadie@it.test"), anyString(), anyString());
    }

    @Test
    @DisplayName("El Admin crea un veterinario sin contraseña y le llega el código para crearla")
    void altaPersonalSinContrasena() throws Exception {
        when(notificacionService.enviarCorreoDirecto(anyString(), anyString(), anyString())).thenReturn(true);
        mvc.perform(post("/api/usuarios/personal").with(como(admin.getId())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Lucia Vet\",\"dni\":\"40000010\",\"correo\":\"lucia@it.test\",\"telefono\":\"\",\"rol_id\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correoEnviado").value(true))
                .andExpect(jsonPath("$.usuario.rol_id").value(3));
        mvc.perform(post("/api/usuarios/personal").with(como(cliente.getId())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Otro Vet\",\"dni\":\"40000012\",\"correo\":\"otro@it.test\",\"telefono\":\"\",\"rol_id\":3}"))
                .andExpect(status().isForbidden());
        Usuario vet = usuarios.findByDni("40000010");
        assertNull(vet.getContrasena());
        verify(notificacionService).enviarCorreoDirecto(eq("lucia@it.test"), eq("Crea tu contraseña"), anyString());
    }

    @Test
    @DisplayName("El restablecimiento del Admin llega al veterinario; un cliente no puede usar ese botón")
    void restablecimientoPorAdmin() throws Exception {
        when(notificacionService.enviarCorreoDirecto(anyString(), anyString(), anyString())).thenReturn(true);
        Usuario vet = guardar("Mateo Vet", "40000011", "mateo@it.test", 3, "Mateo2026");
        mvc.perform(post("/api/usuarios/personal/" + vet.getId() + "/restablecimiento").param("usuarioId", admin.getId().toString()).with(como(admin.getId())))
                .andExpect(status().isOk());
        verify(notificacionService).enviarCorreoDirecto(eq("mateo@it.test"), eq("Restablece tu contraseña"), anyString());
        mvc.perform(post("/api/usuarios/personal/" + vet.getId() + "/restablecimiento").param("usuarioId", cliente.getId().toString()).with(como(cliente.getId())))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/usuarios/personal/999999/restablecimiento").param("usuarioId", admin.getId().toString()).with(como(admin.getId())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("El cliente edita su perfil con validación y no puede editar el de otro")
    void editarPerfil() throws Exception {
        String cuerpo = "{\"nombre\":\"Rosa María\",\"telefono\":\"999888777\",\"direccion\":\"Av. Larco 100\"}";
        mvc.perform(put("/api/usuarios/" + cliente.getId() + "/perfil").param("usuarioId", cliente.getId().toString()).with(como(cliente.getId()))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telefono").value("999888777"));
        mvc.perform(put("/api/usuarios/" + cliente.getId() + "/perfil").param("usuarioId", admin.getId().toString()).with(como(admin.getId()))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/usuarios/" + cliente.getId() + "/perfil").param("usuarioId", cliente.getId().toString()).with(como(cliente.getId()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Rosa\",\"telefono\":\"12\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.telefono").exists());
    }
}
