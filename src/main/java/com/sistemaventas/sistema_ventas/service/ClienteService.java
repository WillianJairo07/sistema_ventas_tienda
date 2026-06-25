package com.sistemaventas.sistema_ventas.service;

import com.sistemaventas.sistema_ventas.model.Cliente;
import com.sistemaventas.sistema_ventas.repository.ClienteRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    public Page<Cliente> listarPaginado(boolean estado, String buscar, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        String b = (buscar != null) ? buscar.trim() : "";
        return clienteRepository.listarPaginado(estado, b, pageable);
    }

    public List<Cliente> listarParaCombos() {
        return clienteRepository.findByEstadoTrueOrderByIdClienteDesc();
    }

    @Transactional
    public Cliente guardar(Cliente cliente) {
        // Limpieza de espacios extras
        String nombreLimpio = limpiarTexto(cliente.getNombre());
        String apePatLimpio = limpiarTexto(cliente.getApellidoPat());
        String apeMatLimpio = limpiarTexto(cliente.getApellidoMat());

        if (nombreLimpio.isEmpty() || apePatLimpio.isEmpty()) {
            throw new IllegalArgumentException("Nombre y Apellido Paterno son obligatorios.");
        }

        // NUEVA VALIDACIÓN OPTIMIZADA (Usa la consulta que ignora tildes de la BD)
        Optional<Cliente> existenteOpt = clienteRepository.findClienteDuplicadoSinTildesNiEspacios(
                nombreLimpio, apePatLimpio, apeMatLimpio);

        if (existenteOpt.isPresent()) {
            Cliente existente = existenteOpt.get();

            // Si es un registro NUEVO
            if (cliente.getIdCliente() == null) {
                if (!existente.isEstado()) {
                    // Auto-revivir intacto
                    existente.setEstado(true);
                    existente.setNombre(nombreLimpio);
                    existente.setApellidoPat(apePatLimpio);
                    existente.setApellidoMat(apeMatLimpio);
                    return clienteRepository.save(existente);
                }
                throw new IllegalArgumentException("¡Ya existe un cliente similar");
            }

            // Si es una EDICIÓN
            if (cliente.getIdCliente() != null && !existente.getIdCliente().equals(cliente.getIdCliente())) {
                throw new IllegalArgumentException("Ya existe un cliente con el mismo nombre y apellidos.");
            }
        }

        // Seteo de datos limpios
        cliente.setNombre(nombreLimpio);
        cliente.setApellidoPat(apePatLimpio);
        cliente.setApellidoMat(apeMatLimpio);

        if (cliente.getIdCliente() == null) {
            cliente.setEstado(true);
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
        return (texto == null) ? "" : texto.trim().replaceAll("\\s+", " ");
    }
}