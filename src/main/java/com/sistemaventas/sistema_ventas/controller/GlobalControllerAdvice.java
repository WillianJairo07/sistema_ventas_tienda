package com.sistemaventas.sistema_ventas.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    @ModelAttribute("nombreUsuario")
    public String añadirNombreUsuario(Authentication authentication) {
        // 1. Si no hay nadie logueado o la sesión expiró
        if (authentication == null || !authentication.isAuthenticated()) {
            return "Invitado";
        }

        // 2. Obtenemos el objeto Principal (el usuario logueado)
        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetails) {
            // Retorna el username (ej: "admin" o "juan.perez")
            return ((UserDetails) principal).getUsername();
        }

        // 3. Fallback por si el principal es un String (poco común en configuraciones estándar)
        return authentication.getName();
    }
}