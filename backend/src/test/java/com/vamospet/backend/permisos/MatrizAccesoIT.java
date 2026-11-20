package com.vamospet.backend.permisos;

import com.vamospet.backend.modelo.Mascota;
import com.vamospet.backend.modelo.Notificacion;
import com.vamospet.backend.modelo.Usuario;
import com.vamospet.backend.repositorio.MascotaRepository;
import com.vamospet.backend.repositorio.NotificacionRepository;
import com.vamospet.backend.repositorio.UsuarioRepository;
import com.vamospet.backend.servicio.NotificacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.vamospet.backend.ConToken.como;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MatrizAccesoIT {

    @Autowired private MockMvc mvc;
    @Autowired private ApplicationContext contexto;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private MascotaRepository mascotas;
    @Autowired private NotificacionRepository notificaciones;
    @MockitoBean private NotificacionService notificacionService;

    private final Map<Rol, Usuario> porRol = new EnumMap<>(Rol.class);
    private Usuario otroCliente;

    @BeforeEach
    void datos() {
        notificaciones.deleteAll();
        mascotas.deleteAll();
        usuarios.deleteAll();
        int dni = 50000001;
        for (Rol rol : Rol.values()) {
            porRol.put(rol, guardar("Persona " + rol.name(), String.valueOf(dni++), rol.name().toLowerCase() + "@matriz.test", rol.getId()));
        }
        otroCliente = guardar("Otro Cliente", String.valueOf(dni), "otro@matriz.test", Rol.CLIENTE.getId());
    }

    private Usuario guardar(String nombre, String dni, String correo, int rol) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setDni(dni);
        u.setCorreo(correo);
        u.setContrasena("Clave2026");
        u.setRol_id(rol);
        u.setActivo(true);
        return usuarios.save(u);
    }

    private Mascota mascotaDe(Usuario dueno) {
        Mascota m = new Mascota();
        m.setUsuario_id(dueno.getId());
        m.setNombre("Firu");
        m.setEspecie("Perro");
        return mascotas.save(m);
    }

    private static String rutaEjemplo(String patron) {
        return patron.replaceAll("\\{[^}]+}", "1");
    }

    @Test
    @DisplayName("Cada endpoint, con cada rol que no le corresponde, responde 403")
    void matrizCompletaRechazaRolesAjenos() throws Exception {
        List<String> fallos = new ArrayList<>();
        int revisados = 0;
        for (Regla regla : PermisosApi.reglas()) {
            if (regla.isPublica()) continue;
            for (Rol rol : Rol.values()) {
                if (regla.permite(rol)) continue;
                revisados++;
                int estado = mvc.perform(request(HttpMethod.valueOf(regla.getMetodo()), rutaEjemplo(regla.getPatron()))
                                .with(como(porRol.get(rol).getId())))
                        .andReturn().getResponse().getStatus();
                if (estado != 403) fallos.add(rol + " " + regla.getMetodo() + " " + regla.getPatron() + " -> " + estado);
            }
        }
        assertTrue(revisados > 100, "Se revisaron muy pocas combinaciones: " + revisados);
        assertTrue(fallos.isEmpty(), "Combinaciones que no devolvieron 403:\n" + String.join("\n", fallos));
    }

    @Test
    @DisplayName("Sin token, todo lo que no es público responde 401")
    void sinIdentidad() throws Exception {
        List<String> fallos = new ArrayList<>();
        for (Regla regla : PermisosApi.reglas()) {
            if (regla.isPublica()) continue;
            int estado = mvc.perform(request(HttpMethod.valueOf(regla.getMetodo()), rutaEjemplo(regla.getPatron())))
                    .andReturn().getResponse().getStatus();
            if (estado != 401) fallos.add(regla.getMetodo() + " " + regla.getPatron() + " -> " + estado);
        }
        assertTrue(fallos.isEmpty(), String.join("\n", fallos));
    }

    @Test
    @DisplayName("Todos los endpoints del backend están en la matriz, y la matriz no tiene reglas sin endpoint")
    void coberturaTotal() {
        RequestMappingHandlerMapping mapeo = contexto.getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class);
        Set<String> existentes = new HashSet<>();
        List<String> sinRegla = new ArrayList<>();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> e : mapeo.getHandlerMethods().entrySet()) {
            for (String patron : e.getKey().getPatternValues()) {
                if (!patron.startsWith("/api/")) continue;
                for (RequestMethod metodo : e.getKey().getMethodsCondition().getMethods()) {
                    existentes.add(metodo.name() + " " + patron);
                    PermisosApi.Coincidencia c = PermisosApi.buscar(metodo.name(), rutaEjemplo(patron));
                    if (c == null || !c.regla().getPatron().equals(patron)) sinRegla.add(metodo.name() + " " + patron);
                }
            }
        }
        assertTrue(sinRegla.isEmpty(), "Endpoints sin regla propia en la matriz:\n" + String.join("\n", sinRegla));
        List<String> sobrantes = PermisosApi.reglas().stream()
                .map(r -> r.getMetodo() + " " + r.getPatron())
                .filter(r -> !existentes.contains(r)).toList();
        assertTrue(sobrantes.isEmpty(), "Reglas sin endpoint:\n" + String.join("\n", sobrantes));
    }

    @Test
    @DisplayName("Ningún endpoint toma la identidad de un parámetro usuarioId o usuario_id: siempre sale del token")
    void ningunaIdentidadPorParametro() {
        RequestMappingHandlerMapping mapeo = contexto.getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class);
        List<String> encontrados = new ArrayList<>();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> e : mapeo.getHandlerMethods().entrySet()) {
            for (MethodParameter p : e.getValue().getMethodParameters()) {
                RequestParam rp = p.getParameterAnnotation(RequestParam.class);
                if (rp == null) continue;
                p.initParameterNameDiscovery(new DefaultParameterNameDiscoverer());
                String nombre = !rp.name().isEmpty() ? rp.name() : !rp.value().isEmpty() ? rp.value() : p.getParameterName();
                if ("usuarioId".equals(nombre) || "usuario_id".equals(nombre)) {
                    encontrados.add(e.getValue().getBeanType().getSimpleName() + "." + e.getValue().getMethod().getName());
                }
            }
        }
        assertTrue(encontrados.isEmpty(), "Endpoints que todavía leen la identidad de la URL:\n" + String.join("\n", encontrados));
    }

    @Test
    @DisplayName("Un cliente no puede leer las mascotas, citas ni pedidos de otro cliente")
    void propiedadDelCliente() throws Exception {
        Usuario cliente = porRol.get(Rol.CLIENTE);
        Mascota ajena = mascotaDe(otroCliente);
        Mascota propia = mascotaDe(cliente);
        mvc.perform(get("/api/mascotas/usuario/" + otroCliente.getId()).with(como(cliente.getId())))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/mascotas/usuario/" + cliente.getId()).with(como(cliente.getId())))
                .andExpect(status().isOk());
        mvc.perform(get("/api/citas/mascota/" + ajena.getId()).with(como(cliente.getId())))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/citas/mascota/" + propia.getId()).with(como(cliente.getId())))
                .andExpect(status().isOk());
        mvc.perform(get("/api/historial/mascota/" + ajena.getId()).with(como(cliente.getId())))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/citas/usuario/" + otroCliente.getId()).with(como(cliente.getId())))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/pedidos/usuario/" + otroCliente.getId()).with(como(cliente.getId())))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/historial/mascota/" + ajena.getId()).with(como(porRol.get(Rol.VETERINARIO).getId())))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Nadie lee ni marca las notificaciones de otra persona, ni siquiera el Admin")
    void notificacionesPropias() throws Exception {
        Usuario admin = porRol.get(Rol.ADMIN);
        Usuario cliente = porRol.get(Rol.CLIENTE);
        mvc.perform(get("/api/notificaciones/usuario/" + cliente.getId()).with(como(admin.getId())))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/notificaciones/usuario/" + cliente.getId()).with(como(cliente.getId())))
                .andExpect(status().isOk());
        Notificacion n = new Notificacion();
        n.setUsuario_id(cliente.getId());
        n.setTitulo("Prueba");
        n.setMensaje("Hola");
        n = notificaciones.save(n);
        mvc.perform(request(HttpMethod.PATCH, "/api/notificaciones/" + n.getId() + "/leer").with(como(otroCliente.getId())))
                .andExpect(status().isForbidden());
        mvc.perform(request(HttpMethod.PATCH, "/api/notificaciones/" + n.getId() + "/leer").with(como(cliente.getId())))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Si el token y el parámetro usuarioId no coinciden se rechaza, y el parámetro solo ya no identifica a nadie")
    void identidadInconsistente() throws Exception {
        mvc.perform(get("/api/usuarios").with(como(porRol.get(Rol.CLIENTE).getId()))
                        .param("usuarioId", porRol.get(Rol.ADMIN).getId().toString()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/usuarios").param("usuarioId", porRol.get(Rol.ADMIN).getId().toString()))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/usuarios").with(como(porRol.get(Rol.ADMIN).getId())).param("usuarioId", porRol.get(Rol.ADMIN).getId().toString()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Una cuenta dada de baja no pasa aunque su token siga vigente")
    void cuentaDeBaja() throws Exception {
        Usuario aux = porRol.get(Rol.AUXILIAR);
        aux.setActivo(false);
        usuarios.save(aux);
        mvc.perform(get("/api/pagos").with(como(aux.getId())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Solo el Admin da de baja al personal; el Auxiliar solo a clientes")
    void bajaDePersonal() throws Exception {
        Usuario aux = porRol.get(Rol.AUXILIAR);
        Usuario vet = porRol.get(Rol.VETERINARIO);
        mvc.perform(request(HttpMethod.PATCH, "/api/usuarios/" + vet.getId() + "/baja").with(como(aux.getId())))
                .andExpect(status().isForbidden());
        mvc.perform(request(HttpMethod.PATCH, "/api/usuarios/" + otroCliente.getId() + "/baja").with(como(aux.getId())))
                .andExpect(status().isOk());
        mvc.perform(request(HttpMethod.PATCH, "/api/usuarios/" + vet.getId() + "/baja").with(como(porRol.get(Rol.ADMIN).getId())))
                .andExpect(status().isOk());
    }
}
