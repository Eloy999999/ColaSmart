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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
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

/**
 * Controlador principal para la gestion de colas de espera.
 * Gestiona operaciones como crear, editar, abrir/cerrar colas,
 * avanzar turnos y mostrar paneles de informacion.
 */
@Controller
public class ColaController {

    private static final Logger log = LogManager.getLogger(ColaController.class);

    // Repositorio de acceso a datos de colas
    @Autowired
    private ColaRepository colaRepository;

    // Repositorio de acceso a datos de usuarios
    @Autowired
    private UserRepository userRepository;

    // Acceso al sistema de ficheros local para imagenes y otros recursos
    @Autowired
    private LocalData localData;

    /**
     * Inyecta atributos comunes de sesion en el modelo antes de cada peticion.
     * Esto evita tener que recuperarlos manualmente en cada metodo del controlador.
     */
    @ModelAttribute
    public void populateModel(HttpSession session, Model model) {
        for (String name : new String[] { "u", "url", "ws", "topics" }) {
            model.addAttribute(name, session.getAttribute(name));
        }
    }

    /**
     * Elimina una cola por su ID y redirige al panel de administracion.
     * Si la cola no existe, muestra un mensaje de error al usuario.
     *
     * @param id             ID de la cola a eliminar
     * @param redirectAttrs  atributos para pasar mensajes a la vista tras la redireccion
     */
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

