package com.vamospet.backend.servicio;

import com.vamospet.backend.modelo.Banner;
import com.vamospet.backend.modelo.Usuario;
import com.vamospet.backend.repositorio.BannerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BannerServiceTest {

    private static final int AUX = 50;
    private static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x10};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0x00};
    private static final byte[] WEBP = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P', 0};

    @Mock private BannerRepository repositorio;
    @Mock private AutorizacionService autorizacion;
    @InjectMocks private BannerService servicio;

    private final List<String> creados = new ArrayList<>();

    @AfterEach
    void limpiar() {
        creados.forEach(BannerService::borrarArchivo);
    }

    private void comoAuxiliar() {
        Usuario u = new Usuario();
        u.setId(AUX);
        u.setRol_id(4);
        when(autorizacion.conRol(AUX, 4)).thenReturn(u);
    }

    private Banner banner(int id, int orden, boolean activo) {
        Banner b = new Banner();
        b.setId(id);
        b.setSeccion("Farmacia");
        b.setOrden(orden);
        b.setActivo(activo);
        b.setImagen_pc("/uploads/banner-x" + id + ".jpg");
        return b;
    }

    private List<Banner> activos(int n) {
        List<Banner> l = new ArrayList<>();
        for (int i = 1; i <= n; i++) l.add(banner(i, i, true));
        return l;
    }

    @Test
    @DisplayName("Acepta JPG, PNG y WebP reconociendo el contenido real")
    void formatosValidos() {
        assertEquals("jpg", BannerService.validarImagen(new BannerService.Archivo("a.jpeg", JPG), "imagen"));
        assertEquals("png", BannerService.validarImagen(new BannerService.Archivo("a.PNG", PNG), "imagen"));
        assertEquals("webp", BannerService.validarImagen(new BannerService.Archivo("a.webp", WEBP), "imagen"));
    }

    @Test
    @DisplayName("Rechaza formatos no permitidos, extensión falsa y archivos de más de 2 MB")
    void formatosInvalidos() {
        byte[] gif = "GIF89a....".getBytes();
        assertEquals(400, assertThrows(NegocioException.class, () -> BannerService.validarImagen(new BannerService.Archivo("a.gif", gif), "imagen")).getEstado());
        assertEquals(400, assertThrows(NegocioException.class, () -> BannerService.validarImagen(new BannerService.Archivo("a.png", JPG), "imagen")).getEstado());
        byte[] grande = new byte[BannerService.TAMANO_MAXIMO_BYTES + 1];
        grande[0] = (byte) 0xFF; grande[1] = (byte) 0xD8; grande[2] = (byte) 0xFF;
        NegocioException ex = assertThrows(NegocioException.class, () -> BannerService.validarImagen(new BannerService.Archivo("a.jpg", grande), "imagen"));
        assertTrue(ex.getMessage().contains("2 MB"));
    }

    @Test
    @DisplayName("Solo el Auxiliar sube, ordena, activa o elimina banners")
    void soloAuxiliar() {
        when(autorizacion.conRol(9, 4)).thenReturn(null);
        BannerService.Archivo img = new BannerService.Archivo("a.jpg", JPG);
        assertEquals(403, assertThrows(NegocioException.class, () -> servicio.crear(9, "Farmacia", img, null, true)).getEstado());
        assertEquals(403, assertThrows(NegocioException.class, () -> servicio.cambiarActivo(9, 1, true)).getEstado());
        assertEquals(403, assertThrows(NegocioException.class, () -> servicio.mover(9, 1, 2)).getEstado());
        assertEquals(403, assertThrows(NegocioException.class, () -> servicio.eliminar(9, 1)).getEstado());
        verifyNoInteractions(repositorio);
    }

    @Test
    @DisplayName("No se sube un séptimo banner activo en la misma sección")
    void limiteAlCrear() {
        comoAuxiliar();
        when(repositorio.deSeccion("Farmacia")).thenReturn(activos(6));
        NegocioException ex = assertThrows(NegocioException.class,
                () -> servicio.crear(AUX, "Farmacia", new BannerService.Archivo("a.jpg", JPG), null, true));
        assertEquals(409, ex.getEstado());
        assertTrue(ex.getMessage().contains("6 banners activos"));
        verify(repositorio, never()).save(any());
    }

    @Test
    @DisplayName("Con 6 activos se puede subir uno inactivo, que queda al final")
    void inactivoNoCuenta() {
        comoAuxiliar();
        when(repositorio.deSeccion("Farmacia")).thenReturn(activos(6));
        when(repositorio.save(any(Banner.class))).thenAnswer(i -> i.getArgument(0));
        Banner b = servicio.crear(AUX, "Farmacia", new BannerService.Archivo("a.png", PNG), null, false);
        creados.add(b.getImagen_pc());
        assertFalse(b.getActivo());
        assertEquals(7, b.getOrden());
        assertTrue(b.getImagen_pc().startsWith("/uploads/banner-") && b.getImagen_pc().endsWith(".png"));
        assertNull(b.getImagen_movil());
    }

    @Test
    @DisplayName("Activar un séptimo banner se rechaza; desactivar siempre se puede")
    void limiteAlActivar() {
        comoAuxiliar();
        List<Banner> lista = activos(6);
        Banner apagado = banner(7, 7, false);
        lista.add(apagado);
        when(repositorio.findById(7)).thenReturn(Optional.of(apagado));
        when(repositorio.deSeccion("Farmacia")).thenReturn(lista);
        assertEquals(409, assertThrows(NegocioException.class, () -> servicio.cambiarActivo(AUX, 7, true)).getEstado());
        when(repositorio.findById(3)).thenReturn(Optional.of(lista.get(2)));
        when(repositorio.save(any(Banner.class))).thenAnswer(i -> i.getArgument(0));
        assertFalse(servicio.cambiarActivo(AUX, 3, false).getActivo());
    }

    @Test
    @DisplayName("La sección debe ser Farmacia o Accesorios")
    void seccionValida() {
        assertEquals(400, assertThrows(NegocioException.class, () -> servicio.activos("Inicio")).getEstado());
        assertEquals(400, assertThrows(NegocioException.class, () -> servicio.activos(null)).getEstado());
    }

    @Test
    @DisplayName("La lista pública solo trae los activos en su orden")
    void listaPublica() {
        when(repositorio.deSeccion("Farmacia")).thenReturn(List.of(banner(1, 1, true), banner(2, 2, false), banner(3, 3, true)));
        assertEquals(List.of(1, 3), servicio.activos("Farmacia").stream().map(Banner::getId).toList());
    }

    @Test
    @DisplayName("Mover un banner reordena la sección sin huecos")
    void mover() {
        comoAuxiliar();
        List<Banner> lista = activos(4);
        when(repositorio.findById(4)).thenReturn(Optional.of(lista.get(3)));
        when(repositorio.deSeccion("Farmacia")).thenReturn(lista);
        when(repositorio.saveAll(any())).thenAnswer(i -> i.getArgument(0));
        List<Banner> r = servicio.mover(AUX, 4, 1);
        assertEquals(List.of(4, 1, 2, 3), r.stream().map(Banner::getId).toList());
        assertEquals(List.of(1, 2, 3, 4), r.stream().map(Banner::getOrden).toList());
    }

    @Test
    @DisplayName("Reemplazar sin ninguna imagen nueva pide elegir una")
    void reemplazarSinImagen() {
        comoAuxiliar();
        when(repositorio.findById(1)).thenReturn(Optional.of(banner(1, 1, true)));
        assertEquals(400, assertThrows(NegocioException.class, () -> servicio.reemplazar(AUX, 1, null, null, false)).getEstado());
    }
}
