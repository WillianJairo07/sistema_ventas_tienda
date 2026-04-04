package com.sistemaventas.sistema_ventas.repository;

import com.sistemaventas.sistema_ventas.model.DetalleCompra;
import com.sistemaventas.sistema_ventas.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface DetalleCompraRepository extends JpaRepository<DetalleCompra, Integer> {

    boolean existsByProductoIdProducto(Integer idProducto);
    // En DetalleCompraRepository.java
    List<DetalleCompra> findByProductoAndStockActualGreaterThanOrderByCompraFechaAsc(Producto producto, BigDecimal stock);
}
