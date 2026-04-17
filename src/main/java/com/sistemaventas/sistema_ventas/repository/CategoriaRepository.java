package com.sistemaventas.sistema_ventas.repository;

import com.sistemaventas.sistema_ventas.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Integer> {

    List<Categoria> findByEstadoTrueOrderByNombreCategoriaAsc();


    List<Categoria> findByEstadoTrueOrderByIdCategoriaAsc();

    List<Categoria> findByEstadoFalseOrderByIdCategoriaAsc();


    Categoria findByNombreCategoriaIgnoreCase(String nombreCategoria);

    Optional<Categoria> findByNombreCategoriaIgnoreCaseAndEstadoFalse(String nombre);
}
