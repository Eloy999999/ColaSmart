package es.ucm.fdi.iw.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import es.ucm.fdi.iw.model.Cola;
import es.ucm.fdi.iw.model.ColaRepository;
import es.ucm.fdi.iw.model.Lorem;
import es.ucm.fdi.iw.model.Message;
import es.ucm.fdi.iw.model.Topic;
import es.ucm.fdi.iw.model.Transferable;
import es.ucm.fdi.iw.model.User;
import es.ucm.fdi.iw.model.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;

/**
 * Controlador para la administración del sitio.
 * Acceso autenticado: solo admins
 */
@Controller
@RequestMapping("panelAdmin")
public class AdminController {

    // Codifica contraseñas
    @Autowired
    private PasswordEncoder passwordEncoder;
    // Manejo manual de entidades JPA
    @Autowired
    private EntityManager entityManager;
    // Repositorio de colas
    @Autowired
    private ColaRepository colaRepository;
    // Repositorio de usuarios
    @Autowired
    private UserRepository userRepository;
    // Envío de mensajes WebSocket
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private static final Logger log = LogManager.getLogger(AdminController.class);

    // Añade el usuario actual al modelo
    @ModelAttribute
    public void populateModel(HttpSession session, Model model) {
        model.addAttribute("u", session.getAttribute("u"));
    }

    // Muestra el panel principal
    @GetMapping({ "", "/" })
    public String panelAdmin(Model model, HttpSession session) {
        log.info("Admin entra a panelAdmin");
        List<Cola> colas = colaRepository.findAll();
        List<User> users = userRepository.findAll();

        // Filtra solo pacientes
        List<User> pacientes = users.stream()
                .filter(u -> u.hasRole(User.Role.PACIENTE))
                .collect(Collectors.toList());

        // Relaciona paciente con su cola
        Map<Long, Cola> colaDelPaciente = new java.util.HashMap<>();
        for (Cola cola : colas) {
            if (cola.getListaClientes() != null) {
                for (User cliente : cola.getListaClientes()) {
                    if (cliente.hasRole(User.Role.PACIENTE)) {
                        colaDelPaciente.put(cliente.getId(), cola);
                    }
                }
            }
        }

        // Guarda el tamaño de cada cola
        Map<Long, Integer> maxPuestoPorCola = new HashMap<>();
        for (Cola cola : colas) {
            maxPuestoPorCola.put(cola.getId(), cola.getWaiting());
        }

        // Envía datos a la vista
        model.addAttribute("colas", colas);
        model.addAttribute("users", users);
        model.addAttribute("pacientes", pacientes);
        model.addAttribute("colaDelPaciente", colaDelPaciente);
        model.addAttribute("maxPuestoPorCola", maxPuestoPorCola);

        List<Map<String, String>> puestosActivos = new java.util.ArrayList<>();
        java.util.Set<String> puestosRegistrados = new java.util.HashSet<>();
        java.time.LocalTime ahora = java.time.LocalTime.now();

        for (Cola cola : colas) {
            // Solo procesamos colas abiertas que tengan clientes
            if (cola.isAbierto() && cola.getListaClientes() != null) {
                
                String nombrePersonal = "Sin asignar";
                if (cola.getTrabajadores() != null && !cola.getTrabajadores().isEmpty()) {
                    User medico = cola.getTrabajadores().get(0);
                    nombrePersonal = medico.getFirstName() + " " + medico.getLastName();
                }

                //Prioridad a la hora del turno actual
                java.time.LocalTime tiempoReferencia = (cola.getInicioTurnoActual() != null) 
                        ? cola.getInicioTurnoActual() 
                        : ahora;

                // Mostrar el puesto que está atendiendo en ese momento
                int posActual = cola.getFirst() - 1;
                User usuarioActual = cola.getListaClientes().stream()
                        .filter(u -> u.getPosicion() == posActual)
                        .findFirst()
                        .orElse(null);

                if (usuarioActual != null && usuarioActual.getLugar() != null) {
                    String puestoActual = usuarioActual.getLugar();
                    
                    if (!puestosRegistrados.contains(puestoActual)) {
                        puestosRegistrados.add(puestoActual);
                        
                        Map<String, String> fila = new HashMap<>();
                        fila.put("puesto", puestoActual);
                        fila.put("cola", cola.getNombre());
                        fila.put("lugar", cola.getLugar() != null ? cola.getLugar() : "-");
                        fila.put("personal", nombrePersonal);
                        fila.put("estado", "Atendiendo ahora");
                        puestosActivos.add(fila);
                    }
                }

                // Mostrar el puesto anterior si entró en los 10 minutos previos
                if (cola.getFinUltimoTurno() != null) {
                    // Calculamos la distancia entre el fin del turno anterior y el inicio del actual
                    long minutosTranscurridos = java.time.Duration.between(cola.getFinUltimoTurno(), tiempoReferencia).toMinutes();
                    
                    // Si el hueco de tiempo entre turnos fue de 10 minutos o menos
                    if (minutosTranscurridos >= 0 && minutosTranscurridos <= 10) {
                        
                        int posAnterior = posActual - 1;
                        User usuarioAnterior = cola.getListaClientes().stream()
                                .filter(u -> u.getPosicion() == posAnterior)
                                .findFirst()
                                .orElse(null);

                        if (usuarioAnterior != null && usuarioAnterior.getLugar() != null) {
                            String puestoAnterior = usuarioAnterior.getLugar();
                            
                            if (!puestosRegistrados.contains(puestoAnterior)) {
                                puestosRegistrados.add(puestoAnterior);
                                
                                Map<String, String> fila = new HashMap<>();
                                fila.put("puesto", puestoAnterior);
                                fila.put("cola", cola.getNombre());
                                fila.put("lugar", cola.getLugar() != null ? cola.getLugar() : "-");
                                fila.put("personal", nombrePersonal);
                                fila.put("estado", "Activo reciente (" + minutosTranscurridos + " min de margen)");
                                puestosActivos.add(fila);
                            }
                        }
                    }
                }
            }
        }

        model.addAttribute("puestosActivos", puestosActivos);
        return "panelAdmin";
    }

