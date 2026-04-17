package com.sistemaventas.sistema_ventas.repository;

import com.sistemaventas.sistema_ventas.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Integer> {


    List<Cliente> findByEstadoTrueOrderByIdClienteAsc();
    List<Cliente> findByEstadoFalseOrderByIdClienteAsc();


    boolean existsByNombreIgnoreCaseAndApellidoPatIgnoreCaseAndApellidoMatIgnoreCase(
            String nombre, String apellidoPat, String apellidoMat);



    Optional<Cliente> findByNombreIgnoreCaseAndApellidoPatIgnoreCaseAndApellidoMatIgnoreCaseAndEstadoFalse(
            String nombre, String apellidoPat, String apellidoMat);


    Optional<Cliente> findByNombreIgnoreCaseAndApellidoPatIgnoreCaseAndApellidoMatIgnoreCase(
            String nombre, String apellidoPat, String apellidoMat);

}