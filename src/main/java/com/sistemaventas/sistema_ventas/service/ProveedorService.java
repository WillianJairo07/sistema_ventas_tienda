package com.sistemaventas.sistema_ventas.service;

import com.sistemaventas.sistema_ventas.model.Proveedor;
import java.util.List;

public interface ProveedorService {

    // 1. LISTADOS: Diferenciamos claramente entre las dos "vistas" de tu tabla
    List<Proveedor> listarTodos(); // Este devolverá solo los Activos (estado = true)
    List<Proveedor> listarSoloInactivos(); // Este devolverá los de la "Papelera"

    // 2. OPERACIONES DE PERSISTENCIA
    Proveedor guardar(Proveedor proveedor);
    Proveedor buscarPorId(Integer id);

    // 3. GESTIÓN DE ESTADO (Borrado Lógico)
    // Cambiamos el concepto de "borrar físicamente" por "deshabilitar"
    void eliminar(Integer id);

    // El método para traer de vuelta un proveedor desde los inactivos
    void restaurar(Integer id);
}