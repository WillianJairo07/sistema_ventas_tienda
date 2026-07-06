package com.sistemaventas.sistema_ventas.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sistemaventas.sistema_ventas.dto.MovimientoEnvaseDTO;
import com.sistemaventas.sistema_ventas.model.Compra;
import com.sistemaventas.sistema_ventas.model.CostoAdicionalEnvase;
import com.sistemaventas.sistema_ventas.model.Envase;
import com.sistemaventas.sistema_ventas.model.MovimientoEnvase;
import com.sistemaventas.sistema_ventas.repository.*;
import com.sistemaventas.sistema_ventas.service.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
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
    @Autowired private EnvaseService envaseService;




    @GetMapping("/historial")
    public String historial(
            @RequestParam(required = false) String proveedor,
            @RequestParam(required = false) String fechaDesde,
            @RequestParam(required = false) String fechaHasta,
            @RequestParam(defaultValue = "0") int page,
            Model model,
            RedirectAttributes flash) { // 1. Agregamos RedirectAttributes

        try {
            // 2. Intentamos realizar la búsqueda
            Page<Compra> resultado = compraService.filtrarCompras(proveedor, fechaDesde, fechaHasta, page, 20);

            model.addAttribute("compras", resultado.getContent());
            model.addAttribute("totalPages", resultado.getTotalPages());
            model.addAttribute("currentPage", page);
            model.addAttribute("proveedorElegido", proveedor);
            model.addAttribute("fechaDesde", fechaDesde);
            model.addAttribute("fechaHasta", fechaHasta);

            return "historialcompras";

        } catch (RuntimeException e) {
            // 3. Si el Service lanza error (por fechas inválidas), lo capturamos aquí
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/compras/historial";
        }
    }

    @GetMapping("/nueva")
    public String formulario(Model model) {
        Compra compra = new Compra();
        // El total se inicializa en cero, el Service lo calculará al guardar
        model.addAttribute("compra", compra);
        model.addAttribute("envasesExistentes", "[]");
        model.addAttribute("faltantesExistentes", "[]");
        cargarCombos(model);
        return "compras";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Compra compra,
                          @RequestParam(required = false) String envasesJson, // <-- AGREGAR ESTO
                          @RequestParam(required = false) String faltantesJson,
                          Principal principal,
                          RedirectAttributes flash) {
        try {
            if (principal == null) throw new RuntimeException("Sesión inválida.");

            usuarioRepository.findByUsernameIgnoreCase(principal.getName())
                    .ifPresentOrElse(compra::setUsuario, () -> {
                        throw new RuntimeException("Usuario no encontrado.");
                    });

            // --- PROCESAR ENVASES (Igual que en Ventas) ---
            if (envasesJson != null && !envasesJson.isEmpty()) {
                ObjectMapper objectMapper = new ObjectMapper(); // O inyéctalo con @Autowired
                List<Map<String, Object>> listaRaw = objectMapper.readValue(envasesJson,
                        new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>(){});

                List<MovimientoEnvase> listaEnvases = listaRaw.stream().map(map -> {
                    MovimientoEnvase mov = new MovimientoEnvase();
                    mov.setCantidad(Integer.parseInt(map.get("cantidad").toString()));
                    mov.setTipo((String) map.get("tipo"));

                    // Garantía
                    Object montoObj = map.get("montoGarantiaDejado");
                    mov.setMontoGarantiaDejado(montoObj != null ? new BigDecimal(montoObj.toString()) : BigDecimal.ZERO);

                    Envase env = new Envase();
                    env.setIdEnvase(Integer.parseInt(map.get("idEnvase").toString()));
                    mov.setEnvase(env);
                    mov.setCompra(compra); // Vinculación con la compra
                    return mov;
                }).collect(Collectors.toList());

                compra.setMovimientosEnvase(listaEnvases);
            }

            // 2. NUEVO: PROCESAR FALTANTES (Versión Normalizada)
            if (faltantesJson != null && !faltantesJson.isEmpty()) {
                ObjectMapper objectMapper = new ObjectMapper();
                List<Map<String, Object>> listaRaw = objectMapper.readValue(faltantesJson,
                        new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>(){});

                List<CostoAdicionalEnvase> listaFaltantes = listaRaw.stream().map(map -> {
                    CostoAdicionalEnvase costo = new CostoAdicionalEnvase();
                    // 1. Vincular hacia atrás
                    costo.setCompra(compra);

                    // 2. Vincular el envase
                    Envase env = new Envase();
                    Object idEnvaseObj = map.get("idEnvase");
                    env.setIdEnvase(idEnvaseObj != null ? Integer.parseInt(idEnvaseObj.toString()) : null);
                    costo.setEnvase(env);

                    // 3. Setear resto de campos
                    costo.setCantidad(Integer.parseInt(map.get("cantidad").toString()));
                    costo.setMonto(new BigDecimal(map.get("total").toString()));
                    costo.setFechaRegistro(LocalDateTime.now());

                    return costo;
                }).collect(Collectors.toList());

                // IMPORTANTE: Asignamos la lista a la compra
                compra.setCostosAdicionales(listaFaltantes);
            }

            // --- GUARDAR ---
            if (compra.getIdCompra() != null) {
                compraService.actualizarCompraPendiente(compra);
                flash.addFlashAttribute("success", "La orden #" + compra.getIdCompra() + " ha sido actualizada.");
            } else {
                compraService.registrarCompra(compra);
                flash.addFlashAttribute("success", "¡Orden de compra registrada con éxito!");
            }

            return "redirect:/compras/historial";

        } catch (Exception e) {
            e.printStackTrace(); // Útil para ver errores de JSON en consola
            flash.addFlashAttribute("error", "Error: " + e.getMessage());
            return (compra.getIdCompra() != null) ? "redirect:/compras/editar/" + compra.getIdCompra() : "redirect:/compras/nueva";
        }
    }

    @GetMapping(value = {"", "/"})
    public String index() {
        return "redirect:/compras/historial";
    }

    @GetMapping("/confirmar/{id}")
    public String confirmarCompra(@PathVariable Integer id,
                                  @RequestParam("tipo") String tipoComprobante,
                                  @RequestParam("esComprobantePropio") boolean esComprobantePropio,
                                  RedirectAttributes attribute) {
        try {
            // Validamos que no llegue vacío por seguridad del negocio
            if (tipoComprobante == null || tipoComprobante.isEmpty()) {
                throw new RuntimeException("Debe seleccionar el tipo de comprobante (BOLETA, FACTURA o NOTA DE VENTA)");
            }

            // Pasamos ambos datos al service (aquí viaja "NOTA_VENTA" exacto a la BD)
            compraService.confirmarRecepcion(id, tipoComprobante, esComprobantePropio);

            // Formateamos visualmente el texto para que el mensaje flotante sea natural
            String tipoVisual = "NOTA_VENTA".equals(tipoComprobante) ? "NOTA DE VENTA" : tipoComprobante;

            // El mensaje ahora es más específico y limpio
            attribute.addFlashAttribute("success", "Pedido aceptado con " + tipoVisual + " exitosamente");

        } catch (Exception e) {
            // Si algo falla (ej. compra no encontrada o falta de stock), el error viaja al historial
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

        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

            // 1. Mapeo de Envases (Lo que ya tenías)
            List<MovimientoEnvaseDTO> dtosEnvases = compra.getMovimientosEnvase().stream()
                    .map(MovimientoEnvaseDTO::fromEntity)
                    .toList();
            model.addAttribute("envasesExistentes", mapper.writeValueAsString(dtosEnvases));

            // 2. NUEVO: Mapeo de Costos Adicionales (Faltantes)
            // Convertimos a un mapa simple para evitar problemas con proxies de Hibernate
            List<Map<String, Object>> listaFaltantes = compra.getCostosAdicionales().stream().map(c -> {
                Map<String, Object> map = new HashMap<>();
                map.put("idEnvase", c.getEnvase().getIdEnvase());
                map.put("nombreEnvase", c.getEnvase().getNombre());
                map.put("cantidad", c.getCantidad());
                map.put("total", c.getMonto());
                return map;
            }).collect(Collectors.toList());

            model.addAttribute("faltantesExistentes", mapper.writeValueAsString(listaFaltantes));

        } catch (Exception e) {
            model.addAttribute("envasesExistentes", "[]");
            model.addAttribute("faltantesExistentes", "[]");
            e.printStackTrace();
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
        json.put("esComprobantePropio", c.isEsComprobantePropio());
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
        model.addAttribute("envases", envaseService.listarEnvasesParaVenta());
    }


    @GetMapping("/api/envases/{id}")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> obtenerEnvases(@PathVariable Integer id) {
        Compra c = compraService.buscarPorId(id);
        if (c == null) return ResponseEntity.notFound().build();

        // 1. Movimientos de envases
        List<Map<String, Object>> listaEnvases = c.getMovimientosEnvase().stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("nombre", m.getEnvase().getNombre());
            map.put("cantidad", m.getCantidad());
            map.put("montoGarantiaDejado", m.getMontoGarantiaDejado());
            return map;
        }).collect(Collectors.toList());

        // 2. Faltantes (Costos adicionales)
        List<Map<String, Object>> listaFaltantes = c.getCostosAdicionales().stream().map(f -> {
            Map<String, Object> map = new HashMap<>();
            map.put("nombre", f.getEnvase().getNombre());
            map.put("cantidad", f.getCantidad());
            map.put("total", f.getMonto());
            return map;
        }).collect(Collectors.toList());

        // CALCULAMOS EL TOTAL AQUÍ: sumamos el 'total' de cada mapa dentro de la lista de faltantes
        BigDecimal totalGeneralFaltantes = listaFaltantes.stream()
                .map(m -> (BigDecimal) m.get("total"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. Unificamos en un solo mapa de respuesta
        Map<String, Object> respuestaUnificada = new HashMap<>();
        respuestaUnificada.put("envases", listaEnvases);
        respuestaUnificada.put("faltantes", listaFaltantes);
        respuestaUnificada.put("totalGeneralFaltantes", totalGeneralFaltantes); // <-- AGREGADO

        return ResponseEntity.ok(respuestaUnificada);
    }
}