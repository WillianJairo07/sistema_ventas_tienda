package com.sistemaventas.sistema_ventas.repository;

import com.sistemaventas.sistema_ventas.model.Proveedor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Integer> {

    Page<Proveedor> findByEstadoOrderByIdProveedorDesc(boolean estado, Pageable pageable);

    Page<Proveedor> findByNombreContainingIgnoreCaseAndEstadoOrderByIdProveedorDesc(String nombre, boolean estado, Pageable pageable);

    List<Proveedor> findByEstadoTrueOrderByIdProveedorDesc();

    // =========================================================================
    // OPTIMIZACIÓN QUIRÚRGICA: Control de duplicados ignorando tildes en la BD
    // =========================================================================

    @Query("SELECT p FROM Proveedor p WHERE " +
            "LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(p.nombre, ' ', ''), 'á','a'), 'é','e'), 'í','i'), 'ó','o'), 'ú','u')) = " +
            "LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(:nombre, ' ', ''), 'á','a'), 'é','e'), 'í','i'), 'ó','o'), 'ú','u'))")
    Proveedor findByNombreSinTildesNiEspacios(@Param("nombre") String nombre);

    @Query("SELECT COUNT(p) > 0 FROM Proveedor p WHERE p.idProveedor != :id AND " +
            "LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(p.nombre, ' ', ''), 'á','a'), 'é','e'), 'í','i'), 'ó','o'), 'ú','u')) = " +
            "LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(:nombre, ' ', ''), 'á','a'), 'é','e'), 'í','i'), 'ó','o'), 'ú','u'))")
    boolean existsByNombreSinTildesNiEspaciosYIdDistinto(@Param("nombre") String nombre, @Param("id") Integer id);
}