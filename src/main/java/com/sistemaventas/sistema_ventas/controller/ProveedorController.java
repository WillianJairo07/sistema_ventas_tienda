package com.sistemaventas.sistema_ventas.controller;

import com.sistemaventas.sistema_ventas.model.Proveedor;
import com.sistemaventas.sistema_ventas.service.ProveedorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/proveedores")
public class ProveedorController {

    @Autowired
    private ProveedorService service;

    @GetMapping
    public String listar(
            @RequestParam(name = "verInactivos", defaultValue = "false") boolean verInactivos,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "buscar", required = false) String buscar,
            Model model) {

        int size = 10;
        boolean estadoABuscar = !verInactivos;

        Page<Proveedor> paginaProveedores = service.listarPaginado(estadoABuscar, buscar, page, size);

        model.addAttribute("proveedores", paginaProveedores.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", paginaProveedores.getTotalPages());
        model.addAttribute("verInactivos", verInactivos);
        model.addAttribute("buscar", buscar);

        model.addAttribute("proveedor", new Proveedor());
        return "proveedores";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Proveedor proveedor,
                          @RequestParam(name = "verInactivos", defaultValue = "false") boolean verInactivos,
                          RedirectAttributes flash) {
        try {
            service.guardar(proveedor);
            flash.addFlashAttribute("success", "Proveedor procesado correctamente.");
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Error inesperado al guardar el proveedor.");
        }
        // Redirigimos manteniendo el estado de la vista (Activos o Inactivos)
        return "redirect:/proveedores" + (verInactivos ? "?verInactivos=true" : "");
    }

    @GetMapping("/restaurar/{id}")
    public String restaurar(@PathVariable Integer id, RedirectAttributes flash) {
        try {
            service.restaurar(id);
            flash.addFlashAttribute("success", "Proveedor restaurado con éxito.");
            return "redirect:/proveedores";
        } catch (Exception e) {
            flash.addFlashAttribute("error", "No se pudo restaurar el proveedor.");
            return "redirect:/proveedores?verInactivos=true";
        }
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id,
                           @RequestParam(name = "verInactivos", defaultValue = "false") boolean verInactivos,
                           RedirectAttributes flash) {
        try {
            service.eliminar(id);
            flash.addFlashAttribute("success", "El proveedor ha sido enviado a inactivos.");
        } catch (Exception e) {
            flash.addFlashAttribute("error", "No se pudo deshabilitar el proveedor.");
        }
        return "redirect:/proveedores" + (verInactivos ? "?verInactivos=true" : "");
    }

    @GetMapping("/editar/{id}")
    @ResponseBody
    public ResponseEntity<Proveedor> obtenerParaEdicion(@PathVariable Integer id) {
        Proveedor p = service.buscarPorId(id);
        return (p != null) ? ResponseEntity.ok(p) : ResponseEntity.notFound().build();
    }
}