package com.sistemaventas.sistema_ventas.repository;

import com.sistemaventas.sistema_ventas.model.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface ProductoRepository extends JpaRepository<Producto, Integer> {

    List<Producto> findByEstadoTrue();

    @Query(value = "SELECT p FROM Producto p JOIN FETCH p.categoria " +
            "WHERE p.estado = :estado " +
            "AND (:termino IS NULL OR LOWER(p.nombreProducto) LIKE LOWER(CONCAT('%', CAST(:termino AS string), '%')) " +
            "OR LOWER(p.codigoBarras) LIKE LOWER(CONCAT('%', CAST(:termino AS string), '%'))) " +
            "ORDER BY p.idProducto ASC",
            countQuery = "SELECT count(p) FROM Producto p " +
                    "WHERE p.estado = :estado " +
                    "AND (:termino IS NULL OR LOWER(p.nombreProducto) LIKE LOWER(CONCAT('%', CAST(:termino AS string), '%')) " +
                    "OR LOWER(p.codigoBarras) LIKE LOWER(CONCAT('%', CAST(:termino AS string), '%')))")
    Page<Producto> findByEstadoPaginado(@Param("estado") boolean estado,
                                        @Param("termino") String termino,
                                        Pageable pageable);

    // Validaciones para evitar duplicados
    boolean existsByNombreProductoIgnoreCase(String nombre);
    boolean existsByCodigoBarras(String codigo);
    boolean existsByCodigoBarrasAndIdProductoNot(String codigo, Integer id);

    // Para la lógica de "Auto-revivir"
    Producto findByNombreProductoIgnoreCaseAndEstadoFalse(String nombre);

    // Para alertas de stock bajo (Dashboard o reportes)
    long countByStockLessThanEqualAndEstadoTrue(Integer limite);
}