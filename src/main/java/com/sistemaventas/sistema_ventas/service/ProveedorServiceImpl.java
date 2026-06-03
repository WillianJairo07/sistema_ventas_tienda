package com.sistemaventas.sistema_ventas.service;

import com.sistemaventas.sistema_ventas.model.Proveedor;
import com.sistemaventas.sistema_ventas.repository.ProveedorRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Page;         // La interfaz que contiene los resultados
import org.springframework.data.domain.PageRequest;  // Para crear la solicitud (page, size)
import org.springframework.data.domain.Pageable;

import java.util.List;

@Service
public class ProveedorServiceImpl implements ProveedorService {

    @Autowired
    private ProveedorRepository repository;

    private String normalizar(String texto) {
        if (texto == null) return "";
        // Elimina tildes y caracteres especiales
        String normalizado = java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD);
        normalizado = normalizado.replaceAll("\\p{M}", "");
        // Elimina todos los espacios y pasa a minúsculas
        return normalizado.replace(" ", "").toLowerCase();
    }

    @Override
    public Page<Proveedor> listarPaginado(boolean estado, String buscar, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        if (buscar != null && !buscar.trim().isEmpty()) {
            return repository.findByNombreContainingIgnoreCaseAndEstadoOrderByIdProveedorDesc(buscar.trim(), estado, pageable);
        }

        return repository.findByEstadoOrderByIdProveedorDesc(estado, pageable);
    }

    @Override
    public List<Proveedor> listarParaCombos() {
        return repository.findByEstadoTrueOrderByIdProveedorDesc();
    }

    @Override
    @Transactional
    public void restaurar(Integer id) {
        // findById ya encuentra proveedores aunque estén en estado false
        Proveedor p = repository.findById(id).orElse(null);
        if (p != null) {
            p.setEstado(true);
            repository.save(p);
        } else {
            throw new RuntimeException("No se encontró el proveedor para restaurar.");
        }
    }

    @Override
    @Transactional
    public Proveedor guardar(Proveedor proveedor) {
        // 1. Validación de nulidad
        if (proveedor.getNombre() == null || proveedor.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del proveedor no puede estar vacío.");
        }

        // 2. Limpieza de espacios (Mantenemos formato original del usuario)
        String nombreNuevo = proveedor.getNombre().trim().replaceAll("\\s+", " ");

        // 3. VALIDACIÓN QUIRÚRGICA (Sin cargar toda la lista en memoria)

        // Escenario A: Estamos CREANDO un proveedor nuevo (id es null)
        if (proveedor.getIdProveedor() == null) {
            // Buscamos si ya existe alguien con ese nombre (ignora mayúsculas/minúsculas)
            Proveedor existente = repository.findByNombreIgnoreCase(nombreNuevo);

            if (existente != null) {
                // Si existe pero está oculto (estado=false), lo RE-ACTIVAMOS (Auto-revivir)
                if (!existente.isEstado()) {
                    existente.setEstado(true);
                    existente.setNombre(nombreNuevo); // Actualizamos al nombre exacto
                    existente.setTelefono(proveedor.getTelefono());
                    return repository.save(existente);
                }
                // Si existe y ya está activo, lanzamos el error corto
                throw new IllegalArgumentException("Ya existe un proveedor similar");
            }
        }
        // Escenario B: Estamos EDITANDO uno existente
        else {
            // Verificamos si el nombre choca con OTRO ID que no sea el nuestro
            if (repository.existsByNombreIgnoreCaseAndIdProveedorNot(nombreNuevo, proveedor.getIdProveedor())) {
                throw new IllegalArgumentException("Ya existe un proveedor similar");
            }
        }

        // 4. GUARDADO FINAL
        proveedor.setNombre(nombreNuevo);

        // Solo forzamos true si es una creación desde cero
        if (proveedor.getIdProveedor() == null) {
            proveedor.setEstado(true);
        }

        return repository.save(proveedor);
    }

    @Override
    public Proveedor buscarPorId(Integer id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    public void eliminar(Integer id) {
        // IMPORTANTE: Cambio a Borrado Lógico
        Proveedor p = buscarPorId(id);
        if (p != null) {
            p.setEstado(false);
            repository.save(p);
        }
    }

}
