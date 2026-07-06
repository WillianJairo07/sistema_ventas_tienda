package com.sistemaventas.sistema_ventas.repository;

import com.sistemaventas.sistema_ventas.model.CostoAdicionalEnvase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Repository
public interface CostoAdicionalEnvaseRepository extends JpaRepository<CostoAdicionalEnvase, Integer> {
    @Query("SELECT COALESCE(SUM(c.monto), 0) FROM CostoAdicionalEnvase c WHERE c.fechaRegistro BETWEEN :inicio AND :fin")
    BigDecimal sumMontoByFechaRegistroBetween(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);
}
