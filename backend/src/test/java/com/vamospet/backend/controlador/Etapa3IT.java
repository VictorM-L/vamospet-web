package com.vamospet.backend.controlador;

import com.vamospet.backend.modelo.Cita;
import com.vamospet.backend.modelo.CirugiaProgramada;
import com.vamospet.backend.modelo.HorarioAtencion;
import com.vamospet.backend.modelo.Mascota;
import com.vamospet.backend.modelo.OrdenExamen;
import com.vamospet.backend.modelo.Servicio;
import com.vamospet.backend.modelo.Usuario;
import com.vamospet.backend.repositorio.CirugiaProgramadaRepository;
import com.vamospet.backend.repositorio.CitaRepository;
import com.vamospet.backend.repositorio.HorarioAtencionRepository;
import com.vamospet.backend.repositorio.MascotaRepository;
import com.vamospet.backend.repositorio.OrdenExamenRepository;
import com.vamospet.backend.repositorio.RecetaRepository;
import com.vamospet.backend.repositorio.ServicioRepository;
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
class Etapa3IT {


    @Autowired private MockMvc mvc;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private MascotaRepository mascotas;
    @Autowired private CitaRepository citas;
    @Autowired private HorarioAtencionRepository horarios;
    @Autowired private ServicioRepository servicios;
    @Autowired private OrdenExamenRepository ordenes;
    @Autowired private CirugiaProgramadaRepository cirugias;
    @Autowired private RecetaRepository recetas;
    @MockitoBean private NotificacionService notificacionService;

    private Usuario admin, vet, groomer, aux, cliente;
    private Mascota mascota;

    @BeforeEach
    void datos() {
        cirugias.deleteAll();
        ordenes.deleteAll();
        recetas.deleteAll();
        citas.deleteAll();
        horarios.deleteAll();
        mascotas.deleteAll();
        usuarios.deleteAll();
        servicios.deleteAll();
        admin = guardar("Admin", "61000001", 1);
        vet = guardar("Lucia Vet", "61000002", 3);
        groomer = guardar("Mateo Groomer", "61000003", 3);
        aux = guardar("Rosa Aux", "61000004", 4);
        cliente = guardar("Ana Cliente", "61000005", 2);
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
        u.setCorreo(dni + "@etapa3.test");
        u.setContrasena("Clave2026");
        u.setRol_id(rol);
        u.setActivo(true);
        return usuarios.save(u);
    }

    private Servicio servicio(String nombre, String tipo, String precio, int duracion) {
        Servicio s = new Servicio();
        s.setNombre(nombre);
        s.setTipo(tipo);
        s.setPrecio_base(new BigDecimal(precio));
        s.setDuracion_min(duracion);
        s.setActivo(true);
        return servicios.save(s);
    }

    private void horario(Usuario u, LocalDate dia) {
        HorarioAtencion h = new HorarioAtencion();
        h.setVeterinario_id(u.getId());
        h.setDia_semana(dia.getDayOfWeek().getValue());
        h.setHora_inicio(LocalTime.of(9, 0));
        h.setHora_fin(LocalTime.of(13, 0));
        horarios.save(h);
    }

    private static String json(String s) {
        return s.replace('\'', '"');
    }

