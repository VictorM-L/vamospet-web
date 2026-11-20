package com.vamospet.backend.servicio;

import com.vamospet.backend.dto.ConfiguracionRequest;
import com.vamospet.backend.modelo.ConfiguracionClinica;
import com.vamospet.backend.modelo.Usuario;
import com.vamospet.backend.repositorio.ConfiguracionClinicaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfiguracionServiceTest {

    @Mock private ConfiguracionClinicaRepository repositorio;
    @Mock private AutorizacionService autorizacion;
    @InjectMocks private ConfiguracionService servicio;

    private ConfiguracionRequest pedido(int apertura, int cierre, int horas) {
        return new ConfiguracionRequest("Vamos Pet", "Calle San Martín 849, Miraflores", "+51 987 654 321", "51987654321",
                "hola@vamospet.pe", "Lun a Sáb 9 a 18", true, apertura, cierre, horas);
    }

    @Test
    @DisplayName("Sin fila guardada se usan 24 h para cancelar y hospedaje de 8 a 20")
    void valoresPorDefecto() {
        when(repositorio.findById(1)).thenReturn(Optional.empty());
        assertEquals(24, servicio.horasCancelacion());
        assertEquals(8, servicio.aperturaHospedaje());
        assertEquals(20, servicio.cierreHospedaje());
    }

    @Test
    @DisplayName("Solo el Administrador guarda la configuración")
    void soloAdmin() {
        when(autorizacion.conRol(19, 1)).thenReturn(null);
        assertEquals(403, assertThrows(NegocioException.class, () -> servicio.guardar(19, pedido(8, 20, 24))).getEstado());
        verify(repositorio, never()).save(any());
    }

    @Test
    @DisplayName("El cierre del hospedaje debe ser después de la apertura")
    void horarioCoherente() {
        when(autorizacion.conRol(1, 1)).thenReturn(new Usuario());
        assertEquals(400, assertThrows(NegocioException.class, () -> servicio.guardar(1, pedido(18, 9, 24))).getEstado());
        verify(repositorio, never()).save(any());
    }

    @Test
    @DisplayName("Guardar cambia lo que leen los demás servicios")
    void guardar() {
        when(autorizacion.conRol(1, 1)).thenReturn(new Usuario());
        ConfiguracionClinica[] guardada = {null};
        when(repositorio.findById(1)).thenAnswer(i -> Optional.ofNullable(guardada[0]));
        when(repositorio.save(any(ConfiguracionClinica.class))).thenAnswer(i -> guardada[0] = i.getArgument(0));
        Map<String, Object> r = servicio.guardar(1, pedido(9, 19, 48));
        assertEquals("+51 987 654 321", r.get("telefono"));
        assertEquals(48, servicio.horasCancelacion());
        assertEquals(9, servicio.aperturaHospedaje());
        assertEquals(19, servicio.cierreHospedaje());
        assertEquals(1, guardada[0].getId());
        assertEquals(1, guardada[0].getActualizado_por());
    }
}
