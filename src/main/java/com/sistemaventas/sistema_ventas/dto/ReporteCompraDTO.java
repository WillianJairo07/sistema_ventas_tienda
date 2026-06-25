package com.sistemaventas.sistema_ventas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReporteCompraDTO(Integer id, LocalDate fecha, BigDecimal total, String proveedor) {

}