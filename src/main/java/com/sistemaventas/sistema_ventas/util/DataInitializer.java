package com.sistemaventas.sistema_ventas.util;

import com.sistemaventas.sistema_ventas.model.Rol;
import com.sistemaventas.sistema_ventas.model.Usuario;
import com.sistemaventas.sistema_ventas.repository.RolRepository;
import com.sistemaventas.sistema_ventas.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.util.List;

@Component // Esto hace que Spring lo ejecute al iniciar
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    // Inyectamos las dependencias
    public DataInitializer(UsuarioRepository usuarioRepository, RolRepository rolRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Solo creamos el usuario si la tabla está vacía
        if (usuarioRepository.count() == 0) {

            // 1. Crear el Rol ADMIN
            Rol adminRol = new Rol();
            adminRol.setNombreRol("ADMIN");
            rolRepository.save(adminRol);

            // 2. Crear el Usuario
            Usuario admin = new Usuario();
            admin.setNombre("Jairo");
            admin.setUsername("admin");

            // AQUÍ OCURRE EL HASHEO:
            // Usamos el passwordEncoder que definiste en SecurityConfig
            admin.setPassword(passwordEncoder.encode("admin12345"));

            admin.setEstado(true);
            admin.setRoles(List.of(adminRol));

            usuarioRepository.save(admin);

        }
    }
}