    @Test
    @DisplayName("Servicios: la duración va de 15 a 480 en múltiplos de 15 y no se activa un servicio con precio 0")
    void validacionDeServicios() throws Exception {
        mvc.perform(post("/api/servicios").with(como(admin.getId())).contentType(MediaType.APPLICATION_JSON)
                .content(json("{'nombre':'Ecografía','precio_base':80,'duracion_min':20,'activo':true}"))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/servicios").with(como(admin.getId())).contentType(MediaType.APPLICATION_JSON)
                .content(json("{'nombre':'Ecografía','precio_base':0,'duracion_min':30,'activo':true}"))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/servicios").with(como(admin.getId())).contentType(MediaType.APPLICATION_JSON)
                .content(json("{'nombre':'Guardería por día','precio_base':0,'cupo_diario':0,'activo':false}"))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/servicios").with(como(admin.getId())).contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'nombre':'Ecografía','precio_base':80,'duracion_min':45,'activo':true}")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.duracion_min").value(45)).andExpect(jsonPath("$.tipo").value("Cita"));
        mvc.perform(post("/api/servicios").with(como(aux.getId())).contentType(MediaType.APPLICATION_JSON)
                .content(json("{'nombre':'X','precio_base':10,'activo':true}"))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Grooming: solo con un veterinario habilitado; el precio sale del paquete y ocupa toda su duración")
    void groomingConVeterinarioHabilitado() throws Exception {
        LocalDate dia = LocalDate.now().plusDays(7);
        Servicio completo = servicio("Grooming - Completo", "Grooming", "70.00", 120);
        horario(vet, dia);
        horario(groomer, dia);
        mvc.perform(patch("/api/usuarios/" + groomer.getId() + "/grooming").with(como(aux.getId()))
                .contentType(MediaType.APPLICATION_JSON).content(json("{'hace_grooming':true}"))).andExpect(status().isForbidden());
        mvc.perform(patch("/api/usuarios/" + aux.getId() + "/grooming").with(como(admin.getId()))
                .contentType(MediaType.APPLICATION_JSON).content(json("{'hace_grooming':true}"))).andExpect(status().isBadRequest());
        mvc.perform(patch("/api/usuarios/" + groomer.getId() + "/grooming").with(como(admin.getId()))
                        .contentType(MediaType.APPLICATION_JSON).content(json("{'hace_grooming':true}")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.hace_grooming").value(true));
        mvc.perform(get("/api/agenda/grooming/veterinarios"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(groomer.getId()));

        String cuando = dia.atTime(9, 0).toString();
        mvc.perform(post("/api/citas").with(como(cliente.getId())).contentType(MediaType.APPLICATION_JSON)
                .content(json("{'mascota_id':" + mascota.getId() + ",'veterinario_id':" + vet.getId() + ",'servicio_id':" + completo.getId()
                        + ",'fecha_hora':'" + cuando + "'}"))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/citas").with(como(cliente.getId())).contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'mascota_id':" + mascota.getId() + ",'veterinario_id':" + groomer.getId() + ",'servicio_id':" + completo.getId()
                                + ",'fecha_hora':'" + cuando + "','costo':1}")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.costo").value(70.0));
        mvc.perform(post("/api/citas").with(como(cliente.getId())).contentType(MediaType.APPLICATION_JSON)
                .content(json("{'mascota_id':" + mascota.getId() + ",'veterinario_id':" + groomer.getId() + ",'servicio_id':" + completo.getId()
                        + ",'fecha_hora':'" + dia.atTime(10, 0) + "'}"))).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Alergias y peso: solo el veterinario los registra y el cliente no los sobrescribe al editar su mascota")
    void datosClinicos() throws Exception {
        mvc.perform(patch("/api/mascotas/" + mascota.getId() + "/datos-clinicos").with(como(cliente.getId()))
                .contentType(MediaType.APPLICATION_JSON).content(json("{'alergias':'Penicilina','peso_kg':12.5}"))).andExpect(status().isForbidden());
        mvc.perform(patch("/api/mascotas/" + mascota.getId() + "/datos-clinicos").with(como(vet.getId()))
                .contentType(MediaType.APPLICATION_JSON).content(json("{'alergias':'Penicilina','peso_kg':-3}"))).andExpect(status().isBadRequest());
        mvc.perform(patch("/api/mascotas/" + mascota.getId() + "/datos-clinicos").with(como(vet.getId()))
                        .contentType(MediaType.APPLICATION_JSON).content(json("{'alergias':'Penicilina','peso_kg':12.5}")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.alergias").value("Penicilina"));
        mvc.perform(post("/api/mascotas").with(como(cliente.getId())).contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'id':" + mascota.getId() + ",'nombre':'Firu','especie':'Perro','alergias':null,'peso_kg':99}")))
                .andExpect(status().isOk());
        Mascota m = mascotas.findById(mascota.getId()).orElseThrow();
        assertEquals("Firu", m.getNombre());
        assertEquals("Penicilina", m.getAlergias());
        assertEquals(0, new BigDecimal("12.50").compareTo(m.getPeso_kg()));
    }

    @Test
    @DisplayName("Cirugía de punta a punta: iniciar, notas, finalizar con receta y alta; el auxiliar solo ve estado y cobro")
    void flujoDeCirugia() throws Exception {
        Servicio menor = servicio("Cirugía menor", "Cita", "300.00", 90);
        OrdenExamen o = new OrdenExamen();
        o.setMascota_id(mascota.getId());
        o.setVeterinario_id(vet.getId());
        o.setTipo_examen("Hemograma");
        o.setEstado("Apto_Cirugia");
        o.setFecha_evaluacion(LocalDateTime.now().minusDays(1));
        o = ordenes.save(o);
        Cita c = new Cita();
        c.setMascota_id(mascota.getId());
        c.setVeterinario_id(vet.getId());
        c.setServicio_id(menor.getId());
        c.setFecha_hora(LocalDateTime.now().plusMinutes(20).withSecond(0).withNano(0));
        c.setEstado("Confirmada");
        c.setCosto(new BigDecimal("300.00"));
        c = citas.save(c);
        CirugiaProgramada q = new CirugiaProgramada();
        q.setOrden_id(o.getId());
        q.setCita_id(c.getId());
        q.setMascota_id(mascota.getId());
        q.setVeterinario_id(vet.getId());
        q.setServicio_id(menor.getId());
        q.setTipo_cirugia("Esterilización");
        q.setFecha_hora(c.getFecha_hora());
        q.setEstado("Programada");
        q = cirugias.save(q);
        String base = "/api/cirugias/" + q.getId();

        mvc.perform(patch(base + "/siguiente").with(como(groomer.getId())).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch(base + "/siguiente").with(como(aux.getId())).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch(base + "/notas").with(como(vet.getId())).contentType(MediaType.APPLICATION_JSON)
                .content(json("{'notas':'antes de tiempo'}"))).andExpect(status().isConflict());
        mvc.perform(patch(base + "/siguiente").with(como(vet.getId())).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("En_curso"));
        assertEquals("En curso", citas.findById(c.getId()).orElseThrow().getEstado());
        mvc.perform(patch(base + "/notas").with(como(vet.getId())).contentType(MediaType.APPLICATION_JSON)
                .content(json("{'notas':'Sutura intradérmica'}"))).andExpect(status().isOk());
        mvc.perform(patch(base + "/siguiente").with(como(vet.getId())).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch(base + "/siguiente").with(como(vet.getId())).contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'resultado':'Sin complicaciones','medicamentos':'Meloxicam 0.1 mg/kg por 3 días'}")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("Postoperatorio"));
        Cita atendida = citas.findById(c.getId()).orElseThrow();
        assertEquals("Atendida", atendida.getEstado());
        assertNotNull(atendida.getFin_real());
        assertTrue(recetas.findAll().stream().anyMatch(r -> "Aprobada".equals(r.getEstado()) && r.getContenido().contains("Meloxicam")));

        mvc.perform(get("/api/cirugias/tablero").with(como(aux.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("Postoperatorio"))
                .andExpect(jsonPath("$[0].cobro").value("Por cobrar"))
                .andExpect(jsonPath("$[0].resultado").doesNotExist())
                .andExpect(jsonPath("$[0].alergias").doesNotExist());
        mvc.perform(get("/api/cirugias/tablero").with(como(cliente.getId()))).andExpect(status().isForbidden());
        mvc.perform(get("/api/cirugias/tablero").with(como(vet.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].notas_intra").value("Sutura intradérmica"));

        mvc.perform(patch(base + "/siguiente").with(como(vet.getId())).contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'decision':'Alta directa','observaciones':'Reposo y collar isabelino'}")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("Realizada"))
                .andExpect(jsonPath("$.postoperatorio").value("Alta directa"));
        mvc.perform(get(base + "/linea-tiempo").with(como(vet.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("Cirugía iniciada"))
                .andExpect(jsonPath("$[1].tipo").value("Cirugía realizada"))
                .andExpect(jsonPath("$[2].tipo").value("Alta directa"));
        mvc.perform(patch("/api/veterinario/cirugias/" + q.getId() + "/estado").with(como(vet.getId()))
                .contentType(MediaType.APPLICATION_JSON).content(json("{'estado':'Realizada'}"))).andExpect(status().isBadRequest());
    }
}
