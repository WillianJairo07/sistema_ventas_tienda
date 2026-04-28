package com.sistemaventas.sistema_ventas.controller;

import com.sistemaventas.sistema_ventas.model.Compra;
import com.sistemaventas.sistema_ventas.repository.*;
import com.sistemaventas.sistema_ventas.service.CategoriaService;
import com.sistemaventas.sistema_ventas.service.CompraService;
import com.sistemaventas.sistema_ventas.service.ProductoService;
import com.sistemaventas.sistema_ventas.service.ProveedorService;
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
@RequestMapping("/compras")
public class CompraController {

    @Autowired private CompraService compraService;
    @Autowired private ProveedorService service;
    @Autowired private ProductoService productoService;
    @Autowired private CategoriaService categoriaService;
    @Autowired private UsuarioRepository usuarioRepository;




    @GetMapping("/historial")
    public String historial(
            @RequestParam(required = false) String proveedor,
            @RequestParam(required = false) String fechaDesde,
            @RequestParam(required = false) String fechaHasta,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        // Cambiamos el tamaño a 20 para que coincida con ventas
        Page<Compra> resultado = compraService.filtrarCompras(proveedor, fechaDesde, fechaHasta, page, 20);

        model.addAttribute("compras", resultado.getContent());
        model.addAttribute("totalPages", resultado.getTotalPages());
        model.addAttribute("currentPage", page);

        // IMPORTANTE: Estos nombres deben coincidir con los th:value del HTML
        model.addAttribute("proveedorElegido", proveedor);
        model.addAttribute("fechaDesde", fechaDesde);
        model.addAttribute("fechaHasta", fechaHasta);

        return "historialcompras";
    }

    @GetMapping("/nueva")
    public String formulario(Model model) {
        Compra compra = new Compra();
        // El total se inicializa en cero, el Service lo calculará al guardar
        model.addAttribute("compra", compra);
        cargarCombos(model);
        return "compras";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Compra compra, Principal principal, RedirectAttributes flash) {
        try {
            if (principal == null) throw new RuntimeException("Sesión inválida.");

            // CORRECCIÓN AQUÍ: Usamos el nombre exacto del método del Repository
            usuarioRepository.findByUsernameIgnoreCase(principal.getName())
                    .ifPresentOrElse(compra::setUsuario, () -> {
                        throw new RuntimeException("Usuario no encontrado.");
                    });

            if (compra.getIdCompra() != null) {
                compraService.actualizarCompraPendiente(compra);
                flash.addFlashAttribute("success", "La orden #" + compra.getIdCompra() + " ha sido actualizada.");
            } else {
                compraService.registrarCompra(compra);
                flash.addFlashAttribute("success", "¡Orden de compra registrada con éxito!");
            }

            return "redirect:/compras/historial";

        } catch (Exception e) {
            flash.addFlashAttribute("error", "Error: " + e.getMessage());
            return (compra.getIdCompra() != null)
                    ? "redirect:/compras/editar/" + compra.getIdCompra()
                    : "redirect:/compras/nueva";
        }
    }

    @GetMapping(value = {"", "/"})
    public String index() {
        return "redirect:/compras/historial";
    }

    @GetMapping("/confirmar/{id}")
    public String confirmarCompra(@PathVariable Integer id,
                                  @RequestParam("tipo") String tipoComprobante, // <--- RECIBIMOS EL DATO
                                  RedirectAttributes attribute) {
        try {
            // Validamos que no llegue vacío por seguridad del negocio
            if (tipoComprobante == null || tipoComprobante.isEmpty()) {
                throw new RuntimeException("Debe seleccionar el tipo de comprobante (BOLETA o FACTURA)");
            }

            // Pasamos ambos datos al service
            compraService.confirmarRecepcion(id, tipoComprobante);

            // El mensaje ahora es más específico
            attribute.addFlashAttribute("success", "Pedido aceptado con " + tipoComprobante + " exitosamente");

        } catch (Exception e) {
            // Si algo falla (ej. compra no encontrada), el error viaja al historial
            attribute.addFlashAttribute("error", "Error al procesar: " + e.getMessage());
        }

        // REDIRIGE DIRECTO AL HISTORIAL
        return "redirect:/compras/historial";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Integer id, Model model) {
        Compra compra = compraService.buscarPorId(id);
        if (compra == null || !"PENDIENTE".equals(compra.getEstado())) {
            return "redirect:/compras?error=No+se+puede+editar+esta+compra";
        }
        model.addAttribute("compra", compra);
        model.addAttribute("modoEdicion", true);
        cargarCombos(model);
        return "compras";
    }

    @GetMapping("/api/{id}")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> obtenerDetalleJson(@PathVariable Integer id) {
        Compra c = compraService.buscarPorId(id);
        if (c == null) return ResponseEntity.notFound().build();

        Map<String, Object> json = new HashMap<>();
        json.put("idCompra", c.getIdCompra());
        json.put("fecha", c.getFecha());
        json.put("fechaInicio", c.getFechaInicio());
        json.put("fechaRecepcion", c.getFechaRecepcion());
        json.put("estado", c.getEstado());
        json.put("total", c.getTotal());

        // --- ESTA ES LA LÍNEA QUE FALTABA ---
        json.put("tipoComprobante", c.getTipoComprobante());
        // ------------------------------------

        json.put("proveedor", Map.of("nombre", c.getProveedor().getNombre()));

        List<Map<String, Object>> detallesJson = c.getDetalles().stream().map(d -> {
            Map<String, Object> det = new HashMap<>();
            // Usamos scale para asegurar que el JSON lleve los decimales correctos
            det.put("cantidad", d.getCantidad());
            det.put("precioCompra", d.getPrecioCompra());

            if (d.getProducto() != null) {
                det.put("producto", Map.of(
                        "nombreProducto", d.getProducto().getNombreProducto(),
                        "codigoBarras", d.getProducto().getCodigoBarras() != null ? d.getProducto().getCodigoBarras() : "N/A",
                        "unidadMedida", d.getProducto().getUnidadMedida() != null ? d.getProducto().getUnidadMedida() : "UNIDAD"
                ));
            }
            return det;
        }).collect(Collectors.toList());

        json.put("detalles", detallesJson);
        return ResponseEntity.ok(json);
    }

    private void cargarCombos(Model model) {
        model.addAttribute("productos", productoService.listarParaCombos());
        model.addAttribute("proveedores", service.listarParaCombos());
        model.addAttribute("categorias", categoriaService.listarParaCombos());
    }
}