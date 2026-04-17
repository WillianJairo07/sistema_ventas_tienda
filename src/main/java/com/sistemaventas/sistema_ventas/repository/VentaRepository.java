package com.sistemaventas.sistema_ventas.repository;

import com.sistemaventas.sistema_ventas.model.Venta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Integer> {


    @Query("SELECT v FROM Venta v JOIN FETCH v.cliente JOIN FETCH v.usuario ORDER BY v.fecha DESC, v.idVenta DESC")
    List<Venta> findAllOptimized();


    @Query("SELECT v FROM Venta v LEFT JOIN FETCH v.detalles d LEFT JOIN FETCH d.producto WHERE v.idVenta = :id")
    Optional<Venta> findByIdWithDetails(@Param("id") Integer id);

    @Query(value = "SELECT v.id_venta, " +
            "c.nombre || ' ' || COALESCE(c.apellido_pat, ''), " +
            "v.total_venta, " +
            "COALESCE(SUM(p.monto), 0) as pagado, " +
            "(v.total_venta - COALESCE(SUM(p.monto), 0)) as saldo " +
            "FROM venta v " +
            "JOIN cliente c ON c.id_cliente = v.id_cliente " +
            "LEFT JOIN pago p ON p.id_venta = v.id_venta " +
            "WHERE UPPER(v.tipo_venta) = 'CREDITO' " +
            "AND (:buscar IS NULL OR :buscar = '' OR " +
            "     CAST(v.id_venta AS TEXT) LIKE CONCAT('%', :buscar, '%') OR " +
            "     LOWER(c.nombre) LIKE LOWER(CONCAT('%', :buscar, '%')) OR " +
            "     LOWER(c.apellido_pat) LIKE LOWER(CONCAT('%', :buscar, '%'))) " +
            "GROUP BY v.id_venta, c.nombre, c.apellido_pat, v.total_venta " + // <--- Agregamos los campos necesarios
            "HAVING (v.total_venta - COALESCE(SUM(p.monto), 0)) > 0 " +
            "ORDER BY v.id_venta DESC",
            countQuery = "SELECT count(DISTINCT v.id_venta) FROM venta v " +
                    "JOIN cliente c ON c.id_cliente = v.id_cliente " +
                    "LEFT JOIN pago p ON p.id_venta = v.id_venta " +
                    "WHERE UPPER(v.tipo_venta) = 'CREDITO' " +
                    "AND (:buscar IS NULL OR :buscar = '' OR " +
                    "     CAST(v.id_venta AS TEXT) LIKE CONCAT('%', :buscar, '%') OR " +
                    "     LOWER(c.nombre) LIKE LOWER(CONCAT('%', :buscar, '%'))) " +
                    "GROUP BY v.id_venta, v.total_venta " +
                    "HAVING (v.total_venta - COALESCE(SUM(p.monto), 0)) > 0",
            nativeQuery = true)
    Page<Object[]> findVentasPendientesRaw(@Param("buscar") String buscar, Pageable pageable);


    List<Venta> findByFechaBetweenOrderByFechaDesc(LocalDateTime inicio, LocalDateTime fin);


    @Query("SELECT v FROM Venta v JOIN FETCH v.cliente WHERE v.cliente.idCliente = :idCliente ORDER BY v.fecha DESC, v.idVenta DESC")
    List<Venta> findByClienteId(@Param("idCliente") Integer idCliente);


    long countByFechaAfter(LocalDateTime fecha);


    @Query("SELECT COALESCE(SUM(v.totalVenta), 0) FROM Venta v WHERE v.fecha BETWEEN :inicio AND :fin")
    BigDecimal sumVentasEntreFechas(@Param("inicio") LocalDateTime inicio,
                                    @Param("fin") LocalDateTime fin);

    @Query("SELECT v FROM Venta v WHERE v.tipoVenta = 'CREDITO'")
    List<Venta> findVentasCredito();


    @Query("SELECT v.idVenta, v.totalVenta " +
            "FROM Venta v WHERE v.tipoVenta = 'CREDITO'")
    List<Object[]> findVentasCreditoResumen();
}