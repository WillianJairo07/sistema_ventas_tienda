package com.sistemaventas.sistema_ventas.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "deuda")
public class Deuda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_deuda")
    private Integer idDeuda;

    @ManyToOne
    @JoinColumn(name = "id_venta", nullable = false)
    private Venta venta;

    // Definimos precisión y escala exactas para numeric(38,2)
    @Column(name = "monto_deuda", precision = 38, scale = 2)
    private BigDecimal montoDeuda;

    @Column(name = "fecha_pago")
    private LocalDate fechaPago;

    // Importante: false = Debe, true = Pagado
    // Definimos columnDefinition para asegurar el tipo boolean en Postgres
    @Column(name = "estado", columnDefinition = "boolean default false")
    private Boolean estado = false;
}
