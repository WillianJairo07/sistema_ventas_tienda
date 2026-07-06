package com.sistemaventas.sistema_ventas.repository;

import com.sistemaventas.sistema_ventas.model.Envase;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnvaseRepository extends JpaRepository<Envase, Integer> {
    @Query("SELECT e FROM Envase e WHERE e.estado = true ORDER BY e.nombre ASC")
    List<Envase> listarEnvasesParaVenta();

    // 1. PARA LA TABLA (Paginación)
    Page<Envase> findByEstadoTrueOrderByIdEnvaseDesc(Pageable pageable);
    Page<Envase> findByEstadoFalseOrderByIdEnvaseDesc(Pageable pageable);

    // 2. PARA EL BUSCADOR (Paginación)
    Page<Envase> findByNombreContainingIgnoreCaseAndEstadoOrderByIdEnvaseDesc(String nombre, boolean estado, Pageable pageable);

    // 3. Validación de duplicados (Ignorando tildes y espacios)
    @Query("SELECT e FROM Envase e WHERE " +
            "LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(e.nombre, ' ', ''), 'á','a'), 'é','e'), 'í','i'), 'ó','o'), 'ú','u')) = " +
            "LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(:nombre, ' ', ''), 'á','a'), 'é','e'), 'í','i'), 'ó','o'), 'ú','u'))")
    Envase findByNombreSinTildesNiEspacios(@Param("nombre") String nombre);

    @Query("SELECT COUNT(e) > 0 FROM Envase e WHERE " +
            "LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(e.nombre, ' ', ''), 'á','a'), 'é','e'), 'í','i'), 'ó','o'), 'ú','u')) = " +
            "LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(:nombre, ' ', ''), 'á','a'), 'é','e'), 'í','i'), 'ó','o'), 'ú','u')) " +
            "AND e.idEnvase != :id")
    boolean existsByNombreSinTildesNiEspaciosYIdNot(@Param("nombre") String nombre, @Param("id") Integer id);


    long countByStockLessThan(Integer stock);


}
