package com.sistemaventas.sistema_ventas.repository;

import com.sistemaventas.sistema_ventas.model.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

    // Único método necesario para combos
    List<Cliente> findByEstadoTrueOrderByIdClienteDesc();

    // Tu paginación se queda intacta y perfecta
    @Query("SELECT c FROM Cliente c WHERE c.estado = :estado AND " +
            "(LOWER(c.nombre) LIKE LOWER(CONCAT('%', :b, '%')) OR " +
            "LOWER(c.apellidoPat) LIKE LOWER(CONCAT('%', :b, '%')) OR " +
            "CAST(c.idCliente AS string) LIKE CONCAT('%', :b, '%')) " +
            "ORDER BY c.idCliente DESC")
    Page<Cliente> listarPaginado(@Param("estado") boolean estado,
                                 @Param("b") String buscar,
                                 Pageable pageable);

    // =========================================================================
    // OPTIMIZACIÓN DE DUPLICADOS: Blindado contra tildes y mayúsculas en la BD
    // =========================================================================
    @Query("SELECT c FROM Cliente c WHERE " +
            "LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(c.nombre, ' ', ''), 'á','a'), 'é','e'), 'í','i'), 'ó','o'), 'ú','u')) = LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(:nom, ' ', ''), 'á','a'), 'é','e'), 'í','i'), 'ó','o'), 'ú','u')) AND " +
            "LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(c.apellidoPat, ' ', ''), 'á','a'), 'é','e'), 'í','i'), 'ó','o'), 'ú','u')) = LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(:pat, ' ', ''), 'á','a'), 'é','e'), 'í','i'), 'ó','o'), 'ú','u')) AND " +
            "LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(c.apellidoMat, ' ', ''), 'á','a'), 'é','e'), 'í','i'), 'ó','o'), 'ú','u')) = LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(:mat, ' ', ''), 'á','a'), 'é','e'), 'í','i'), 'ó','o'), 'ú','u'))")
    Optional<Cliente> findClienteDuplicadoSinTildesNiEspacios(@Param("nom") String nombre,
                                                              @Param("pat") String apePat,
                                                              @Param("mat") String apeMat);
}