    /**
     * Actualiza los datos editables de una cola existente.
     * Conserva los campos operacionales (turno actual, lista de clientes, etc.)
     * para no perder el estado en curso de la cola.
     * Si se proporciona una imagen, se guarda en disco con el nombre "{id}.jpg".
     *
     * @param id      ID de la cola a actualizar
     * @param cola    objeto Cola con los nuevos datos del formulario
     * @param imagen  imagen opcional para la cola (puede ser null)
     */
    @PostMapping("/panelAdmin/colas/editar/{id}")
    public String actualizarCola(@PathVariable Long id, Cola cola,
            @RequestParam(value = "imagen", required = false) MultipartFile imagen) throws IOException {

        // Recuperar la cola existente para preservar su estado operacional
        Cola colaExistente = colaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cola no encontrada"));

        // Sobreescribir con el ID correcto y restaurar campos que no deben editarse
        cola.setId(id);
        cola.setAbierto(colaExistente.getAbierto());
        cola.setListaClientes(colaExistente.getListaClientes());
        cola.setTurnoActual(colaExistente.getTurnoActual());
        cola.setInicioTurnoActual(colaExistente.getInicioTurnoActual());
        cola.setUltimoTurno(colaExistente.getUltimoTurno());
        cola.setInicioUltimoTurno(colaExistente.getInicioUltimoTurno());
        cola.setFinUltimoTurno(colaExistente.getFinUltimoTurno());
        cola.setFirst(colaExistente.getFirst());
        cola.setLast(colaExistente.getLast());
        cola.setWaiting(colaExistente.getWaiting());

        colaRepository.save(cola);

        // Guardar imagen si se ha subido una
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

    /**
     * Muestra el formulario de edicion de una cola concreta.
     *
     * @param id     ID de la cola a modificar
     * @param model  modelo de la vista
     */
    @GetMapping("/modificar_cola/{id}")
    public String mostrarModificarCola(@PathVariable Long id, Model model) {
        Cola cola = colaRepository.findById(id).orElse(null);
        model.addAttribute("cola", cola);
        return "modificar_cola";
    }

    /**
     * Muestra la vista de seguimiento de colas para el usuario autenticado.
     * Los administradores ven todas las colas; el resto solo las suyas.
     * Tambien calcula el numero maximo de personas en espera por cola
     * para mostrarlo en la vista.
     *
     * @param model  modelo de la vista
     * @param auth   informacion de autenticacion del usuario actual
     */
    @GetMapping("/seguimientoCola")
    public String seguimientoCola(Model model, Authentication auth) {

        // Si el usuario no esta autenticado, mostrar la vista sin datos de cola
        if (auth == null || !auth.isAuthenticated()) {
            model.addAttribute("u", null);
            return "seguimientoCola";
        }

        String username = auth.getName();
        User u = userRepository.findByUsername(username).orElseThrow();

        // Los administradores ven todas las colas; los trabajadores solo las propias
        List<Cola> colas;
        if (u.hasRole(User.Role.ADMIN)) {
            colas = colaRepository.findAll();
        } else {
            colas = colaRepository.findByTrabajadores_Id(u.getId());
        }

        // Calcular el numero de personas en espera para cada cola
        Map<Long, Integer> maxPuestoPorCola = new HashMap<>();
        for (Cola cola : colas) {
            maxPuestoPorCola.put(cola.getId(), cola.getWaiting());
        }

        model.addAttribute("maxPuestoPorCola", maxPuestoPorCola);
        model.addAttribute("colas", colas);
        model.addAttribute("u", u);

        return "seguimientoCola";
    }

    /**
     * Abre una cola para que pueda recibir clientes.
     *
     * @param id  ID de la cola a abrir
     */
    @PostMapping("/colas/{id}/abrir")
    public String abrirCola(@PathVariable Long id) {
        Cola cola = colaRepository.findById(id).orElse(null);
        if (cola != null) {
            cola.abrir();
            colaRepository.save(cola);
        }
        return "redirect:/seguimientoCola";
    }

    /**
     * Cierra una cola para que deje de aceptar nuevos clientes.
     *
     * @param id  ID de la cola a cerrar
     */
    @PostMapping("/colas/{id}/cerrar")
    public String cerrarCola(@PathVariable Long id) {
        Cola cola = colaRepository.findById(id).orElse(null);
        if (cola != null) {
            cola.cerrar();
            colaRepository.save(cola);
        }
        return "redirect:/seguimientoCola";
    }

    /**
     * Devuelve el detalle completo de una cola en formato JSON.
     * Incluye informacion del turno actual, el ultimo turno atendido
     * y la lista de clientes en espera con su posicion y lugar asignado.
     *
     * @param id  ID de la cola
     * @return mapa con todos los campos de estado de la cola
     */
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
        data.put("first", cola.getFirst());
        data.put("last", cola.getLast());
        data.put("waiting", cola.getWaiting());
        data.put("tiempo", cola.getTiempo());

        // Informacion del turno que se esta atendiendo ahora mismo
        if (cola.getInicioTurnoActual() != null) {
            data.put("inicioTurnoActual", cola.getInicioTurnoActual().toString().substring(0, 5));
            long minutos = Duration.between(cola.getInicioTurnoActual(), LocalTime.now()).toMinutes();
            data.put("minutosTurnoActual", minutos);
        } else {
            data.put("inicioTurnoActual", null);
            data.put("minutosTurnoActual", 0);
        }

        // Informacion del ultimo turno ya atendido
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

        // Lista de clientes en espera con su posicion y sala asignada
        data.put("listaClientes", cola.getListaClientes().stream()
                .map(u -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", u.getId());
                    m.put("username", u.getUsername());
                    m.put("posicion", u.getPosicion());
                    m.put("lugar", u.getLugar());
                    return m;
                }).collect(Collectors.toList()));

