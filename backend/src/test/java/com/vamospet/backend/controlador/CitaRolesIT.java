package com.vamospet.backend.controlador;

import com.vamospet.backend.modelo.Cita;
import com.vamospet.backend.modelo.CitaCostoAdicional;
import com.vamospet.backend.modelo.HorarioAtencion;
import com.vamospet.backend.modelo.Mascota;
import com.vamospet.backend.modelo.Usuario;
import com.vamospet.backend.repositorio.CitaCostoAdicionalRepository;
import com.vamospet.backend.repositorio.CitaRepository;
import com.vamospet.backend.repositorio.HorarioAtencionRepository;
import com.vamospet.backend.repositorio.MascotaRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static com.vamospet.backend.ConToken.como;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CitaRolesIT {


    @Autowired private MockMvc mvc;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private MascotaRepository mascotas;
    @Autowired private CitaRepository citas;
    @Autowired private CitaCostoAdicionalRepository costos;
    @Autowired private HorarioAtencionRepository horarios;
    @MockitoBean private NotificacionService notificacionService;

    private Usuario admin, vet, otroVet, aux, cliente;
    private Mascota mascota;

    @BeforeEach
    void datos() {
        costos.deleteAll();
        citas.deleteAll();
        horarios.deleteAll();
        mascotas.deleteAll();
        usuarios.deleteAll();
        admin = guardar("Admin", "60000001", 1);
        vet = guardar("Lucia Vet", "60000002", 3);
        otroVet = guardar("Mateo Vet", "60000003", 3);
        aux = guardar("Rosa Aux", "60000004", 4);
        cliente = guardar("Ana Cliente", "60000005", 2);
        Mascota m = new Mascota();
        m.setUsuario_id(cliente.getId());
        m.setNombre("Firulais");
        m.setEspecie("Perro");
        mascota = mascotas.save(m);
    }

    private Usuario guardar(String nombre, String dni, int rol) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setDni(dni);
        u.setCorreo(dni + "@citas.test");
        u.setContrasena("Clave2026");
        u.setRol_id(rol);
        u.setActivo(true);
        return usuarios.save(u);
    }

    private Cita cita(String estado, LocalDateTime cuando, Usuario veterinario) {
        Cita c = new Cita();
        c.setMascota_id(mascota.getId());
        c.setVeterinario_id(veterinario.getId());
        c.setFecha_hora(cuando);
        c.setEstado(estado);
        c.setCosto(new BigDecimal("50.00"));
        return citas.save(c);
    }

    private static LocalDateTime enDias(int dias) {
        return LocalDate.now().plusDays(dias).atTime(10, 0);
    }

    @Test
    @DisplayName("El veterinario confirma su cita; el Auxiliar no puede confirmar y el veterinario no puede cancelar")
    void confirmarYCancelarPorRol() throws Exception {
        Cita c = cita("Pendiente", enDias(3), vet);
        mvc.perform(patch("/api/citas/" + c.getId() + "/confirmar").with(como(aux.getId()))).andExpect(status().isForbidden());
        mvc.perform(patch("/api/citas/" + c.getId() + "/confirmar").with(como(otroVet.getId()))).andExpect(status().isForbidden());
        mvc.perform(patch("/api/citas/" + c.getId() + "/confirmar").with(como(vet.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("Confirmada"))
                .andExpect(jsonPath("$.costo").doesNotExist());
        mvc.perform(patch("/api/citas/" + c.getId() + "/estado").with(como(vet.getId()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"Cancelada\"}")).andExpect(status().isForbidden());
        mvc.perform(patch("/api/citas/" + c.getId() + "/estado").with(como(admin.getId()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"Cancelada\"}")).andExpect(status().isForbidden());
        mvc.perform(patch("/api/citas/" + c.getId() + "/estado").with(como(aux.getId()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"Cancelada\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("Cancelada"));
    }

    @Test
    @DisplayName("\"No puedo atender\" crea el caso; el Auxiliar ve quién está libre y reasigna")
    void colaPorReasignar() throws Exception {
        LocalDateTime cuando = enDias(3);
        HorarioAtencion h = new HorarioAtencion();
        h.setVeterinario_id(otroVet.getId());
        h.setDia_semana(cuando.getDayOfWeek().getValue());
        h.setHora_inicio(LocalTime.of(8, 0));
        h.setHora_fin(LocalTime.of(18, 0));
        horarios.save(h);
        Cita c = cita("Confirmada", cuando, vet);
        mvc.perform(patch("/api/citas/" + c.getId() + "/no-puedo-atender").with(como(vet.getId()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Capacitación externa\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("Por reasignar"));
        mvc.perform(get("/api/citas/" + c.getId() + "/veterinarios-disponibles").with(como(aux.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(otroVet.getId()))
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(patch("/api/citas/" + c.getId() + "/reasignar").with(como(aux.getId()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"veterinario_id\":" + otroVet.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("Pendiente"))
                .andExpect(jsonPath("$.veterinario_id").value(otroVet.getId()));
    }

    @Test
    @DisplayName("El cliente cancela con 24 h o más; con menos, solo puede solicitar la cancelación")
    void cancelacionDelCliente() throws Exception {
        Cita lejana = cita("Confirmada", enDias(5), vet);
        Cita cercana = cita("Confirmada", LocalDateTime.now().plusHours(5), otroVet);
        mvc.perform(patch("/api/citas/" + lejana.getId() + "/cancelar").with(como(cliente.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("Cancelada"));
        mvc.perform(patch("/api/citas/" + cercana.getId() + "/cancelar").with(como(cliente.getId())))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/citas/" + cercana.getId() + "/solicitud-cancelacion").with(como(cliente.getId()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Se me cruzó el trabajo\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cancelacion_solicitada").exists());
        mvc.perform(patch("/api/citas/" + cercana.getId() + "/solicitud-cancelacion/resolver").with(como(aux.getId()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"aprobar\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("Cancelada"));
    }

    @Test
    @DisplayName("El veterinario registra un costo adicional sin ver totales; el Auxiliar sí ve el total")
    void costosSinTotalesParaVeterinario() throws Exception {
        Cita c = cita("Confirmada", enDias(2), vet);
        mvc.perform(post("/api/citas/" + c.getId() + "/costos-adicionales").with(como(vet.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario_id\":" + otroVet.getId() + ",\"concepto\":\"Anestesia local\",\"monto\":30}"))
                .andExpect(status().isOk());
        List<CitaCostoAdicional> guardados = costos.findAll();
        assertEquals(1, guardados.size());
        assertEquals(vet.getId(), guardados.get(0).getAgregado_por());
        mvc.perform(get("/api/citas/" + c.getId() + "/costos-adicionales").with(como(vet.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.extras[0].concepto").value("Anestesia local"))
                .andExpect(jsonPath("$.total").doesNotExist())
                .andExpect(jsonPath("$.base").doesNotExist());
        mvc.perform(get("/api/citas/" + c.getId() + "/costos-adicionales").with(como(aux.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(80.0));
        mvc.perform(post("/api/citas/" + c.getId() + "/costos-adicionales").with(como(aux.getId()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"concepto\":\"Otro\",\"monto\":5}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("La lista de citas que recibe el veterinario no trae montos")
    void listaSinMontosParaVeterinario() throws Exception {
        cita("Pendiente", enDias(2), vet);
        mvc.perform(get("/api/citas").with(como(vet.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].costo").doesNotExist());
        mvc.perform(get("/api/citas").with(como(aux.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].costo").value(50.0));
    }

    @Test
    @DisplayName("Un cliente no agenda para la mascota de otro, y el Admin no agenda")
    void agendarSoloPropio() throws Exception {
        Usuario intruso = guardar("Intruso", "60000009", 2);
        String cuerpo = "{\"mascota_id\":" + mascota.getId() + ",\"fecha_hora\":\"" + enDias(4) + "\"}";
        mvc.perform(post("/api/citas").with(como(intruso.getId())).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/citas").with(como(admin.getId())).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isForbidden());
    }
}
