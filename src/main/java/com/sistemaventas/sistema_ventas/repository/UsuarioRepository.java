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

    // Verificación directa de credenciales ignorando mayúsculas/minúsculas
    Optional<Usuario> findByUsernameIgnoreCase(String username);

    // Listado veloz optimizado por estado (cuando no hay texto de búsqueda)
    Page<Usuario> findByEstadoOrderByIdUsuarioDesc(boolean estado, Pageable pageable);

    // Búsqueda avanzada por coincidencia parcial de texto
    @Query("SELECT u FROM Usuario u WHERE u.estado = :estado AND " +
            "(LOWER(u.nombre) LIKE LOWER(CONCAT('%', :b, '%')) OR " +
            "LOWER(u.username) LIKE LOWER(CONCAT('%', :b, '%'))) " +
            "ORDER BY u.idUsuario DESC")
    Page<Usuario> listarPaginado(@Param("estado") boolean estado,
                                 @Param("b") String buscar,
                                 Pageable pageable);

    // ESCUDO ANTI-DUPLICADOS DE EMPLEADOS: Ignora mayúsculas, minúsculas, espacios y tildes
    @Query("SELECT u FROM Usuario u WHERE " +
            "LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(u.nombre, ' ', ''), 'á', 'a'), 'é', 'e'), 'í', 'i'), 'ó', 'o'), 'ú', 'u')) = " +
            "LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(:nombre, ' ', ''), 'á', 'a'), 'é', 'e'), 'í', 'i'), 'ó', 'o'), 'ú', 'u')) AND " +
            "LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(u.apellidoPaterno, ' ', ''), 'á', 'a'), 'é', 'e'), 'í', 'i'), 'ó', 'o'), 'ú', 'u')) = " +
            "LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(:apePat, ' ', ''), 'á', 'a'), 'é', 'e'), 'í', 'i'), 'ó', 'o'), 'ú', 'u'))")
    Optional<Usuario> encontrarPorNombreYApellidoCompletoSinEspacios(@Param("nombre") String nombre, @Param("apePat") String apePat);
}