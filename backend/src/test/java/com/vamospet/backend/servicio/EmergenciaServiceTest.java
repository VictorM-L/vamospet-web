package com.vamospet.backend.servicio;

import com.vamospet.backend.modelo.ContactoEmergencia;
import com.vamospet.backend.modelo.Mascota;
import com.vamospet.backend.modelo.Usuario;
import com.vamospet.backend.repositorio.ContactoEmergenciaRepository;
import com.vamospet.backend.repositorio.HistorialEventoRepository;
import com.vamospet.backend.repositorio.MascotaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmergenciaServiceTest {

    @Mock private MascotaRepository mascotaRepositorio;
    @Mock private ContactoEmergenciaRepository contactoRepositorio;
    @Mock private HistorialEventoRepository historialRepositorio;
    @Mock private AutorizacionService autorizacion;
    @InjectMocks private EmergenciaService servicio;

    private Usuario usuario(int id, int rol) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setRol_id(rol);
        u.setNombre("Persona " + id);
        return u;
    }

    private EmergenciaService.DatosEmergencia datos(String tel) {
        return new EmergenciaService.DatosEmergencia("Carlos Ruiz", tel, "Sin nombre", "Perro", null, null, "Atropello");
    }

    private ContactoEmergencia contacto(String codigo) {
        ContactoEmergencia c = new ContactoEmergencia();
        c.setCodigo_emitido(codigo);
        return c;
    }

    @Test
    @DisplayName("El primer ingreso recibe VET-PET-1001, sin dueño y sin pedir DNI ni correo")
    void primerCodigo() {
        when(autorizacion.conRol(12, 1, 3, 4)).thenReturn(usuario(12, 4));
        when(contactoRepositorio.findAll()).thenReturn(List.of());
        when(mascotaRepositorio.saveAndFlush(any(Mascota.class))).thenAnswer(i -> { Mascota m = i.getArgument(0); m.setId(50); return m; });
        Mascota m = servicio.registrar(12, datos("987 654 321"));
        assertEquals("VET-PET-1001", m.getCodigo_unico_temporal());
        assertNull(m.getUsuario_id());
        verify(contactoRepositorio).saveAndFlush(any(ContactoEmergencia.class));
        verify(historialRepositorio).save(any());
    }

    @Test
    @DisplayName("El número siguiente se calcula sobre los códigos ya emitidos, aunque estén vinculados (no se reutiliza)")
    void noReutilizaCodigos() {
        when(autorizacion.conRol(12, 1, 3, 4)).thenReturn(usuario(12, 4));
        when(contactoRepositorio.findAll()).thenReturn(List.of(contacto("VET-PET-1044"), contacto("VET-PET-1045")));
        when(mascotaRepositorio.saveAndFlush(any(Mascota.class))).thenAnswer(i -> { Mascota m = i.getArgument(0); m.setId(51); return m; });
        assertEquals("VET-PET-1046", servicio.registrar(12, datos("987654321")).getCodigo_unico_temporal());
    }

    @Test
    @DisplayName("Teléfono inválido o datos faltantes se rechazan")
    void validaciones() {
        when(autorizacion.conRol(12, 1, 3, 4)).thenReturn(usuario(12, 4));
        assertEquals(400, assertThrows(NegocioException.class, () -> servicio.registrar(12, datos("abc"))).getEstado());
        assertEquals(400, assertThrows(NegocioException.class, () -> servicio.registrar(12,
                new EmergenciaService.DatosEmergencia("", "987654321", "X", "Perro", null, null, null))).getEstado());
        verify(mascotaRepositorio, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Un cliente no puede registrar ingresos por emergencia")
    void soloPersonal() {
        when(autorizacion.conRol(2, 1, 3, 4)).thenReturn(null);
        assertEquals(403, assertThrows(NegocioException.class, () -> servicio.registrar(2, datos("987654321"))).getEstado());
    }

    @Test
    @DisplayName("Vincular asigna el dueño y borra el código; el historial no se toca")
    void vincular() {
        Mascota m = new Mascota();
        m.setId(50);
        m.setNombre("Rex");
        m.setCodigo_unico_temporal("VET-PET-1045");
        when(autorizacion.conRol(2, 2)).thenReturn(usuario(2, 2));
        when(mascotaRepositorio.findAll()).thenReturn(List.of(m));
        when(mascotaRepositorio.save(any(Mascota.class))).thenAnswer(i -> i.getArgument(0));
        Mascota r = servicio.vincular(2, " vet-pet-1045 ");
        assertEquals(2, r.getUsuario_id());
        assertNull(r.getCodigo_unico_temporal());
        verify(historialRepositorio).save(any());
        verify(historialRepositorio, never()).deleteAll();
    }

    @Test
    @DisplayName("Un código inexistente o ya usado no vincula")
    void codigoInvalido() {
        Mascota ya = new Mascota();
        ya.setId(60);
        ya.setUsuario_id(3);
        ya.setCodigo_unico_temporal("VET-PET-1000");
        when(autorizacion.conRol(2, 2)).thenReturn(usuario(2, 2));
        when(mascotaRepositorio.findAll()).thenReturn(List.of(ya));
        assertEquals(404, assertThrows(NegocioException.class, () -> servicio.vincular(2, "VET-PET-9999")).getEstado());
        assertEquals(404, assertThrows(NegocioException.class, () -> servicio.vincular(2, "VET-PET-1000")).getEstado());
        verify(mascotaRepositorio, never()).save(any());
    }
}
