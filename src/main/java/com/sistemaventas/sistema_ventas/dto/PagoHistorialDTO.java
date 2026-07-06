package com.sistemaventas.sistema_ventas.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PagoHistorialDTO(
        LocalDateTime fecha,
        String metodoPago,
        BigDecimal monto
) {}
