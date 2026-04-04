package com.sistemaventas.sistema_ventas.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Data
@Entity
@Table(name = "rol")
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idRol;

    @Column(name = "nombre_rol", nullable = false, length = 100)
    private String nombreRol;

    @ManyToMany(mappedBy = "roles")
    private List<Usuario> usuarios;
}