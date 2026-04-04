package com.sistemaventas.sistema_ventas.model;



import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.List;

@Data
@Entity
@Table(name = "categoria")
@SQLDelete(sql = "UPDATE categoria SET estado = false WHERE id_categoria = ?")
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idCategoria;

    @Column(name = "nombre_categoria", nullable = false, length = 100)
    private String nombreCategoria;

    @Column(columnDefinition = "boolean default true")
    private boolean estado = true; // El interruptor

    @OneToMany(mappedBy = "categoria")
    @JsonIgnore
    private List<Producto> productos;
}