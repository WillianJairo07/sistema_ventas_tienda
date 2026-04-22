package com.sistemaventas.sistema_ventas.controller;

import com.sistemaventas.sistema_ventas.model.Producto;
import com.sistemaventas.sistema_ventas.service.CategoriaService;
import com.sistemaventas.sistema_ventas.service.ProductoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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
    public String listar(
            @RequestParam(name = "verInactivos", defaultValue = "false") boolean verInactivos,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "buscar", required = false) String buscar, // NUEVO: Parámetro de búsqueda
            Model model) {

        int size = 10;

        // Pasamos el término de búsqueda al servicio
        Page<Producto> paginaProductos = productoService.listarPaginado(!verInactivos, buscar, page, size);

        model.addAttribute("productos", paginaProductos.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", paginaProductos.getTotalPages());
        model.addAttribute("verInactivos", verInactivos);
        model.addAttribute("buscar", buscar); // Lo enviamos a la vista para mantener el texto en el input

        model.addAttribute("categorias", categoriaService.listarTodasOrdenadas());
        model.addAttribute("producto", new Producto());

        return "productos";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Producto producto,
                          @RequestParam(name = "verInactivos", defaultValue = "false") boolean verInactivos,
                          RedirectAttributes flash) {
        try {
            productoService.guardar(producto);
            flash.addFlashAttribute("success", "Producto procesado correctamente.");
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Error inesperado: " + e.getMessage());
        }
        // Redirige respetando si estábamos viendo inactivos
        return "redirect:/productos" + (verInactivos ? "?verInactivos=true" : "");
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
    public String eliminar(@PathVariable Integer id, RedirectAttributes flash,
                           @RequestParam(name = "verInactivos", defaultValue = "false") boolean verInactivos) {
        try {
            productoService.eliminar(id);
            flash.addFlashAttribute("success", "Movido a la papelera.");
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Error al deshabilitar.");
        }
        return "redirect:/productos" + (verInactivos ? "?verInactivos=true" : "");
    }

    @GetMapping("/editar/{id}")
    @ResponseBody
    public ResponseEntity<Producto> editar(@PathVariable Integer id) {
        Producto p = productoService.buscarPorId(id);
        return p != null ? ResponseEntity.ok(p) : ResponseEntity.notFound().build();
    }
}