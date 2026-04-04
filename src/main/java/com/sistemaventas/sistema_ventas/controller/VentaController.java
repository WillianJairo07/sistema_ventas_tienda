package com.sistemaventas.sistema_ventas.controller;

import com.sistemaventas.sistema_ventas.model.Usuario;
import com.sistemaventas.sistema_ventas.model.Venta;
import com.sistemaventas.sistema_ventas.repository.ClienteRepository;
import com.sistemaventas.sistema_ventas.repository.ProductoRepository;
import com.sistemaventas.sistema_ventas.repository.UsuarioRepository;
import com.sistemaventas.sistema_ventas.service.VentaService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/ventas")
public class VentaController {

    @Autowired private VentaService ventaService;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    // 1. VER HISTORIAL DE VENTAS

    @GetMapping
    public String historialVentas(
            @RequestParam(required = false) String buscar,
            @RequestParam(required = false) String fechaDesde,
            @RequestParam(required = false) String fechaHasta,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        // 20 registros es un buen número para rendimiento
        Page<Venta> paginaVentas = ventaService.obtenerVentasPaginadas(buscar, fechaDesde, fechaHasta, page, 20);

        model.addAttribute("ventas", paginaVentas.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", paginaVentas.getTotalPages());

        // CRÍTICO: Mantener los filtros en el modelo para los links de paginación
        model.addAttribute("buscar", buscar);
        model.addAttribute("fechaDesde", fechaDesde);
        model.addAttribute("fechaHasta", fechaHasta);

        return "historialventas";
    }

    // 2. FORMULARIO DE NUEVA VENTA
    @GetMapping("/nueva")
    public String nuevaVenta(Model model) {
        model.addAttribute("venta", new Venta());
        cargarCombos(model);
        return "ventas";
    }

    @PostMapping("/guardar")
    public String guardarVenta(@ModelAttribute("venta") Venta venta, Principal principal, RedirectAttributes flash) {
        try {
            if (principal == null) throw new RuntimeException("Sesión no válida.");

            Usuario usuarioLogueado = usuarioRepository.findByUsername(principal.getName())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            venta.setUsuario(usuarioLogueado);

            // --- AGREGAR ESTO PARA EVITAR ERROR DE CONSTRAINT ---
            if (venta.getTipoVenta() != null) {
                venta.setTipoVenta(venta.getTipoVenta().toUpperCase());
            }
            // ----------------------------------------------------

            ventaService.registrarVenta(venta);
            flash.addFlashAttribute("success", "¡Venta realizada con éxito!");
            return "redirect:/ventas";

        } catch (Exception e) {
            flash.addFlashAttribute("error", "Error: " + e.getMessage());
            return "redirect:/ventas/nueva";
        }
    }

    // 4. API PARA EL MODAL (IGUAL QUE EN COMPRAS)
    @GetMapping("/api/{id}")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> obtenerDetalleJson(@PathVariable Integer id) {
        Venta v = ventaService.buscarPorId(id);
        if (v == null) return ResponseEntity.notFound().build();

        Map<String, Object> json = new HashMap<>();
        json.put("idVenta", v.getIdVenta());
        json.put("fecha", v.getFecha());
        json.put("metodoPago", v.getMetodoPago());
        json.put("totalVenta", v.getTotalVenta());

        // --- NUEVOS CAMPOS PARA EL MODAL DE DEUDA ---
        json.put("tipoVenta", v.getTipoVenta()); // CONTADO o CREDITO
        json.put("montoPagado", v.getMontoPagado()); // La inicial que dejó el cliente
        // --------------------------------------------

        json.put("cliente", Map.of("nombreCompleto", v.getCliente().getNombre() + " " + v.getCliente().getApellidoPat()));

        List<Map<String, Object>> detallesJson = v.getDetalles().stream().map(d -> {
            Map<String, Object> det = new HashMap<>();
            det.put("cantidad", d.getCantidad());
            det.put("precioVenta", d.getPrecioVenta());
            if (d.getProducto() != null) {
                det.put("producto", Map.of(
                        "nombreProducto", d.getProducto().getNombreProducto(),
                        "codigoBarras", d.getProducto().getCodigoBarras() != null ? d.getProducto().getCodigoBarras() : "N/A"
                ));
            }
            return det;
        }).collect(Collectors.toList());

        json.put("detalles", detallesJson);
        return ResponseEntity.ok(json);
    }

    private void cargarCombos(Model model) {
        model.addAttribute("productos", productoRepository.findByEstadoTrue());
        model.addAttribute("clientes", clienteRepository.findByEstadoTrueOrderByIdClienteAsc());
    }
}