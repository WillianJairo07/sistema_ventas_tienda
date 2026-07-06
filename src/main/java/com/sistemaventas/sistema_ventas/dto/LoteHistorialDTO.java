package com.sistemaventas.sistema_ventas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record LoteHistorialDTO(
        Integer idDetalleCompra,
        LocalDate fecha,
        BigDecimal precioCompra,
        BigDecimal cantidad,
        BigDecimal stockActual,
        String unidadMedida
) { }
