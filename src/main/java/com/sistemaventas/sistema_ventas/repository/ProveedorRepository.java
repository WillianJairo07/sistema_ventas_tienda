package com.sistemaventas.sistema_ventas.repository;

import com.sistemaventas.sistema_ventas.model.Proveedor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Integer> {

    Page<Proveedor> findByEstadoOrderByIdProveedorDesc(boolean estado, Pageable pageable);

    // Búsqueda con orden por ID
    Page<Proveedor> findByNombreContainingIgnoreCaseAndEstadoOrderByIdProveedorDesc(String nombre, boolean estado, Pageable pageable);

    // Para los combos también por ID
    List<Proveedor> findByEstadoTrueOrderByIdProveedorDesc();

    // --- VALIDACIONES ---
    boolean existsByNombreIgnoreCase(String nombre);
    boolean existsByNombreIgnoreCaseAndIdProveedorNot(String nombre, Integer id);
    Proveedor findByNombreIgnoreCase(String nombre);
    Proveedor findByNombreIgnoreCaseAndEstadoFalse(String nombre);
}