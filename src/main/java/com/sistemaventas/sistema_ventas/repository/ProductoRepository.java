package com.sistemaventas.sistema_ventas.repository;

import com.sistemaventas.sistema_ventas.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Integer> {

    // 1. LISTADOS OPTIMIZADOS (JOIN FETCH para evitar el problema N+1)
    @Query("SELECT p FROM Producto p JOIN FETCH p.categoria WHERE p.estado = true ORDER BY p.idProducto ASC")
    List<Producto> findByEstadoTrueOptimized();

    @Query("SELECT p FROM Producto p JOIN FETCH p.categoria WHERE p.estado = false ORDER BY p.idProducto ASC")
    List<Producto> findByEstadoFalseOptimized();


    List<Producto> findByEstadoTrue();
    // 2. VALIDACIONES DE DUPLICADOS
    boolean existsByNombreProductoIgnoreCase(String nombre);
    boolean existsByCodigoBarras(String codigo);
    boolean existsByCodigoBarrasAndIdProductoNot(String codigo, Integer id);

    // 3. AUTO-REVIVIR
    Producto findByNombreProductoIgnoreCaseAndEstadoFalse(String nombre);

    // 4. INDICADORES PARA DASHBOARD (Solución al error)
    // Cuenta productos con stock bajo (<= límite) pero que estén ACTIVOS
    long countByStockLessThanEqualAndEstadoTrue(Integer limite);



}