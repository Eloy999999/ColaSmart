package es.ucm.fdi.iw.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.time.Duration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import es.ucm.fdi.iw.model.AtencionLogRepository;
import es.ucm.fdi.iw.model.Cola;
import es.ucm.fdi.iw.model.ColaRepository;
import es.ucm.fdi.iw.model.User;
import es.ucm.fdi.iw.model.UserRepository;
import es.ucm.fdi.iw.model.Message;
import es.ucm.fdi.iw.model.MessageRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Non-authenticated requests only.
 * Controlador para peticiones que no requieren autenticación previa.
 * Se encarga de devolver vistas públicas y de preparar datos comunes en el modelo.
 */
@Controller
public class RootController {

    @Autowired
    private ColaRepository colaRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AtencionLogRepository atencionLogRepository;

    @Autowired
    private MessageRepository messageRepository;

    private static final Logger log = LogManager.getLogger(RootController.class);

    /**
     * Añade al modelo algunos atributos que pueden estar guardados en la sesión.
     * Así se reutilizan en distintas vistas sin repetir código.
     */
    @ModelAttribute
    public void populateModel(HttpSession session, Model model) {
        for (String name : new String[] { "u", "url", "ws", "topics" }) {
            model.addAttribute(name, session.getAttribute(name));
        }
    }

    /**
     * Muestra la página de login.
     * Si la URL contiene "error", se marca en el modelo para mostrar el mensaje correspondiente.
     */
    @GetMapping("/login")
    public String login(Model model, HttpServletRequest request) {
        boolean error = request.getQueryString() != null && request.getQueryString().indexOf("error") != -1;
        model.addAttribute("loginError", error);
        return "login";
    }

    /**
     * Muestra la página principal.
     */
    @GetMapping("/")
    public String index(Model model) {

        LocalDateTime ultimas24h = LocalDateTime.now().minusHours(24);

        long atendidosTotal = atencionLogRepository.countByHoraFinAtencionIsNotNull();

        long atendidosUltimoDia = atencionLogRepository.countByHoraFinAtencionIsNotNullAndHoraFinAtencionAfter(ultimas24h);

        long genteEnColasAhora = colaRepository.findAll().stream()
                .mapToLong(Cola::getWaiting)
                .sum();

        long numeroColas = colaRepository.count();

        model.addAttribute("atendidosTotal", atendidosTotal);
        model.addAttribute("atendidosUltimoDia", atendidosUltimoDia);
        model.addAttribute("genteEnColasAhora", genteEnColasAhora);
        model.addAttribute("numeroColas", numeroColas);

        return "index";
    }

    /**
     * Devuelve la página con información de autores.
     */
    @GetMapping("/autor")
    public String autor(Model model) {
        return "autor"; // página con autores
    }

    /**
        Pregunta C: muestra “/” no PanelQR/975 como default
     */
    @GetMapping("/panelQR")
    public String panelQR(Model model) {
        return "redirect:/";
    }

    /**
     * Muestra la pantalla "tuTurno".
     * Recupera de sesión la cola temporal y el usuario temporal para enseñar su posición.
     */
    @GetMapping("/tuTurno")
    public String tuTurno(HttpSession session, Model model) {

        Long colaIdNum = (Long) session.getAttribute("colaId");
        Long userIdNum = (Long) session.getAttribute("userId");

        if (colaIdNum == null || userIdNum == null) {
            return "redirect:/";
        }

        Long colaId = colaIdNum.longValue();
        Long userId = userIdNum.longValue();

        Cola cola = colaRepository.findById(colaId).orElseThrow();
        User user = userRepository.findById(userId).orElseThrow();

        List<Message> mensajesDeLaCola = messageRepository.findByColaOrderByDateSentDesc(cola);
        Message mensajeActivo = null;

        long minutosRestantesMensaje = 0;

        for (Message m : mensajesDeLaCola) {
            if (m.getMinutesExpiration() == 0) {
                // El organizador puso 0: Nunca expira
                mensajeActivo = m;
                minutosRestantesMensaje = -1; // Usamos -1 para "infinito"
                break; 
            } else {
                // Calculamos cuánto tiempo ha pasado desde que se envió
                Duration transcurrido = Duration.between(m.getDateSent(), java.time.LocalDateTime.now());
                long minutosQueLlevaActivo = transcurrido.toMinutes();

                // Si lleva menos minutos activo de lo que configuró el organizador
                if (minutosQueLlevaActivo < m.getMinutesExpiration()) {
                    mensajeActivo = m;
                    //Restamos el total de expiración menos los minutos que ya han pasado
                    minutosRestantesMensaje = m.getMinutesExpiration() - minutosQueLlevaActivo;
                    break;
                }
            }
        }

        model.addAttribute("mensajeAviso", mensajeActivo);
        model.addAttribute("minutosRestantesAviso", minutosRestantesMensaje);

        model.addAttribute("cola", cola);
        model.addAttribute("user", user);

        return "tuTurno";
    }

    /**
     * Devuelve la vista de configuración de colas.
     */
    @GetMapping("/configCola")
    public String configCola(Model model) {
        return "configCola";
    }
    /*
     * @GetMapping("/vista4")
     * public String vista4(Model model) {
     * return "vista4";
     * }
     */

    /*
     * Explota si se descomenta esto, porque en admin ya hay un GetMapping a vista5
     * 
     * @GetMapping("/vista5")
     * public String vista5(Model model) {
     * return "vista5";
     * }
     */

    /**
     * Devuelve la vista para manejar personal.
     */
    @GetMapping("/Manejar Personal")
    public String vista6(Model model) {
        return "Manejar Personal";
    }
}
