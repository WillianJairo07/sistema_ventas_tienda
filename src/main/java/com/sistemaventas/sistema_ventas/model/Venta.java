package com.sistemaventas.sistema_ventas.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "venta")
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idVenta;

    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    // Cambiado a BigDecimal para precisión financiera total
    @Column(name = "monto_pagado", precision = 10, scale = 2)
    private BigDecimal montoPagado;

    @Column(name = "metodo_pago", length = 50)
    private String metodoPago; // "Efectivo", "Tarjeta", "Yape", etc.

    @Column(name = "total_venta", precision = 10, scale = 2)
    private BigDecimal totalVenta;

    @Column(name = "tipo_venta", length = 20)
    private String tipoVenta; // "contado" o "credito"

    @ManyToOne
    @JoinColumn(name = "id_cliente")
    private Cliente cliente;

    @ManyToOne
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    // Agregamos orphanRemoval para que si quitas un producto del carrito se borre de la DB
    // Y JsonManagedReference para que el historial en JS funcione perfecto
    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<DetalleVenta> detalles;
}