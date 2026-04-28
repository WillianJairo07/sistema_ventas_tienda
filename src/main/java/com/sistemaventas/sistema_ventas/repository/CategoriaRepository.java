package com.sistemaventas.sistema_ventas.repository;

import com.sistemaventas.sistema_ventas.model.Categoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Integer> {

    // 1. PARA LA TABLA (Paginación)
    Page<Categoria> findByEstadoTrueOrderByIdCategoriaAsc(Pageable pageable);
    Page<Categoria> findByEstadoFalseOrderByIdCategoriaAsc(Pageable pageable);

    // 2. PARA EL BUSCADOR (Paginación)
    Page<Categoria> findByNombreCategoriaContainingIgnoreCaseAndEstado(String nombre, boolean estado, Pageable pageable);

    // 3. PARA EL COMBO EN PRODUCTOS (Lista completa)
    // Es vital que este devuelva List y no Page para que el select de Productos no falle
    List<Categoria> findByEstadoTrueOrderByNombreCategoriaAsc();

    // 4. PARA VALIDACIONES (Guardar/Editar)
    Categoria findByNombreCategoriaIgnoreCase(String nombreCategoria);
}