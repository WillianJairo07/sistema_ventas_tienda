package com.sistemaventas.sistema_ventas.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;

import java.time.LocalDateTime;



@Data
@Entity
@Table(name = "cliente")
// Esta línea es VITAL: intercepta el borrado físico y lo vuelve lógico
@SQLDelete(sql = "UPDATE cliente SET estado = false WHERE id_cliente = ?")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    private Integer idCliente;

    @Column(length = 100, nullable = false)
    private String nombre;

    @Column(name = "apellido_pat", length = 100, nullable = false)
    private String apellidoPat;

    @Column(name = "apellido_mat", length = 100)
    private String apellidoMat;

    @Column(name = "fecha_registro", updatable = false)
    @CreationTimestamp
    private LocalDateTime fechaRegistro;

    // Usamos el primitivo boolean para evitar valores NULL inesperados
    @Column(nullable = false)
    private boolean estado = true;
}