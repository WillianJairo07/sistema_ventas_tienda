package com.sistemaventas.sistema_ventas.controller;

import com.sistemaventas.sistema_ventas.model.Usuario;
import com.sistemaventas.sistema_ventas.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping
    public String listar(Model model, @RequestParam(name = "verInactivos", required = false) Boolean verInactivos) {
        boolean mostrarInactivos = (verInactivos != null && verInactivos);
        model.addAttribute("usuarios", usuarioService.listarPorEstado(!mostrarInactivos));
        model.addAttribute("rolesDisponibles", usuarioService.listarRoles());
        model.addAttribute("usuario", new Usuario());
        return "usuarios";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Usuario usuario, org.springframework.web.servlet.mvc.support.RedirectAttributes flash) {
        try {
            usuarioService.guardar(usuario);
            flash.addFlashAttribute("success", "Usuario procesado con éxito.");
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Error interno al guardar.");
        }
        return "redirect:/usuarios";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id, org.springframework.web.servlet.mvc.support.RedirectAttributes flash) {
        try {
            Usuario u = usuarioService.buscarPorId(id);
            if (u != null) {
                u.setEstado(false);
                usuarioService.guardar(u); // Esto usará el guardado lógico
                flash.addFlashAttribute("success", "Usuario desactivado.");
            }
        } catch (Exception e) {
            flash.addFlashAttribute("error", "No se pudo desactivar.");
        }
        return "redirect:/usuarios";
    }

    @GetMapping("/restaurar/{id}")
    public String restaurar(@PathVariable Integer id, org.springframework.web.servlet.mvc.support.RedirectAttributes flash) {
        try {
            usuarioService.restaurar(id);
            flash.addFlashAttribute("success", "Usuario restaurado correctamente.");
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Error al restaurar usuario.");
        }
        return "redirect:/usuarios";
    }

    @GetMapping("/editar/{id}")
    @ResponseBody
    public Usuario editar(@PathVariable Integer id) {
        return usuarioService.buscarPorId(id);
    }
}