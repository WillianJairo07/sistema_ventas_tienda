package com.sistemaventas.sistema_ventas.repository;

import com.sistemaventas.sistema_ventas.model.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Integer> {

    List<Producto> findByEstadoTrueOrderByIdProductoDesc();

    @Query(value = "SELECT p FROM Producto p JOIN FETCH p.categoria " +
            "WHERE p.estado = :estado " +
            "AND (:termino IS NULL OR LOWER(p.nombreProducto) LIKE LOWER(CONCAT('%', CAST(:termino AS string), '%')) " +
            "OR LOWER(p.codigoBarras) LIKE LOWER(CONCAT('%', CAST(:termino AS string), '%'))) " +
            "ORDER BY p.idProducto DESC",
            countQuery = "SELECT count(p) FROM Producto p " +
                    "WHERE p.estado = :estado " +
                    "AND (:termino IS NULL OR LOWER(p.nombreProducto) LIKE LOWER(CONCAT('%', CAST(:termino AS string), '%')) " +
                    "OR LOWER(p.codigoBarras) LIKE LOWER(CONCAT('%', CAST(:termino AS string), '%')))")
    Page<Producto> findByEstadoPaginado(@Param("estado") boolean estado,
                                        @Param("termino") String termino,
                                        Pageable pageable);

    boolean existsByCodigoBarras(String codigo);
    boolean existsByCodigoBarrasAndIdProductoNot(String codigo, Integer id);
    long countByStockLessThanEqualAndEstadoTrue(BigDecimal limite);

    // =========================================================================
    // OPTIMIZACIÓN DE TEXTO CON TILDES (JPQL PORTABLE)
    // =========================================================================

    // 1. Verifica si ya existe por nombre ignorando tildes
    @Query("SELECT COUNT(p) > 0 FROM Producto p WHERE " +
            "LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(p.nombreProducto, ' ', ''), 'á','a'), 'é','e'), 'í','i'), 'ó','o'), 'ú','u')) = " +
            "LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(:nombre, ' ', ''), 'á','a'), 'é','e'), 'í','i'), 'ó','o'), 'ú','u')) " +
            "AND (:id IS NULL OR p.idProducto != :id)")
    boolean existsByNombreSinTildesNiEspacios(@Param("nombre") String nombre, @Param("id") Integer id);

    // 2. Busca inactivo ignorando tildes Y espacios para el Auto-revivir
    @Query("SELECT p FROM Producto p WHERE p.estado = false AND " +
            "LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(p.nombreProducto, ' ', ''), 'á','a'), 'é','e'), 'í','i'), 'ó','o'), 'ú','u')) = " +
            "LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(:nombre, ' ', ''), 'á','a'), 'é','e'), 'í','i'), 'ó','o'), 'ú','u'))")
    Producto findInactivoByNombreSinTildesNiEspacios(@Param("nombre") String nombre);

    // 3. Método requerido por el módulo de Compras para no usar findAll()
    @Query("SELECT p FROM Producto p WHERE " +
            "LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(p.nombreProducto, ' ', ''), 'á','a'), 'é','e'), 'í','i'), 'ó','o'), 'ú','u')) = " +
            "LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(:nombre, ' ', ''), 'á','a'), 'é','e'), 'í','i'), 'ó','o'), 'ú','u'))")
    Optional<Producto> findByNombreSinTildesNiEspacios(@Param("nombre") String nombre);
}