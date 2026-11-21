package com.vamospet.backend.servicio;

import com.vamospet.backend.modelo.Banner;
import com.vamospet.backend.repositorio.BannerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class BannerService {

    public static final Set<String> SECCIONES = Set.of("Farmacia", "Accesorios");
    public static final int MAXIMO_ACTIVOS = 6;
    public static final int TAMANO_MAXIMO_BYTES = 2 * 1024 * 1024;
    public static final String PREFIJO_URL = "/uploads/";
    static final Path CARPETA = Paths.get("uploads");
    private static final Map<String, Set<String>> EXTENSIONES = Map.of(
            "jpg", Set.of("jpg", "jpeg"), "png", Set.of("png"), "webp", Set.of("webp"));

    public record Archivo(String nombre, byte[] contenido) {
        public boolean presente() {
            return contenido != null && contenido.length > 0;
        }
    }

    @Autowired private BannerRepository repositorio;
    @Autowired private AutorizacionService autorizacion;

    public static String validarImagen(Archivo a, String etiqueta) {
        if (a == null || !a.presente()) throw new NegocioException(400, "Falta la " + etiqueta + ".");
        if (a.contenido().length > TAMANO_MAXIMO_BYTES) throw new NegocioException(400, "La " + etiqueta + " supera los 2 MB permitidos.");
        String tipo = tipoReal(a.contenido());
        if (tipo == null) throw new NegocioException(400, "La " + etiqueta + " debe ser JPG, PNG o WebP.");
        String nombre = a.nombre() == null ? "" : a.nombre();
        int punto = nombre.lastIndexOf('.');
        String ext = punto >= 0 ? nombre.substring(punto + 1).toLowerCase(Locale.ROOT) : "";
        if (!EXTENSIONES.get(tipo).contains(ext)) {
            throw new NegocioException(400, "La extensión de la " + etiqueta + " no coincide con su contenido. Usa JPG, PNG o WebP.");
        }
        return tipo;
    }

    static String tipoReal(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return "jpg";
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) return "png";
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return "webp";
        return null;
    }

    public List<Banner> activos(String seccion) {
        String s = exigirSeccion(seccion);
        return repositorio.deSeccion(s).stream().filter(b -> Boolean.TRUE.equals(b.getActivo())).toList();
    }

    public List<Banner> gestion(Integer actorId, String seccion) {
        if (autorizacion.conRol(actorId, AutorizacionService.ADMIN, AutorizacionService.AUXILIAR) == null) {
            throw new NegocioException(403, "No tienes permiso para ver la gestión de banners.");
        }
        return repositorio.deSeccion(exigirSeccion(seccion));
    }

    @Transactional
    public synchronized Banner crear(Integer actorId, String seccion, Archivo pc, Archivo movil, Boolean activo) {
        exigirAuxiliar(actorId);
        String s = exigirSeccion(seccion);
        String tipoPc = validarImagen(pc, "imagen para PC");
        String tipoMovil = movil != null && movil.presente() ? validarImagen(movil, "imagen para celular") : null;
        boolean activar = !Boolean.FALSE.equals(activo);
        List<Banner> actuales = repositorio.deSeccion(s);
        if (activar) exigirCupo(s, actuales, null);
        List<String> guardados = new ArrayList<>();
        try {
            Banner b = new Banner();
            b.setSeccion(s);
            b.setImagen_pc(guardar(pc, tipoPc, guardados));
            b.setImagen_movil(tipoMovil == null ? null : guardar(movil, tipoMovil, guardados));
            b.setActivo(activar);
            b.setOrden(actuales.stream().mapToInt(x -> x.getOrden() == null ? 0 : x.getOrden()).max().orElse(0) + 1);
            return repositorio.save(b);
        } catch (RuntimeException e) {
            guardados.forEach(BannerService::borrarArchivo);
            throw e;
        }
    }

    @Transactional
    public synchronized Banner reemplazar(Integer actorId, Integer id, Archivo pc, Archivo movil, boolean quitarMovil) {
        exigirAuxiliar(actorId);
        Banner b = obtener(id);
        boolean hayPc = pc != null && pc.presente();
        boolean hayMovil = movil != null && movil.presente();
        if (!hayPc && !hayMovil && !quitarMovil) throw new NegocioException(400, "Elige la imagen que quieres reemplazar.");
        String tipoPc = hayPc ? validarImagen(pc, "imagen para PC") : null;
        String tipoMovil = hayMovil ? validarImagen(movil, "imagen para celular") : null;
        List<String> guardados = new ArrayList<>();
        List<String> viejos = new ArrayList<>();
        try {
            if (hayPc) {
                viejos.add(b.getImagen_pc());
                b.setImagen_pc(guardar(pc, tipoPc, guardados));
            }
            if (hayMovil || quitarMovil) {
                if (b.getImagen_movil() != null) viejos.add(b.getImagen_movil());
                b.setImagen_movil(hayMovil ? guardar(movil, tipoMovil, guardados) : null);
            }
            Banner guardado = repositorio.save(b);
            viejos.forEach(BannerService::borrarArchivo);
            return guardado;
        } catch (RuntimeException e) {
            guardados.forEach(BannerService::borrarArchivo);
            throw e;
        }
    }

    @Transactional
    public synchronized List<Banner> mover(Integer actorId, Integer id, Integer posicion) {
        exigirAuxiliar(actorId);
        Banner b = obtener(id);
        if (posicion == null || posicion < 1) throw new NegocioException(400, "Indica la nueva posición (1 o más).");
        List<Banner> lista = new ArrayList<>(repositorio.deSeccion(b.getSeccion()));
        lista.removeIf(x -> x.getId().equals(b.getId()));
        lista.add(Math.min(posicion, lista.size() + 1) - 1, b);
        return renumerar(lista);
    }

    @Transactional
    public synchronized Banner cambiarActivo(Integer actorId, Integer id, Boolean activo) {
        exigirAuxiliar(actorId);
        if (activo == null) throw new NegocioException(400, "Indica si el banner queda activo o no.");
        Banner b = obtener(id);
        if (activo && !Boolean.TRUE.equals(b.getActivo())) exigirCupo(b.getSeccion(), repositorio.deSeccion(b.getSeccion()), b.getId());
        b.setActivo(activo);
        return repositorio.save(b);
    }

    @Transactional
    public synchronized void eliminar(Integer actorId, Integer id) {
        exigirAuxiliar(actorId);
        Banner b = obtener(id);
        repositorio.delete(b);
        List<Banner> resto = new ArrayList<>(repositorio.deSeccion(b.getSeccion()));
        resto.removeIf(x -> x.getId().equals(b.getId()));
        renumerar(resto);
        borrarArchivo(b.getImagen_pc());
        if (b.getImagen_movil() != null) borrarArchivo(b.getImagen_movil());
    }

    private List<Banner> renumerar(List<Banner> lista) {
        for (int i = 0; i < lista.size(); i++) lista.get(i).setOrden(i + 1);
        return repositorio.saveAll(lista);
    }

    private static void exigirCupo(String seccion, List<Banner> actuales, Integer ignorar) {
        long activos = actuales.stream().filter(x -> Boolean.TRUE.equals(x.getActivo()) && !x.getId().equals(ignorar)).count();
        if (activos >= MAXIMO_ACTIVOS) {
            throw new NegocioException(409, "Ya hay " + MAXIMO_ACTIVOS + " banners activos en " + seccion
                    + ". Desactiva uno antes de activar otro.");
        }
    }

    private void exigirAuxiliar(Integer actorId) {
        if (autorizacion.conRol(actorId, AutorizacionService.AUXILIAR) == null) {
            throw new NegocioException(403, "Solo el Auxiliar gestiona los banners de la tienda.");
        }
    }

    private static String exigirSeccion(String seccion) {
        if (seccion == null || !SECCIONES.contains(seccion)) throw new NegocioException(400, "La sección debe ser Farmacia o Accesorios.");
        return seccion;
    }

    private Banner obtener(Integer id) {
        if (id == null) throw new NegocioException(404, "El banner no existe.");
        return repositorio.findById(id).orElseThrow(() -> new NegocioException(404, "El banner no existe."));
    }

    private static String guardar(Archivo a, String tipo, List<String> guardados) {
        String nombre = "banner-" + UUID.randomUUID() + "." + tipo;
        try {
            Files.createDirectories(CARPETA);
            Files.write(CARPETA.resolve(nombre), a.contenido());
        } catch (IOException e) {
            throw new NegocioException(500, "No se pudo guardar la imagen. Intenta de nuevo.");
        }
        String url = PREFIJO_URL + nombre;
        guardados.add(url);
        return url;
    }

    static void borrarArchivo(String url) {
        if (url == null || !url.startsWith(PREFIJO_URL + "banner-")) return;
        String nombre = url.substring(PREFIJO_URL.length());
        if (nombre.contains("/") || nombre.contains("\\") || nombre.contains("..")) return;
        try { Files.deleteIfExists(CARPETA.resolve(nombre)); } catch (IOException ignorado) { }
    }
}
