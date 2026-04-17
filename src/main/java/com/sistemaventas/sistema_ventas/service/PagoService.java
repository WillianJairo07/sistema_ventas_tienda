package com.sistemaventas.sistema_ventas.service;

import com.sistemaventas.sistema_ventas.model.Pago;
import com.sistemaventas.sistema_ventas.model.Venta;
import com.sistemaventas.sistema_ventas.repository.PagoRepository;
import com.sistemaventas.sistema_ventas.repository.VentaRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class PagoService {

    @Autowired
    private PagoRepository pagoRepo;

    @Autowired
    private VentaRepository ventaRepo;

    @Transactional
    public Pago registrarPago(Integer idVenta, BigDecimal montoAbono, String metodo, String nota) {
        Venta venta = ventaRepo.findById(idVenta)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada"));

        BigDecimal pagadoActualmente = pagoRepo.totalPagadoVenta(idVenta);
        if (pagadoActualmente == null) pagadoActualmente = BigDecimal.ZERO;

        BigDecimal saldoPendiente = venta.getTotalVenta().subtract(pagadoActualmente);

        // Validación con margen de error para decimales
        if (montoAbono.compareTo(saldoPendiente.add(new BigDecimal("0.01"))) > 0) {
            throw new RuntimeException("El monto excede el saldo pendiente (S/ " + saldoPendiente + ")");
        }

        Pago nuevoPago = new Pago();
        nuevoPago.setVenta(venta);
        nuevoPago.setMonto(montoAbono);
        nuevoPago.setMetodoPago(metodo);
        nuevoPago.setNota(nota);

        // GUARDAR CON HORA Y MINUTOS
        nuevoPago.setFecha(LocalDateTime.now());

        return pagoRepo.save(nuevoPago);
    }
}
