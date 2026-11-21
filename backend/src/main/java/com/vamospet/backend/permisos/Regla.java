package com.vamospet.backend.permisos;

import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class Regla {

    private static final PathPatternParser PARSER = new PathPatternParser();

    private final String metodo;
    private final String patron;
    private final PathPattern ruta;
    private final Set<Rol> roles;
    private final boolean publica;
    private final Propiedad propiedad;
    private final String variable;
    private final boolean propiedadParaTodos;
    private final String area;
    private final String funcion;

    private Regla(String metodo, String patron, Set<Rol> roles, boolean publica, Propiedad propiedad,
                  String variable, boolean propiedadParaTodos, String area, String funcion) {
        this.metodo = metodo;
        this.patron = patron;
        this.ruta = PARSER.parse(patron);
        this.roles = roles.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(EnumSet.copyOf(roles));
        this.publica = publica;
        this.propiedad = propiedad;
        this.variable = variable;
        this.propiedadParaTodos = propiedadParaTodos;
        this.area = area;
        this.funcion = funcion;
    }

    static Regla publica(String metodo, String patron, String area, String funcion) {
        return new Regla(metodo, patron, EnumSet.noneOf(Rol.class), true, Propiedad.NINGUNA, null, false, area, funcion);
    }

    static Regla de(String metodo, String patron, Set<Rol> roles, String area, String funcion) {
        return new Regla(metodo, patron, roles, false, Propiedad.NINGUNA, null, false, area, funcion);
    }

    static Regla propia(String metodo, String patron, Set<Rol> roles, Propiedad propiedad, String variable,
                        String area, String funcion) {
        return new Regla(metodo, patron, roles, false, propiedad, variable, false, area, funcion);
    }

    static Regla propiaParaTodos(String metodo, String patron, Set<Rol> roles, Propiedad propiedad, String variable,
                                 String area, String funcion) {
        return new Regla(metodo, patron, roles, false, propiedad, variable, true, area, funcion);
    }

    Map<String, String> coincide(String metodoPedido, String rutaPedida) {
        if (!metodo.equalsIgnoreCase(metodoPedido)) return null;
        PathPattern.PathMatchInfo info = ruta.matchAndExtract(PathContainer.parsePath(rutaPedida));
        return info == null ? null : info.getUriVariables();
    }

    public boolean permite(Rol rol) {
        return publica || (rol != null && roles.contains(rol));
    }

    public boolean exigePropiedad(Rol rol) {
        if (propiedad == Propiedad.NINGUNA) return false;
        return propiedadParaTodos || rol == Rol.CLIENTE;
    }

    public String getMetodo() { return metodo; }
    public String getPatron() { return patron; }
    public Set<Rol> getRoles() { return roles; }
    public boolean isPublica() { return publica; }
    public Propiedad getPropiedad() { return propiedad; }
    public String getVariable() { return variable; }
    public boolean isPropiedadParaTodos() { return propiedadParaTodos; }
    public String getArea() { return area; }
    public String getFuncion() { return funcion; }
}
