package es.ucm.fdi.iw.controller;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import es.ucm.fdi.iw.model.Cola;
import es.ucm.fdi.iw.model.ColaRepository;
import es.ucm.fdi.iw.model.User;
import es.ucm.fdi.iw.model.UserRepository;
import jakarta.servlet.http.HttpSession;

@Controller
public class ColaController {

    @Autowired
    private ColaRepository colaRepository;

    @Autowired
    private UserRepository userRepository;

    @ModelAttribute
    public void populateModel(HttpSession session, Model model) {
        for (String name : new String[] { "u", "url", "ws", "topics"}) {
            model.addAttribute(name, session.getAttribute(name));
        }
    }

    @PostMapping("/colas/eliminar/{id}")
    public String eliminarCola(@PathVariable("id") Long id, RedirectAttributes redirectAttrs) {
        if (colaRepository.existsById(id)) {
            colaRepository.deleteById(id);
            redirectAttrs.addFlashAttribute("msg", "Cola eliminada correctamente");
        } else {
            redirectAttrs.addFlashAttribute("msg", "La cola no existe");
        }
        return "redirect:/panelAdmin";
    }

    @PostMapping("/panelAdmin/colas/editar/{id}")
    public String actualizarCola(@PathVariable Long id, Cola cola) {
        cola.setId(id);
        colaRepository.save(cola);
        return "redirect:/panelAdmin";
    }

    @GetMapping("/modificar_cola/{id}")
    public String mostrarModificarCola(@PathVariable Long id, Model model) {
        Cola cola = colaRepository.findById(id).orElse(null);
        model.addAttribute("cola", cola);
        return "modificar_cola";
    }

    @GetMapping("/seguimientoCola")
    public String seguimientoCola(Model model) {
        model.addAttribute("colas", colaRepository.findAll());
        return "seguimientoCola";
    }

    @PostMapping("/colas/{id}/abrir")
    public String abrirCola(@PathVariable Long id) {
        Cola cola = colaRepository.findById(id).orElse(null);
        if (cola != null) {
            cola.abrir();
            colaRepository.save(cola);
        }
        return "redirect:/seguimientoCola";
    }

    @PostMapping("/colas/{id}/cerrar")
    public String cerrarCola(@PathVariable Long id) {
        Cola cola = colaRepository.findById(id).orElse(null);
        if (cola != null) {
            cola.cerrar();
            colaRepository.save(cola);
        }
        return "redirect:/seguimientoCola";
    }

    @GetMapping("/colas/{id}/detalle")
    @ResponseBody
    public Map<String, Object> detalleCola(@PathVariable long id) {
        Cola cola = colaRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Map<String, Object> data = new HashMap<>();
        data.put("id", cola.getId());
        data.put("nombre", cola.getNombre());
        data.put("turnoActual", cola.getTurnoActual());
        data.put("estado", cola.getEstado().name());

        // Turno actual: hora inicio y minutos transcurridos
        if (cola.getInicioTurnoActual() != null) {
            data.put("inicioTurnoActual", cola.getInicioTurnoActual().toString().substring(0, 5));
            long minutos = Duration.between(cola.getInicioTurnoActual(), LocalTime.now()).toMinutes();
            data.put("minutosTurnoActual", minutos);
        } else {
            data.put("inicioTurnoActual", null);
            data.put("minutosTurnoActual", 0);
        }

        // Último turno atendido
        data.put("ultimoTurno", cola.getUltimoTurno());
        if (cola.getInicioUltimoTurno() != null) {
            data.put("inicioUltimoTurno", cola.getInicioUltimoTurno().toString().substring(0, 5));
        } else {
            data.put("inicioUltimoTurno", null);
        }
        if (cola.getFinUltimoTurno() != null) {
            data.put("finUltimoTurno", cola.getFinUltimoTurno().toString().substring(0, 5));
            if (cola.getInicioUltimoTurno() != null) {
                long mins = Duration.between(cola.getInicioUltimoTurno(), cola.getFinUltimoTurno()).toMinutes();
                data.put("minutosUltimoTurno", mins);
            } else {
                data.put("minutosUltimoTurno", 0);
            }
        } else {
            data.put("finUltimoTurno", null);
            data.put("minutosUltimoTurno", 0);
        }

        data.put("listaClientes", cola.getListaClientes().stream()
            .map(u -> Map.of("id", u.getId(), "username", u.getUsername(),  "posicion", u.getPosicion()))
            .collect(Collectors.toList()));

        return data;
    }


