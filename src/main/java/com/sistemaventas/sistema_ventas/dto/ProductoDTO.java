package com.sistemaventas.sistema_ventas.dto;

import java.math.BigDecimal;

public record ProductoDTO(
        String nombreProducto,
        BigDecimal stock,      // Usamos BigDecimal para precisión (ej: 1.5 kg)
        String unidadMedida    // "kg", "unidad", "gr", etc.
) {
    public String getStockFormateado() {
        if ("kg".equalsIgnoreCase(this.unidadMedida)) {
            // Formato para peso: 2 decimales
            return String.format("%.2f kg", this.stock);
        } else {
            // Formato para unidades: entero (sin decimales)
            return String.format("%d", this.stock.intValue());
        }
    }
}