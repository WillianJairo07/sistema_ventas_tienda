package com.sistemaventas.sistema_ventas.controller;

import com.sistemaventas.sistema_ventas.model.Categoria;
import com.sistemaventas.sistema_ventas.service.CategoriaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.Page;

@Controller
@RequestMapping("/categorias")
public class CategoriaController {

    @Autowired
    private CategoriaService categoriaService;

    @GetMapping
    public String listarCategorias(
            @RequestParam(name = "verInactivos", required = false, defaultValue = "false") boolean verInactivos,
            @RequestParam(name = "buscar", required = false) String buscar,
            @RequestParam(name = "page", defaultValue = "0") int page,
            Model model) {

        // Tamaño de página (ejemplo: 10 categorías por página)
        int size = 10;
        boolean estadoABuscar = !verInactivos;

        Page<Categoria> pagina = categoriaService.listarPaginado(estadoABuscar, page, size, buscar);

        model.addAttribute("categorias", pagina.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", pagina.getTotalPages());
        model.addAttribute("buscar", buscar);
        model.addAttribute("verInactivos", verInactivos);
        model.addAttribute("categoria", new Categoria());

        return "categorias";
    }

    @PostMapping("/guardar")
    public String guardarCategoria(@ModelAttribute Categoria categoria, RedirectAttributes flash) {
        try {
            categoriaService.guardar(categoria);
            flash.addFlashAttribute("success", "Categoría procesada correctamente.");
        } catch (IllegalArgumentException e) {
            // Aquí capturamos el mensaje de "Ya existe" o "Similar" del Service
            flash.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Error inesperado al procesar la categoría.");
        }
        return "redirect:/categorias";
    }

    @GetMapping("/restaurar/{id}")
    public String restaurar(@PathVariable Integer id, RedirectAttributes flash) {
        try {
            categoriaService.restaurar(id);
            flash.addFlashAttribute("success", "Categoría restaurada correctamente.");
            // TIP: Redirigimos a la lista normal para ver la categoría devuelta
            return "redirect:/categorias";
        } catch (Exception e) {
            flash.addFlashAttribute("error", "No se pudo restaurar la categoría.");
            return "redirect:/categorias?verInactivos=true";
        }
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id, RedirectAttributes flash) {
        try {
            // Ahora este método en el Service hace borrado LÓGICO (estado = false)
            categoriaService.eliminar(id);
            flash.addFlashAttribute("success", "La categoría ha sido enviada a inactivos.");
        } catch (Exception e) {
            flash.addFlashAttribute("error", "No se pudo deshabilitar la categoría.");
        }
        return "redirect:/categorias";
    }

    @GetMapping("/editar/{id}")
    @ResponseBody
    public ResponseEntity<Categoria> obtenerCategoriaParaEditar(@PathVariable Integer id) {
        Categoria cat = categoriaService.buscarPorId(id);
        return cat != null ? ResponseEntity.ok(cat) : ResponseEntity.notFound().build();
    }
}