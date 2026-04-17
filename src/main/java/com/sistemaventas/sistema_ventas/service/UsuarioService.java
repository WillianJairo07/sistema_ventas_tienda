package com.sistemaventas.sistema_ventas.service;

import com.sistemaventas.sistema_ventas.model.Rol;
import com.sistemaventas.sistema_ventas.model.Usuario;
import com.sistemaventas.sistema_ventas.repository.RolRepository;
import com.sistemaventas.sistema_ventas.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UsuarioService {
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public List<Usuario> listarPorEstado(Boolean estado) {
        return usuarioRepository.findAll().stream()
                .filter(u -> u.getEstado().equals(estado))
                .collect(Collectors.toList());
    }

    public void guardar(Usuario usuario) {
        String usernameNuevo = usuario.getUsername().trim();

        // --- VALIDACIÓN DE DUPLICADOS Y AUTO-REVIVIR ---
        Usuario existente = usuarioRepository.findByUsernameIgnoreCase(usernameNuevo).orElse(null);

        if (existente != null) {
            // Caso A: El usuario existe pero está inactivo (Lo revivimos)
            if (!existente.getEstado() && usuario.getIdUsuario() == null) {
                existente.setEstado(true);
                existente.setNombre(usuario.getNombre());
                existente.setRoles(usuario.getRoles());
                // Si envió contraseña nueva la ciframos, si no, mantenemos la que tenía
                if (usuario.getPassword() != null && !usuario.getPassword().isEmpty()) {
                    existente.setPassword(passwordEncoder.encode(usuario.getPassword()));
                }
                usuarioRepository.save(existente);
                return;
            }

            // Caso B: Duplicado en un usuario activo
            if (existente.getEstado()) {
                if (usuario.getIdUsuario() == null || !usuario.getIdUsuario().equals(existente.getIdUsuario())) {
                    throw new IllegalArgumentException("El usuario '" + usernameNuevo + "' ya está en uso.");
                }
            }
        }

        // --- LÓGICA DE CIFRADO NORMAL ---
        if (usuario.getIdUsuario() == null) {
            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
            usuario.setEstado(true);
        } else {
            Usuario userDb = usuarioRepository.findById(usuario.getIdUsuario()).orElse(null);
            if (usuario.getPassword() == null || usuario.getPassword().isEmpty()) {
                usuario.setPassword(userDb.getPassword());
            } else {
                usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
            }
        }
        usuarioRepository.save(usuario);
    }

    public void restaurar(Integer id) {
        Usuario user = usuarioRepository.findById(id).orElse(null);
        if (user != null) {
            user.setEstado(true);
            usuarioRepository.save(user);
        }
    }

    public Usuario buscarPorId(Integer id) {
        return usuarioRepository.findById(id).orElse(null);
    }

    public List<Rol> listarRoles() {
        return rolRepository.findAll();
    }
}