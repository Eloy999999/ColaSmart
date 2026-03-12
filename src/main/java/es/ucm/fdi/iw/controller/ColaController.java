package es.ucm.fdi.iw.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import es.ucm.fdi.iw.model.ColaRepository;

@Controller
public class ColaController {

    private final ColaRepository colaRepository;

    public ColaController(ColaRepository colaRepository) {
        this.colaRepository = colaRepository;
    }

    @PostMapping("/colas/eliminar/{id}")
    public String eliminarCola(@PathVariable("id") Long id, RedirectAttributes redirectAttrs) {
        if (colaRepository.existsById(id)) {
            colaRepository.deleteById(id);
            redirectAttrs.addFlashAttribute("msg", "Cola eliminada correctamente");
        } else {
            redirectAttrs.addFlashAttribute("msg", "La cola no existe");
        }
        return "redirect:/vista5"; // página donde vuelves después de eliminar
    }
}