        return data;
    }

    // Plantilla para enviar mensajes por WebSocket a los clientes suscritos
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Llama al siguiente cliente en la cola y actualiza el estado.
     * Guarda el turno anterior como ultimo atendido, avanza el puntero
     * al siguiente cliente y le asigna la sala indicada.
     * Tras guardar, notifica a todos los suscriptores via WebSocket.
     *
     * @param id    ID de la cola
     * @param sala  sala o ventanilla a la que se llama al siguiente cliente
     * @return 200 OK si la operacion se completa correctamente
     */
    @PostMapping("/colas/{id}/siguiente")
    @ResponseBody
    public ResponseEntity<?> llamarSiguiente(
            @PathVariable long id,
            @RequestParam String sala) {

        Cola cola = colaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // Registrar el turno que acaba de ser atendido
        int actual = cola.getFirst() - 1;
        cola.setUltimoTurno(String.valueOf(actual));
        cola.setInicioUltimoTurno(cola.getInicioTurnoActual());
        cola.setFinUltimoTurno(LocalTime.now());

        // Avanzar al siguiente cliente si hay alguien esperando
        if (cola.getWaiting() > 0) {
            cola.setFirst(cola.getFirst() + 1);
            cola.setWaiting(cola.getWaiting() - 1);
            cola.setInicioTurnoActual(LocalTime.now());

            // Asignar la sala al nuevo turno actual
            int nuevaPosActual = cola.getFirst() - 1;
            cola.getListaClientes().stream()
                .filter(u -> u.getPosicion() == nuevaPosActual)
                .findFirst()
                .ifPresent(u -> {
                    u.setLugar(sala);
                    userRepository.save(u);
                });
        }

        colaRepository.saveAndFlush(cola);

        // Notificar a todos los clientes WebSocket suscritos al topic de esta cola
        messagingTemplate.convertAndSend(
                "/topic/cola/" + id + "/actualizar",
                "{\"colaId\":" + id + ", \"tipo\":\"SIGUIENTE\"}");

        return ResponseEntity.ok().build();
    }

    /**
     * Muestra el panel QR de una cola concreta.
     * Si la cola esta cerrada, redirige a una vista de cola cerrada.
     * En caso contrario, calcula el turno actual y los ultimos 6 atendidos
     * para mostrarlos en el panel.
     *
     * @param id       ID de la cola
     * @param session  sesion HTTP para guardar el ID de la cola activa
     * @param model    modelo de la vista
     */
    @GetMapping("/panelQR/{id}")
    public String mostrarPanelQR(@PathVariable Long id, HttpSession session, Model model) {
        session.setAttribute("panelQrColaId", id);
        Cola cola = colaRepository.findById(id).orElse(null);
        model.addAttribute("cola", cola);

        if (!cola.isAbierto()) {
            return "cola_cerrada";
        }

        // El turno actual corresponde a la posicion "first - 1"
        int posActual = cola.getFirst() - 1;
        Optional<User> turnoActualOpt = cola.getListaClientes().stream()
            .filter(u -> u.getPosicion() == posActual)
            .findFirst();
        User turnoActual = turnoActualOpt.orElse(null);

        // Recuperar los ultimos 6 turnos atendidos (posiciones anteriores al turno actual)
        List<User> atendidos = new ArrayList<>();
        for (int i = posActual - 1; atendidos.size() < 6 && i >= 0; i--) {
            final int pos = i;
            cola.getListaClientes().stream()
                .filter(u -> u.getPosicion() == pos)
                .findFirst()
                .ifPresent(atendidos::add);
        }
        // Rellenar con null si hay menos de 6 turnos atendidos
        while (atendidos.size() < 6) atendidos.add(null);

        model.addAttribute("atendidos", atendidos);
        model.addAttribute("turnoActual", turnoActual);
        return "panelQR";
    }

    /**
     * Recibe y guarda una imagen para una cola concreta.
     * La imagen se almacena en disco con el nombre "{id}.jpg".
     *
     * @param imagen  fichero de imagen subido
     * @param id      ID de la cola
     * @return JSON con el resultado de la operacion
     */
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

    /**
     * Devuelve la imagen asociada a una cola.
     * Si no existe imagen propia, sirve la imagen por defecto del servidor.
     *
     * @param id  ID de la cola
     * @return stream con los bytes de la imagen
     */
    @GetMapping("/colas/{id}/imagen")
    public StreamingResponseBody getImagenCola(@PathVariable long id) throws IOException {
        File f = localData.getFile("cola", id + ".jpg");
        InputStream in = new BufferedInputStream(
                f.exists() ? new FileInputStream(f)
                        : UserController.class.getClassLoader().getResourceAsStream("static/img/default-bg.jpg"));
        return os -> FileCopyUtils.copy(in, os);
    }
}