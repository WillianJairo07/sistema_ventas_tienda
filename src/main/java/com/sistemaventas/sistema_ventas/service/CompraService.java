package com.sistemaventas.sistema_ventas.service;

import com.sistemaventas.sistema_ventas.model.Compra;
import com.sistemaventas.sistema_ventas.model.DetalleCompra;
import com.sistemaventas.sistema_ventas.model.Producto;
import com.sistemaventas.sistema_ventas.repository.CompraRepository;
import com.sistemaventas.sistema_ventas.repository.ProductoRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class CompraService {

    @Autowired private CompraRepository compraRepository;
    @Autowired private ProductoRepository productoRepository;

    public Page<Compra> filtrarCompras(String proveedor, String fechaDesde, String fechaHasta,
                                       int page, int size) {

        Pageable pageable = PageRequest.of(page, size);

        LocalDate desde = null;
        LocalDate hasta = null;

        try {
            if (fechaDesde != null && !fechaDesde.trim().isEmpty()) desde = LocalDate.parse(fechaDesde);
            if (fechaHasta != null && !fechaHasta.trim().isEmpty()) hasta = LocalDate.parse(fechaHasta);
        } catch (DateTimeParseException e) {
            throw new RuntimeException("Error: El formato de fecha debe ser YYYY-MM-DD");
        }

        // Si el proveedor viene vacío o con espacios, lo mandamos como null
        // Dentro de filtrarCompras en CompraService
        String proveedorLimpio = (proveedor != null && !proveedor.trim().isEmpty()) ? proveedor.trim() : null;

        return compraRepository.filtrar(proveedorLimpio, desde, hasta, pageable);
    }

    @Transactional
    public void registrarCompra(Compra compra) {
        // 1. Validaciones de negocio (Fechas, lista no vacía)
        validarDatosBasicos(compra);

        // 2. Cargar productos actuales para evitar duplicados por nombre
        List<Producto> existentes = productoRepository.findAll();

        // 3. Asegurar que la fecha de contabilización exista (Evita el error NOT NULL de SQL)
        if (compra.getFechaContabilizacion() == null) {
            compra.setFechaContabilizacion(LocalDate.now());
        }

        for (DetalleCompra detalle : compra.getDetalles()) {
            // Validar cantidad > 0
            validarDetalle(detalle);

            // Buscar si el producto ya existe o crearlo si es nuevo
            procesarProducto(detalle, existentes);

            // 4. Sincronizar el stock del lote con la cantidad comprada
            // detalle.getCantidad() ya es BigDecimal según tu código previo
            detalle.setStockActual(detalle.getCantidad());

            // 5. Establecer la relación bidireccional (Indispensable para que JPA guarde el detalle)
            detalle.setCompra(compra);
        }

        // 6. Estado inicial de la orden
        compra.setEstado("PENDIENTE");

        // 7. Calcular el total final basado en los precios de los detalles
        calcularYAsignarTotal(compra);

        // 8. Guardar en cascada (Compra + Detalles)
        compraRepository.save(compra);
    }

    @Transactional
    public void actualizarCompraPendiente(Compra compraActualizada) {
        Compra existente = compraRepository.findById(compraActualizada.getIdCompra())
                .orElseThrow(() -> new RuntimeException("No existe la compra a editar."));

        if (!"PENDIENTE".equals(existente.getEstado())) {
            throw new RuntimeException("Solo se pueden editar compras en estado PENDIENTE.");
        }

        // 1. Mantener integridad de la fecha original
        compraActualizada.setFechaInicio(existente.getFechaInicio());

        // 2. Procesar productos y asegurar campos obligatorios
        List<Producto> existentesBD = productoRepository.findAll();

        for (DetalleCompra detalle : compraActualizada.getDetalles()) {
            validarDetalle(detalle);

            // Vincula o crea el producto (esto ya lo tenías bien)
            procesarProducto(detalle, existentesBD);

            // --- LA SOLUCIÓN AL ERROR: Sincronizar stockActual ---
            // Como la compra sigue PENDIENTE, el stockActual del lote
            // debe ser igual a la cantidad que se está pidiendo.
            if (detalle.getStockActual() == null) {
                detalle.setStockActual(detalle.getCantidad());
            }

            detalle.setCompra(compraActualizada);
        }

        // 3. Recalcular total
        calcularYAsignarTotal(compraActualizada);

        // 4. Limpiar detalles antiguos y actualizar la cabecera
        // IMPORTANTE: Al hacer .clear(), JPA eliminará los registros viejos en la DB
        // gracias al orphanRemoval = true.
        existente.getDetalles().clear();
        existente.getDetalles().addAll(compraActualizada.getDetalles());

        // Copiamos los campos actualizados a la entidad persistida
        existente.setFecha(compraActualizada.getFecha());
        existente.setProveedor(compraActualizada.getProveedor());
        existente.setTotal(compraActualizada.getTotal());

        // Si manejas fecha de contabilización, asegúrate de mantenerla o actualizarla
        existente.setFechaContabilizacion(LocalDate.now());

        compraRepository.save(existente);
    }

    @Transactional
    public void confirmarRecepcion(Integer idCompra, String tipoComprobante) {
        Compra compra = compraRepository.findById(idCompra)
                .orElseThrow(() -> new RuntimeException("Compra no encontrada"));

        // Verificamos que no se haya procesado antes
        if (!"PENDIENTE".equals(compra.getEstado())) {
            throw new RuntimeException("Esta compra ya fue completada.");
        }

        // 1. ASIGNAMOS LOS NUEVOS DATOS DE RECEPCIÓN
        compra.setTipoComprobante(tipoComprobante); // <--- El nuevo campo
        compra.setFechaRecepcion(LocalDateTime.now());

        for (DetalleCompra detalle : compra.getDetalles()) {
            Producto prod = productoRepository.findById(detalle.getProducto().getIdProducto())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

            // --- CORRECCIÓN CLAVE: Suma de stock con BigDecimal ---
            BigDecimal stockActualEnProducto = prod.getStock() != null ? prod.getStock() : BigDecimal.ZERO;

            // Sumamos la cantidad recibida al stock general del producto
            prod.setStock(stockActualEnProducto.add(detalle.getCantidad()));

            // El stockActual del detalle (lote) se inicializa con la cantidad que acaba de llegar
            detalle.setStockActual(detalle.getCantidad());

            productoRepository.save(prod);
        }

        // 2. CAMBIAMOS EL ESTADO Y GUARDAMOS
        compra.setEstado("COMPLETADA");
        compraRepository.save(compra);
    }

    // --- MÉTODOS DE APOYO (Lógica Interna) ---
    private void calcularYAsignarTotal(Compra compra) {
        BigDecimal total = compra.getDetalles().stream()
                .map(d -> {
                    BigDecimal precio = d.getPrecioCompra() != null ? d.getPrecioCompra() : BigDecimal.ZERO;
                    // --- CORRECCIÓN: detalle.getCantidad() ya es BigDecimal, no necesita 'new BigDecimal()' ---
                    BigDecimal cant = d.getCantidad() != null ? d.getCantidad() : BigDecimal.ZERO;
                    return precio.multiply(cant);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        compra.setTotal(total);
    }

    private void procesarProducto(DetalleCompra detalle, List<Producto> existentes) {
        Producto prod = detalle.getProducto();

        // Si el ID es <= 0 (como el idTemp del JS), lo tratamos como nuevo
        if (prod.getIdProducto() != null && prod.getIdProducto() <= 0) prod.setIdProducto(null);

        if (prod.getIdProducto() == null) {
            String nombreBusqueda = limpiarTexto(prod.getNombreProducto());

            // 1. Verificar si ya existe por nombre para no duplicar
            Producto productoExistente = existentes.stream()
                    .filter(p -> limpiarTexto(p.getNombreProducto()).equals(nombreBusqueda))
                    .findFirst()
                    .orElse(null);

            if (productoExistente != null) {
                detalle.setProducto(productoExistente);
            } else {
                // 2. ES UN PRODUCTO REALMENTE NUEVO
                prod.setStock(BigDecimal.ZERO);
                prod.setEstado(true); // <--- IMPORTANTE: que nazca activo

                if (prod.getPrecio() == null) prod.setPrecio(BigDecimal.ZERO);
                if (prod.getUnidadMedida() == null) prod.setUnidadMedida("UNIDAD");

                // VALIDACIÓN DE CATEGORÍA:
                // Si el front mandó el ID de categoría, Spring lo mete en prod.getCategoria().getIdCategoria()
                if (prod.getCategoria() == null || prod.getCategoria().getIdCategoria() == null) {
                    throw new RuntimeException("Debe seleccionar una categoría para el producto nuevo: " + prod.getNombreProducto());
                }

                // Guardamos el nuevo producto antes de asignarlo al detalle
                Producto nuevoGuardado = productoRepository.save(prod);
                detalle.setProducto(nuevoGuardado);
                existentes.add(nuevoGuardado); // Actualizamos la lista local para el siguiente ciclo del loop
            }
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
        // --- CORRECCIÓN: Validación con compareTo para BigDecimal ---
        if (d.getCantidad() == null || d.getCantidad().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Cantidad inválida para el producto: " + d.getProducto().getNombreProducto());
        }
        if (d.getPrecioCompra() == null || d.getPrecioCompra().compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Precio de compra negativo.");
        }
    }

    private String limpiarTexto(String texto) {
        if (texto == null) return "";
        String normalizado = Normalizer.normalize(texto.trim().toLowerCase(), Normalizer.Form.NFD);
        return normalizado.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "").replaceAll("\\s+", "");
    }

    public List<Compra> listarHistorial() { return compraRepository.findAll(); }
    public Compra buscarPorId(Integer id) { return compraRepository.findById(id).orElse(null); }
}