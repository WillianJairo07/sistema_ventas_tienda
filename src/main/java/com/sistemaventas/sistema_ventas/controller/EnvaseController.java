package com.sistemaventas.sistema_ventas.controller;

import com.sistemaventas.sistema_ventas.model.Envase;
import com.sistemaventas.sistema_ventas.repository.EnvaseRepository;
import com.sistemaventas.sistema_ventas.service.EnvaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.ui.Model;


@Controller
@RequestMapping("/envases")
public class EnvaseController {
    @Autowired private EnvaseService envaseService;
    @Autowired private EnvaseRepository envaseRepository;

    @GetMapping
    public String listar(@RequestParam(defaultValue = "false") boolean verInactivos,
                         @RequestParam(required = false) String buscar,
                         @RequestParam(defaultValue = "0") int page, Model model) {
        Page<Envase> pagina = envaseService.listarPaginado(!verInactivos, page, 10, buscar);
        model.addAttribute("envases", pagina.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", pagina.getTotalPages());
        model.addAttribute("verInactivos", verInactivos);
        model.addAttribute("buscar", buscar);
        model.addAttribute("envase", new Envase());
        return "envases";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Envase envase, RedirectAttributes flash) {
        try { envaseService.guardar(envase); flash.addFlashAttribute("success", "Guardado con éxito"); }
        catch (Exception e) { flash.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/envases";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id, RedirectAttributes flash) {
        envaseService.eliminar(id);
        flash.addFlashAttribute("success", "Enviado a inactivos");
        return "redirect:/envases";
    }

    @GetMapping("/editar/{id}")
    @ResponseBody
    public Envase editar(@PathVariable Integer id) {
        return envaseRepository.findById(id).orElse(null);
    }

    @GetMapping("/restaurar/{id}")
    public String restaurar(@PathVariable Integer id, RedirectAttributes flash) {
        try {
            envaseService.restaurar(id);
            flash.addFlashAttribute("success", "Envase restaurado con éxito.");
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Error al restaurar el envase: " + e.getMessage());
        }
        // Redirige de vuelta a la lista, manteniendo el filtro de inactivos
        return "redirect:/envases?verInactivos=true";
    }
}
