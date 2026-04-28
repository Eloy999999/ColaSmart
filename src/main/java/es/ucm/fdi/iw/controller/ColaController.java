package es.ucm.fdi.iw.controller;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import es.ucm.fdi.iw.LocalData;
import es.ucm.fdi.iw.model.Cola;
import es.ucm.fdi.iw.model.ColaRepository;
import es.ucm.fdi.iw.model.User;
import es.ucm.fdi.iw.model.UserRepository;
import jakarta.servlet.http.HttpSession;

@Controller
public class ColaController {

    private static final Logger log = LogManager.getLogger(ColaController.class);

    @Autowired
    private ColaRepository colaRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LocalData localData;

    @ModelAttribute
    public void populateModel(HttpSession session, Model model) {
        for (String name : new String[] { "u", "url", "ws", "topics" }) {
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
        return "redirect:/panelAdmin?modal=listas";
    }

    @PostMapping("/panelAdmin/colas/editar/{id}")
    public String actualizarCola(@PathVariable Long id, Cola cola,
            @RequestParam(value = "imagen", required = false) MultipartFile imagen) throws IOException {

        Cola colaExistente = colaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cola no encontrada"));

        cola.setId(id);
        cola.setAbierto(colaExistente.getAbierto());
        cola.setListaClientes(colaExistente.getListaClientes());
        cola.setTurnoActual(colaExistente.getTurnoActual());
        cola.setInicioTurnoActual(colaExistente.getInicioTurnoActual());
        cola.setUltimoTurno(colaExistente.getUltimoTurno());
        cola.setInicioUltimoTurno(colaExistente.getInicioUltimoTurno());
        cola.setFinUltimoTurno(colaExistente.getFinUltimoTurno());

        colaRepository.save(cola);

        if (imagen != null && !imagen.isEmpty()) {
            log.info("Subiendo imagen para cola {}, tamaño: {} bytes", id, imagen.getSize());
            File f = localData.getFile("cola", id + ".jpg");
            log.info("Guardando en: {}", f.getAbsolutePath());
            try (BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(f))) {
                stream.write(imagen.getBytes());
                log.info("Imagen guardada correctamente");
            }
        } else {
            log.warn("No se recibió imagen o estaba vacía. imagen={}", imagen);
        }

        return "redirect:/panelAdmin?modal=listas";
    }

    @GetMapping("/modificar_cola/{id}")
    public String mostrarModificarCola(@PathVariable Long id, Model model) {
        Cola cola = colaRepository.findById(id).orElse(null);
        model.addAttribute("cola", cola);
        return "modificar_cola";
    }

    @GetMapping("/seguimientoCola")
    public String seguimientoCola(Model model) {
        List<User> users = userRepository.findAll();
        List<Cola> colas = colaRepository.findAll();

        Map<Long, Integer> maxPuestoPorCola = new HashMap<>();

        for (Cola cola : colas) {
            int maxPuesto = 0;

            if (cola.getListaClientes() != null) {
                maxPuesto = cola.getListaClientes().stream()
                        .mapToInt(User::getPosicion)
                        .max()
                        .orElse(0);
            }

            if (maxPuesto < 1) {
                maxPuesto = 0;
            }

            maxPuestoPorCola.put(cola.getId(), maxPuesto);
        }

        model.addAttribute("maxPuestoPorCola", maxPuestoPorCola);
        model.addAttribute("colas", colas);
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
                .map(u -> Map.of("id", u.getId(), "username", u.getUsername(), "posicion", u.getPosicion()))
                .collect(Collectors.toList()));