    // Cargar los datos de pacientes y colas en el refresco
    @GetMapping("/panelAdmin/datos")
    @ResponseBody
    public Map<String, Object> datosAdmin() {

        List<User> pacientes = userRepository.findAll();
        List<Cola> colas = colaRepository.findAll();

        return Map.of(
            "pacientes", pacientes,
            "colas", colas
        );
    }

    // Activa o desactiva un usuario
    @PostMapping("/toggle/{id}")
    @Transactional
    @ResponseBody
    public String toggleUser(@PathVariable long id) {
        User target = entityManager.find(User.class, id);
        if (target != null) {
            target.setEnabled(!target.isEnabled());
            return "{\"enabled\":" + target.isEnabled() + "}";
        }
        return "{\"error\":\"Usuario no encontrado\"}";
    }

    // Retorna últimos 5 mensajes como JSON
    @GetMapping(path = "all-messages", produces = "application/json")
    @Transactional
    @ResponseBody
    public List<Message.Transfer> retrieveMessages() {
        TypedQuery<Message> query = entityManager.createQuery("SELECT m FROM Message m", Message.class);
        query.setMaxResults(5);
        return query.getResultList().stream().map(Transferable::toTransfer).collect(Collectors.toList());
    }

    // Crea una nueva cola
    @PostMapping("/colas")
    @Transactional
    public String crearCola(@ModelAttribute Cola nuevaCola) {

        // Inicializa datos básicos
        nuevaCola.setQrToken(java.util.UUID.randomUUID().toString());
        nuevaCola.setFirst(1);
        nuevaCola.setLast(0);
        nuevaCola.setWaiting(0);
        entityManager.persist(nuevaCola);
        return "redirect:/panelAdmin?modal=listas";
    }

    // Crear nuevo personal
    @PostMapping("/personal")
    @Transactional
    public String crearPersonal(@ModelAttribute User nuevoPersonal, @RequestParam("rol") String rol) {
        // Cifra la contraseña
        nuevoPersonal.setPassword(passwordEncoder.encode(nuevoPersonal.getPassword()));
        nuevoPersonal.setEnabled(true);
        // Asigna rol de organizador
        nuevoPersonal.setRoles(rol);
        nuevoPersonal.setNumSesiones(0);
        entityManager.persist(nuevoPersonal);
        return "redirect:/panelAdmin?modal=personal";
    }

    // Abrir/Cerrar cola
    @PostMapping("/colas/{id}/toggle")
    @Transactional
    @ResponseBody
    public Map<String, String> toggleCola(@PathVariable long id, HttpServletResponse response) {
        Cola cola = entityManager.find(Cola.class, id);
        // Comprueba si existe
        if (cola == null) {
            response.setStatus(404);
            return Map.of("error", "Cola no encontrada");
        }
        cola.abrir();
        return Map.of("estado", cola.getEstado().name());
    }

    // Eliminar cola
    @PostMapping("/colas/eliminar/{id}")
    public String eliminarCola(@PathVariable Long id) {
        if (colaRepository.existsById(id)) {
            colaRepository.deleteById(id);
        }
        return "redirect:/panelAdmin?modal=listas";
    }

    // Eliminar personal
    @PostMapping("/personal/eliminar/{id}")
    public String eliminarPersonal(@PathVariable Long id,
            @RequestParam(required = false, defaultValue = "/panelAdmin?modal=personal") String redirect) {

        if (userRepository.existsById(id)) {
            // Elimina al usuario de las colas
            for (Cola cola : colaRepository.findAll()) {
                boolean estaba = cola.getListaClientes().removeIf(u -> u.getId() == id);
                if (estaba) {
                    colaRepository.save(cola);
                    // Notifica actualización
                    messagingTemplate.convertAndSend(
                            "/topic/cola/" + cola.getId() + "/actualizar",
                            "{\"colaId\":" + cola.getId() + ", \"tipo\":\"ABANDONAR\"}");
                }
            }
            userRepository.deleteById(id);
        }
        return "redirect:" + redirect;
    }

