package com.sistemaventas.sistema_ventas.service;

import com.sistemaventas.sistema_ventas.dto.PagoHistorialDTO;
import com.sistemaventas.sistema_ventas.model.Pago;
import com.sistemaventas.sistema_ventas.model.Venta;
import com.sistemaventas.sistema_ventas.repository.PagoRepository;
import com.sistemaventas.sistema_ventas.repository.VentaRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PagoService {

    @Autowired
    private PagoRepository pagoRepo;

    @Autowired
    private VentaRepository ventaRepo;

    @Transactional
    public Pago registrarPago(Integer idVenta, BigDecimal montoAbono, String metodo, String nota) {
        // 1. Validar existencia de la venta objetivo
        Venta venta = ventaRepo.findById(idVenta)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada"));

        // 2. Calcular el estado financiero real antes del nuevo abono
        BigDecimal pagadoActualmente = pagoRepo.totalPagadoVenta(idVenta);
        if (pagadoActualmente == null) {
            pagadoActualmente = BigDecimal.ZERO;
        }

        BigDecimal saldoPendiente = venta.getTotalVenta().subtract(pagadoActualmente);

        // 3. Validación de límite con margen de seguridad para evitar desbordes de centavos
        if (montoAbono.compareTo(saldoPendiente.add(new BigDecimal("0.01"))) > 0) {
            throw new IllegalArgumentException("El monto excede el saldo pendiente (Saldo actual: S/ " + saldoPendiente + ")");
        }

        // 4. Instanciar y configurar la entidad del nuevo abono
        Pago nuevoPago = new Pago();
        nuevoPago.setVenta(venta);
        nuevoPago.setMonto(montoAbono);
        nuevoPago.setMetodoPago(metodo != null && !metodo.isBlank() ? metodo.trim() : "EFECTIVO");
        nuevoPago.setNota(nota != null ? nota.trim() : null);
        nuevoPago.setFecha(LocalDateTime.now()); // Registro preciso de auditoría temporal

        // 5. Persistir el abono en la base de datos
        Pago pagoGuardado = pagoRepo.save(nuevoPago);

        // ====================================================================
        // OPTIMIZACIÓN DE SINCRONIZACIÓN ATÓMICA (EVITA DESFASES DEFINITIVAMENTE)
        // ====================================================================
        // Forzamos un flush de Hibernate para asegurar que el nuevo pago ya se sume en el motor
        pagoRepo.flush();

        // Recalculamos la suma real directo desde la tabla de pagos para actualizar la cabecera
        BigDecimal nuevoTotalPagado = pagoRepo.totalPagadoVenta(idVenta);
        venta.setMontoPagado(nuevoTotalPagado);

        // Sincronizamos la venta con su estado de cuentas al día
        ventaRepo.save(venta);
        // ====================================================================

        return pagoGuardado;
    }


    public List<PagoHistorialDTO> obtenerPorVenta(Integer idVenta) {
        return pagoRepo.findHistorialSimplificado(idVenta);
    }
}