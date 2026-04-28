package com.sistemaventas.sistema_ventas.service;

import com.sistemaventas.sistema_ventas.model.Proveedor;
import org.springframework.data.domain.Page;
import java.util.List;

public interface ProveedorService {


    Page<Proveedor> listarPaginado(boolean estado, String buscar, int page, int size);


    List<Proveedor> listarParaCombos();


    Proveedor guardar(Proveedor proveedor);
    Proveedor buscarPorId(Integer id);


    void eliminar(Integer id);
    void restaurar(Integer id);
}