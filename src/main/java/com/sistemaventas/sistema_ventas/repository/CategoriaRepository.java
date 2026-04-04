package com.sistemaventas.sistema_ventas.repository;

import com.sistemaventas.sistema_ventas.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Integer> {

    // Cambia el orden a NombreCategoria para una mejor UX en el modal
    List<Categoria> findByEstadoTrueOrderByNombreCategoriaAsc();

    // 1. Listados por estado (Reemplaza a tus queries de findInactivos)
    List<Categoria> findByEstadoTrueOrderByIdCategoriaAsc();

    List<Categoria> findByEstadoFalseOrderByIdCategoriaAsc();

    // 2. Validación de duplicados (Ya lo tenías bien)
    Categoria findByNombreCategoriaIgnoreCase(String nombreCategoria);

    // 3. El "Auto-revivir" (Reemplaza a tu buscarPorNombreInactivo)
    // Spring entiende esto y lo hace mucho más rápido que el SQL manual
    Optional<Categoria> findByNombreCategoriaIgnoreCaseAndEstadoFalse(String nombre);
}
