package com.sistemaventas.sistema_ventas.service;

import com.sistemaventas.sistema_ventas.model.Producto;
import com.sistemaventas.sistema_ventas.repository.ProductoRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page; // IMPORTANTE
import org.springframework.data.domain.PageRequest; // IMPORTANTE
import org.springframework.data.domain.Pageable; // IMPORTANTE
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ProductoService {

    @Autowired
    private ProductoRepository productoRepository;

    public Page<Producto> listarPaginado(boolean estado, String buscar, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        // Si el buscador está vacío, le pasamos null para que el Query lo ignore
        String termino = (buscar != null && !buscar.trim().isEmpty()) ? buscar.trim() : null;
        return productoRepository.findByEstadoPaginado(estado, termino, pageable);
    }

    @Transactional
    public void restaurar(Integer id) {
        Producto p = productoRepository.findById(id).orElse(null);
        if (p != null) {
            p.setEstado(true);
            productoRepository.save(p);
        }
    }

    @Transactional
    public void guardar(Producto producto) {
        String nombreLimpio = producto.getNombreProducto().trim().replaceAll("\\s+", " ");

        // --- VALIDACIÓN DE STOCK VS LOTES (BigDecimal) ---
        if (producto.getIdProducto() != null) {
            Producto productoExistente = productoRepository.findById(producto.getIdProducto()).orElse(null);

            if (productoExistente != null && productoExistente.getDetallesCompra() != null) {
                BigDecimal sumaStockLotes = productoExistente.getDetallesCompra().stream()
                        .filter(lote -> lote != null && lote.getStockActual() != null)
                        .map(lote -> lote.getStockActual())
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                if (sumaStockLotes.compareTo(BigDecimal.ZERO) > 0 &&
                        producto.getStock().compareTo(sumaStockLotes) != 0) {
                    throw new IllegalArgumentException("No puedes modificar el stock manualmente. " +
                            "El stock actual según los lotes es: " + sumaStockLotes + " " + producto.getUnidadMedida());
                }
            }
        }

        // 1. Lógica de Auto-revivir
        Producto inactivo = productoRepository.findByNombreProductoIgnoreCaseAndEstadoFalse(nombreLimpio);
        if (inactivo != null && producto.getIdProducto() == null) {
            inactivo.setEstado(true);
            inactivo.setPrecio(producto.getPrecio());
            inactivo.setStock(producto.getStock());
            inactivo.setUnidadMedida(producto.getUnidadMedida());
            inactivo.setCodigoBarras(producto.getCodigoBarras());
            inactivo.setCategoria(producto.getCategoria());
            productoRepository.save(inactivo);
            return;
        }

        // 2. Validaciones de Duplicados
        validarDuplicados(producto, nombreLimpio);

        // 3. Guardar
        validarDatosProducto(producto);
        producto.setNombreProducto(nombreLimpio);
        producto.setEstado(true);

        if (producto.getUnidadMedida() == null) producto.setUnidadMedida("UNIDAD");

        productoRepository.save(producto);
    }

    private void validarDuplicados(Producto p, String nombre) {
        if (p.getIdProducto() == null) {
            if (productoRepository.existsByNombreProductoIgnoreCase(nombre)) {
                throw new IllegalArgumentException("Ya existe un producto con ese nombre.");
            }
            if (p.getCodigoBarras() != null && !p.getCodigoBarras().isEmpty() &&
                    productoRepository.existsByCodigoBarras(p.getCodigoBarras())) {
                throw new IllegalArgumentException("El código de barras ya está registrado.");
            }
        } else {
            if (p.getCodigoBarras() != null && !p.getCodigoBarras().isEmpty() &&
                    productoRepository.existsByCodigoBarrasAndIdProductoNot(p.getCodigoBarras(), p.getIdProducto())) {
                throw new IllegalArgumentException("El código de barras ya lo tiene otro producto.");
            }
        }
    }

    private void validarDatosProducto(Producto p) {
        if (p.getPrecio() == null || p.getPrecio().compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Precio inválido.");

        if (p.getStock() == null || p.getStock().compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Stock inválido.");

        if (p.getCategoria() == null)
            throw new IllegalArgumentException("Seleccione una categoría.");
    }

    public Producto buscarPorId(Integer id) {
        return productoRepository.findById(id).orElse(null);
    }

    @Transactional
    public void eliminar(Integer id) {
        Producto p = buscarPorId(id);
        if (p != null) {
            p.setEstado(false);
            productoRepository.save(p);
        }
    }
}