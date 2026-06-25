package com.sistemaventas.sistema_ventas.dto;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DeudorDTO(Integer idVenta,
                        String nombreCliente,
                        BigDecimal saldo,
                        LocalDateTime fecha) {


}
