package com.sistemaventas.sistema_ventas.repository;

import com.sistemaventas.sistema_ventas.model.DetalleVenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DetalleVentaRepository extends JpaRepository<DetalleVenta, Integer> {
    boolean existsByProductoIdProducto(Integer idProducto);
}
