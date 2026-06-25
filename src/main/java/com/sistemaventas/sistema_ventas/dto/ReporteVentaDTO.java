package com.sistemaventas.sistema_ventas.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReporteVentaDTO(Integer id, LocalDateTime fecha, BigDecimal total, String cliente) {

}
