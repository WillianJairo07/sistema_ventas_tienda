package com.sistemaventas.sistema_ventas.controller;

import com.sistemaventas.sistema_ventas.model.Proveedor;
import com.sistemaventas.sistema_ventas.service.ProveedorService;
import org.springframework.beans.factory.annotation.Autowired;
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
    public String listar(@RequestParam(name = "verInactivos", required = false) Boolean verInactivos, Model model) {
        // Usamos una variable booleana limpia para decidir qué lista mostrar
        boolean mostrarInactivos = (verInactivos != null && verInactivos);

        model.addAttribute("proveedores", mostrarInactivos ?
                service.listarSoloInactivos() :
                service.listarTodos());

        model.addAttribute("proveedor", new Proveedor());
        return "proveedores";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Proveedor proveedor, RedirectAttributes flash) {
        try {
            service.guardar(proveedor);
            flash.addFlashAttribute("success", "Proveedor procesado correctamente.");
        } catch (IllegalArgumentException e) {
            // Capturamos el error de validación (nombre duplicado, etc.)
            flash.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Error inesperado al guardar el proveedor.");
        }
        return "redirect:/proveedores";
    }

    @GetMapping("/restaurar/{id}")
    public String restaurar(@PathVariable Integer id, RedirectAttributes flash) {
        try {
            service.restaurar(id);
            flash.addFlashAttribute("success", "Proveedor restaurado con éxito.");
            // Al restaurar, volvemos a la lista principal para ver al proveedor activo
            return "redirect:/proveedores";
        } catch (Exception e) {
            flash.addFlashAttribute("error", "No se pudo restaurar el proveedor.");
            return "redirect:/proveedores?verInactivos=true";
        }
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id, RedirectAttributes flash) {
        try {
            // El service ahora hace el cambio de estado (borrado lógico)
            service.eliminar(id);
            flash.addFlashAttribute("success", "El proveedor ha sido enviado a inactivos.");
        } catch (Exception e) {
            flash.addFlashAttribute("error", "No se pudo deshabilitar el proveedor.");
        }
        return "redirect:/proveedores";
    }

    @GetMapping("/editar/{id}")
    @ResponseBody
    public ResponseEntity<Proveedor> obtenerParaEdicion(@PathVariable Integer id) {
        Proveedor p = service.buscarPorId(id);
        return (p != null) ? ResponseEntity.ok(p) : ResponseEntity.notFound().build();
    }
}