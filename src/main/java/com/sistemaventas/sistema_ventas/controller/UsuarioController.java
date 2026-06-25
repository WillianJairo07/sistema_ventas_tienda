package com.sistemaventas.sistema_ventas.controller;

import com.sistemaventas.sistema_ventas.model.Usuario;
import com.sistemaventas.sistema_ventas.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.data.domain.Page;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    // ====================================================================
    // PERMITIDO PARA CUALQUIERA: ADMIN y VENDEDOR pueden gestionar su perfil
    // ====================================================================
    @GetMapping("/perfil")
    public String verPerfil(Model model, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return "redirect:/login";

        Usuario usuario = usuarioService.buscarPorUsername(auth.getName());
        model.addAttribute("usuario", usuario);
        return "perfil";
    }

    @PostMapping("/perfil/cambiar-password")
    public String cambiarPasswordMiPerfil(@RequestParam("passwordActual") String passwordActual,
                                          @RequestParam("nuevaPassword") String nuevaPassword,
                                          Authentication auth,
                                          RedirectAttributes flash) {
        if (auth == null || !auth.isAuthenticated()) return "redirect:/login";

        try {
            String usernameLogueado = auth.getName();
            usuarioService.cambiarPasswordPersonal(usernameLogueado, passwordActual, nuevaPassword);
            flash.addFlashAttribute("success", "Contraseña cambiada con éxito.");
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Error interno al cambiar la contraseña.");
        }

        return "redirect:/usuarios/perfil";
    }

    // ====================================================================
    // EXCLUSIVOS PARA ADMIN: El Vendedor será rechazado inmediatamente
    // ====================================================================
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String listar(Model model,
                         @RequestParam(name = "verInactivos", defaultValue = "false") boolean verInactivos,
                         @RequestParam(name = "page", defaultValue = "0") int page,
                         @RequestParam(name = "buscar", required = false) String buscar) {

        int size = 10;
        Page<Usuario> pagina = usuarioService.listarPaginado(!verInactivos, buscar, page, size);

        model.addAttribute("usuarios", pagina.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", pagina.getTotalPages());
        model.addAttribute("buscar", buscar);
        model.addAttribute("verInactivos", verInactivos);

        model.addAttribute("rolesDisponibles", usuarioService.listarRoles());
        model.addAttribute("usuario", new Usuario());
        return "usuarios";
    }

    @PostMapping("/guardar")
    @PreAuthorize("hasRole('ADMIN')")
    public String guardar(@ModelAttribute Usuario usuario,
                          @RequestParam(name = "verInactivos", defaultValue = "false") boolean verInactivos,
                          RedirectAttributes flash) {
        try {
            usuarioService.guardar(usuario);
            flash.addFlashAttribute("success", "Usuario procesado con éxito.");
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Error interno al guardar.");
        }
        return "redirect:/usuarios?verInactivos=" + verInactivos;
    }

    @GetMapping("/eliminar/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String eliminar(@PathVariable Integer id,
                           @RequestParam(name = "verInactivos", defaultValue = "false") boolean verInactivos,
                           RedirectAttributes flash) {
        try {
            Usuario u = usuarioService.buscarPorId(id);
            if (u != null) {
                u.setEstado(false);
                usuarioService.guardar(u);
                flash.addFlashAttribute("success", "Usuario desactivado.");
            }
        } catch (Exception e) {
            flash.addFlashAttribute("error", "No se pudo desactivar.");
        }
        return "redirect:/usuarios?verInactivos=" + verInactivos;
    }

    @GetMapping("/restaurar/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String restaurar(@PathVariable Integer id, RedirectAttributes flash) {
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
    @PreAuthorize("hasRole('ADMIN')")
    public Usuario editar(@PathVariable Integer id) {
        return usuarioService.buscarPorId(id);
    }

    @GetMapping("/reset-password/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String resetPassword(@PathVariable Integer id,
                                @RequestParam String password,
                                @RequestParam(defaultValue = "false") boolean verInactivos,
                                RedirectAttributes flash) {
        try {
            usuarioService.actualizarPassword(id, password);
            flash.addFlashAttribute("success", "Contraseña actualizada correctamente.");
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Error al cambiar la contraseña.");
        }
        return "redirect:/usuarios?verInactivos=" + verInactivos;
    }
}