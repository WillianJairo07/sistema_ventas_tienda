package com.sistemaventas.sistema_ventas.service;

import com.sistemaventas.sistema_ventas.model.Cliente;
import com.sistemaventas.sistema_ventas.model.Envase;
import com.sistemaventas.sistema_ventas.model.MovimientoEnvase;
import com.sistemaventas.sistema_ventas.model.Venta;
import com.sistemaventas.sistema_ventas.repository.EnvaseRepository;
import com.sistemaventas.sistema_ventas.repository.MovimientoEnvaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EnvaseService {

    @Autowired
    private MovimientoEnvaseRepository movimientoEnvaseRepo;

    @Autowired
    private EnvaseRepository envaseRepo;

    public List<Envase> listarEnvasesParaVenta() {
        return envaseRepository.listarEnvasesParaVenta();
    }

    @Transactional
    public void registrarMovimiento(Cliente cliente, Envase envase, Integer cantidad, String tipo, Venta venta, BigDecimal montoDejado) {
        MovimientoEnvase movimiento = new MovimientoEnvase();
        movimiento.setCliente(cliente);
        movimiento.setEnvase(envase);
        movimiento.setCantidad(cantidad); // Positivo para préstamo, negativo para devolución
        movimiento.setTipo(tipo);
        movimiento.setFecha(LocalDateTime.now());
        movimiento.setVenta(venta);
        movimiento.setMontoGarantiaDejado(montoDejado != null ? montoDejado : BigDecimal.ZERO);
        movimientoEnvaseRepo.save(movimiento);
    }

    @Autowired private EnvaseRepository envaseRepository;

    public Page<Envase> listarPaginado(boolean activos, int page, int size, String buscar) {
        Pageable pageable = PageRequest.of(page, size);
        if (buscar != null && !buscar.trim().isEmpty()) {
            return envaseRepository.findByNombreContainingIgnoreCaseAndEstadoOrderByIdEnvaseDesc(buscar.trim(), activos, pageable);
        }
        return activos ? envaseRepository.findByEstadoTrueOrderByIdEnvaseDesc(pageable)
                : envaseRepository.findByEstadoFalseOrderByIdEnvaseDesc(pageable);
    }

    @Transactional
    public void guardar(Envase envase) {
        String nombreNuevo = envase.getNombre().trim();
        if (envase.getIdEnvase() == null) {
            Envase existente = envaseRepository.findByNombreSinTildesNiEspacios(nombreNuevo);
            if (existente != null) {
                if (!existente.isEstado()) { existente.setEstado(true); envaseRepository.save(existente); return; }
                throw new IllegalArgumentException("Ya existe un envase similar");
            }
        } else if (envaseRepository.existsByNombreSinTildesNiEspaciosYIdNot(nombreNuevo, envase.getIdEnvase())) {
            throw new IllegalArgumentException("Ya existe otro envase similar");
        }
        envase.setNombre(nombreNuevo);
        envaseRepository.save(envase);
    }

    @Transactional
    public void restaurar(Integer id) {
        Envase e = envaseRepository.findById(id).orElseThrow();
        e.setEstado(true);
        envaseRepository.save(e);
    }

    @Transactional
    public void eliminar(Integer id) {
        Envase e = envaseRepository.findById(id).orElseThrow();
        e.setEstado(false);
        envaseRepository.save(e);
    }
}
