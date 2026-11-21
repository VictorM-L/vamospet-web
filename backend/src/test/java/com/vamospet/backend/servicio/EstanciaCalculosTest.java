package com.vamospet.backend.servicio;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EstanciaCalculosTest {

    @Test
    void unaNocheMinimo() {
        LocalDateTime i = LocalDateTime.of(2026, 11, 1, 9, 0);
        assertEquals(1, EstanciaService.unidades(i, i.plusHours(3)));
        assertEquals(1, EstanciaService.unidades(i, i.plusHours(24)));
    }

    @Test
    void cobraNocheCompletaPorFraccion() {
        LocalDateTime i = LocalDateTime.of(2026, 11, 1, 9, 0);
        assertEquals(2, EstanciaService.unidades(i, i.plusHours(25)));
        assertEquals(3, EstanciaService.unidades(i, i.plusDays(3)));
    }

    @Test
    void solapamientoDeRangos() {
        LocalDateTime a = LocalDateTime.of(2026, 11, 1, 9, 0);
        assertTrue(EstanciaService.solapa(a, a.plusDays(2), a.plusDays(1), a.plusDays(3)));
        assertFalse(EstanciaService.solapa(a, a.plusDays(1), a.plusDays(1), a.plusDays(2)));
        assertFalse(EstanciaService.solapa(a.plusDays(5), a.plusDays(6), a, a.plusDays(1)));
    }

    @Test
    void horarioDeAtencion() {
        assertTrue(EstanciaService.dentroDeHorario(LocalDateTime.of(2026, 11, 1, 8, 0), 8, 20));
        assertTrue(EstanciaService.dentroDeHorario(LocalDateTime.of(2026, 11, 1, 20, 0), 8, 20));
        assertFalse(EstanciaService.dentroDeHorario(LocalDateTime.of(2026, 11, 1, 7, 59), 8, 20));
        assertFalse(EstanciaService.dentroDeHorario(LocalDateTime.of(2026, 11, 1, 20, 1), 8, 20));
        assertTrue(EstanciaService.dentroDeHorario(LocalDateTime.of(2026, 11, 1, 7, 0), 7, 22));
        assertFalse(EstanciaService.dentroDeHorario(LocalDateTime.of(2026, 11, 1, 21, 0), 9, 18));
    }

    @Test
    void precioDeViajeCreceConMascotas() {
        BigDecimal base = new BigDecimal("150");
        BigDecimal extra = new BigDecimal("80");
        assertEquals(new BigDecimal("150"), ViajeService.calcularTotal(base, extra, 1));
        assertEquals(new BigDecimal("310"), ViajeService.calcularTotal(base, extra, 3));
    }
}
