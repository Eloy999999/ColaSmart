package es.ucm.fdi.iw.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ColaRepository colaRepository;

    @Autowired
    private UserRepository userRepository;

    private static final Logger log = LogManager.getLogger(AdminController.class);

    // Popula datos comunes
    @ModelAttribute
    public void populateModel(HttpSession session, Model model) {
        model.addAttribute("u", session.getAttribute("u"));
    }

    // GET principal que carga colas y usuarios
    @GetMapping({ "", "/" })
    public String panelAdmin(Model model, HttpSession session) {
        log.info("Admin entra a panelAdmin");
        List<Cola> colas = colaRepository.findAll();
        List<User> users = userRepository.findAll();

        List<User> pacientes = users.stream()
                .filter(u -> u.hasRole(User.Role.PACIENTE))
                .collect(Collectors.toList());

        // Mapa pacienteId -> Cola
        Map<Long, Cola> colaDelPaciente = new java.util.HashMap<>();
        for (Cola cola : colas) {
            if (cola.getListaClientes() != null) {
                for (User cliente : cola.getListaClientes()) {
                    colaDelPaciente.put(cliente.getId(), cola);
                }
            }
        }

        model.addAttribute("colas", colas);
        model.addAttribute("users", users);
        model.addAttribute("pacientes", pacientes);
        model.addAttribute("colaDelPaciente", colaDelPaciente);
        return "panelAdmin";
    }

    // Toggle estado de usuario
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

    // Crear nueva cola
    @PostMapping("/colas")
    @Transactional
    public String crearCola(@ModelAttribute Cola nuevaCola) {
        entityManager.persist(nuevaCola);
        return "redirect:/panelAdmin?modal=listas";
    }

    // Crear nuevo personal
    @PostMapping("/personal")
    @Transactional
    public String crearPersonal(@ModelAttribute User nuevoPersonal) {
        nuevoPersonal.setPassword(passwordEncoder.encode(nuevoPersonal.getPassword()));
        nuevoPersonal.setEnabled(true);
        nuevoPersonal.setRoles(User.Role.ORGANIZADOR.toString());
        entityManager.persist(nuevoPersonal);
        return "redirect:/panelAdmin?modal=personal";
    }

    // Abrir/Cerrar cola
    @PostMapping("/colas/{id}/toggle")
    @Transactional
    @ResponseBody
    public Map<String, String> toggleCola(@PathVariable long id, HttpServletResponse response) {
        Cola cola = entityManager.find(Cola.class, id);
        if (cola == null) {
            response.setStatus(404);
            return Map.of("error", "Cola no encontrada");
        }
        cola.abrir(); // O cerrar según tu lógica
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
    public String eliminarPersonal(@PathVariable Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
        }
        return "redirect:/panelAdmin?modal=personal";
    }

    // Eliminar Pacientes
    @PostMapping("/pacientes/eliminar/{id}")
    public String eliminarPacientes(@PathVariable Long id) {

        User paciente = userRepository.findById(id).orElse(null);

        if (paciente != null) {

            List<Cola> colas = colaRepository.findAll();

            for (Cola cola : colas) {
                if (cola.getListaClientes() != null &&
                        cola.getListaClientes().contains(paciente)) {

                    cola.getListaClientes().remove(paciente);
                    Cola.adelantarPacientesDetrasUno(cola, paciente.getPosicion());
                    colaRepository.save(cola);
                }
            }

            userRepository.delete(paciente);
        }

        return "redirect:/panelAdmin?modal=usuarios";
    }

    // Método opcional para poblar DB con datos de prueba
    @RequestMapping("/populate")
    @ResponseBody
    @Transactional
    public String populate() {
        Topic g1 = new Topic();
        g1.setName("g1");
        g1.setKey(UserController.generateRandomBase64Token(6));
        entityManager.persist(g1);
        Topic g2 = new Topic();
        g2.setName("g2");
        g2.setKey(UserController.generateRandomBase64Token(6));
        entityManager.persist(g2);

        for (int i = 0; i < 15; i++) {
            User u = new User();
            u.setUsername("user" + i);
            u.setPassword(passwordEncoder.encode("aa"));
            u.setEnabled(true);
            u.setRoles(User.Role.PACIENTE.toString());
            u.setFirstName(Lorem.nombreAlAzar());
            u.setLastName(Lorem.apellidoAlAzar());
            entityManager.persist(u);
            if (i % 2 == 0)
                g1.getMembers().add(u);
            if (i % 3 == 0)
                g2.getMembers().add(u);
        }
        return "{\"admin\": \"populated\"}";
    }

}