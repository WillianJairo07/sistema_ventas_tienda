package com.sistemaventas.sistema_ventas.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sistemaventas.sistema_ventas.model.Envase;
import com.sistemaventas.sistema_ventas.model.MovimientoEnvase;
import com.sistemaventas.sistema_ventas.model.Usuario;
import com.sistemaventas.sistema_ventas.model.Venta;
import com.sistemaventas.sistema_ventas.repository.*;
import com.sistemaventas.sistema_ventas.service.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/ventas")
public class VentaController {

    @Autowired private VentaService ventaService;
    @Autowired private ProductoService productoService;
    @Autowired private ClienteService clienteService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private TicketService ticketService;
    @Autowired private PagoRepository pagoRepository;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private EnvaseService envaseService;
    @Autowired private EnvaseRepository envaseRepository;
    @Autowired private MovimientoEnvaseRepository movimientoEnvaseRepository;

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
        model.addAttribute("envases", envaseService.listarEnvasesParaVenta());
        return "ventas";
    }

    @PostMapping("/guardar")
    public String guardarVenta(@ModelAttribute("venta") Venta venta, @RequestParam(required = false) String envasesJson, Principal principal, RedirectAttributes flash) {
        try {
            if (principal == null) throw new RuntimeException("Sesión no válida.");

            // --- 1. PROCESAR ENVASES CON SEGURIDAD ---
            if (envasesJson != null && !envasesJson.isEmpty()) {
                List<Map<String, Object>> listaRaw = objectMapper.readValue(envasesJson,
                        new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>(){});

                List<MovimientoEnvase> listaEnvases = listaRaw.stream().map(map -> {
                    MovimientoEnvase mov = new MovimientoEnvase();

                    // Mapeo de datos básicos
                    mov.setCantidad(Integer.parseInt(map.get("cantidad").toString()));
                    mov.setTipo((String) map.get("tipo"));

                    // --- CORRECCIÓN: Captura del Monto de Garantía ---
                    Object montoObj = map.get("montoGarantiaDejado");
                    if (montoObj != null && !montoObj.toString().isEmpty()) {
                        // Ahora puedes usar BigDecimal directamente gracias al import
                        mov.setMontoGarantiaDejado(new BigDecimal(montoObj.toString()));
                    } else {
                        mov.setMontoGarantiaDejado(BigDecimal.ZERO);
                    }
                    // --------------------------------------------------

                    Envase env = new Envase();
                    env.setIdEnvase(Integer.parseInt(map.get("idEnvase").toString()));
                    mov.setEnvase(env);

                    // Vinculación con la venta padre
                    mov.setVenta(venta);

                    return mov;
                }).collect(Collectors.toList());

                venta.setMovimientosEnvase(listaEnvases);
            }

            // --- 2. PREPARACIÓN DE LA VENTA ---
            Usuario usuarioLogueado = usuarioRepository.findByUsernameIgnoreCase(principal.getName())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            venta.setUsuario(usuarioLogueado);

            if (venta.getTipoVenta() != null) {
                venta.setTipoVenta(venta.getTipoVenta().toUpperCase());
            }

            // --- 3. REGISTRO Y PERSISTENCIA ---
            // Al tener CascadeType.ALL en la entidad Venta, al guardar la venta
            // se guardarán automáticamente los MovimientosEnvase creados arriba.
            Venta ventaGuardada = ventaService.registrarVenta(venta);

            flash.addFlashAttribute("success", "¡Venta realizada con éxito!");
            flash.addFlashAttribute("idVentaGenerada", ventaGuardada.getIdVenta());

            return "redirect:/ventas";

        } catch (Exception e) {
            // Log del error para depuración
            e.printStackTrace();
            flash.addFlashAttribute("error", "Error al procesar la venta: " + e.getMessage());
            return "redirect:/ventas/nueva";
        }
    }

    // 4. NUEVO ENDPOINT: GENERAR EL PDF DEL TICKET
    @GetMapping("/ticket/pdf/{id}")
    @ResponseBody
    public ResponseEntity<byte[]> descargarTicket(@PathVariable Integer id) {
        try {
            Venta v = ventaService.buscarPorId(id);
            byte[] contents = ticketService.generarTicketPDF(v);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            // "inline" para que el navegador intente abrirlo/imprimirlo directamente
            headers.setContentDispositionFormData("inline", "ticket_" + id + ".pdf");

            return ResponseEntity.ok().headers(headers).body(contents);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // 4. API PARA EL MODAL (IGUAL QUE EN COMPRAS)
    @GetMapping("/api/{id}")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> obtenerDetalleJson(@PathVariable Integer id) {
        Venta v = ventaService.buscarPorId(id);
        if (v == null) return ResponseEntity.notFound().build();

        // 1. CALCULAMOS EL TOTAL PAGADO Y SALDO PENDIENTE REAL
        java.math.BigDecimal totalPagado = pagoRepository.totalPagadoVenta(id);
        if (totalPagado == null) totalPagado = java.math.BigDecimal.ZERO;

        java.math.BigDecimal saldoPendiente = v.getTotalVenta().subtract(totalPagado);

        Map<String, Object> json = new HashMap<>();
        json.put("idVenta", v.getIdVenta());
        json.put("fecha", v.getFecha());
        json.put("metodoPago", v.getMetodoPago());
        json.put("totalVenta", v.getTotalVenta());
        json.put("tipoVenta", v.getTipoVenta());

        // --- NUEVO: MONTO CON EL QUE PAGÓ EL CLIENTE ---
        // Si la venta es CONTADO, usamos el montoPagado de la entidad.
        // Si es nulo por algún error viejo, por defecto ponemos el totalVenta.
        json.put("montoPagado", v.getMontoPagado() != null ? v.getMontoPagado() : v.getTotalVenta());

        // --- INFORMACIÓN DE PAGOS/ABONOS ---
        json.put("totalPagado", totalPagado);     // Suma de todos los abonos realizados
        json.put("saldoPendiente", saldoPendiente); // Lo que falta cobrar

        // Datos del Cliente
        json.put("cliente", Map.of("nombreCompleto", v.getCliente().getNombre() + " " + v.getCliente().getApellidoPat()));

        // Detalles de productos
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

        // ENVASES
        List<MovimientoEnvase> listaEnvases = v.getMovimientosEnvase();

        List<Map<String, Object>> envasesJson = (listaEnvases != null ? listaEnvases : new java.util.ArrayList<MovimientoEnvase>())
                .stream().map(m -> {
                    Map<String, Object> env = new HashMap<>();
                    // Como usas Lombok, m.getEnvase() ya existe
                    env.put("nombre", (m.getEnvase() != null) ? m.getEnvase().getNombre() : "Sin nombre");
                    env.put("cantidad", m.getCantidad());
                    env.put("tipo", m.getTipo());
                    env.put("montoGarantiaDejado", m.getMontoGarantiaDejado() != null ? m.getMontoGarantiaDejado() : 0);
                    env.put("fecha", (m.getFecha() != null) ? m.getFecha().toLocalDate().toString() : "N/A");
                    return env;
                }).collect(Collectors.toList());

        json.put("envases", envasesJson);
        // --- FIN DE LA INSERCIÓN ---

        return ResponseEntity.ok(json);
    }

    private void cargarCombos(Model model) {
        model.addAttribute("productos", productoService.listarParaCombos());
        model.addAttribute("clientes", clienteService.listarParaCombos());
    }



    @PostMapping("/api/envases/registrar")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> registrarEnvase(@RequestParam Integer idVenta,
                                             @RequestParam Integer idEnvase,
                                             @RequestParam Integer cantidad,
                                             @RequestParam String tipo,
                                             @RequestParam(required = false) java.math.BigDecimal montoDejado) {
        try {
            // 0. BUSCAR VENTA Y VALIDAR ESTADO (Seguridad Backend)
            Venta v = ventaService.buscarPorId(idVenta);
            if (v == null) return ResponseEntity.badRequest().body("Venta no encontrada.");

            // Obtenemos el saldo actual para verificar si la cuenta está saldada
            Envase env = envaseRepository.findById(idEnvase).orElseThrow();
            Integer saldoActual = movimientoEnvaseRepository.obtenerSaldoEnvase(v.getCliente().getIdCliente(), env.getIdEnvase());

            // Si no debe nada y el usuario intenta registrar un "PRESTAMO", bloqueamos
            // (Si el saldo es 0 o null, asumimos que no hay deuda pendiente para este envase)
            if ("PRESTAMO".equalsIgnoreCase(tipo) && (saldoActual == null || saldoActual <= 0)) {
                // Si quieres bloquear, añade aquí el return:
                return ResponseEntity.badRequest().body("Error: No se pueden realizar nuevos préstamos en esta cuenta.");
            }

            // 1. OBTENER SALDO PENDIENTE ACTUAL
            // (Ya lo obtuvimos arriba)

            // 2. VALIDACIÓN DE DEVOLUCIÓN
            if ("DEVOLUCION".equalsIgnoreCase(tipo)) {
                if (saldoActual == null || cantidad > saldoActual) {
                    return ResponseEntity.badRequest().body("Error: No puedes devolver " + cantidad + " unidades. El cliente solo debe " + (saldoActual != null ? saldoActual : 0) + ".");
                }
            }

            // LÓGICA DE LIQUIDACIÓN
            int cantidadMovimiento = "DEVOLUCION".equalsIgnoreCase(tipo) ? -Math.abs(cantidad) : Math.abs(cantidad);

            // 3. AJUSTE DE STOCK DE TIENDA
            env.setStock(env.getStock() - cantidadMovimiento);
            envaseRepository.save(env);

            // 4. REGISTRAR MOVIMIENTO
            envaseService.registrarMovimiento(v.getCliente(), env, cantidadMovimiento, tipo, v, montoDejado);

            return ResponseEntity.ok("Movimiento registrado correctamente.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/api/envases/lista")
    @ResponseBody
    public List<Envase> listarEnvasesApi() {
        // Ya no usamos findAll(), usamos nuestro método filtrado
        return envaseRepository.listarEnvasesParaVenta();
    }

}