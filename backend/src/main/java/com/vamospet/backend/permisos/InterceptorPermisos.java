package com.vamospet.backend.permisos;

import com.vamospet.backend.modelo.Usuario;
import com.vamospet.backend.repositorio.MascotaRepository;
import com.vamospet.backend.repositorio.PedidoRepository;
import com.vamospet.backend.repositorio.UsuarioRepository;
import com.vamospet.backend.seguridad.FiltroJwt;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Component
public class InterceptorPermisos implements HandlerInterceptor {

    @Autowired
    private UsuarioRepository usuarioRepositorio;

    @Autowired
    private MascotaRepository mascotaRepositorio;

    @Autowired
    private PedidoRepository pedidoRepositorio;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if (CorsUtils.isPreFlightRequest(request) || "OPTIONS".equalsIgnoreCase(request.getMethod())) return true;

        String ruta = request.getRequestURI().substring(request.getContextPath().length());
        PermisosApi.Coincidencia coincidencia = PermisosApi.buscar(request.getMethod(), ruta);
        if (coincidencia == null) {
            return rechazar(response, 403, "Esta operación no está habilitada.");
        }

        Integer parametro;
        Integer parametroCuerpo;
        try {
            parametro = entero(request.getParameter("usuarioId"));
            parametroCuerpo = entero(request.getParameter("usuario_id"));
        } catch (NumberFormatException e) {
            return rechazar(response, 400, "El identificador de usuario no es válido.");
        }
        Integer id = idDelToken();
        boolean tokenInvalido = Boolean.TRUE.equals(request.getAttribute(FiltroJwt.ATRIBUTO_INVALIDO));
        if (id != null && ((parametro != null && !parametro.equals(id)) || (parametroCuerpo != null && !parametroCuerpo.equals(id)))) {
            return rechazar(response, 403, "La identidad enviada no coincide con tu sesión.");
        }

        Usuario usuario = id == null ? null : usuarioRepositorio.findById(id).orElse(null);
        boolean activo = usuario != null && !Boolean.FALSE.equals(usuario.getActivo());
        if (activo) UsuarioActual.guardar(request, usuario);

        Regla regla = coincidencia.regla();
        if (regla.isPublica()) return true;
        if (tokenInvalido) {
            return rechazar(response, 401, "Tu sesión expiró o no es válida. Vuelve a iniciar sesión.");
        }
        if (id == null) {
            return rechazar(response, 401, "Inicia sesión para continuar.");
        }
        if (!activo) {
            return rechazar(response, 401, "Tu cuenta no está habilitada. Comunícate con la veterinaria.");
        }
        Rol rol = Rol.deId(usuario.getRol_id());
        if (!regla.permite(rol)) {
            return rechazar(response, 403, "Tu rol no tiene permiso para esta operación.");
        }
        if (regla.exigePropiedad(rol) && !esPropio(regla, coincidencia, usuario)) {
            return rechazar(response, 403, "Solo puedes acceder a tu propia información.");
        }
        return true;
    }

    private boolean esPropio(Regla regla, PermisosApi.Coincidencia coincidencia, Usuario usuario) {
        Integer valor;
        try {
            valor = entero(coincidencia.variables().get(regla.getVariable()));
        } catch (NumberFormatException e) {
            return false;
        }
        if (valor == null) return false;
        return switch (regla.getPropiedad()) {
            case USUARIO -> valor.equals(usuario.getId());
            case MASCOTA -> mascotaRepositorio.findById(valor)
                    .map(m -> Objects.equals(m.getUsuario_id(), usuario.getId())).orElse(false);
            case PEDIDO -> pedidoRepositorio.findById(valor)
                    .map(p -> Objects.equals(p.getUsuario_id(), usuario.getId())).orElse(false);
            case NINGUNA -> true;
        };
    }

    private static Integer idDelToken() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        return autenticacion != null && autenticacion.getPrincipal() instanceof Integer id ? id : null;
    }

    private static Integer entero(String texto) {
        if (texto == null || texto.isBlank() || "null".equals(texto) || "undefined".equals(texto)) return null;
        return Integer.valueOf(texto.trim());
    }

    private static boolean rechazar(HttpServletResponse response, int estado, String mensaje) throws IOException {
        response.setStatus(estado);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"" + mensaje.replace("\"", "'") + "\"}");
        return false;
    }
}
