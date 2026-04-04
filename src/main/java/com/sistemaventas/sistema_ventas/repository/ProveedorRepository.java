package com.sistemaventas.sistema_ventas.repository;

import com.sistemaventas.sistema_ventas.model.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Integer> {

    // 1. LISTADOS POR ESTADO
    List<Proveedor> findByEstadoTrueOrderByNombreAsc();
    List<Proveedor> findByEstadoTrueOrderByIdProveedorAsc();
    List<Proveedor> findByEstadoFalseOrderByIdProveedorAsc();

    // 2. VALIDACIONES DE DUPLICADOS (Existencia rápida)
    boolean existsByNombreIgnoreCase(String nombre);
    boolean existsByNombreIgnoreCaseAndIdProveedorNot(String nombre, Integer id);

    // 3. BÚSQUEDA POR NOMBRE (Para el Service)
    // Agregamos este método para que el Service pueda encontrar al proveedor
    // sin importar si está activo o inactivo y comparar sus datos.
    Proveedor findByNombreIgnoreCase(String nombre);

    // 4. AUTO-REVIVIR (Específico para inactivos)
    Proveedor findByNombreIgnoreCaseAndEstadoFalse(String nombre);
}