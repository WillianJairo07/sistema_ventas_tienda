package com.sistemaventas.sistema_ventas.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReporteVentaDTO(
        Integer id,
        LocalDateTime fecha,
        String tipoVenta,      // "contado" o "credito"
        String metodoPago,     // "Efectivo", "Yape", etc.
        BigDecimal totalVenta, // El valor total original de la venta
        BigDecimal cobrado,    // Dinero real ingresado (Abonos o total si fue al contado)
        BigDecimal pendiente,  // Saldo restante por pagar (totalVenta - cobrado)
        String cliente
) {}
