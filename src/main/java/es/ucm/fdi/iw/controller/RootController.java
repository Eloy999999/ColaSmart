package es.ucm.fdi.iw.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

import es.ucm.fdi.iw.model.Cola;
import es.ucm.fdi.iw.model.ColaRepository;
import es.ucm.fdi.iw.model.User;
import es.ucm.fdi.iw.model.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 *  Non-authenticated requests only.
 */
@Controller
public class RootController {

    @Autowired
    private ColaRepository colaRepository;

    @Autowired
    private UserRepository userRepository;

    private static final Logger log = LogManager.getLogger(RootController.class);

    @ModelAttribute
    public void populateModel(HttpSession session, Model model) {        
        for (String name : new String[] { "u", "url", "ws", "topics"}) {
          model.addAttribute(name, session.getAttribute(name));
        }
    }

	@GetMapping("/login")
    public String login(Model model, HttpServletRequest request) {
        boolean error = request.getQueryString() != null && request.getQueryString().indexOf("error") != -1;
        model.addAttribute("loginError", error);
        return "login";
    }

	@GetMapping("/")
    public String index(Model model) {
        return "index";
    }

     @GetMapping("/autor")
    public String autor(Model model) {
        return "autor";   // página con autores
    }

    @GetMapping("/panelQR")
    public String panelQR(Model model) {
        long idDefault = 975;
        Cola cola = colaRepository.findById(idDefault).orElse(null);
        model.addAttribute("cola", cola); // Le meto la cola para poder sacar de ahi toda la info de esta, y meter al user nuevo a esta cola
        
        if (!cola.isAbierto()) { //Si esta cerrada la cola, poner que la cola esta cerrada
            model.addAttribute("cola", cola);
            return "cola_cerrada";
        }
        
        return "panelQR";
    }

    @GetMapping("/tuTurno")
        public String tuTurno(HttpSession session, Model model) {

        Cola cola = (Cola) session.getAttribute("colaTemporal");
        User user = (User) session.getAttribute("usuarioTemporal");
        int posicion = cola.getListaClientes().indexOf(user) + 1;
        model.addAttribute("posicion", posicion);
        model.addAttribute("cola", cola);
        model.addAttribute("user", user);

        return "tuTurno";
    }

    @GetMapping("/configCola")
    public String configCola(Model model) {
        return "configCola";
    }
    /* 
    @GetMapping("/vista4")
    public String vista4(Model model) {
        return "vista4";
    }*/

    /* Explota si se descomenta esto, porque en admin ya hay un GetMapping a vista5
    @GetMapping("/vista5")
    public String vista5(Model model) {
        return "vista5";
    }*/

    @GetMapping("/Manejar Personal")
    public String vista6(Model model) {
        return "Manejar Personal";
    }
}
