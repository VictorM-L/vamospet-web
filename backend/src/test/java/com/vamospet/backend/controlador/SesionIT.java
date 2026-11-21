package com.vamospet.backend.controlador;

import com.vamospet.backend.ConToken;
import com.vamospet.backend.modelo.Usuario;
import com.vamospet.backend.repositorio.UsuarioRepository;
import com.vamospet.backend.seguridad.SesionService;
import com.vamospet.backend.servicio.NotificacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SesionIT {

    private static final String AUTH = "Authorization";

    @Autowired private MockMvc mvc;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private PasswordEncoder encoder;
    @MockitoBean private NotificacionService notificacionService;

    private Usuario cliente, aux;

    @BeforeEach
    void datos() {
        usuarios.deleteAll();
        cliente = guardar("Ana Sesion", "61000001", "ana@sesion.test", 2);
        aux = guardar("Rosa Sesion", "61000002", "rosa@sesion.test", 4);
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

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private String[] entrar(String correo) throws Exception {
        String cuerpo = mvc.perform(post("/api/usuarios/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + correo + "\",\"contrasena\":\"Clave2026\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return new String[]{campo(cuerpo, "token"), campo(cuerpo, "token_expira")};
    }

    private static String campo(String json, String nombre) {
        Matcher m = Pattern.compile("\"" + nombre + "\":\"?([^\",}]+)").matcher(json);
        assertTrue(m.find(), nombre);
        return m.group(1);
    }

    @Test
    @DisplayName("El cliente recibe un token de 20 minutos y el personal uno de 8 horas desde que entra")
    void vigenciaPorRol() throws Exception {
        long ahora = System.currentTimeMillis();
        long cli = Long.parseLong(entrar("ana@sesion.test")[1]);
        long per = Long.parseLong(entrar("rosa@sesion.test")[1]);
        assertTrue(Math.abs(cli - (ahora + SesionService.INACTIVIDAD_CLIENTE.toMillis())) < 5000);
        assertTrue(Math.abs(per - (ahora + SesionService.TOTAL_PERSONAL.toMillis())) < 5000);
    }

    @Test
    @DisplayName("Un token vencido, alterado o mal formado responde 401 en lo que no es público")
    void tokenInvalido() throws Exception {
        Instant hace = Instant.now().minus(Duration.ofMinutes(30));
        String vencido = ConToken.JWT.emitir(cliente.getId(), hace, hace.plus(SesionService.INACTIVIDAD_CLIENTE));
        mvc.perform(get("/api/mascotas/usuario/" + cliente.getId()).header(AUTH, bearer(vencido)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Tu sesión expiró o no es válida. Vuelve a iniciar sesión."));
        String bueno = ConToken.token(cliente.getId());
        String[] p = bueno.split("\\.");
        String otroCuerpo = ConToken.token(aux.getId()).split("\\.")[1];
        mvc.perform(get("/api/pagos").header(AUTH, bearer(p[0] + "." + otroCuerpo + "." + p[2]))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/mascotas/usuario/" + cliente.getId()).header(AUTH, bearer("abc"))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/mascotas/usuario/" + cliente.getId()).header(AUTH, "Basic abc")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/mascotas/usuario/" + cliente.getId()).header("X-Usuario-Id", cliente.getId())).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/mascotas/usuario/" + cliente.getId()).header(AUTH, bearer(bueno))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Con un token vencido se puede ver lo público y volver a iniciar sesión")
    void vencidoEnPublico() throws Exception {
        Instant hace = Instant.now().minus(Duration.ofHours(9));
        String vencido = ConToken.JWT.emitir(aux.getId(), hace, hace.plus(SesionService.TOTAL_PERSONAL));
        mvc.perform(get("/api/servicios").header(AUTH, bearer(vencido))).andExpect(status().isOk());
        mvc.perform(post("/api/usuarios/login").header(AUTH, bearer(vencido)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"rosa@sesion.test\",\"contrasena\":\"Clave2026\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Renovar da al cliente 20 minutos más e invalida el token anterior")
    void renovarCliente() throws Exception {
        String viejo = entrar("ana@sesion.test")[0];
        String cuerpo = mvc.perform(post("/api/sesion/renovar").header(AUTH, bearer(viejo)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String nuevo = campo(cuerpo, "token");
        assertNotEquals(viejo, nuevo);
        assertTrue(Math.abs(Long.parseLong(campo(cuerpo, "token_expira")) - (System.currentTimeMillis() + SesionService.INACTIVIDAD_CLIENTE.toMillis())) < 5000);
        mvc.perform(get("/api/mascotas/usuario/" + cliente.getId()).header(AUTH, bearer(viejo))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/mascotas/usuario/" + cliente.getId()).header(AUTH, bearer(nuevo))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Renovar no alarga la sesión del personal más allá de 8 horas desde que entró")
    void renovarPersonal() throws Exception {
        Instant inicio = Instant.now().minus(Duration.ofHours(7));
        String token = ConToken.JWT.emitir(aux.getId(), inicio, inicio.plus(SesionService.TOTAL_PERSONAL));
        String cuerpo = mvc.perform(post("/api/sesion/renovar").header(AUTH, bearer(token)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long expira = Long.parseLong(campo(cuerpo, "token_expira"));
        assertTrue(Math.abs(expira - inicio.plus(SesionService.TOTAL_PERSONAL).toEpochMilli()) < 2000);
    }

    @Test
    @DisplayName("Cerrar sesión invalida el token en el servidor")
    void cerrar() throws Exception {
        String token = entrar("rosa@sesion.test")[0];
        mvc.perform(get("/api/pagos").header(AUTH, bearer(token))).andExpect(status().isOk());
        mvc.perform(post("/api/sesion/cerrar").header(AUTH, bearer(token))).andExpect(status().isOk());
        mvc.perform(get("/api/pagos").header(AUTH, bearer(token))).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/sesion/cerrar")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Si el rol cambia, el token ya emitido usa el rol nuevo")
    void rolActualizado() throws Exception {
        String token = entrar("rosa@sesion.test")[0];
        mvc.perform(get("/api/pagos").header(AUTH, bearer(token))).andExpect(status().isOk());
        aux.setRol_id(2);
        usuarios.save(aux);
        mvc.perform(get("/api/pagos").header(AUTH, bearer(token))).andExpect(status().isForbidden());
    }
}
