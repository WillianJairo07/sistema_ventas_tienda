package com.sistemaventas.sistema_ventas.service;

import com.sistemaventas.sistema_ventas.model.Cliente;
import com.sistemaventas.sistema_ventas.repository.ClienteRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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
    public void guardar(Cliente cliente) {
        // --- LIMPIEZA Y FORMATEO (Garantiza orden en la BD) ---
        String nombreLimpio = limpiarTexto(cliente.getNombre());
        String apePatLimpio = limpiarTexto(cliente.getApellidoPat());
        String apeMatLimpio = limpiarTexto(cliente.getApellidoMat());

        if (nombreLimpio.isEmpty() || apePatLimpio.isEmpty()) {
            throw new IllegalArgumentException("Nombre y Apellido Paterno son obligatorios.");
        }

        // --- LÓGICA DE AUTO-REVIVIR / DUPLICADOS ---
        if (cliente.getIdCliente() == null) {
            // Buscamos si ya existe en la "papelera" para restaurarlo
            Cliente inactivo = clienteRepository.findByNombreIgnoreCaseAndApellidoPatIgnoreCaseAndApellidoMatIgnoreCaseAndEstadoFalse(
                    nombreLimpio, apePatLimpio, apeMatLimpio).orElse(null);

            if (inactivo != null) {
                inactivo.setEstado(true);
                // Si el modelo tiene DNI, podrías actualizarlo aquí: inactivo.setDni(cliente.getDni());
                clienteRepository.save(inactivo);
                return;
            }

            // Validación de duplicados en registros activos
            if (clienteRepository.existsByNombreIgnoreCaseAndApellidoPatIgnoreCaseAndApellidoMatIgnoreCase(
                    nombreLimpio, apePatLimpio, apeMatLimpio)) {
                throw new IllegalArgumentException("¡ERROR! Este cliente ya se encuentra registrado y activo.");
            }

            cliente.setEstado(true); // Nace activo
        }

        // --- ASIGNACIÓN DE DATOS FORMATEADOS ---
        cliente.setNombre(nombreLimpio);
        cliente.setApellidoPat(apePatLimpio);
        cliente.setApellidoMat(apeMatLimpio);

        clienteRepository.save(cliente);
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