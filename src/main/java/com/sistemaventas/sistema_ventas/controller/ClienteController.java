package com.sistemaventas.sistema_ventas.controller;

import com.sistemaventas.sistema_ventas.model.Cliente;
import com.sistemaventas.sistema_ventas.service.ClienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/clientes")
public class ClienteController {

    @Autowired
    private ClienteService clienteService;

    @GetMapping
    public String listar(
            @RequestParam(name = "verInactivos", defaultValue = "false") boolean verInactivos,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "buscar", required = false) String buscar,
            Model model) {

        int size = 10;
        // Obtenemos la página desde el Service con el filtro de búsqueda
        Page<Cliente> paginaClientes = clienteService.listarPaginado(!verInactivos, buscar, page, size);

        model.addAttribute("clientes", paginaClientes.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", paginaClientes.getTotalPages());
        model.addAttribute("verInactivos", verInactivos);
        model.addAttribute("buscar", buscar);

        model.addAttribute("cliente", new Cliente());
        return "clientes";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Cliente cliente,
                          @RequestParam(name = "verInactivos", defaultValue = "false") boolean verInactivos,
                          RedirectAttributes flash) {
        try {
            clienteService.guardar(cliente);
            flash.addFlashAttribute("success", "Cliente procesado correctamente.");
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Error inesperado al guardar.");
        }
        return "redirect:/clientes" + (verInactivos ? "?verInactivos=true" : "");
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id,
                           @RequestParam(name = "verInactivos", defaultValue = "false") boolean verInactivos,
                           RedirectAttributes flash) {
        try {
            clienteService.eliminarLogico(id);
            flash.addFlashAttribute("success", "Cliente movido a inactivos.");
        } catch (Exception e) {
            flash.addFlashAttribute("error", "No se pudo deshabilitar.");
        }
        return "redirect:/clientes" + (verInactivos ? "?verInactivos=true" : "");
    }

    @GetMapping("/restaurar/{id}")
    public String restaurar(@PathVariable Integer id, RedirectAttributes flash) {
        try {
            clienteService.restaurar(id);
            flash.addFlashAttribute("success", "Cliente restaurado correctamente.");
            return "redirect:/clientes";
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Error al restaurar.");
            return "redirect:/clientes?verInactivos=true";
        }
    }

    @GetMapping("/editar/{id}")
    @ResponseBody
    public ResponseEntity<Cliente> obtenerParaEdicion(@PathVariable Integer id) {
        Cliente c = clienteService.buscarPorId(id);
        return (c != null) ? ResponseEntity.ok(c) : ResponseEntity.notFound().build();
    }

    @PostMapping("/guardar-rapido")
    @ResponseBody
    public ResponseEntity<?> guardarRapido(@RequestBody Cliente cliente) {
        try {
            Cliente guardado = clienteService.guardar(cliente);
            return ResponseEntity.ok(guardado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
}