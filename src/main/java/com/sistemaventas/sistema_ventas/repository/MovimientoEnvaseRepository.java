package com.sistemaventas.sistema_ventas.repository;


import com.sistemaventas.sistema_ventas.model.MovimientoEnvase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;



public interface MovimientoEnvaseRepository extends JpaRepository<MovimientoEnvase, Integer> {

    // Calcula cuántos envases debe (o tiene a favor) un cliente específico
    @Query("SELECT COALESCE(SUM(m.cantidad), 0) FROM MovimientoEnvase m WHERE m.cliente.idCliente = :idCliente AND m.envase.idEnvase = :idEnvase")
    Integer obtenerSaldoEnvase(@Param("idCliente") Integer idCliente, @Param("idEnvase") Integer idEnvase);


}
