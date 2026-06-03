package com.sistemaventas.sistema_ventas.model;


import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer idUsuario;

    @Column(nullable = false, length = 100)
    private String nombre;

    // Mapeo simple sin @NotBlank
    @Column(name = "apellido_paterno", nullable = false, length = 100)
    private String apellidoPaterno;

    // Mapeo simple sin @NotBlank
    @Column(name = "apellido_materno", nullable = false, length = 100)
    private String apellidoMaterno;

    @Column(name = "usuario", unique = true, nullable = false, length = 50)
    private String username;

    @Column(name = "contrasena", nullable = false, length = 100)
    private String password;

    @Column(nullable = false)
    private Boolean estado = true;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_rol", nullable = false)
    private Rol rol;
}
