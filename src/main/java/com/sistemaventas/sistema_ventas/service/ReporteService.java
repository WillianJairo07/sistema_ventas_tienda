package com.sistemaventas.sistema_ventas.service;


import com.sistemaventas.sistema_ventas.repository.VentaRepository;
import com.sistemaventas.sistema_ventas.repository.CompraRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReporteService {
    @Autowired private VentaRepository ventaRepo;
    @Autowired private CompraRepository compraRepo;

    @Transactional(readOnly = true)
    public List<?> obtenerDatos(String tipo, LocalDateTime inicio, LocalDateTime fin) {
        if ("ventas".equals(tipo)) {
            return ventaRepo.findReporteVentas(inicio, fin);
        } else {
            return compraRepo.findReporteCompras(inicio.toLocalDate(), fin.toLocalDate());
        }
    }
}