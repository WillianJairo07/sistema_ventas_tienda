package com.sistemaventas.sistema_ventas.controller;

import com.sistemaventas.sistema_ventas.repository.CompraRepository;
import com.sistemaventas.sistema_ventas.repository.ProductoRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
public class HomeController {


    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private CompraRepository compraRepository;

    @GetMapping("/")
    public String inicio(HttpServletRequest request) {
        // Si el usuario ya tiene una sesión activa, lo mandamos al home directamente
        // para que no tenga que ver el login otra vez.
        if (request.getUserPrincipal() != null) {
            return "redirect:/home";
        }
        // Si no está logueado, al login.
        return "redirect:/login";
    }

    @GetMapping("/home")
    public String home(Model model) {
        // El nombreUsuario ya lo pone el GlobalControllerAdvice solo.

        long stockCritico = productoRepository.countByStockLessThanEqualAndEstadoTrue(5);
        model.addAttribute("productosCriticos", stockCritico);

        LocalDate inicioMes = LocalDate.now().withDayOfMonth(1);
        LocalDate hoy = LocalDate.now();

        BigDecimal totalComprasMes = compraRepository.sumTotalComprasMes(inicioMes, hoy);
        model.addAttribute("totalComprasMes", totalComprasMes);

        model.addAttribute("cantidadDeudores", 0);
        model.addAttribute("totalDeudas", "0.00");
        model.addAttribute("datosVentas", new Double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0});

        return "home";
    }
}
