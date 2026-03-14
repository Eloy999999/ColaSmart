package es.ucm.fdi.iw.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import es.ucm.fdi.iw.model.Cola;
import es.ucm.fdi.iw.model.ColaRepository;
import jakarta.servlet.http.HttpSession;

@Controller
public class ColaController {

@Autowired
private ColaRepository colaRepository;

    @ModelAttribute // añadir sesion al modelo que estamos pasando
    public void populateModel(HttpSession session, Model model) {        
        for (String name : new String[] { "u", "url", "ws", "topics"}) {
            model.addAttribute(name, session.getAttribute(name));
        }
    }

    @PostMapping("/colas/eliminar/{id}") // liminiar cola por id y redirigir a vista de admin
    public String eliminarCola(@PathVariable("id") Long id, RedirectAttributes redirectAttrs) {
        if (colaRepository.existsById(id)) {
            colaRepository.deleteById(id);
            redirectAttrs.addFlashAttribute("msg", "Cola eliminada correctamente");
        } else {
            redirectAttrs.addFlashAttribute("msg", "La cola no existe");
        }
        return "redirect:/vista5";
    }

    @PostMapping("/vista5/colas/editar/{id}") // Guardar los cambios en la cola del id y redirigir a vista de admin
    public String actualizarCola(@PathVariable Long id, Cola cola) {
    cola.setId(id);
    colaRepository.save(cola);
    return "redirect:/vista5";
    }

    @GetMapping("/modificar_cola/{id}") // Que el programa lleve a la vista de modificar cola
    public String mostrarModificarCola(@PathVariable Long id, Model model) {
        Cola cola = colaRepository.findById(id).orElse(null);
        model.addAttribute("cola", cola);
        return "modificar_cola";
    }
}