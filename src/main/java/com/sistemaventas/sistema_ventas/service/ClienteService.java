package com.sistemaventas.sistema_ventas.service;

import com.sistemaventas.sistema_ventas.model.Cliente;
import com.sistemaventas.sistema_ventas.repository.ClienteRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    // 1. LISTADO DINÁMICO (Activos o Inactivos)
    public List<Cliente> listarClientes(boolean verInactivos) {
        return verInactivos ?
                clienteRepository.findByEstadoFalseOrderByIdClienteAsc() :
                clienteRepository.findByEstadoTrueOrderByIdClienteAsc();
    }

    @Transactional
    public Cliente guardar(Cliente cliente) {
        // 1. Limpieza de texto (Mantiene tu BD ordenada)
        String nombreLimpio = limpiarTexto(cliente.getNombre());
        String apePatLimpio = limpiarTexto(cliente.getApellidoPat());
        String apeMatLimpio = limpiarTexto(cliente.getApellidoMat());

        if (nombreLimpio.isEmpty() || apePatLimpio.isEmpty()) {
            throw new IllegalArgumentException("Nombre y Apellido Paterno son obligatorios.");
        }

        // 2. Lógica para registros nuevos (cuando idCliente es null)
        if (cliente.getIdCliente() == null) {
            // Buscamos si el cliente existe en TOTAL (activos + inactivos)
            Optional<Cliente> existenteOpt = clienteRepository.findByNombreIgnoreCaseAndApellidoPatIgnoreCaseAndApellidoMatIgnoreCase(
                    nombreLimpio, apePatLimpio, apeMatLimpio);

            if (existenteOpt.isPresent()) {
                Cliente existente = existenteOpt.get();

                if (!existente.isEstado()) {
                    // ESCENARIO A: Estaba en la "papelera" -> Lo revivimos
                    existente.setEstado(true);
                    return clienteRepository.save(existente);
                } else {
                    // ESCENARIO B: Ya está activo -> Error para evitar duplicado real
                    throw new IllegalArgumentException("¡ERROR! Este cliente ya se encuentra registrado y activo.");
                }
            }

            // Si no existe ni rastro de él, nace activo
            cliente.setEstado(true);
        }

        // 3. Seteo de datos limpios y guardado final
        cliente.setNombre(nombreLimpio);
        cliente.setApellidoPat(apePatLimpio);
        cliente.setApellidoMat(apeMatLimpio);

        return clienteRepository.save(cliente);
    }

    @Transactional
    public void eliminarLogico(Integer id) {
        Cliente c = buscarPorId(id);
        if (c != null) {
            c.setEstado(false); // @SQLDelete en el modelo se encargará del resto
            clienteRepository.save(c);
        } else {
            throw new IllegalArgumentException("El cliente con ID " + id + " no existe.");
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
        // Al no tener @Where en el modelo, este método encuentra activos e inactivos
        return clienteRepository.findById(id).orElse(null);
    }

    private String limpiarTexto(String texto) {
        if (texto == null) return "";
        // Trim, quita espacios dobles y pasa a MAYÚSCULAS
        return texto.trim().replaceAll("\\s+", " ").toUpperCase();
    }
}