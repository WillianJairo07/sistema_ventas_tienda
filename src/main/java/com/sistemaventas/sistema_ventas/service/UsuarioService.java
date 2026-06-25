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

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // EVITA TRABAJO PESADO AL MOTOR SI EL BUSCADOR LLEGA VACÍO
    public Page<Usuario> listarPaginado(boolean estado, String buscar, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        if (buscar != null && !buscar.trim().isEmpty()) {
            return usuarioRepository.listarPaginado(estado, buscar.trim(), pageable);
        }
        return usuarioRepository.findByEstadoOrderByIdUsuarioDesc(estado, pageable);
    }

    // FLUJO CONSOLIDADO DE GUARDADO Y EDICIÓN CON VALIDACIONES ATÓMICAS
    @Transactional
    public void guardar(Usuario usuario) {
        if (usuario.getUsername() == null || usuario.getUsername().isBlank()) {
            throw new IllegalArgumentException("El nombre de usuario es obligatorio.");
        }
        if (usuario.getNombre() == null || usuario.getNombre().isBlank() ||
                usuario.getApellidoPaterno() == null || usuario.getApellidoPaterno().isBlank()) {
            throw new IllegalArgumentException("El Nombre y Apellido Paterno del trabajador son requeridos.");
        }

        // 1. Sanitizar el identificador de acceso (Username sin ningún tipo de espacios)
        String usernameLimpio = usuario.getUsername().trim().replaceAll("\\s+", "");
        usuario.setUsername(usernameLimpio);

        // 2. Sanitizar datos de texto real del empleado (Evita dobles espacios accidentales)
        String nombreLimpio = usuario.getNombre().trim().replaceAll("\\s+", " ");
        String apePatLimpio = usuario.getApellidoPaterno().trim().replaceAll("\\s+", " ");
        usuario.setNombre(nombreLimpio);
        usuario.setApellidoPaterno(apePatLimpio);
        if (usuario.getApellidoMaterno() != null) {
            usuario.setApellidoMaterno(usuario.getApellidoMaterno().trim().replaceAll("\\s+", " "));
        }

        // ====================================================================
        // VALIDACIÓN DE IDENTIDAD: DETECTA EL MISMO TRABAJADOR (CON TILDES/CAJAS)
        // ====================================================================
        Usuario trabajadorExistente = usuarioRepository
                .encontrarPorNombreYApellidoCompletoSinEspacios(nombreLimpio, apePatLimpio).orElse(null);

        if (trabajadorExistente != null && (usuario.getIdUsuario() == null || !usuario.getIdUsuario().equals(trabajadorExistente.getIdUsuario()))) {
            if (trabajadorExistente.getEstado()) {
                throw new IllegalArgumentException("Ya existe un trabajador activo registrado.");
            } else if (usuario.getIdUsuario() == null) {
                revivirUsuario(trabajadorExistente, usuario);
                return;
            }
        }

        // ====================================================================
        // VALIDACIÓN DE CREDENCIALES: EVITA DUPLICADOS DE USERNAME
        // ====================================================================
        Usuario cuentaExistente = usuarioRepository.findByUsernameIgnoreCase(usernameLimpio).orElse(null);

        if (cuentaExistente != null && (usuario.getIdUsuario() == null || !usuario.getIdUsuario().equals(cuentaExistente.getIdUsuario()))) {
            if (!cuentaExistente.getEstado() && usuario.getIdUsuario() == null) {
                revivirUsuario(cuentaExistente, usuario);
                return;
            }
            throw new IllegalArgumentException("El nombre de usuario '" + usernameLimpio + "' ya se encuentra asignado.");
        }

        // ====================================================================
        // PERSISTENCIA CRIPTOGRÁFICA DE CONTRASEÑAS
        // ====================================================================
        if (usuario.getIdUsuario() == null) {
            if (usuario.getPassword() == null || usuario.getPassword().isBlank()) {
                throw new IllegalArgumentException("La contraseña es obligatoria para la creación de la cuenta.");
            }
            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
            usuario.setEstado(true);
        } else {
            Usuario userDb = buscarPorId(usuario.getIdUsuario());
            // Si el input de contraseña llega en blanco, preservamos el hash actual intacto
            if (usuario.getPassword() == null || usuario.getPassword().isEmpty()) {
                usuario.setPassword(userDb.getPassword());
            } else {
                usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
            }
            usuario.setEstado(userDb.getEstado());
        }

        usuarioRepository.save(usuario);
    }

    // ENCARGADO DE RESTAURAR CUENTAS DE TRABAJADORES DADOS DE BAJA PREVIAMENTE
    private void revivirUsuario(Usuario existente, Usuario nuevo) {
        existente.setEstado(true);
        existente.setNombre(nuevo.getNombre());
        existente.setApellidoPaterno(nuevo.getApellidoPaterno());
        existente.setApellidoMaterno(nuevo.getApellidoMaterno());
        existente.setRol(nuevo.getRol());
        existente.setUsername(nuevo.getUsername());

        if (nuevo.getPassword() != null && !nuevo.getPassword().isEmpty()) {
            existente.setPassword(passwordEncoder.encode(nuevo.getPassword()));
        }
        usuarioRepository.save(existente);
    }

    @Transactional
    public void restaurar(Integer id) {
        Usuario user = buscarPorId(id);
        if (user != null) {
            user.setEstado(true);
            usuarioRepository.save(user);
        }
    }

    @Transactional
    public void eliminar(Integer id) {
        Usuario user = buscarPorId(id);
        if (user != null) {
            user.setEstado(false);
            usuarioRepository.save(user);
        }
    }

    public Usuario buscarPorId(Integer id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + id));
    }

    public List<Rol> listarRoles() {
        return rolRepository.findAll();
    }

    @Transactional
    public void actualizarPassword(Integer id, String nuevaPassword) {
        if (nuevaPassword == null || nuevaPassword.isBlank()) {
            throw new IllegalArgumentException("La nueva contraseña no puede estar en blanco.");
        }
        Usuario u = buscarPorId(id);
        u.setPassword(passwordEncoder.encode(nuevaPassword));
        usuarioRepository.save(u);
    }

    public Usuario buscarPorUsername(String username) {
        return usuarioRepository.findByUsernameIgnoreCase(username.trim())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + username));
    }

    @Transactional
    public void cambiarPasswordPersonal(String username, String passwordActual, String nuevaPassword) {
        if (nuevaPassword == null || nuevaPassword.isBlank()) {
            throw new IllegalArgumentException("La contraseña de destino no es válida.");
        }
        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(username.trim())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (!passwordEncoder.matches(passwordActual, usuario.getPassword())) {
            throw new IllegalArgumentException("La contraseña actual ingresada es incorrecta.");
        }

        usuario.setPassword(passwordEncoder.encode(nuevaPassword));
        usuarioRepository.save(usuario);
    }
}