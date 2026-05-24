package es.ucm.fdi.iw.controller;

import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import es.ucm.fdi.iw.model.Cola;
import es.ucm.fdi.iw.model.ColaRepository;
import es.ucm.fdi.iw.model.User;
import es.ucm.fdi.iw.model.UserRepository;
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
     * Muestra el panel QR de la cola por defecto (supermercado).
     * Usa una cola por defecto y prepara en el modelo:
     * - la cola
     * - los últimos atendidos
     * - el turno actual
     * Si la cola está cerrada, redirige a una vista específica.
     */
    @GetMapping("/panelQR")
    public String panelQR(Model model) {
        long idDefault = 975;
        Cola cola = colaRepository.findById(idDefault).orElse(null);
        model.addAttribute("cola", cola); // Le meto la cola para poder sacar de ahi toda la info de esta, y meter al
                                          // user nuevo a esta cola

        if (!cola.isAbierto()) { // Si esta cerrada la cola, poner que la cola esta cerrada
            model.addAttribute("cola", cola);
            return "cola_cerrada";
        }

        // Coger posiciones de -6 a -1 por cola, los usuarios atendidos en posiciones recientes de la cola
        List<User> atendidos = userRepository.findAtendidosByColaId(idDefault, -6, -1);

        // Rellena con null hasta tener 6 elementos para la vista
        while (atendidos.size() < 6) {
            atendidos.add(null);
        }

        // Turno actual por cola
        Optional<User> turnoActualOpt = userRepository.findTurnoActualByColaId(idDefault,0);
        User turnoActual = turnoActualOpt.orElse(null);

        model.addAttribute("atendidos", atendidos);
        model.addAttribute("turnoActual", turnoActual);

        return "panelQR";
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
