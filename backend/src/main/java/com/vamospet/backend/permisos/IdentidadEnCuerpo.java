package com.vamospet.backend.permisos;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.lang.reflect.Type;
import java.util.Map;

@ControllerAdvice
public class IdentidadEnCuerpo extends RequestBodyAdviceAdapter {

    @Override
    public boolean supports(MethodParameter parametro, Type tipo, Class<? extends HttpMessageConverter<?>> convertidor) {
        if (!Map.class.isAssignableFrom(parametro.getParameterType())) return false;
        if (parametro.getContainingClass().getSimpleName().equals("NotificacionController")) return false;
        Class<?> valor = ResolvableType.forMethodParameter(parametro).asMap().getGeneric(1).resolve();
        return valor == null || valor == Object.class;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object afterBodyRead(Object cuerpo, HttpInputMessage mensaje, MethodParameter parametro, Type tipo,
                                Class<? extends HttpMessageConverter<?>> convertidor) {
        Integer id = UsuarioActual.id();
        if (id != null && cuerpo instanceof Map<?, ?> mapa) {
            ((Map<String, Object>) mapa).put("usuario_id", id);
        }
        return cuerpo;
    }
}
