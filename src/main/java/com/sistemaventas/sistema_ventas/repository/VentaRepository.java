package com.sistemaventas.sistema_ventas.repository;

import com.sistemaventas.sistema_ventas.dto.DeudorDTO;
import com.sistemaventas.sistema_ventas.dto.ReporteVentaDTO;
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



    @Query("SELECT v FROM Venta v LEFT JOIN FETCH v.detalles d LEFT JOIN FETCH d.producto WHERE v.idVenta = :id")
    Optional<Venta> findByIdWithDetails(@Param("id") Integer id);

    // =========================================================================
    // NUEVO: FILTRADO Y PAGINACIÓN REAL EN BASE DE DATOS (EVITA OUT OF MEMORY)
    // =========================================================================
    @Query(value = "SELECT DISTINCT v FROM Venta v " +
            "JOIN FETCH v.cliente c " +
            "JOIN FETCH v.usuario u " +
            "WHERE (:buscar IS NULL OR :buscar = '' OR " +
            "       CAST(v.idVenta AS string) LIKE CONCAT('%', :buscar, '%') OR " +
            "       LOWER(c.nombre) LIKE LOWER(CONCAT('%', :buscar, '%')) OR " +
            "       LOWER(c.apellidoPat) LIKE LOWER(CONCAT('%', :buscar, '%'))) " +
            "AND (CAST(:fechaDesde AS timestamp) IS NULL OR v.fecha >= :fechaDesde) " +
            "AND (CAST(:fechaHasta AS timestamp) IS NULL OR v.fecha <= :fechaHasta) " +
            "ORDER BY v.fecha DESC, v.idVenta DESC",
            countQuery = "SELECT COUNT(v) FROM Venta v WHERE " +
                    "(:buscar IS NULL OR :buscar = '' OR " +
                    " CAST(v.idVenta AS string) LIKE CONCAT('%', :buscar, '%') OR " +
                    " LOWER(v.cliente.nombre) LIKE LOWER(CONCAT('%', :buscar, '%')) OR " +
                    " LOWER(v.cliente.apellidoPat) LIKE LOWER(CONCAT('%', :buscar, '%'))) " +
                    "AND (CAST(:fechaDesde AS timestamp) IS NULL OR v.fecha >= :fechaDesde) " +
                    "AND (CAST(:fechaHasta AS timestamp) IS NULL OR v.fecha <= :fechaHasta)")
    Page<Venta> filtrarVentas(@Param("buscar") String buscar,
                              @Param("fechaDesde") LocalDateTime fechaDesde,
                              @Param("fechaHasta") LocalDateTime fechaHasta,
                              Pageable pageable);

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
            "GROUP BY v.id_venta, c.nombre, c.apellido_pat, v.total_venta " +
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


    @Query("SELECT new com.sistemaventas.sistema_ventas.dto.DeudorDTO(" +
            "v.idVenta, CONCAT(c.nombre, ' ', COALESCE(c.apellidoPat, '')), " +
            "(v.totalVenta - COALESCE(SUM(p.monto), 0)), v.fecha) " +
            "FROM Venta v JOIN v.cliente c LEFT JOIN v.pagos p " +
            "GROUP BY v.idVenta, c.nombre, c.apellidoPat, v.totalVenta, v.fecha " +
            "HAVING (v.totalVenta - COALESCE(SUM(p.monto), 0)) > 0 " +
            "ORDER BY v.fecha ASC")
    List<DeudorDTO> findDeudoresPendientes(Pageable pageable);

    @Query("SELECT new com.sistemaventas.sistema_ventas.dto.ReporteVentaDTO(v.idVenta, v.fecha, v.totalVenta, c.nombre) " +
            "FROM Venta v JOIN v.cliente c WHERE v.fecha BETWEEN :inicio AND :fin")
    List<ReporteVentaDTO> findReporteVentas(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

}