package com.sistemaventas.sistema_ventas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReporteCompraDTO(
        Integer id,              // 1. idCompra
        LocalDate fecha,     // 2. fecha
        String tipoComprobante,  // 3. tipoComprobante
        BigDecimal total,        // 4. total
        String proveedor         // 5. nombre del proveedor
) {}