package com.sistemaventas.sistema_ventas.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "costos_adicionales_envase")
public class CostoAdicionalEnvase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idCostoEnvase;

    private Integer cantidad;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;


    @Column(nullable = false)
    private LocalDateTime fechaRegistro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_compra", nullable = false)
    private Compra compra;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_envase")
    private Envase envase;

    @PrePersist
    protected void onCreate() {
        this.fechaRegistro = LocalDateTime.now();
    }
}