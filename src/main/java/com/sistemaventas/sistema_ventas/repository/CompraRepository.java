package com.sistemaventas.sistema_ventas.repository;

import com.sistemaventas.sistema_ventas.model.Compra;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;

@Repository
public interface CompraRepository extends JpaRepository<Compra, Integer> {

    @Query("SELECT COALESCE(SUM(c.total), 0) FROM Compra c " +
            "WHERE c.fechaContabilizacion BETWEEN :inicio AND :fin")
    BigDecimal sumTotalComprasMes(@Param("inicio") LocalDate inicio,
                                  @Param("fin") LocalDate fin);

    @Query("SELECT c FROM Compra c " +
            "WHERE (:proveedor IS NULL OR CAST(:proveedor AS String) = '' " +
            "   OR LOWER(c.proveedor.nombre) LIKE LOWER(CONCAT('%', CAST(:proveedor AS String), '%'))) " +
            "AND (CAST(:fechaDesde AS LocalDate) IS NULL OR c.fechaInicio >= :fechaDesde) " +
            "AND (CAST(:fechaHasta AS LocalDate) IS NULL OR c.fechaInicio <= :fechaHasta) " +
            "ORDER BY c.fechaInicio DESC, c.idCompra DESC") // <--- ORDEN PROFESIONAL
    Page<Compra> filtrar(@Param("proveedor") String proveedor,
                         @Param("fechaDesde") LocalDate fechaDesde,
                         @Param("fechaHasta") LocalDate fechaHasta,
                         Pageable pageable);
}