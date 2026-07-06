package com.sistemaventas.sistema_ventas.service;

import java.util.Map;
import java.util.stream.Collectors;

import com.sistemaventas.sistema_ventas.dto.DeudorDTO;
import com.sistemaventas.sistema_ventas.dto.ProductoDTO;
import com.sistemaventas.sistema_ventas.model.Producto;
import com.sistemaventas.sistema_ventas.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DashboardService {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private PagoRepository pagoRepository;

    @Autowired
    private EnvaseRepository envaseRepository;

    @Autowired
    private CostoAdicionalEnvaseRepository costoAdicionalEnvaseRepository;

    // STOCK CRÍTICO
    public long obtenerProductosCriticos() {
        // Definimos el límite como BigDecimal (5 unidades)
        BigDecimal limiteCritico = new BigDecimal("5.0");
        return productoRepository.countByStockLessThanEqualAndEstadoTrue(limiteCritico);
    }

    // COMPRAS MES
    public BigDecimal obtenerComprasMes() {
        LocalDate inicio = LocalDate.now().withDayOfMonth(1);
        return compraRepository.sumTotalComprasMes(inicio, LocalDate.now());
    }

    // VENTAS MES
    public BigDecimal obtenerVentasMes() {
        System.out.println("Dashboard ejecutado - obtenerVentasMes");
        LocalDateTime inicio = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        return ventaRepository.sumVentasEntreFechas(inicio, LocalDateTime.now());
    }

    // DEUDAS (OPTIMIZADO)
    public BigDecimal obtenerTotalDeudas() {

        List<Object[]> creditos = ventaRepository.findVentasCreditoResumen();
        List<Object[]> pagos = pagoRepository.sumarPagosAgrupados();

        Map<Integer, BigDecimal> pagosMap = pagos.stream()
                .collect(Collectors.toMap(
                        p -> (Integer) p[0],
                        p -> (BigDecimal) p[1]
                ));

        BigDecimal totalDeudas = BigDecimal.ZERO;

        for (Object[] v : creditos) {

            Integer idVenta = (Integer) v[0];
            BigDecimal totalVenta = (BigDecimal) v[1];

            BigDecimal pagado = pagosMap.getOrDefault(idVenta, BigDecimal.ZERO);

            BigDecimal deuda = totalVenta.subtract(pagado);

            if (deuda.compareTo(BigDecimal.ZERO) > 0) {
                totalDeudas = totalDeudas.add(deuda);
            }
        }

        return totalDeudas;
    }

    // CANTIDAD DE DEUDORES (OPTIMIZADO)
    public long obtenerCantidadDeudores() {

        List<Object[]> creditos = ventaRepository.findVentasCreditoResumen();
        List<Object[]> pagos = pagoRepository.sumarPagosAgrupados();

        Map<Integer, BigDecimal> pagosMap = pagos.stream()
                .collect(Collectors.toMap(
                        p -> (Integer) p[0],
                        p -> (BigDecimal) p[1]
                ));

        long count = 0;

        for (Object[] v : creditos) {

            Integer idVenta = (Integer) v[0];
            BigDecimal totalVenta = (BigDecimal) v[1];

            BigDecimal pagado = pagosMap.getOrDefault(idVenta, BigDecimal.ZERO);

            if (totalVenta.compareTo(pagado) > 0) {
                count++;
            }
        }

        return count;
    }

    // En tu DashboardService
    public List<ProductoDTO> obtenerTopProductosCriticos() {
        return productoRepository.findByEstadoTrueOrderByIdProductoDesc()
                .stream()
                .filter(p -> p.getStock().compareTo(new BigDecimal("5.0")) <= 0)
                .limit(5)
                // Aquí haces la magia: conviertes la entidad al DTO
                .map(p -> new ProductoDTO(
                        p.getNombreProducto(),
                        p.getStock(),
                        p.getUnidadMedida()
                ))
                .collect(Collectors.toList());
    }

    public List<DeudorDTO> obtenerDeudoresAntiguos() {
        // Pedimos solo los 5 más antiguos usando Pageable
        return ventaRepository.findDeudoresPendientes(PageRequest.of(0, 5));
    }


    // 1. STOCK CRÍTICO DE ENVASES
    public long obtenerEnvasesCriticos() {
        // Asumimos un stock bajo si es menor a 5 unidades
        return envaseRepository.countByStockLessThan(5);
    }

    // 2. COSTOS ADICIONALES POR MES
    public BigDecimal obtenerCostosEnvaseMes() {
        LocalDateTime inicioMes = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime finMes = LocalDateTime.now();
        // Este método debe estar en tu CostoAdicionalEnvaseRepository
        return costoAdicionalEnvaseRepository.sumMontoByFechaRegistroBetween(inicioMes, finMes);
    }


}