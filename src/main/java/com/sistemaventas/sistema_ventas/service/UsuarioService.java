package com.sistemaventas.sistema_ventas.service;

import com.sistemaventas.sistema_ventas.model.Rol;
import com.sistemaventas.sistema_ventas.model.Usuario;
import com.sistemaventas.sistema_ventas.repository.RolRepository;
import com.sistemaventas.sistema_ventas.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService {
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    public Page<Usuario> listarPaginado(boolean estado, String buscar, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        String b = (buscar != null) ? buscar.trim() : "";
        return usuarioRepository.listarPaginado(estado, b, pageable);
    }

    @Transactional
    public void guardar(Usuario usuario) {
        String usernameLimpio = usuario.getUsername().trim();
        Usuario existente = usuarioRepository.findByUsernameIgnoreCase(usernameLimpio).orElse(null);

        // Lógica de validación y auto-revivir
        if (existente != null) {
            if (!existente.getEstado() && usuario.getIdUsuario() == null) {
                revivirUsuario(existente, usuario);
                return;
            }
            if (existente.getEstado() && (usuario.getIdUsuario() == null || !usuario.getIdUsuario().equals(existente.getIdUsuario()))) {
                throw new IllegalArgumentException("El usuario '" + usernameLimpio + "' ya está en uso.");
            }
        }

        if (usuario.getIdUsuario() == null) {
            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
            usuario.setEstado(true);
        } else {
            Usuario userDb = buscarPorId(usuario.getIdUsuario());
            if (usuario.getPassword() == null || usuario.getPassword().isEmpty()) {
                usuario.setPassword(userDb.getPassword());
            } else {
                usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
            }
            usuario.setEstado(userDb.getEstado());
        }
        usuarioRepository.save(usuario);
    }

    private void revivirUsuario(Usuario existente, Usuario nuevo) {
        existente.setEstado(true);
        existente.setNombre(nuevo.getNombre());
        existente.setRoles(nuevo.getRoles());
        if (nuevo.getPassword() != null && !nuevo.getPassword().isEmpty()) {
            existente.setPassword(passwordEncoder.encode(nuevo.getPassword()));
        }
        usuarioRepository.save(existente);
    }

    public void restaurar(Integer id) {
        Usuario user = buscarPorId(id);
        if (user != null) { user.setEstado(true); usuarioRepository.save(user); }
    }

    public Usuario buscarPorId(Integer id) {
        return usuarioRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }

    public List<Rol> listarRoles() { return rolRepository.findAll(); }

    @Transactional
    public void actualizarPassword(Integer id, String nuevaPassword) {
        Usuario u = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // IMPORTANTE: Encriptar antes de guardar
        u.setPassword(passwordEncoder.encode(nuevaPassword));
        usuarioRepository.save(u);
    }


    public Usuario buscarPorUsername(String username) {
        return usuarioRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + username));
    }
}