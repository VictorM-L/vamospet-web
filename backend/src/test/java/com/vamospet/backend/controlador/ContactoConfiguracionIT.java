package com.vamospet.backend.controlador;

import com.vamospet.backend.modelo.Cita;
import com.vamospet.backend.modelo.Mascota;
import com.vamospet.backend.modelo.Usuario;
import com.vamospet.backend.repositorio.CitaRepository;
import com.vamospet.backend.repositorio.ConfiguracionClinicaRepository;
import com.vamospet.backend.repositorio.MascotaRepository;
import com.vamospet.backend.repositorio.MensajeContactoRepository;
import com.vamospet.backend.repositorio.UsuarioRepository;
import com.vamospet.backend.servicio.NotificacionService;
import org.junit.jupiter.api.AfterEach;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.vamospet.backend.ConToken.como;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContactoConfiguracionIT {


    @Autowired private MockMvc mvc;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private MensajeContactoRepository mensajes;
    @Autowired private ConfiguracionClinicaRepository configuraciones;
    @Autowired private MascotaRepository mascotas;
    @Autowired private CitaRepository citas;
    @MockitoBean private NotificacionService notificacionService;

    private Usuario admin, aux, cliente;

    @BeforeEach
    void datos() {
        mensajes.deleteAll();
        configuraciones.deleteAll();
        citas.deleteAll();
        mascotas.deleteAll();
        usuarios.deleteAll();
        admin = guardar("Admin", "63000001", 1);
        aux = guardar("Rosa Aux", "63000002", 4);
        cliente = guardar("Ana Torres", "63000003", 2);
    }

    @AfterEach
    void limpiar() {
        configuraciones.deleteAll();
    }

    private Usuario guardar(String nombre, String dni, int rol) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setDni(dni);
        u.setCorreo(dni + "@contacto.test");
        u.setContrasena("Clave2026");
        u.setRol_id(rol);
        u.setActivo(true);
        return usuarios.save(u);
    }

    private static String mensaje(String correo) {
        return "{\"nombre\":\"Ana Torres\",\"correo\":\"" + correo + "\",\"telefono\":\"987654321\",\"asunto\":\"Otro\","
                + "\"mensaje\":\"Quisiera saber si atienden el domingo.\"}";
    }

    private static String configuracion(int horas) {
        return "{\"nombre\":\"Vamos Pet\",\"direccion\":\"Calle San Martín 849, Miraflores\",\"telefono\":\"+51 987 654 321\","
                + "\"whatsapp\":\"51987654321\",\"correo\":\"hola@vamospet.pe\",\"horario_atencion\":\"Lun a Dom 8 a 21\","
                + "\"emergencias_24h\":true,\"hospedaje_apertura\":8,\"hospedaje_cierre\":20,\"horas_cancelacion\":" + horas + "}";
    }

    @Test
    @DisplayName("El mensaje público llega a la bandeja del Auxiliar y se responde por correo")
    void bandejaYRespuesta() throws Exception {
        mvc.perform(post("/api/contacto").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"A\",\"correo\":\"malo\",\"asunto\":\"Otro\",\"mensaje\":\"corto\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.correo").exists())
                .andExpect(jsonPath("$.campos.mensaje").exists());
        mvc.perform(post("/api/contacto").contentType(MediaType.APPLICATION_JSON).content(mensaje("ana@correo.pe")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/contacto").with(como(cliente.getId()))).andExpect(status().isForbidden());
        mvc.perform(get("/api/contacto").param("estado", "Nuevo").with(como(admin.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        Integer id = mensajes.findAll().get(0).getId();
        mvc.perform(patch("/api/contacto/" + id + "/responder").with(como(admin.getId())).contentType(MediaType.APPLICATION_JSON)
                .content("{\"respuesta\":\"Sí, de 9 a 13.\"}")).andExpect(status().isForbidden());
        when(notificacionService.enviarCorreo(eq("ana@correo.pe"), eq("Re: Otro"), anyString(), any(), any())).thenReturn(true);
        mvc.perform(patch("/api/contacto/" + id + "/responder").with(como(aux.getId())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"respuesta\":\"Sí, de 9 a 13.\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("Respondido"));
        verify(notificacionService).enviarCorreo(eq("ana@correo.pe"), eq("Re: Otro"),
                org.mockito.ArgumentMatchers.argThat(t -> t.contains("> Quisiera saber si atienden el domingo.")), any(), any());
        mvc.perform(patch("/api/contacto/" + id + "/archivar").with(como(aux.getId()))).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("Archivado"));
    }

    @Test
    @DisplayName("Desde la misma conexión, el sexto mensaje en una hora se rechaza")
    void antiSpamPorConexion() throws Exception {
        for (int i = 1; i <= 5; i++) {
            mvc.perform(post("/api/contacto").contentType(MediaType.APPLICATION_JSON).content(mensaje("persona" + i + "@correo.pe")))
                    .andExpect(status().isOk());
        }
        mvc.perform(post("/api/contacto").contentType(MediaType.APPLICATION_JSON).content(mensaje("persona6@correo.pe")))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("Solo el Admin cambia la configuración, el sitio la lee y el umbral cambia la cancelación")
    void configuracionYUmbral() throws Exception {
        mvc.perform(get("/api/configuracion")).andExpect(status().isOk()).andExpect(jsonPath("$.horas_cancelacion").value(24));
        mvc.perform(put("/api/configuracion").with(como(aux.getId())).contentType(MediaType.APPLICATION_JSON).content(configuracion(48)))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/configuracion").with(como(admin.getId())).contentType(MediaType.APPLICATION_JSON)
                        .content(configuracion(48).replace("\"whatsapp\":\"51987654321\"", "\"whatsapp\":\"+51 98\"")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.whatsapp").exists());

        Mascota m = new Mascota();
        m.setUsuario_id(cliente.getId());
        m.setNombre("Luna");
        m.setEspecie("Gato");
        m = mascotas.save(m);
        Cita c = new Cita();
        c.setMascota_id(m.getId());
        c.setFecha_hora(LocalDateTime.now().plusHours(30));
        c.setEstado("Confirmada");
        c.setCosto(new BigDecimal("60.00"));
        c = citas.save(c);

        mvc.perform(put("/api/configuracion").with(como(admin.getId())).contentType(MediaType.APPLICATION_JSON).content(configuracion(48)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.telefono").value("+51 987 654 321"));
        mvc.perform(get("/api/configuracion")).andExpect(jsonPath("$.horas_cancelacion").value(48))
                .andExpect(jsonPath("$.whatsapp").value("51987654321"));
        mvc.perform(patch("/api/citas/" + c.getId() + "/cancelar").with(como(cliente.getId()))).andExpect(status().isConflict());
        mvc.perform(put("/api/configuracion").with(como(admin.getId())).contentType(MediaType.APPLICATION_JSON).content(configuracion(24)))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/citas/" + c.getId() + "/cancelar").with(como(cliente.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("Cancelada"));
    }
}
