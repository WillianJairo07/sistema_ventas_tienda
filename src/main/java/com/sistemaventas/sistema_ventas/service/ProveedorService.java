package com.sistemaventas.sistema_ventas.service;

import com.sistemaventas.sistema_ventas.model.Proveedor;
import com.sistemaventas.sistema_ventas.repository.ProveedorRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Service
public class ProveedorService {

    @Autowired
    private ProveedorRepository repository;

    // 1. LISTADO PAGINADO OPTIMIZADO
    public Page<Proveedor> listarPaginado(boolean estado, String buscar, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        if (buscar != null && !buscar.trim().isEmpty()) {
            return repository.findByNombreContainingIgnoreCaseAndEstadoOrderByIdProveedorDesc(buscar.trim(), estado, pageable);
        }

        return repository.findByEstadoOrderByIdProveedorDesc(estado, pageable);
    }

    // 2. LISTADO PARA COMBOS (Formularios de compras)
    public List<Proveedor> listarParaCombos() {
        return repository.findByEstadoTrueOrderByIdProveedorDesc();
    }

    // 3. RECUPERACIÓN POR ID
    public Proveedor buscarPorId(Integer id) {
        return repository.findById(id).orElse(null);
    }

    // 4. RESTAURAR REGISTRO INACTIVO
    @Transactional
    public void restaurar(Integer id) {
        Proveedor p = repository.findById(id).orElse(null);
        if (p != null) {
            p.setEstado(true);
            repository.save(p);
        } else {
            throw new RuntimeException("No se encontró el proveedor para restaurar.");
        }
    }

    // 5. GUARDAR / EDITAR CON VALIDACIÓN INTEGRAL DE DUPLICADOS Y TILDES
    @Transactional
    public Proveedor guardar(Proveedor proveedor) {
        // Validación de nulidad o espacios vacíos
        if (proveedor.getNombre() == null || proveedor.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del proveedor no puede estar vacío.");
        }

        // Limpieza de espacios redundantes para el nombre a guardar
        String nombreNuevo = proveedor.getNombre().trim().replaceAll("\\s+", " ");

        if (proveedor.getIdProveedor() == null) {
            // Usamos la nueva consulta que ignora espacios y tildes
            Proveedor existente = repository.findByNombreSinTildesNiEspacios(nombreNuevo);

            if (existente != null) {
                if (!existente.isEstado()) {
                    // ... (lógica de auto-revivir igual)
                    return repository.save(existente);
                }
                throw new IllegalArgumentException("Ya existe un proveedor similar");
            }
        } else {
            // Validamos edición ignorando espacios y tildes
            if (repository.existsByNombreSinTildesNiEspaciosYIdDistinto(nombreNuevo, proveedor.getIdProveedor())) {
                throw new IllegalArgumentException("Ya existe un proveedor similar");
            }
        }

        // Seteo de datos limpios y persistencia final
        proveedor.setNombre(nombreNuevo);

        if (proveedor.getIdProveedor() == null) {
            proveedor.setEstado(true);
        }

        return repository.save(proveedor);
    }

    // 6. ELIMINACIÓN LÓGICA (Ocultar sin borrar de la BD)
    @Transactional
    public void eliminar(Integer id) {
        Proveedor p = buscarPorId(id);
        if (p != null) {
            p.setEstado(false);
            repository.save(p);
        }
    }
}