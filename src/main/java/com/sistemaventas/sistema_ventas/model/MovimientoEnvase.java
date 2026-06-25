package com.sistemaventas.sistema_ventas.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
public class MovimientoEnvase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idMovimiento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_envase")
    private Envase envase;

    private Integer cantidad; // Positivo (sale/prestas), Negativo (entra/devuelven)

    private String tipo; // "PRESTAMO", "DEPOSITO", "CANJE"

    private BigDecimal montoGarantiaDejado;

    private boolean garantiasDevueltas = false;

    private LocalDateTime fecha;

    @ManyToOne(fetch = FetchType.LAZY) // Es mejor para el rendimiento
    @JoinColumn(name = "id_venta", nullable = false) // 'nullable = false' asegura integridad
    private Venta venta;
}
