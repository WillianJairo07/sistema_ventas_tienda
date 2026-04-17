package com.sistemaventas.sistema_ventas.repository;

import com.sistemaventas.sistema_ventas.model.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Integer> {


    List<Proveedor> findByEstadoTrueOrderByNombreAsc();
    List<Proveedor> findByEstadoTrueOrderByIdProveedorAsc();
    List<Proveedor> findByEstadoFalseOrderByIdProveedorAsc();


    boolean existsByNombreIgnoreCase(String nombre);
    boolean existsByNombreIgnoreCaseAndIdProveedorNot(String nombre, Integer id);


    Proveedor findByNombreIgnoreCase(String nombre);


    Proveedor findByNombreIgnoreCaseAndEstadoFalse(String nombre);
}