package com.sistemaventas.sistema_ventas.util;

import com.sistemaventas.sistema_ventas.model.Rol;
import com.sistemaventas.sistema_ventas.model.Usuario;
import com.sistemaventas.sistema_ventas.repository.RolRepository;
import com.sistemaventas.sistema_ventas.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component // Spring lo ejecuta automáticamente al levantar el sistema
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    // Constructor para la inyección de dependencias
    public DataInitializer(UsuarioRepository usuarioRepository, RolRepository rolRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {

        // 1. PRECARGAR ROLES (Si no existen en la base de datos)

        // Buscar o crear Rol ADMIN
        Rol adminRol = rolRepository.findAll().stream()
                .filter(r -> r.getNombreRol().equals("ADMIN"))
                .findFirst()
                .orElse(null);

        if (adminRol == null) {
            adminRol = new Rol();
            adminRol.setNombreRol("ADMIN");
            adminRol = rolRepository.save(adminRol);
        }

        // Buscar o crear Rol VENDEDOR
        Rol vendedorRol = rolRepository.findAll().stream()
                .filter(r -> r.getNombreRol().equals("VENDEDOR"))
                .findFirst()
                .orElse(null);

        if (vendedorRol == null) {
            vendedorRol = new Rol();
            vendedorRol.setNombreRol("VENDEDOR");
            rolRepository.save(vendedorRol);
        }


        // 2. CREAR USUARIO ADMINISTRADOR INICIAL (Solo si la tabla de usuarios está vacía)
        if (usuarioRepository.count() == 0) {
            Usuario admin = new Usuario();
            admin.setNombre("Usuario");
            admin.setApellidoPaterno("Primero");
            admin.setApellidoMaterno("Prueba");
            admin.setUsername("usuarioprueba01");
            admin.setPassword(passwordEncoder.encode("usuarioprueba01"));
            admin.setEstado(true);

            // Le asignamos el objeto de Rol que acabamos de asegurar arriba
            admin.setRol(adminRol);

            usuarioRepository.save(admin);
            System.out.println(">> DataInitializer: Roles y usuario administrador inicial creados con éxito.");
        }
    }
}