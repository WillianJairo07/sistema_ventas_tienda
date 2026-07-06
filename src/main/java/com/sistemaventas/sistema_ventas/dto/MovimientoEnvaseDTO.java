package com.sistemaventas.sistema_ventas.dto;

import com.sistemaventas.sistema_ventas.model.MovimientoEnvase;

public record MovimientoEnvaseDTO(
        Integer idEnvase,
        String nombre,
        Integer cantidad,
        Double montoGarantiaDejado
) {
    public static MovimientoEnvaseDTO fromEntity(MovimientoEnvase m) {
        // Accedemos a los datos aquí mientras el objeto está "vivo"
        return new MovimientoEnvaseDTO(
                m.getEnvase() != null ? m.getEnvase().getIdEnvase() : 0,
                m.getEnvase() != null ? m.getEnvase().getNombre() : "N/A",
                m.getCantidad() != null ? m.getCantidad() : 0,
                m.getMontoGarantiaDejado() != null ? m.getMontoGarantiaDejado().doubleValue() : 0.0
        );
    }
}
