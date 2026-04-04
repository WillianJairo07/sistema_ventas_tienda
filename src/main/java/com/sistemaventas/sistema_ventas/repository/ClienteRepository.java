package com.sistemaventas.sistema_ventas.repository;

import com.sistemaventas.sistema_ventas.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

    // 1. LISTADOS POR ESTADO (Para tu tabla y tu papelera)
    List<Cliente> findByEstadoTrueOrderByIdClienteAsc();
    List<Cliente> findByEstadoFalseOrderByIdClienteAsc();

    // 2. VALIDACIÓN DE DUPLICADOS
    // Nota: El nombre del método debe coincidir EXACTAMENTE con los atributos del Model
    boolean existsByNombreIgnoreCaseAndApellidoPatIgnoreCaseAndApellidoMatIgnoreCase(
            String nombre, String apellidoPat, String apellidoMat);

    // 3. AUTO-REVIVIR
    // Buscamos al cliente inactivo para activarlo si intentan registrarlo de nuevo
    Optional<Cliente> findByNombreIgnoreCaseAndApellidoPatIgnoreCaseAndApellidoMatIgnoreCaseAndEstadoFalse(
            String nombre, String apellidoPat, String apellidoMat);

    // 4. BÚSQUEDA ADICIONAL (Opcional pero recomendado)
    // Si decides agregar DNI al modelo, usa esto:
    // boolean existsByDni(String dni);
}