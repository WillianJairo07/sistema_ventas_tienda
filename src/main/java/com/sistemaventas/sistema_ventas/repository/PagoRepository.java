package com.sistemaventas.sistema_ventas.repository;

import com.sistemaventas.sistema_ventas.model.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Integer> {


    List<Pago> findByVentaIdVenta(Integer idVenta);

    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM Pago p WHERE p.venta.idVenta = :idVenta")
    BigDecimal totalPagadoVenta(@Param("idVenta") Integer idVenta);

    @Query("SELECT p.venta.idVenta, COALESCE(SUM(p.monto),0) " +
            "FROM Pago p GROUP BY p.venta.idVenta")
    List<Object[]> sumarPagosAgrupados();
}
