package com.sistemaventas.sistema_ventas.service;

import com.sistemaventas.sistema_ventas.model.*;
import com.sistemaventas.sistema_ventas.repository.DetalleCompraRepository;
import com.sistemaventas.sistema_ventas.repository.DeudaRepository;
import com.sistemaventas.sistema_ventas.repository.ProductoRepository;
import com.sistemaventas.sistema_ventas.repository.VentaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VentaService {

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private DetalleCompraRepository detalleCompraRepository;

    @Autowired
    private DeudaRepository deudaRepository;


    @Transactional
    public Venta registrarVenta(Venta venta) {
        if (venta.getDetalles() == null || venta.getDetalles().isEmpty()) {
            throw new RuntimeException("No se puede realizar una venta sin productos.");
        }

        for (DetalleVenta detalle : venta.getDetalles()) {
            Producto producto = productoRepository.findById(detalle.getProducto().getIdProducto())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

            BigDecimal cantidadAVender = detalle.getCantidad();

            // 1. VALIDACIÓN DE STOCK
            if (producto.getStock().compareTo(cantidadAVender) < 0) {
                throw new RuntimeException("Stock insuficiente para: " + producto.getNombreProducto()
                        + " (Disponible: " + producto.getStock() + " " + producto.getUnidadMedida() + ")");
            }

            // 2. LÓGICA DE LOTES (PEPS)
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

            // 3. ACTUALIZAR STOCK GENERAL
            producto.setStock(producto.getStock().subtract(cantidadAVender));
            productoRepository.save(producto);

            detalle.setVenta(venta);
        }

        // --- GUARDAMOS LA VENTA PRIMERO ---
        Venta ventaGuardada = ventaRepository.save(venta);

        // --- LÓGICA DE DEUDA (SOLO SI ES CRÉDITO) ---
        if ("CREDITO".equalsIgnoreCase(ventaGuardada.getTipoVenta())) {
            Deuda nuevaDeuda = new Deuda();
            nuevaDeuda.setVenta(ventaGuardada);

            // Calculamos: Total - Monto Pagado (Inicial)
            BigDecimal total = ventaGuardada.getTotalVenta() != null ? ventaGuardada.getTotalVenta() : BigDecimal.ZERO;
            BigDecimal pagoInicial = ventaGuardada.getMontoPagado() != null ? ventaGuardada.getMontoPagado() : BigDecimal.ZERO;
            BigDecimal saldoPendiente = total.subtract(pagoInicial);

            // Solo creamos deuda si realmente hay un saldo pendiente
            if (saldoPendiente.compareTo(BigDecimal.ZERO) > 0) {
                nuevaDeuda.setMontoDeuda(saldoPendiente);
                nuevaDeuda.setEstado(false); // false = Sigue debiendo
                nuevaDeuda.setFechaPago(null);
                deudaRepository.save(nuevaDeuda);
            }
        }

        return ventaGuardada;
    }


    public Page<Venta> obtenerVentasPaginadas(String buscar, String fechaDesde, String fechaHasta, int page, int size) {
        // 1. Traemos todo en un solo viaje a la BD usando tu método optimizado
        List<Venta> todas = ventaRepository.findAllOptimized();

        // 2. Filtramos en memoria (Java) para evitar errores de tipos en PostgreSQL
        List<Venta> filtradas = todas.stream()
                .filter(v -> {
                    // Filtro de búsqueda (ID o Nombre de Cliente)
                    if (buscar != null && !buscar.trim().isEmpty()) {
                        String b = buscar.toLowerCase().trim();
                        String nombreCliente = (v.getCliente().getNombre() + " " + v.getCliente().getApellidoPat()).toLowerCase();
                        if (!nombreCliente.contains(b) && !v.getIdVenta().toString().contains(b)) {
                            return false;
                        }
                    }

                    // Filtro de Fecha Desde
                    if (fechaDesde != null && !fechaDesde.isEmpty()) {
                        LocalDateTime desde = LocalDate.parse(fechaDesde).atStartOfDay();
                        if (v.getFecha().isBefore(desde)) return false;
                    }

                    // Filtro de Fecha Hasta
                    if (fechaHasta != null && !fechaHasta.isEmpty()) {
                        LocalDateTime hasta = LocalDate.parse(fechaHasta).atTime(LocalTime.MAX);
                        if (v.getFecha().isAfter(hasta)) return false;
                    }

                    return true;
                })
                .collect(Collectors.toList());

        // 3. Paginación manual de la lista filtrada
        int start = (int) PageRequest.of(page, size).getOffset();
        int end = Math.min((start + size), filtradas.size());

        // Si la página solicitada está fuera de rango, devolvemos lista vacía
        if (start > filtradas.size()) {
            return new PageImpl<>(List.of(), PageRequest.of(page, size), filtradas.size());
        }

        List<Venta> subList = filtradas.subList(start, end);
        return new PageImpl<>(subList, PageRequest.of(page, size), filtradas.size());
    }

    public Venta buscarPorId(Integer id) {
        return ventaRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada con ID: " + id));
    }
}

