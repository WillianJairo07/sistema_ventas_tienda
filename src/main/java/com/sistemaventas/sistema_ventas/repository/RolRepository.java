package com.sistemaventas.sistema_ventas.repository;

import com.sistemaventas.sistema_ventas.model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RolRepository extends JpaRepository<Rol, Integer> {
}