package com.vamospet.backend.controlador;

import com.vamospet.backend.modelo.Banner;
import com.vamospet.backend.modelo.Usuario;
import com.vamospet.backend.repositorio.BannerRepository;
import com.vamospet.backend.repositorio.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static com.vamospet.backend.ConToken.como;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BannerIT {

    private static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x10};

    @Autowired private MockMvc mvc;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private BannerRepository banners;

    private Usuario aux, admin, cliente;

    @BeforeEach
    void datos() {
        banners.deleteAll();
        usuarios.deleteAll();
        aux = guardar("Rosa Aux", "62000001", 4);
        admin = guardar("Admin", "62000002", 1);
        cliente = guardar("Ana", "62000003", 2);
    }

    @AfterEach
    void limpiar() throws IOException {
        for (Banner b : banners.findAll()) {
            for (String url : new String[]{b.getImagen_pc(), b.getImagen_movil()}) {
                if (url != null && url.startsWith("/uploads/banner-")) Files.deleteIfExists(Paths.get("uploads").resolve(url.substring(9)));
            }
        }
        banners.deleteAll();
    }

    private Usuario guardar(String nombre, String dni, int rol) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setDni(dni);
        u.setCorreo(dni + "@banner.test");
        u.setContrasena("Clave2026");
        u.setRol_id(rol);
        u.setActivo(true);
        return usuarios.save(u);
    }

    private Banner banner(String seccion, int orden, boolean activo) {
        Banner b = new Banner();
        b.setSeccion(seccion);
        b.setOrden(orden);
        b.setActivo(activo);
        b.setImagen_pc("/uploads/prueba-" + seccion + orden + ".jpg");
        return banners.save(b);
    }

    @Test
    @DisplayName("GET /api/banners es público y devuelve solo los activos de la sección, en orden")
    void soloActivosEnOrden() throws Exception {
        Banner tercero = banner("Farmacia", 3, true);
        banner("Farmacia", 2, false);
        Banner primero = banner("Farmacia", 1, true);
        banner("Accesorios", 1, true);
        mvc.perform(get("/api/banners").param("seccion", "Farmacia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(primero.getId()))
                .andExpect(jsonPath("$[1].id").value(tercero.getId()));
        mvc.perform(get("/api/banners").param("seccion", "Inicio")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Solo el Auxiliar sube y gestiona; el Admin ve la gestión; el cliente no")
    void permisos() throws Exception {
        MockMultipartFile img = new MockMultipartFile("imagen_pc", "promo.jpg", "image/jpeg", JPG);
        mvc.perform(multipart("/api/banners").file(img).param("seccion", "Farmacia").with(como(cliente.getId()))).andExpect(status().isForbidden());
        mvc.perform(multipart("/api/banners").file(img).param("seccion", "Farmacia").with(como(admin.getId()))).andExpect(status().isForbidden());
        mvc.perform(multipart("/api/banners").file(img).param("seccion", "Farmacia").with(como(aux.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.orden").value(1)).andExpect(jsonPath("$.activo").value(true));
        mvc.perform(get("/api/banners/gestion").param("seccion", "Farmacia").with(como(admin.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/banners/gestion").param("seccion", "Farmacia").with(como(cliente.getId()))).andExpect(status().isForbidden());
        Integer id = banners.findAll().get(0).getId();
        mvc.perform(patch("/api/banners/" + id + "/activo").with(como(admin.getId())).contentType(MediaType.APPLICATION_JSON)
                .content("{\"activo\":false}")).andExpect(status().isForbidden());
        mvc.perform(patch("/api/banners/" + id + "/activo").with(como(aux.getId())).contentType(MediaType.APPLICATION_JSON)
                .content("{\"activo\":false}")).andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(false));
        mvc.perform(delete("/api/banners/" + id).with(como(aux.getId()))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Una imagen GIF o un séptimo banner activo se rechazan con mensaje claro")
    void validaciones() throws Exception {
        MockMultipartFile gif = new MockMultipartFile("imagen_pc", "promo.gif", "image/gif", "GIF89a".getBytes());
        mvc.perform(multipart("/api/banners").file(gif).param("seccion", "Accesorios").with(como(aux.getId())))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("La imagen para PC debe ser JPG, PNG o WebP."));
        for (int i = 1; i <= 6; i++) banner("Accesorios", i, true);
        MockMultipartFile img = new MockMultipartFile("imagen_pc", "promo.jpg", "image/jpeg", JPG);
        mvc.perform(multipart("/api/banners").file(img).param("seccion", "Accesorios").with(como(aux.getId())))
                .andExpect(status().isConflict());
    }
}
