package com.sistemaventas.sistema_ventas.repository;

import com.sistemaventas.sistema_ventas.model.Cliente;
import org.springframework.data.domain.Page;     // <--- IMPORTANTE
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

    // Único método necesario para combos (Activos y por ID)
    List<Cliente> findByEstadoTrueOrderByIdClienteDesc();

    // Paginación con búsqueda por Nombre o Apellido
    @Query("SELECT c FROM Cliente c WHERE c.estado = :estado AND " +
            "(LOWER(c.nombre) LIKE LOWER(CONCAT('%', :b, '%')) OR " +
            "LOWER(c.apellidoPat) LIKE LOWER(CONCAT('%', :b, '%')) OR " +
            "CAST(c.idCliente AS string) LIKE CONCAT('%', :b, '%'))")
    Page<Cliente> listarPaginado(@Param("estado") boolean estado,
                                 @Param("b") String buscar,
                                 Pageable pageable);

    // Búsqueda para evitar duplicados exactos
    Optional<Cliente> findByNombreIgnoreCaseAndApellidoPatIgnoreCaseAndApellidoMatIgnoreCase(
            String nombre, String apePat, String apeMat);
}