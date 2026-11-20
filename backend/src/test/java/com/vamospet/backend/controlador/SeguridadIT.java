package com.vamospet.backend.controlador;

import com.vamospet.backend.modelo.MensajeContacto;
import com.vamospet.backend.modelo.Usuario;
import com.vamospet.backend.repositorio.MensajeContactoRepository;
import com.vamospet.backend.repositorio.UsuarioRepository;
import com.vamospet.backend.servicio.NotificacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;
import java.util.List;

import static com.vamospet.backend.ConToken.como;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SeguridadIT {

    @Autowired private MockMvc mvc;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private MensajeContactoRepository mensajes;
    @Autowired private PasswordEncoder encoder;
    @MockitoBean private NotificacionService notificacionService;

    private Usuario admin, aux, cliente;

    @BeforeEach
    void datos() {
        mensajes.deleteAll();
        usuarios.deleteAll();
        admin = guardar("Admin Seguro", "63000001", "admin@seguro.test", 1);
        aux = guardar("Rosa Segura", "63000002", "aux@seguro.test", 4);
        cliente = guardar("Ana Segura", "63000003", "ana@seguro.test", 2);
    }

    private Usuario guardar(String nombre, String dni, String correo, int rol) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setDni(dni);
        u.setCorreo(correo);
        u.setContrasena(encoder.encode("Clave2026"));
        u.setRol_id(rol);
        u.setActivo(true);
        return usuarios.save(u);
    }

    private static String login(String correo, String clave) {
        return "{\"correo\":\"" + correo.replace("\"", "\\\"") + "\",\"contrasena\":\"" + clave.replace("\"", "\\\"") + "\"}";
    }

    @Test
    @DisplayName("Inyección SQL en el login: no entra con ningún patrón clásico y la tabla queda intacta")
    void inyeccionLogin() throws Exception {
        long antes = usuarios.count();
        String[] ataques = {"' OR '1'='1", "admin@seguro.test' --", "' OR 1=1; DROP TABLE usuario; --", "\" OR \"\"=\"", "') OR ('a'='a"};
        for (String ataque : ataques) {
            mvc.perform(post("/api/usuarios/login").contentType(MediaType.APPLICATION_JSON).content(login("x" + ataque.hashCode() + "@seguro.test", ataque)))
                    .andExpect(status().isUnauthorized());
            mvc.perform(post("/api/usuarios/login").contentType(MediaType.APPLICATION_JSON).content(login(ataque, "Clave2026")))
                    .andExpect(status().isUnauthorized());
        }
        assertEquals(antes, usuarios.count());
        mvc.perform(post("/api/usuarios/login").contentType(MediaType.APPLICATION_JSON).content(login("admin@seguro.test", "Clave2026")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Inyección SQL en filtros y búsquedas: se trata como texto o se rechaza el tipo, sin filtrar datos")
    void inyeccionFiltros() throws Exception {
        mvc.perform(get("/api/usuarios/existe").param("correo", "' OR '1'='1")).andExpect(status().isOk()).andExpect(jsonPath("$.existe").value(false));
        mvc.perform(get("/api/usuarios/existe").param("dni", "1' OR '1'='1")).andExpect(status().isOk()).andExpect(jsonPath("$.existe").value(false));
        mvc.perform(get(URI.create("/api/usuarios/buscar-dni/63000003%27%20OR%20%271%27%3D%271")).with(como(aux.getId()))).andExpect(status().isNotFound());
        mvc.perform(get("/api/examenes").param("mascotaId", "1 OR 1=1").with(como(admin.getId()))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/examenes").param("estados", "Pendiente') OR ('1'='1").with(como(admin.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get(URI.create("/api/mascotas/usuario/1%20OR%201%3D1")).with(como(admin.getId()))).andExpect(status().isBadRequest());
        assertEquals(3, usuarios.count());
    }

    @Test
    @DisplayName("XSS: el texto del formulario de contacto se guarda tal cual y vuelve como JSON, nunca como HTML")
    void xssContacto() throws Exception {
        String ataque = "<script>alert('x')</script><img src=x onerror=alert(1)>";
        mvc.perform(post("/api/contacto").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana Segura\",\"correo\":\"ana@correo.pe\",\"asunto\":\"Otro\",\"mensaje\":\"" + ataque.replace("'", "\\u0027") + " necesito una cita\"}"))
                .andExpect(status().isOk());
        List<MensajeContacto> guardados = mensajes.findAll();
        assertEquals(1, guardados.size());
        assertTrue(guardados.get(0).getMensaje().startsWith("<script>"));
        MockHttpServletResponse r = mvc.perform(get("/api/contacto").with(como(aux.getId()))).andExpect(status().isOk()).andReturn().getResponse();
        assertTrue(r.getContentType().startsWith(MediaType.APPLICATION_JSON_VALUE));
        assertEquals("nosniff", r.getHeader("X-Content-Type-Options"));
    }

    @Test
    @DisplayName("XSS: el registro rechaza nombres con etiquetas HTML")
    void xssNombre() throws Exception {
        mvc.perform(post("/api/usuarios/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"<b>Ana</b>\",\"dni\":\"63000009\",\"correo\":\"b@seguro.test\",\"telefono\":\"912345678\",\"contrasena\":\"Clave2026\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.nombre").exists());
    }

    @Test
    @DisplayName("CSRF: la API no crea sesión ni cookies, así que un sitio ajeno no tiene credencial que reutilizar")
    void sinSesionNiCookies() throws Exception {
        MockHttpServletResponse r = mvc.perform(post("/api/usuarios/login").contentType(MediaType.APPLICATION_JSON).content(login("admin@seguro.test", "Clave2026")))
                .andExpect(status().isOk()).andReturn().getResponse();
        assertNull(r.getHeader("Set-Cookie"));
        assertEquals(0, r.getCookies().length);
        var peticion = mvc.perform(get("/api/usuarios").with(como(admin.getId()))).andReturn();
        assertNull(peticion.getRequest().getSession(false));
        mvc.perform(post("/api/pedidos").header("Origin", "https://sitio-ajeno.example").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"producto_id\":1,\"cantidad\":1}]}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Cabeceras de seguridad: CSP sin orígenes externos para scripts, anti-iframe y nosniff")
    void cabeceras() throws Exception {
        MockHttpServletResponse r = mvc.perform(get("/api/servicios")).andReturn().getResponse();
        String csp = r.getHeader("Content-Security-Policy");
        assertNotNull(csp);
        assertTrue(csp.contains("script-src 'self' 'unsafe-inline';"));
        assertTrue(csp.contains("object-src 'none'"));
        assertTrue(csp.contains("frame-ancestors 'self'"));
        assertFalse(csp.contains("cdnjs") || csp.contains("googleapis"));
        assertEquals("SAMEORIGIN", r.getHeader("X-Frame-Options"));
        assertEquals("nosniff", r.getHeader("X-Content-Type-Options"));
    }

    @Test
    @DisplayName("Bloqueo del login: 5 intentos fallidos bloquean el correo 15 minutos, aunque luego se escriba bien")
    void bloqueoLogin() throws Exception {
        for (int i = 1; i <= 2; i++) {
            mvc.perform(post("/api/usuarios/login").contentType(MediaType.APPLICATION_JSON).content(login("ana@seguro.test", "Mala" + i)))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").value("Correo o contraseña incorrectos."));
        }
        mvc.perform(post("/api/usuarios/login").contentType(MediaType.APPLICATION_JSON).content(login("ana@seguro.test", "Mala3")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Correo o contraseña incorrectos. Te quedan 2 intentos antes de un bloqueo de 15 minutos."));
        mvc.perform(post("/api/usuarios/login").contentType(MediaType.APPLICATION_JSON).content(login("ana@seguro.test", "Mala4")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Correo o contraseña incorrectos. Te queda 1 intento antes de un bloqueo de 15 minutos."));
        mvc.perform(post("/api/usuarios/login").contentType(MediaType.APPLICATION_JSON).content(login("ana@seguro.test", "Mala5")))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("Por seguridad bloqueamos el ingreso con este correo durante 15 minutos tras 5 intentos fallidos. Vuelve a intentarlo en 15 minutos o recupera tu contraseña."));
        mvc.perform(post("/api/usuarios/login").contentType(MediaType.APPLICATION_JSON).content(login("ANA@seguro.test", "Clave2026")))
                .andExpect(status().isTooManyRequests());
        mvc.perform(post("/api/usuarios/login").contentType(MediaType.APPLICATION_JSON).content(login("admin@seguro.test", "Clave2026")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Un ingreso correcto antes del quinto fallo reinicia el conteo")
    void ingresoReinicia() throws Exception {
        for (int i = 0; i < 4; i++) {
            mvc.perform(post("/api/usuarios/login").contentType(MediaType.APPLICATION_JSON).content(login("aux@seguro.test", "Mala" + i)))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/usuarios/login").contentType(MediaType.APPLICATION_JSON).content(login("aux@seguro.test", "Clave2026"))).andExpect(status().isOk());
        mvc.perform(post("/api/usuarios/login").contentType(MediaType.APPLICATION_JSON).content(login("aux@seguro.test", "Mala9")))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").value("Correo o contraseña incorrectos."));
    }

    @Test
    @DisplayName("Actuator: /actuator/health solo para el Admin; sin token 401 y con otro rol 403")
    void actuatorSoloAdmin() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/health").with(como(cliente.getId()))).andExpect(status().isForbidden());
        mvc.perform(get("/actuator/health").with(como(aux.getId()))).andExpect(status().isForbidden());
        mvc.perform(get("/actuator/health").with(como(admin.getId()))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/actuator/env").with(como(admin.getId()))).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("El JAR sirve el frontend: la página principal y app.js salen del mismo servidor que la API")
    void sirveFrontend() throws Exception {
        mvc.perform(get("/index.html")).andExpect(status().isOk());
        String js = mvc.perform(get("/js/app.js")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertFalse(js.contains("localhost:8080"));
        assertTrue(js.contains("window.location.origin"));
    }
}
