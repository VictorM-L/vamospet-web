package com.vamospet.backend.controlador;

import com.vamospet.backend.modelo.Especialidad;
import com.vamospet.backend.modelo.Usuario;
import com.vamospet.backend.modelo.VeterinarioEspecialidad;
import com.vamospet.backend.repositorio.*;
import com.vamospet.backend.servicio.AgendaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/agenda")
@CrossOrigin(origins = "*")
public class AgendaController {

    @Autowired private AgendaService agendaService;
    @Autowired private UsuarioRepository usuarioRepositorio;
    @Autowired private EspecialidadRepository especialidadRepositorio;
    @Autowired private VeterinarioEspecialidadRepository vetEspRepositorio;

    private boolean vetDisponible(Usuario u) {
        return Integer.valueOf(3).equals(u.getRol_id())
                && !Boolean.FALSE.equals(u.getActivo())
                && agendaService.tieneHorario(u.getId());
    }

    @GetMapping("/especialidades")
    public List<Especialidad> especialidades() {
        Set<Integer> vetsOk = new HashSet<>();
        for (Usuario u : usuarioRepositorio.findAll()) if (vetDisponible(u)) vetsOk.add(u.getId());
        Set<Integer> espOk = new HashSet<>();
        for (VeterinarioEspecialidad v : vetEspRepositorio.findAll()) {
            if (vetsOk.contains(v.getVeterinario_id())) espOk.add(v.getEspecialidad_id());
        }
        return especialidadRepositorio.findAll().stream()
                .filter(e -> !Boolean.FALSE.equals(e.getActivo()) && espOk.contains(e.getId())).toList();
    }

    @GetMapping("/veterinarios")
    public List<Map<String, Object>> veterinarios(@RequestParam Integer especialidad_id) {
        Set<Integer> ids = new HashSet<>();
        for (VeterinarioEspecialidad v : vetEspRepositorio.findAll()) {
            if (especialidad_id.equals(v.getEspecialidad_id())) ids.add(v.getVeterinario_id());
        }
        List<Map<String, Object>> res = new ArrayList<>();
        for (Usuario u : usuarioRepositorio.findAll()) {
            if (ids.contains(u.getId()) && vetDisponible(u)) res.add(Map.of("id", u.getId(), "nombre", u.getNombre()));
        }
        return res;
    }

    @GetMapping("/disponibilidad")
    public ResponseEntity<?> disponibilidad(@RequestParam Integer veterinario_id, @RequestParam String fecha) {
        try {
            List<String> franjas = agendaService.franjasLibres(veterinario_id, LocalDate.parse(fecha))
                    .stream().map(t -> t.toString().substring(0, 5)).toList();
            return ResponseEntity.ok(Map.of("duracion_minutos", AgendaService.DURACION_CITA_MINUTOS, "franjas", franjas));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Fecha inválida."));
        }
    }
}
