package com.sistemaventas.sistema_ventas.repository;

import com.sistemaventas.sistema_ventas.model.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Integer> {

    // 1. LISTADO GENERAL (Historial de Ventas)
    // Se ordena por fecha descendente y por ID descendente para desempate
    @Query("SELECT v FROM Venta v JOIN FETCH v.cliente JOIN FETCH v.usuario ORDER BY v.fecha DESC, v.idVenta DESC")
    List<Venta> findAllOptimized();

    // 2. BUSCAR UNA VENTA ESPECÍFICA CON SUS DETALLES
    @Query("SELECT v FROM Venta v LEFT JOIN FETCH v.detalles d LEFT JOIN FETCH d.producto WHERE v.idVenta = :id")
    Optional<Venta> findByIdWithDetails(@Param("id") Integer id);

    // 3. FILTROS PARA EL DASHBOARD Y REPORTES
    // Mantiene el orden descendente para que los reportes de rango de fechas sean lógicos
    List<Venta> findByFechaBetweenOrderByFechaDesc(LocalDateTime inicio, LocalDateTime fin);

    // 4. BÚSQUEDA POR CLIENTE
    // Se añade el ID como segundo criterio de orden
    @Query("SELECT v FROM Venta v JOIN FETCH v.cliente WHERE v.cliente.idCliente = :idCliente ORDER BY v.fecha DESC, v.idVenta DESC")
    List<Venta> findByClienteId(@Param("idCliente") Integer idCliente);

    // 5. MÉTODO PARA CONTAR VENTAS DEL DÍA
    long countByFechaAfter(LocalDateTime fecha);
}