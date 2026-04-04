package com.sistemaventas.sistema_ventas.controller;

import com.sistemaventas.sistema_ventas.model.Cliente;
import com.sistemaventas.sistema_ventas.service.ClienteService;
import org.springframework.beans.factory.annotation.Autowired;
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
    public String listar(@RequestParam(name = "verInactivos", required = false) Boolean verInactivos, Model model) {
        // Determinamos si mostramos la papelera o los activos
        boolean mostrarInactivos = (verInactivos != null && verInactivos);

        model.addAttribute("clientes", clienteService.listarClientes(mostrarInactivos));
        model.addAttribute("cliente", new Cliente()); // Objeto limpio para el modal

        // Enviamos el estado actual a la vista para que el botón "Ver Inactivos" cambie de color o texto
        model.addAttribute("verInactivos", mostrarInactivos);

        return "clientes";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Cliente cliente, RedirectAttributes flash) {
        try {
            clienteService.guardar(cliente);
            flash.addFlashAttribute("success", "El cliente ha sido procesado correctamente.");
        } catch (IllegalArgumentException e) {
            // Captura: "Nombre duplicado", "Campos vacíos", etc.
            flash.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Error inesperado al guardar los datos del cliente.");
        }
        return "redirect:/clientes";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id, RedirectAttributes flash) {
        try {
            clienteService.eliminarLogico(id);
            flash.addFlashAttribute("success", "Cliente movido a la lista de inactivos.");
        } catch (Exception e) {
            flash.addFlashAttribute("error", "No se pudo deshabilitar al cliente.");
        }
        return "redirect:/clientes";
    }

    @GetMapping("/restaurar/{id}")
    public String restaurar(@PathVariable Integer id, RedirectAttributes flash) {
        try {
            clienteService.restaurar(id);
            flash.addFlashAttribute("success", "¡Cliente restaurado! Ahora aparece en la lista activa.");
            // Al restaurar, lo mandamos a la lista principal para que vea al cliente activo
            return "redirect:/clientes";
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Hubo un error al intentar restaurar al cliente.");
            return "redirect:/clientes?verInactivos=true";
        }
    }

    @GetMapping("/editar/{id}")
    @ResponseBody
    public ResponseEntity<Cliente> obtenerParaEdicion(@PathVariable Integer id) {
        Cliente cliente = clienteService.buscarPorId(id);
        return (cliente != null) ? ResponseEntity.ok(cliente) : ResponseEntity.notFound().build();
    }
}