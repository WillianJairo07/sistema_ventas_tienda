package com.sistemaventas.sistema_ventas.repository;

import com.sistemaventas.sistema_ventas.model.Categoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Integer> {

    // 1. PARA LA TABLA (Paginación)
    Page<Categoria> findByEstadoTrueOrderByIdCategoriaDesc(Pageable pageable);
    Page<Categoria> findByEstadoFalseOrderByIdCategoriaDesc(Pageable pageable);

    // 2. PARA EL BUSCADOR (Paginación)
    Page<Categoria> findByNombreCategoriaContainingIgnoreCaseAndEstadoOrderByIdCategoriaDesc(String nombre, boolean estado, Pageable pageable);

    // 3. PARA EL COMBO EN PRODUCTOS (Lista completa)
    List<Categoria> findByEstadoTrueOrderByNombreCategoriaAsc();

    // 4. PARA VALIDACIONES ORIGINALES
    Categoria findByNombreCategoriaIgnoreCase(String nombreCategoria);
    boolean existsByNombreCategoriaIgnoreCaseAndIdCategoriaNot(String nombre, Integer id);

    // ==========================================
    // NUEVAS CONSULTAS ÓPTIMAS PARA TILDES (JPQL PORTABLE)
    // ==========================================
    @Query("SELECT c FROM Categoria c WHERE " +
            "LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(c.nombreCategoria, ' ', ''), 'á','a'), 'é','e'), 'í','i'), 'ó','o'), 'ú','u')) = " +
            "LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(:nombre, ' ', ''), 'á','a'), 'é','e'), 'í','i'), 'ó','o'), 'ú','u'))")
    Categoria findByNombreSinTildesNiEspacios(@Param("nombre") String nombre);

    @Query("SELECT COUNT(c) > 0 FROM Categoria c WHERE " +
            "LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(c.nombreCategoria, ' ', ''), 'á','a'), 'é','e'), 'í','i'), 'ó','o'), 'ú','u')) = " +
            "LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(:nombre, ' ', ''), 'á','a'), 'é','e'), 'í','i'), 'ó','o'), 'ú','u')) " +
            "AND c.idCategoria != :id")
    boolean existsByNombreSinTildesNiEspaciosYIdNot(@Param("nombre") String nombre, @Param("id") Integer id);
}