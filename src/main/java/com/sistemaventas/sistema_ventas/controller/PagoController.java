package com.sistemaventas.sistema_ventas.controller;

import com.sistemaventas.sistema_ventas.model.Pago;
import com.sistemaventas.sistema_ventas.repository.PagoRepository; // <--- Importado limpiamente aquí
import com.sistemaventas.sistema_ventas.repository.VentaRepository;
import com.sistemaventas.sistema_ventas.service.PagoService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/pagos")
public class PagoController {

    @Autowired
    private PagoService pagoService;

    @Autowired
    private VentaRepository ventaRepo;


    @GetMapping
    public String listarCuentasPorCobrar(
            @RequestParam(name = "buscar", required = false) String buscar,
            @RequestParam(name = "verCompletadas", defaultValue = "false") boolean verCompletadas,
            @RequestParam(name = "page", defaultValue = "0") int page,
            HttpServletRequest request,
            Model model) {

        String busquedaLimpia = (buscar != null && !buscar.trim().isEmpty()) ? buscar.trim() : null;

        // Ejecuta la consulta nativa optimizada que repara los saldos pendientes
        Page<Object[]> paginaDeudas = ventaRepo.findVentasPendientesRaw(busquedaLimpia, verCompletadas, PageRequest.of(page, 10));

        model.addAttribute("ventasConDeuda", paginaDeudas.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", paginaDeudas.getTotalPages());
        model.addAttribute("buscar", buscar);
        model.addAttribute("verCompletadas", verCompletadas);

        // Si es AJAX, devolvemos SOLO el fragmento de la tabla y la paginación
        if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
            return "cuentas_cobrar :: #contenedorTabla";
        }

        return "cuentas_cobrar";
    }

    @PostMapping("/registrar")
    public String registrarAbono(@RequestParam Integer idVenta,
                                 @RequestParam BigDecimal monto,
                                 @RequestParam String metodo,
                                 @RequestParam(required = false) String nota,
                                 RedirectAttributes flash) {
        try {
            pagoService.registrarPago(idVenta, monto, metodo, nota);
            flash.addFlashAttribute("success", "¡Abono registrado correctamente!");
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Error al registrar pago: " + e.getMessage());
        }
        return "redirect:/pagos";
    }




}