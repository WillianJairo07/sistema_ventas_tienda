package com.sistemaventas.sistema_ventas.controller;

import com.sistemaventas.sistema_ventas.service.DashboardService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/")
    public String inicio(HttpServletRequest request) {
        if (request.getUserPrincipal() != null) {
            return "redirect:/home";
        }
        return "redirect:/login";
    }

    @GetMapping("/home")
    public String home(Model model) {

        model.addAttribute("productosCriticos",
                dashboardService.obtenerProductosCriticos());

        model.addAttribute("totalComprasMes",
                dashboardService.obtenerComprasMes());

        model.addAttribute("totalVentasMes",
                dashboardService.obtenerVentasMes());

        model.addAttribute("totalDeudas",
                dashboardService.obtenerTotalDeudas());

        model.addAttribute("cantidadDeudores",
                dashboardService.obtenerCantidadDeudores());

        model.addAttribute("datosVentas",
                new Double[]{0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0});


        return "home";
    }


}