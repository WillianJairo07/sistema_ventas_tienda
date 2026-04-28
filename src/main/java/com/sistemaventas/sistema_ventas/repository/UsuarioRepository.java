package com.sistemaventas.sistema_ventas.repository;

import com.sistemaventas.sistema_ventas.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByUsernameIgnoreCase(String username);

    // Búsqueda paginada por Nombre o Username
    @Query("SELECT u FROM Usuario u WHERE u.estado = :estado AND " +
            "(LOWER(u.nombre) LIKE LOWER(CONCAT('%', :b, '%')) OR " +
            "LOWER(u.username) LIKE LOWER(CONCAT('%', :b, '%')))")
    Page<Usuario> listarPaginado(@Param("estado") boolean estado,
                                 @Param("b") String buscar,
                                 Pageable pageable);
}
