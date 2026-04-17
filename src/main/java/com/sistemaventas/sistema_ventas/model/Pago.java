package com.sistemaventas.sistema_ventas.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime; // Cambiado a LocalDateTime

@Data
@Entity
@Table(name = "pago")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago")
    private Integer idPago;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_venta", nullable = false)
    private Venta venta;

    @Column(name = "monto", precision = 38, scale = 2, nullable = false)
    private BigDecimal monto;

    @Column(name = "fecha")
    private LocalDateTime fecha = LocalDateTime.now(); // Ahora registra hora exacta

    @Column(name = "metodo_pago", nullable = false)
    private String metodoPago;

    @Column(name = "nota")
    private String nota;
}