    /*
    @PostMapping("/colas/{id}/siguiente")
    @ResponseBody
    public ResponseEntity<?> llamarSiguiente(@PathVariable long id) {
        Cola cola = colaRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        List<User> lista = cola.getListaClientes();
        if (!lista.isEmpty()) { 
            // Guardar el turno actual como último antes de avanzar
            if (cola.getTurnoActual() != null) {
                cola.setUltimoTurno(cola.getTurnoActual());
                cola.setInicioUltimoTurno(cola.getInicioTurnoActual());
                cola.setFinUltimoTurno(LocalTime.now());
            }

            // Avanzar al siguiente
            User siguiente = lista.remove(0);
            cola.setTurnoActual(siguiente.getUsername());
            cola.setInicioTurnoActual(LocalTime.now());

            colaRepository.save(cola);
        }

        return ResponseEntity.ok().build();
    }
         */
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @PostMapping("/colas/{id}/siguiente")
    @ResponseBody
    public ResponseEntity<?> llamarSiguiente(@PathVariable long id) {

        Cola cola = colaRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        List<User> lista = cola.getListaClientes();

        // 1. Guardar el actual (posicion 0)
        User actual = lista.stream()
            .filter(u -> u.getPosicion() == 0)
            .findFirst()
            .orElse(null);

        if (actual != null) {
            cola.setUltimoTurno(actual.getUsername());
            cola.setFinUltimoTurno(LocalTime.now());
        }

        // 2. Restar 1 a todos
        for (User u : lista) {
            u.setPosicion(u.getPosicion() - 1);
        }
        
        // 3. Eliminar los de posicion -7
        Iterator<User> it = lista.iterator();
        List<User> toDelete = new ArrayList<>();

        while (it.hasNext()) {
            User u = it.next();

            if (u.getPosicion() <= -7) {
                it.remove();
                toDelete.add(u);
            }
        }

        colaRepository.saveAndFlush(cola);
        userRepository.deleteAll(toDelete);

        // Notifica
        messagingTemplate.convertAndSend(
            "/topic/cola/" + id + "/actualizar",
            "{\"colaId\":" + id + ", \"tipo\":\"SIGUIENTE\"}");

        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/panelQR/{id}")
    public String mostrarPanelQR(@PathVariable Long id, Model model) {
        long idDefault = id;
        Cola cola = colaRepository.findById(idDefault).orElse(null);
        model.addAttribute("cola", cola); // Le meto la cola para poder sacar de ahi toda la info de esta, y meter al
                                          // user nuevo a esta cola

        if (!cola.isAbierto()) { // Si esta cerrada la cola, poner que la cola esta cerrada
            model.addAttribute("cola", cola);
            return "cola_cerrada";
        }

        // Coger posiciones de -6 a -1
        List<User> atendidos = userRepository
                .findByPosicionBetweenOrderByPosicionDesc(-6, -1);

        while (atendidos.size() < 6) {
            atendidos.add(null);
        }

        // Turno actual
        User turnoActual = userRepository.findByPosicion(0);

        model.addAttribute("atendidos", atendidos);
        model.addAttribute("turnoActual", turnoActual);

        return "panelQR";
    }
   /* 
   @GetMapping("/panelQR/{id}")
    public String mostrarPanelQR(@PathVariable Long id, Model model) {
        Cola cola = colaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Cola no encontrada: " + id));
        model.addAttribute("cola", cola);
        return "panelQR";
    }*/
}
