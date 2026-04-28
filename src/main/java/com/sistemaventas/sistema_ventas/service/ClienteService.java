package com.sistemaventas.sistema_ventas.service;

import com.sistemaventas.sistema_ventas.model.Cliente;
import com.sistemaventas.sistema_ventas.repository.ClienteRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;        // <--- Importante
import org.springframework.data.domain.PageRequest; // <--- Importante
import org.springframework.data.domain.Pageable;    // <--- Importante
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    // 1. LISTADO PAGINADO (Optimizado para la tabla principal)
    public Page<Cliente> listarPaginado(boolean estado, String buscar, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        String b = (buscar != null) ? buscar.trim() : "";
        return clienteRepository.listarPaginado(estado, b, pageable);
    }

    // 2. LISTADO PARA COMBOS (Para el formulario de Ventas)
    public List<Cliente> listarParaCombos() {
        return clienteRepository.findByEstadoTrueOrderByIdClienteDesc();
    }

    @Transactional
    public Cliente guardar(Cliente cliente) {
        // Limpieza y estandarización de textos (Todo a MAYÚSCULAS para evitar líos)
        String nombreLimpio = limpiarTexto(cliente.getNombre());
        String apePatLimpio = limpiarTexto(cliente.getApellidoPat());
        String apeMatLimpio = limpiarTexto(cliente.getApellidoMat());

        if (nombreLimpio.isEmpty() || apePatLimpio.isEmpty()) {
            throw new IllegalArgumentException("Nombre y Apellido Paterno son obligatorios.");
        }

        // Lógica de validación de Duplicados / Auto-revivir
        Optional<Cliente> existenteOpt = clienteRepository.findByNombreIgnoreCaseAndApellidoPatIgnoreCaseAndApellidoMatIgnoreCase(
                nombreLimpio, apePatLimpio, apeMatLimpio);

        if (existenteOpt.isPresent()) {
            Cliente existente = existenteOpt.get();

            // Si es un registro NUEVO pero los datos ya existen en la base de datos
            if (cliente.getIdCliente() == null) {
                if (!existente.isEstado()) {
                    // Si estaba inactivo, lo "revivimos" con los nuevos datos (si hubiera cambios)
                    existente.setEstado(true);
                    return clienteRepository.save(existente);
                }
                throw new IllegalArgumentException("¡ERROR! Este cliente ya se encuentra registrado y activo.");
            }

            // Si es una EDICIÓN, verificamos que no estemos duplicando a otro cliente diferente
            if (cliente.getIdCliente() != null && !existente.getIdCliente().equals(cliente.getIdCliente())) {
                throw new IllegalArgumentException("Ya existe otro cliente con el mismo nombre y apellidos.");
            }
        }

        // Seteo de datos limpios
        cliente.setNombre(nombreLimpio);
        cliente.setApellidoPat(apePatLimpio);
        cliente.setApellidoMat(apeMatLimpio);

        if (cliente.getIdCliente() == null) {
            cliente.setEstado(true); // Registros nuevos siempre nacen activos
        }

        return clienteRepository.save(cliente);
    }

    @Transactional
    public void eliminarLogico(Integer id) {
        Cliente c = buscarPorId(id);
        if (c != null) {
            c.setEstado(false);
            clienteRepository.save(c);
        }
    }

    @Transactional
    public void restaurar(Integer id) {
        Cliente c = buscarPorId(id);
        if (c != null) {
            c.setEstado(true);
            clienteRepository.save(c);
        }
    }

    public Cliente buscarPorId(Integer id) {
        return clienteRepository.findById(id).orElse(null);
    }

    private String limpiarTexto(String texto) {
        // Trim, quita espacios dobles y pasa a MAYÚSCULAS
        return (texto == null) ? "" : texto.trim().replaceAll("\\s+", " ").toUpperCase();
    }
}