package com.sistemaventas.sistema_ventas.repository;

import com.sistemaventas.sistema_ventas.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Integer> {


    @Query("SELECT p FROM Producto p JOIN FETCH p.categoria WHERE p.estado = true ORDER BY p.idProducto ASC")
    List<Producto> findByEstadoTrueOptimized();

    @Query("SELECT p FROM Producto p JOIN FETCH p.categoria WHERE p.estado = false ORDER BY p.idProducto ASC")
    List<Producto> findByEstadoFalseOptimized();


    List<Producto> findByEstadoTrue();

    boolean existsByNombreProductoIgnoreCase(String nombre);
    boolean existsByCodigoBarras(String codigo);
    boolean existsByCodigoBarrasAndIdProductoNot(String codigo, Integer id);


    Producto findByNombreProductoIgnoreCaseAndEstadoFalse(String nombre);


    long countByStockLessThanEqualAndEstadoTrue(Integer limite);



}