package es.ucm.fdi.iw.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 *  Non-authenticated requests only.
 */
@Controller
public class RootController {

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

    @GetMapping("/vista1")
    public String vista1(Model model) {
        return "vista1";
    }

    @GetMapping("/vista2")
    public String vista2(Model model) {
        return "vista2";
    }

    @GetMapping("/vista3")
    public String vista3(Model model) {
        return "vista3";
    }

    @GetMapping("/vista4")
    public String vista4(Model model) {
        return "vista4";
    }

    @GetMapping("/vista5")
    public String vista5(Model model) {
        return "vista5";
    }
}
