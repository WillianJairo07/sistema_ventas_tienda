package com.sistemaventas.sistema_ventas.service;

import com.sistemaventas.sistema_ventas.model.Proveedor;
import com.sistemaventas.sistema_ventas.repository.ProveedorRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProveedorServiceImpl implements ProveedorService {

    @Autowired
    private ProveedorRepository repository;

    @Override
    public List<Proveedor> listarTodos() {
        // Ahora usamos el método que filtra solo los activos (estado = true)
        return repository.findByEstadoTrueOrderByIdProveedorAsc();
    }

    @Override
    public List<Proveedor> listarSoloInactivos() {
        // Ya no necesitamos Query Nativa, usamos el método de Spring Data JPA
        return repository.findByEstadoFalseOrderByIdProveedorAsc();
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
        if (proveedor.getNombre() == null || proveedor.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del proveedor no puede estar vacío.");
        }

        // 1. Limpieza estándar (Bonito: "LECHE GLORIA")
        String nombreParaMostrar = proveedor.getNombre().trim().replaceAll("\\s+", " ").toUpperCase();

        // --- BLOQUE AUTO-REVIVIR (Optimizado) ---
        // Buscamos si existe por nombre exacto (ignorando Mayús/Minús)
        Proveedor existente = repository.findByNombreIgnoreCase(nombreParaMostrar);

        if (existente != null) {
            // Caso A: Existe pero está oculto (Lo revivimos)
            if (!existente.isEstado() && proveedor.getIdProveedor() == null) {
                existente.setEstado(true);
                existente.setNombre(nombreParaMostrar);
                existente.setTelefono(proveedor.getTelefono()); // Actualizamos datos si es necesario
                return repository.save(existente);
            }

            // Caso B: Existe, está activo y no es el que estamos editando (Duplicado)
            if (existente.isEstado()) {
                if (proveedor.getIdProveedor() == null || !existente.getIdProveedor().equals(proveedor.getIdProveedor())) {
                    throw new IllegalArgumentException("¡ERROR! El proveedor '" + nombreParaMostrar + "' ya existe.");
                }
            }
        }

        // 2. Si pasó todas las pruebas, configuramos y guardamos
        proveedor.setNombre(nombreParaMostrar);

        // Solo forzamos true si es una creación nueva
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
