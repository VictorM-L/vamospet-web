package com.vamospet.backend.config;

import com.vamospet.backend.permisos.Rol;
import com.vamospet.backend.repositorio.UsuarioRepository;
import com.vamospet.backend.seguridad.FiltroJwt;
import com.vamospet.backend.seguridad.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    public static final String POLITICA_CONTENIDO = "default-src 'self'; "
            + "script-src 'self' 'unsafe-inline'; "
            + "style-src 'self' 'unsafe-inline'; "
            + "font-src 'self'; "
            + "img-src 'self' data: blob:; "
            + "frame-src https://www.google.com; "
            + "connect-src 'self'; "
            + "object-src 'none'; "
            + "form-action 'self'; "
            + "frame-ancestors 'self'; "
            + "base-uri 'self';";

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtService jwt, UsuarioRepository usuarios) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").access((autenticacion, contexto) -> new AuthorizationDecision(esAdmin(autenticacion.get(), usuarios)))
                .anyRequest().permitAll())
            .exceptionHandling(errores -> errores.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .csrf(csrf -> csrf.disable())
            .httpBasic(basico -> basico.disable())
            .formLogin(formulario -> formulario.disable())
            .logout(salida -> salida.disable())
            .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(new FiltroJwt(jwt), UsernamePasswordAuthenticationFilter.class)
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(POLITICA_CONTENIDO))
                .frameOptions(frame -> frame.sameOrigin())
            );

        return http.build();
    }

    static boolean esAdmin(Authentication autenticacion, UsuarioRepository usuarios) {
        if (autenticacion == null || !(autenticacion.getPrincipal() instanceof Integer id)) return false;
        return usuarios.findById(id)
                .filter(u -> !Boolean.FALSE.equals(u.getActivo()))
                .map(u -> Rol.deId(u.getRol_id()) == Rol.ADMIN)
                .orElse(false);
    }
}
