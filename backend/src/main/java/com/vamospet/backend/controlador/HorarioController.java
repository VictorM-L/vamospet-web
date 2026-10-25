package com.vamospet.backend.controlador;

import com.vamospet.backend.modelo.HorarioAtencion;
import com.vamospet.backend.modelo.Usuario;
import com.vamospet.backend.modelo.VeterinarioEspecialidad;
import com.vamospet.backend.repositorio.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class HorarioController {

    @Autowired private HorarioAtencionRepository horarioRepositorio;
    @Autowired private VeterinarioEspecialidadRepository vetEspRepositorio;
    @Autowired private EspecialidadRepository especialidadRepositorio;
    @Autowired private UsuarioRepository usuarioRepositorio;

    private boolean esAdmin(Object usuarioId) {
        if (usuarioId == null) return false;
        try {
            Usuario u = usuarioRepositorio.findById(Integer.valueOf(usuarioId.toString())).orElse(null);
            return u != null && Integer.valueOf(1).equals(u.getRol_id());
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private Map<String, Object> aMapa(HorarioAtencion h) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", h.getId());
        m.put("veterinario_id", h.getVeterinario_id());
        m.put("dia_semana", h.getDia_semana());
        m.put("hora_inicio", h.getHora_inicio().toString().substring(0, 5));
        m.put("hora_fin", h.getHora_fin().toString().substring(0, 5));
        return m;
    }


    @GetMapping("/horarios")
    public List<Map<String, Object>> listar(@RequestParam(required = false) Integer veterinario_id) {
        return horarioRepositorio.findAll().stream()
                .filter(h -> veterinario_id == null || veterinario_id.equals(h.getVeterinario_id()))
                .sorted(Comparator.comparing(HorarioAtencion::getDia_semana).thenComparing(HorarioAtencion::getHora_inicio))
                .map(this::aMapa).toList();
    }

    @PostMapping("/horarios")
    public ResponseEntity<?> crear(@RequestBody Map<String, Object> b) {
        if (!esAdmin(b.get("usuario_id"))) return error(HttpStatus.FORBIDDEN, "Solo el Administrador puede definir horarios.");
        try {
            Integer vetId = Integer.valueOf(b.get("veterinario_id").toString());
            Integer dia = Integer.valueOf(b.get("dia_semana").toString());
            LocalTime ini = LocalTime.parse(b.get("hora_inicio").toString());
            LocalTime fin = LocalTime.parse(b.get("hora_fin").toString());

            Usuario vet = usuarioRepositorio.findById(vetId).orElse(null);
            if (vet == null || !Integer.valueOf(3).equals(vet.getRol_id())) return error(HttpStatus.BAD_REQUEST, "Elige un veterinario válido.");
            if (dia < 1 || dia > 7) return error(HttpStatus.BAD_REQUEST, "Día inválido.");
            if (!fin.isAfter(ini)) return error(HttpStatus.BAD_REQUEST, "La hora de fin debe ser posterior a la de inicio.");
            if (ini.getMinute() % 30 != 0 || fin.getMinute() % 30 != 0) {
                return error(HttpStatus.BAD_REQUEST, "Las horas deben ser en punto o y media (las citas duran 30 minutos).");
            }
            boolean cruza = horarioRepositorio.findAll().stream().anyMatch(h ->
                    vetId.equals(h.getVeterinario_id()) && dia.equals(h.getDia_semana())
                    && ini.isBefore(h.getHora_fin()) && h.getHora_inicio().isBefore(fin));
            if (cruza) return error(HttpStatus.CONFLICT, "Ese bloque se cruza con otro horario del mismo día.");

            HorarioAtencion h = new HorarioAtencion();
            h.setVeterinario_id(vetId);
            h.setDia_semana(dia);
            h.setHora_inicio(ini);
            h.setHora_fin(fin);
            return ResponseEntity.ok(aMapa(horarioRepositorio.save(h)));
        } catch (Exception e) {
            return error(HttpStatus.BAD_REQUEST, "Datos de horario inválidos.");
        }
    }

    @DeleteMapping("/horarios/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Integer id, @RequestParam Integer usuarioId) {
        if (!esAdmin(usuarioId)) return error(HttpStatus.FORBIDDEN, "Solo el Administrador puede quitar horarios.");
        if (!horarioRepositorio.existsById(id)) return ResponseEntity.notFound().build();
        horarioRepositorio.deleteById(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }


    @GetMapping("/veterinarios/{id}/especialidades")
    public List<Integer> especialidadesDe(@PathVariable Integer id) {
        return vetEspRepositorio.findAll().stream()
                .filter(v -> id.equals(v.getVeterinario_id()))
                .map(VeterinarioEspecialidad::getEspecialidad_id).toList();
    }

    @PutMapping("/veterinarios/{id}/especialidades")
    @Transactional
    public ResponseEntity<?> definirEspecialidades(@PathVariable Integer id, @RequestBody Map<String, Object> b) {
        if (!esAdmin(b.get("usuario_id"))) return error(HttpStatus.FORBIDDEN, "Solo el Administrador puede asignar especialidades.");
        Usuario vet = usuarioRepositorio.findById(id).orElse(null);
        if (vet == null || !Integer.valueOf(3).equals(vet.getRol_id())) return error(HttpStatus.BAD_REQUEST, "Veterinario no válido.");
        Set<Integer> nuevos = new LinkedHashSet<>();
        try {
            for (Object o : (List<?>) b.get("especialidad_ids")) nuevos.add(Integer.valueOf(o.toString()));
        } catch (Exception e) {
            return error(HttpStatus.BAD_REQUEST, "Lista de especialidades inválida.");
        }
        for (Integer espId : nuevos) {
            if (!especialidadRepositorio.existsById(espId)) return error(HttpStatus.BAD_REQUEST, "La especialidad " + espId + " no existe.");
        }
        for (VeterinarioEspecialidad v : vetEspRepositorio.findAll()) {
            if (id.equals(v.getVeterinario_id())) vetEspRepositorio.delete(v);
        }
        vetEspRepositorio.flush();
        for (Integer espId : nuevos) {
            VeterinarioEspecialidad v = new VeterinarioEspecialidad();
            v.setVeterinario_id(id);
            v.setEspecialidad_id(espId);
            vetEspRepositorio.save(v);
        }
        return ResponseEntity.ok(new ArrayList<>(nuevos));
    }

    private ResponseEntity<?> error(HttpStatus s, String msg) {
        return ResponseEntity.status(s).body(Map.of("error", msg));
    }
}
