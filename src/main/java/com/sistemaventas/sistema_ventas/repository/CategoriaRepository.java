package com.sistemaventas.sistema_ventas.repository;

import com.sistemaventas.sistema_ventas.model.Categoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Integer> {

    // 1. PARA LA TABLA (Paginación) - Ahora con Desc para ver lo más reciente arriba
    Page<Categoria> findByEstadoTrueOrderByIdCategoriaDesc(Pageable pageable);
    Page<Categoria> findByEstadoFalseOrderByIdCategoriaDesc(Pageable pageable);

    // 2. PARA EL BUSCADOR (Paginación) - Añadimos el ordenamiento también aquí
    Page<Categoria> findByNombreCategoriaContainingIgnoreCaseAndEstadoOrderByIdCategoriaDesc(String nombre, boolean estado, Pageable pageable);

    // 3. PARA EL COMBO EN PRODUCTOS (Lista completa)
    // Aquí mantenemos Asc por NOMBRE, ya que en un select es mejor buscar alfabéticamente
    List<Categoria> findByEstadoTrueOrderByNombreCategoriaAsc();

    // 4. PARA VALIDACIONES (Guardar/Editar)
    Categoria findByNombreCategoriaIgnoreCase(String nombreCategoria);
    boolean existsByNombreCategoriaIgnoreCaseAndIdCategoriaNot(String nombre, Integer id);
}