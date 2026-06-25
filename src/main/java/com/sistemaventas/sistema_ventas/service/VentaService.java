package com.sistemaventas.sistema_ventas.service;

import com.sistemaventas.sistema_ventas.model.*;
import com.sistemaventas.sistema_ventas.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class VentaService {

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private DetalleCompraRepository detalleCompraRepository;

    @Autowired
    private PagoRepository pagoRepository;


    @Autowired
    private MovimientoEnvaseRepository movimientoEnvaseRepository;

    @Autowired
    private  EnvaseRepository envaseRepository;

    @Transactional
    public Venta registrarVenta(Venta venta) {
        if (venta.getDetalles() == null || venta.getDetalles().isEmpty()) {
            throw new RuntimeException("No se puede realizar una venta sin productos.");
        }

        // --- 1. LÓGICA DE STOCK Y PEPS ---
        for (DetalleVenta detalle : venta.getDetalles()) {
            Producto producto = productoRepository.findById(detalle.getProducto().getIdProducto())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

            BigDecimal cantidadAVender = detalle.getCantidad();

            if (producto.getStock().compareTo(cantidadAVender) < 0) {
                throw new RuntimeException("Stock insuficiente para: " + producto.getNombreProducto()
                        + " (Disponible: " + producto.getStock() + " " + producto.getUnidadMedida() + ")");
            }

            List<DetalleCompra> lotes = detalleCompraRepository
                    .findByProductoAndStockActualGreaterThanOrderByCompraFechaAsc(producto, BigDecimal.ZERO);

            BigDecimal pendientePorRestar = cantidadAVender;

            for (DetalleCompra lote : lotes) {
                if (pendientePorRestar.compareTo(BigDecimal.ZERO) <= 0) break;
                BigDecimal stockLote = lote.getStockActual();

                if (stockLote.compareTo(pendientePorRestar) >= 0) {
                    lote.setStockActual(stockLote.subtract(pendientePorRestar));
                    pendientePorRestar = BigDecimal.ZERO;
                } else {
                    pendientePorRestar = pendientePorRestar.subtract(stockLote);
                    lote.setStockActual(BigDecimal.ZERO);
                }
                detalleCompraRepository.save(lote);
            }

            producto.setStock(producto.getStock().subtract(cantidadAVender));
            productoRepository.save(producto);

            // Vínculo bidireccional obligatorio
            detalle.setVenta(venta);
        }

        if (venta.getFecha() == null) {
            venta.setFecha(LocalDateTime.now());
        }

        // --- 2. PREPARACIÓN DE ENVASES ---
        // Procesamos antes de guardar la venta para asegurar integridad en cascada
        if (venta.getMovimientosEnvase() != null) {
            for (MovimientoEnvase mov : venta.getMovimientosEnvase()) {
                Envase envReal = envaseRepository.findById(mov.getEnvase().getIdEnvase())
                        .orElseThrow(() -> new RuntimeException("Envase no existe"));

                int cantidadMovida = mov.getCantidad();
                envReal.setStock(envReal.getStock() - cantidadMovida);

                if (envReal.getStock() < 0) {
                    throw new RuntimeException("Stock insuficiente para: " + envReal.getNombre());
                }
                envaseRepository.save(envReal);

                // Vínculo bidireccional obligatorio
                mov.setEnvase(envReal);
                mov.setVenta(venta);
                mov.setFecha(LocalDateTime.now());
                mov.setCliente(venta.getCliente());
            }
        }

        // --- 3. GUARDAR VENTA (La cascada guardará Detalles y Movimientos automáticamente) ---
        Venta ventaGuardada = ventaRepository.save(venta);

        // --- 4. LÓGICA DE PAGOS INICIALES ---
        BigDecimal pagoInicial = ventaGuardada.getMontoPagado() != null ? ventaGuardada.getMontoPagado() : BigDecimal.ZERO;

        if (pagoInicial.compareTo(BigDecimal.ZERO) > 0) {
            Pago primerPago = new Pago();
            primerPago.setVenta(ventaGuardada);
            primerPago.setMonto(pagoInicial);
            primerPago.setFecha(LocalDateTime.now());
            primerPago.setMetodoPago(ventaGuardada.getMetodoPago() != null ? ventaGuardada.getMetodoPago() : "EFECTIVO");
            primerPago.setNota("Pago inicial realizado al momento de la venta.");

            pagoRepository.save(primerPago);
        }

        return ventaGuardada;
    }

    // =========================================================================
    // OPTIMIZADO: PAGINACIÓN REAL EN BASE DE DATOS (CERO SOBRECARGA EN MEMORIA)
    // =========================================================================
    public Page<Venta> obtenerVentasPaginadas(String buscar, String fechaDesde, String fechaHasta, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        String termino = (buscar != null && !buscar.trim().isEmpty()) ? buscar.trim() : null;
        LocalDateTime desde = null;
        LocalDateTime hasta = null;

        // Conversión segura de filtros temporales
        if (fechaDesde != null && !fechaDesde.trim().isEmpty()) {
            desde = LocalDate.parse(fechaDesde.trim()).atStartOfDay();
        }
        if (fechaHasta != null && !fechaHasta.trim().isEmpty()) {
            hasta = LocalDate.parse(fechaHasta.trim()).atTime(LocalTime.MAX);
        }

        // La base de datos se encarga de resolver los filtros y el ordenamiento en milisegundos
        return ventaRepository.filtrarVentas(termino, desde, hasta, pageable);
    }

    public Venta buscarPorId(Integer id) {
        return ventaRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada con ID: " + id));
    }
}