    // Eliminar Pacientes
    @PostMapping("/pacientes/eliminar/{id}")
    public String eliminarPacientes(@PathVariable Long id,
            @RequestParam(required = false, defaultValue = "/panelAdmin?modal=usuarios") String redirect) {

        User paciente = userRepository.findById(id).orElse(null);

        if (paciente != null) {

            List<Cola> colas = colaRepository.findAll();

            Integer posicionPaciente = paciente.getPosicion();

            // Lo elimina de su cola
            for (Cola cola : colas) {
                if (cola.getListaClientes() != null &&
                        cola.getListaClientes().contains(paciente)) {

                    cola.getListaClientes().remove(paciente);

                    if (posicionPaciente < cola.getFirst() -1){ // En el registro de los usuarios ya atendidos
                        for (User usuario : cola.getListaClientes()) {
                            if (usuario.getPosicion() < posicionPaciente) {
                                usuario.setPosicion(usuario.getPosicion() + 1);
                                userRepository.save(usuario);
                            }
                        }
                    }else{ // en cola o siendo atendido
                        if (posicionPaciente == cola.getFirst() - 1 && cola.getWaiting() == 0) { // si estaba siendo atendido y no hay nadie más detrás
                            cola.setFirst(cola.getFirst() - 1);
                            cola.setLast(cola.getLast() - 1);
                            cola.setUltimoTurno(String.valueOf(Integer.parseInt(cola.getUltimoTurno()) - 1));
                        }else{// en otro caso
                            /*
                            if (posicionPaciente == cola.getFirst() || posicionPaciente == cola.getFirst() -1) { // si era primero o estaba siendo atendido ya, la posicion del nuevo primero es la siguiente
                                cola.setFirst(cola.getFirst() + 1);
                            }
                            */
                            for (User usuario : cola.getListaClientes()) {
                                if (usuario.getPosicion() > posicionPaciente) {
                                    usuario.setPosicion(usuario.getPosicion() - 1);
                                    userRepository.save(usuario);
                                }
                            }
                            cola.setWaiting(cola.getWaiting() - 1);
                            cola.setLast(cola.getLast() - 1);
                        }
                        colaRepository.save(cola);
                    }

                    // Incluido WebSocket para notificar a los usuarios en tiempo real en la vista tuTurno de cuándo otro usuario abandona la cola
                        messagingTemplate.convertAndSend(
                            "/topic/cola/" + cola.getId() + "/actualizar",
                            "{\"colaId\":" + cola.getId() + ", \"tipo\":\"ABANDONO\"}");
                }
            }

            userRepository.delete(paciente);
        }

        return "redirect:" + redirect;
    }

    // Método opcional para poblar DB con datos de prueba
    @RequestMapping("/populate")
    @ResponseBody
    @Transactional
    public String populate() {
        // Crea temas de prueba
        Topic g1 = new Topic();
        g1.setName("g1");
        g1.setKey(UserController.generateRandomBase64Token(6));
        entityManager.persist(g1);
        Topic g2 = new Topic();
        g2.setName("g2");
        g2.setKey(UserController.generateRandomBase64Token(6));
        entityManager.persist(g2);

        // Crea usuarios aleatorios
        for (int i = 0; i < 15; i++) {
            User u = new User();
            u.setUsername("user" + i);
            u.setPassword(passwordEncoder.encode("aa"));
            u.setEnabled(true);
            u.setRoles(User.Role.PACIENTE.toString());
            u.setFirstName(Lorem.nombreAlAzar());
            u.setLastName(Lorem.apellidoAlAzar());
            entityManager.persist(u);
            // Añade usuarios a grupos
            if (i % 2 == 0)
                g1.getMembers().add(u);
            if (i % 3 == 0)
                g2.getMembers().add(u);
        }
        return "{\"admin\": \"populated\"}";
    }

    // Genera tokens para colas sin token
    @GetMapping("/colas/inicializar-tokens")
    @Transactional
    @ResponseBody
    public String inicializarTokensColas() {
        List<Cola> colas = colaRepository.findAll();

        for (Cola cola : colas) {
            if (cola.getQrToken() == null || cola.getQrToken().isBlank()) {
                cola.setQrToken(java.util.UUID.randomUUID().toString());
            }
        }

        return "Tokens inicializados correctamente";
    }

    // Devuelve nombres aleatorios
    @GetMapping("/personal/lorem")
    @ResponseBody
    public Map<String, String> loremPersonal() {
        Map<String, String> datos = new HashMap<>();
        datos.put("firstName", Lorem.nombreAlAzar());
        datos.put("lastName", Lorem.apellidoAlAzar() + " " + Lorem.apellidoAlAzar());
        return datos;
    }

}