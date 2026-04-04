package com.sistemaventas.sistema_ventas.controller;

import com.sistemaventas.sistema_ventas.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import java.security.Principal;

@ControllerAdvice // Esto hace que funcione para TODOS los controladores
public class GlobalControllerAdvice {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @ModelAttribute("nombreUsuario") // El nombre que usas en Thymeleaf ${nombreUsuario}
    public String añadirNombreUsuario(Principal principal) {
        if (principal == null) {
            return "Invitado";
        }

        // Buscamos el nombre real en la base de datos
        return usuarioRepository.findByUsername(principal.getName())
                .map(u -> u.getNombre())
                .orElse("Usuario");
    }
}
