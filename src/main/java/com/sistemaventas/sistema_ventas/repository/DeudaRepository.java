package com.sistemaventas.sistema_ventas.repository;

import com.sistemaventas.sistema_ventas.model.Deuda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeudaRepository extends JpaRepository<Deuda, Integer> {
}
