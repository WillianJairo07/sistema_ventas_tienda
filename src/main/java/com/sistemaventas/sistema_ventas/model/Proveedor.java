package com.sistemaventas.sistema_ventas.model;

import jakarta.persistence.*;
        import lombok.Data;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Data
@Entity
@Table(name = "proveedor")
@SQLDelete(sql = "UPDATE proveedor SET estado = false WHERE id_proveedor = ?")
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_proveedor")
    private Integer idProveedor;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 20)
    private String telefono;

    @Column(columnDefinition = "boolean default true")
    private boolean estado = true;
}