        return data;
    }

    /*
     * @PostMapping("/colas/{id}/siguiente")
     * 
     * @ResponseBody
     * public ResponseEntity<?> llamarSiguiente(@PathVariable long id) {
     * Cola cola = colaRepository.findById(id)
     * .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
     * 
     * List<User> lista = cola.getListaClientes();
     * if (!lista.isEmpty()) {
     * // Guardar el turno actual como último antes de avanzar
     * if (cola.getTurnoActual() != null) {
     * cola.setUltimoTurno(cola.getTurnoActual());
     * cola.setInicioUltimoTurno(cola.getInicioTurnoActual());
     * cola.setFinUltimoTurno(LocalTime.now());
     * }
     * 
     * // Avanzar al siguiente
     * User siguiente = lista.remove(0);
     * cola.setTurnoActual(siguiente.getUsername());
     * cola.setInicioTurnoActual(LocalTime.now());
     * 
     * colaRepository.save(cola);
     * }
     * 
     * return ResponseEntity.ok().build();
     * }
     */
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @PostMapping("/colas/{id}/siguiente")
    @ResponseBody
    public ResponseEntity<?> llamarSiguiente(@PathVariable long id, @RequestParam int sala) {

        Cola cola = colaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        List<User> lista = cola.getListaClientes();

        // 1. Guardar el actual (posicion 0)
        User actual = lista.stream()
                .filter(u -> u.getPosicion() == 0)
                .findFirst()
                .orElse(null);

        LocalTime inicioActual = cola.getInicioTurnoActual(); // 👈 guardar copia segura

        if (actual != null) {
            cola.setUltimoTurno(actual.getUsername());
            cola.setInicioUltimoTurno(inicioActual);
            cola.setFinUltimoTurno(LocalTime.now());
        }

        // 2. Restar 1 a todos
        for (User u : lista) {
            u.setPosicion(u.getPosicion() - 1);
        }

        // 2.5. El nuevo actual (posicion 0) pasa a sala
        User nuevoActual = lista.stream()
                .filter(u -> u.getPosicion() == 0)
                .findFirst()
                .orElse(null);

        if (nuevoActual != null) {
            nuevoActual.setLugar("Puesto " + sala);

            cola.setInicioTurnoActual(LocalTime.now());
        }

        // 3. Eliminar los de posicion -7
        // Iterator<User> it = lista.iterator();

        /*
         * List<User> toDelete = new ArrayList<>();
         * 
         * while (it.hasNext()) {
         * User u = it.next();
         * 
         * if (u.getPosicion() <= -7) {
         * it.remove();
         * toDelete.add(u);
         * }
         * }
         * 
         */

        colaRepository.saveAndFlush(cola);
        // userRepository.deleteAll(toDelete);

        // Notifica
        messagingTemplate.convertAndSend(
                "/topic/cola/" + id + "/actualizar",
                "{\"colaId\":" + id + ", \"tipo\":\"SIGUIENTE\"}");

        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/panelQR/{id}")
    public String mostrarPanelQR(@PathVariable Long id, HttpSession session, Model model) {
        session.setAttribute("panelQrColaId", id);
        long idDefault = id;
        Cola cola = colaRepository.findById(idDefault).orElse(null);
        model.addAttribute("cola", cola); // Le meto la cola para poder sacar de ahi toda la info de esta, y meter al
                                          // user nuevo a esta cola

        if (!cola.isAbierto()) { // Si esta cerrada la cola, poner que la cola esta cerrada
            model.addAttribute("cola", cola);
            return "cola_cerrada";
        }

        // Coger posiciones de -6 a -1 por cola
        List<User> atendidos = userRepository.findAtendidosByColaId(idDefault, -6, -1);

        while (atendidos.size() < 6) {
            atendidos.add(null);
        }

        // Turno actual por cola
        Optional<User> turnoActualOpt = userRepository.findTurnoActualByColaId(idDefault,0);
        User turnoActual = turnoActualOpt.orElse(null);

        //posiciones globales
        //User turnoActual = userRepository.findByPosicion(0);

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

    @PostMapping("/colas/{id}/imagen")
    @ResponseBody
    public String subirImagenCola(@RequestParam("imagen") MultipartFile imagen,
            @PathVariable long id) throws IOException {

        File f = localData.getFile("cola", id + ".jpg");
        try (BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(f))) {
            stream.write(imagen.getBytes());
        }
        return "{\"status\":\"ok\"}";
    }

    @GetMapping("/colas/{id}/imagen")
    public StreamingResponseBody getImagenCola(@PathVariable long id) throws IOException {
        File f = localData.getFile("cola", id + ".jpg");
        InputStream in = new BufferedInputStream(
                f.exists() ? new FileInputStream(f)
                        : UserController.class.getClassLoader().getResourceAsStream("static/img/default-bg.jpg"));
        return os -> FileCopyUtils.copy(in, os);
    }
}
