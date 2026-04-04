package com.sistemaventas.sistema_ventas.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.SQLDelete;

import java.math.BigDecimal;
import java.util.List;

@Data
@Entity
@Table(name = "producto")
@SQLDelete(sql = "UPDATE producto SET estado = false WHERE id_producto = ?")
public class Producto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idProducto;

    @Column(name = "nombre_producto", unique = true, nullable = false, length = 100)
    private String nombreProducto;

    // El precio lo mantenemos en (12,2) para céntimos de dinero
    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal precio;

    // CAMBIO CLAVE: De Integer a BigDecimal para soportar gramos (12,3)
    @Column(precision = 12, scale = 3, nullable = false)
    private BigDecimal stock = BigDecimal.ZERO;

    // NUEVO ATRIBUTO: Para diferenciar "KG" de "UNIDAD"
    @Column(name = "unidad_medida", length = 10)
    private String unidadMedida = "UNIDAD";

    private boolean estado = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_categoria", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "productos"})
    private Categoria categoria;

    @Column(name = "codigo_barras", unique = true, length = 50)
    private String codigoBarras;

    @OneToMany(mappedBy = "producto")
    @JsonIgnore
    private List<DetalleCompra> detallesCompra;
}