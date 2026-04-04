package com.sistemaventas.sistema_ventas.controller;

import com.sistemaventas.sistema_ventas.model.Producto;
import com.sistemaventas.sistema_ventas.service.CategoriaService;
import com.sistemaventas.sistema_ventas.service.ProductoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
@RequestMapping("/productos")
public class ProductoController {

    @Autowired
    private ProductoService productoService;

    @Autowired
    private CategoriaService categoriaService;

    @GetMapping
    public String listar(@RequestParam(name = "verInactivos", required = false) Boolean verInactivos, Model model) {
        boolean mostrarInactivos = (verInactivos != null && verInactivos);

        model.addAttribute("productos", mostrarInactivos ?
                productoService.listarSoloInactivos() :
                productoService.listarTodos());

        model.addAttribute("categorias", categoriaService.listarTodasOrdenadas());

        // --- MEJORA: Pasar el estado del filtro a la vista ---
        model.addAttribute("verInactivos", mostrarInactivos);
        model.addAttribute("producto", new Producto());

        return "productos";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Producto producto, RedirectAttributes flash) {
        try {
            // El Service ya se encarga de las validaciones de BigDecimal y Unidad de Medida
            productoService.guardar(producto);
            flash.addFlashAttribute("success", "Producto procesado correctamente.");
        } catch (IllegalArgumentException e) {
            // Aquí caerán los errores de: "No puedes modificar el stock manualmente"
            flash.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Error inesperado: " + e.getMessage());
        }
        return "redirect:/productos";
    }

    @GetMapping("/restaurar/{id}")
    public String restaurar(@PathVariable Integer id, RedirectAttributes flash) {
        try {
            productoService.restaurar(id);
            flash.addFlashAttribute("success", "El producto ha vuelto al catálogo activo.");
            return "redirect:/productos";
        } catch (Exception e) {
            flash.addFlashAttribute("error", "No se pudo restaurar.");
            return "redirect:/productos?verInactivos=true";
        }
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id, RedirectAttributes flash) {
        try {
            productoService.eliminar(id);
            flash.addFlashAttribute("success", "Movido a la papelera.");
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Error al deshabilitar.");
        }
        // --- MEJORA: Si eliminas estando en la lista de inactivos, quédate ahí ---
        return "redirect:/productos";
    }

    @GetMapping("/editar/{id}")
    @ResponseBody
    public ResponseEntity<Producto> editar(@PathVariable Integer id) {
        Producto p = productoService.buscarPorId(id);
        // Al ser ResponseBody, Jackson convertirá el BigDecimal a número automáticamente para el JS
        return p != null ? ResponseEntity.ok(p) : ResponseEntity.notFound().build();
    }
}