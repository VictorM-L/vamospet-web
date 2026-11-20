package com.vamospet.backend.permisos;

import com.vamospet.backend.modelo.Usuario;
import com.vamospet.backend.seguridad.FiltroJwt;
import com.vamospet.backend.seguridad.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

public final class UsuarioActual {

    public static final String ATRIBUTO = "vamospet.usuarioActual";

    private UsuarioActual() {
    }

    public static Usuario obtener() {
        RequestAttributes atributos = RequestContextHolder.getRequestAttributes();
        if (atributos == null) return null;
        Object u = atributos.getAttribute(ATRIBUTO, RequestAttributes.SCOPE_REQUEST);
        return u instanceof Usuario usuario ? usuario : null;
    }

    public static Integer id() {
        Usuario u = obtener();
        return u == null ? null : u.getId();
    }

    public static Rol rol() {
        Usuario u = obtener();
        return u == null ? null : Rol.deId(u.getRol_id());
    }

    public static boolean es(Rol rol) {
        return rol != null && rol == rol();
    }

    public static JwtService.Token token() {
        RequestAttributes atributos = RequestContextHolder.getRequestAttributes();
        if (atributos == null) return null;
        Object t = atributos.getAttribute(FiltroJwt.ATRIBUTO_TOKEN, RequestAttributes.SCOPE_REQUEST);
        return t instanceof JwtService.Token token ? token : null;
    }

    static void guardar(HttpServletRequest request, Usuario usuario) {
        request.setAttribute(ATRIBUTO, usuario);
    }
}
