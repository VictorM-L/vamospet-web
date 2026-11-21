package com.vamospet.backend.seguridad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class FiltroJwt extends OncePerRequestFilter {

    public static final String ATRIBUTO_TOKEN = "vamospet.token";
    public static final String ATRIBUTO_INVALIDO = "vamospet.tokenInvalido";
    private static final String PREFIJO = "Bearer ";

    private final JwtService jwt;

    public FiltroJwt(JwtService jwt) {
        this.jwt = jwt;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain cadena)
            throws ServletException, IOException {
        String cabecera = request.getHeader("Authorization");
        if (cabecera != null && cabecera.startsWith(PREFIJO)) {
            JwtService.Token token = jwt.validar(cabecera.substring(PREFIJO.length()).trim());
            if (token == null) {
                request.setAttribute(ATRIBUTO_INVALIDO, Boolean.TRUE);
            } else {
                request.setAttribute(ATRIBUTO_TOKEN, token);
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(token.usuarioId(), null, List.of()));
            }
        } else if (cabecera != null && !cabecera.isBlank()) {
            request.setAttribute(ATRIBUTO_INVALIDO, Boolean.TRUE);
        }
        cadena.doFilter(request, response);
    }
}
