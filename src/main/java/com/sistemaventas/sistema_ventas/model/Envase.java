package com.sistemaventas.sistema_ventas.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Entity
public class Envase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idEnvase;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "precio_garantia_base", nullable = false)
    private BigDecimal precioGarantiaBase;

    @Column(columnDefinition = "boolean default true")
    private boolean estado = true;

    @Column(nullable = false)
    private Integer stock = 0;



}
