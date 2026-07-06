package com.sistemaventas.sistema_ventas.service;

import com.sistemaventas.sistema_ventas.model.*;
import com.sistemaventas.sistema_ventas.repository.CompraRepository;
import com.sistemaventas.sistema_ventas.repository.EnvaseRepository;
import com.sistemaventas.sistema_ventas.repository.MovimientoEnvaseRepository;
import com.sistemaventas.sistema_ventas.repository.ProductoRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class CompraService {

    @Autowired private CompraRepository compraRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private EnvaseRepository envaseRepository;
    @Autowired private MovimientoEnvaseRepository movimientoEnvaseRepository;

    public Page<Compra> filtrarCompras(String proveedor, String fechaDesde, String fechaHasta,
                                       int page, int size) {

        LocalDate desde = null;
        LocalDate hasta = null;

        try {
            if (fechaDesde != null && !fechaDesde.trim().isEmpty()) desde = LocalDate.parse(fechaDesde);
            if (fechaHasta != null && !fechaHasta.trim().isEmpty()) hasta = LocalDate.parse(fechaHasta);
        } catch (DateTimeParseException e) {
            throw new RuntimeException("Error: El formato de fecha debe ser YYYY-MM-DD");
        }

        // --- NUEVA VALIDACIÓN DE SEGURIDAD EN BACKEND ---
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw new RuntimeException("La fecha hasta no puede ser anterior a la fecha desde.");
        }
        // -------------------------------------------------

        Pageable pageable = PageRequest.of(page, size,
                Sort.by("estado").descending()
                        .and(Sort.by("fechaInicio").descending())
                        .and(Sort.by("idCompra").descending()));

        String proveedorLimpio = (proveedor != null && !proveedor.trim().isEmpty()) ? proveedor.trim() : null;
        return compraRepository.filtrar(proveedorLimpio, desde, hasta, pageable);
    }

    @Transactional
    public void registrarCompra(Compra compra) {
        // 1. Validaciones de negocio originales
        validarDatosBasicos(compra);
        // DEBUG: ¿Cuántos costos llegan aquí?
        System.out.println("CANTIDAD DE COSTOS RECIBIDOS: " + (compra.getCostosAdicionales() != null ? compra.getCostosAdicionales().size() : "NULO"));

        // 2. Asegurar fecha de contabilización para evitar restricciones Nullable en BD
        if (compra.getFechaContabilizacion() == null) {
            compra.setFechaContabilizacion(LocalDate.now());
        }

        for (DetalleCompra detalle : compra.getDetalles()) {
            validarDetalle(detalle);

            // OPTIMIZADO: Busca quirúrgicamente el producto uno por uno en BD sin usar Listas masivas en RAM
            procesarProducto(detalle);

            detalle.setStockActual(detalle.getCantidad());
            detalle.setCompra(compra);
        }

        compra.setEstado("PENDIENTE");
        calcularYAsignarTotal(compra);
        compraRepository.save(compra);
    }

    @Transactional
    public void actualizarCompraPendiente(Compra compraActualizada) {
        Compra existente = compraRepository.findById(compraActualizada.getIdCompra())
                .orElseThrow(() -> new RuntimeException("No existe la compra a editar."));

        if (!"PENDIENTE".equals(existente.getEstado())) {
            throw new RuntimeException("Solo se pueden editar compras en estado PENDIENTE.");
        }

        compraActualizada.setFechaInicio(existente.getFechaInicio());

        // 1. Sincronizar DETALLES (Ya lo tenías)
        for (DetalleCompra detalle : compraActualizada.getDetalles()) {
            validarDetalle(detalle);
            procesarProducto(detalle);
            if (detalle.getStockActual() == null) detalle.setStockActual(detalle.getCantidad());
            detalle.setCompra(compraActualizada);
        }
        existente.getDetalles().clear();
        existente.getDetalles().addAll(compraActualizada.getDetalles());

        // 2. --- AQUÍ ESTÁ LA SOLUCIÓN: Sincronizar ENVASES ---
        existente.getMovimientosEnvase().clear();
        if (compraActualizada.getMovimientosEnvase() != null) {
            for (MovimientoEnvase mov : compraActualizada.getMovimientosEnvase()) {
                mov.setCompra(existente); // Vincular cada movimiento a la compra persistente
                existente.getMovimientosEnvase().add(mov);
            }
        }
        // ----------------------------------------------------

        // 3. --- NUEVO: Sincronizar FALTANTES (Costos Adicionales) ---
        existente.getCostosAdicionales().clear();
        if (compraActualizada.getCostosAdicionales() != null) {
            for (CostoAdicionalEnvase faltante : compraActualizada.getCostosAdicionales()) {
                faltante.setCompra(existente); // Vinculamos a la compra persistente
                existente.getCostosAdicionales().add(faltante);
            }
        }

        calcularYAsignarTotal(compraActualizada);

        existente.setFecha(compraActualizada.getFecha());
        existente.setProveedor(compraActualizada.getProveedor());
        existente.setTotal(compraActualizada.getTotal());
        existente.setFechaContabilizacion(LocalDate.now());

        compraRepository.save(existente);
    }

    @Transactional
    public void confirmarRecepcion(Integer idCompra, String tipoComprobante, boolean esComprobantePropio) {
        Compra compra = compraRepository.findById(idCompra)
                .orElseThrow(() -> new RuntimeException("Compra no encontrada"));

        if (!"PENDIENTE".equals(compra.getEstado())) {
            throw new RuntimeException("Esta compra ya fue completada.");
        }

        compra.setTipoComprobante(tipoComprobante);
        compra.setEsComprobantePropio(esComprobantePropio);
        compra.setFechaRecepcion(LocalDateTime.now());

        for (DetalleCompra detalle : compra.getDetalles()) {
            Producto prod = productoRepository.findById(detalle.getProducto().getIdProducto())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

            BigDecimal stockActualEnProducto = prod.getStock() != null ? prod.getStock() : BigDecimal.ZERO;
            prod.setStock(stockActualEnProducto.add(detalle.getCantidad()));
            detalle.setStockActual(detalle.getCantidad());

            productoRepository.save(prod);
        }

        // --- LÓGICA DE ENVASES CON VALIDACIÓN DE SEGURIDAD ---
        if (compra.getMovimientosEnvase() != null) {
            for (MovimientoEnvase mov : compra.getMovimientosEnvase()) {
                Envase env = envaseRepository.findById(mov.getEnvase().getIdEnvase())
                        .orElseThrow(() -> new RuntimeException("Envase no existe"));

                // Si el movimiento es negativo (SALIDA), validamos el stock
                if (mov.getCantidad() < 0) {
                    int cantidadSalida = Math.abs(mov.getCantidad());
                    if (env.getStock() < cantidadSalida) {
                        throw new RuntimeException("Stock insuficiente para el envase: " + env.getNombre()
                                + ". Disponible: " + env.getStock());
                    }
                }

                // Actualizamos stock (esto funciona tanto para entrada como para salida)
                env.setStock(env.getStock() + mov.getCantidad());
                envaseRepository.save(env);

                mov.setCompra(compra);
                mov.setFecha(LocalDateTime.now());
                movimientoEnvaseRepository.save(mov);
            }
        }

        compra.setEstado("COMPLETADA");
        compraRepository.save(compra);
    }

    // --- MÉTODOS DE APOYO (Lógica Interna Optimizada) ---
    private void calcularYAsignarTotal(Compra compra) {
        BigDecimal total = compra.getDetalles().stream()
                .map(d -> {
                    BigDecimal precio = d.getPrecioCompra() != null ? d.getPrecioCompra() : BigDecimal.ZERO;
                    BigDecimal cant = d.getCantidad() != null ? d.getCantidad() : BigDecimal.ZERO;
                    return precio.multiply(cant);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        compra.setTotal(total);
    }

    private void procesarProducto(DetalleCompra detalle) {
        Producto prod = detalle.getProducto();

        if (prod.getIdProducto() != null && prod.getIdProducto() <= 0) {
            prod.setIdProducto(null);
        }

        if (prod.getIdProducto() == null) {
            // --- AQUÍ APLICAMOS LA NORMALIZACIÓN ---
            // Limpiamos espacios y dejamos el texto estándar
            String nombreLimpio = prod.getNombreProducto() != null
                    ? prod.getNombreProducto().trim().replaceAll("\\s+", " ")
                    : "";

            // Usamos la lógica de buscar ignorando tildes Y espacios (asegúrate de que tu Repository
            // tenga el método que creamos antes: findByNombreSinTildesNiEspacios)
            Producto productoExistente = productoRepository.findByNombreSinTildesNiEspacios(nombreLimpio).orElse(null);

            if (productoExistente != null) {
                detalle.setProducto(productoExistente);
            } else {
                // Producto nuevo: guardamos con el nombre limpio
                prod.setNombreProducto(nombreLimpio);
                prod.setStock(BigDecimal.ZERO);
                prod.setEstado(true);

                if (prod.getPrecio() == null) prod.setPrecio(BigDecimal.ZERO);
                if (prod.getUnidadMedida() == null) prod.setUnidadMedida("UNIDAD");

                if (prod.getCategoria() == null || prod.getCategoria().getIdCategoria() == null) {
                    throw new RuntimeException("Debe seleccionar una categoría para el producto nuevo: " + nombreLimpio);
                }

                Producto nuevoGuardado = productoRepository.save(prod);
                detalle.setProducto(nuevoGuardado);
            }
        } else {
            detalle.setProducto(productoRepository.findById(prod.getIdProducto()).orElse(prod));
        }
    }

    private void validarDatosBasicos(Compra compra) {
        if (compra.getDetalles() == null || compra.getDetalles().isEmpty()) {
            throw new RuntimeException("La compra debe tener al menos un producto.");
        }
        if (compra.getFechaInicio() == null) compra.setFechaInicio(LocalDate.now());
        if (compra.getFecha() != null && compra.getFecha().isBefore(compra.getFechaInicio())) {
            throw new RuntimeException("Error en fechas: Llegada < Emisión.");
        }
    }

    private void validarDetalle(DetalleCompra d) {
        if (d.getCantidad() == null || d.getCantidad().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Cantidad inválida para el producto: " + d.getProducto().getNombreProducto());
        }
        if (d.getPrecioCompra() == null || d.getPrecioCompra().compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Precio de compra negativo.");
        }
    }

    @Transactional(readOnly = true)
    public Compra buscarPorId(Integer id) {
        Compra compra = compraRepository.findById(id).orElse(null);

        if (compra != null) {
            // Esto es CRÍTICO para que los DTOs funcionen sin errores de "LazyInitialization"
            // Al llamar a .size(), obligas a Hibernate a traer los datos de la BD
            // mientras la transacción aún está abierta.
            if (compra.getMovimientosEnvase() != null) {
                compra.getMovimientosEnvase().size();
            }
            if (compra.getDetalles() != null) {
                compra.getDetalles().size();
            }
        }
        return compra;
    }
}