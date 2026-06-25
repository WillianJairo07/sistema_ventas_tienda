package com.sistemaventas.sistema_ventas.service;

import com.sistemaventas.sistema_ventas.model.DetalleCompra;
import com.sistemaventas.sistema_ventas.model.Producto;
import com.sistemaventas.sistema_ventas.repository.ProductoRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProductoService {

    @Autowired
    private ProductoRepository productoRepository;

    public List<Producto> listarParaCombos() {
        return productoRepository.findByEstadoTrueOrderByIdProductoDesc();
    }

    public Page<Producto> listarPaginado(boolean estado, String buscar, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
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
        // 1. Limpieza de espacios redundantes
        String nombreLimpio = producto.getNombreProducto().trim().replaceAll("\\s+", " ");

        // 2. Lógica de Lotes (Se mantiene intacta)
        if (producto.getIdProducto() != null) {
            Producto productoExistente = productoRepository.findById(producto.getIdProducto()).orElse(null);
            if (productoExistente != null && productoExistente.getDetallesCompra() != null) {
                BigDecimal sumaStockLotes = productoExistente.getDetallesCompra().stream()
                        .filter(lote -> lote != null && lote.getStockActual() != null)
                        .map(DetalleCompra::getStockActual)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                if (sumaStockLotes.compareTo(BigDecimal.ZERO) > 0 &&
                        producto.getStock().compareTo(sumaStockLotes) != 0) {
                    throw new IllegalArgumentException("No puedes modificar el stock manualmente. Stock actual en lotes: " + sumaStockLotes);
                }
            }
        }

        // 3. Auto-revivir (Solo si es nuevo registro)
        if (producto.getIdProducto() == null) {
            Producto inactivo = productoRepository.findInactivoByNombreSinTildesNiEspacios(nombreLimpio);
            if (inactivo != null) {
                inactivo.setEstado(true);
                inactivo.setPrecio(producto.getPrecio());
                inactivo.setStock(producto.getStock());
                inactivo.setUnidadMedida(producto.getUnidadMedida());
                inactivo.setCodigoBarras(producto.getCodigoBarras());
                inactivo.setCategoria(producto.getCategoria());
                productoRepository.save(inactivo);
                return;
            }
        }

        // 4. Validaciones unificadas
        validarDuplicados(producto, nombreLimpio);
        validarDatosProducto(producto);

        // 5. Persistencia
        producto.setNombreProducto(nombreLimpio);
        producto.setEstado(true);
        if (producto.getUnidadMedida() == null) producto.setUnidadMedida("UNIDAD");

        productoRepository.save(producto);
    }

    private void validarDuplicados(Producto p, String nombre) {
        // Validar Nombre: Busca duplicados ignorando tildes/espacios, excluyendo el ID actual si existe
        if (productoRepository.existsByNombreSinTildesNiEspacios(nombre, p.getIdProducto())) {
            throw new IllegalArgumentException("Ya existe un producto con ese nombre.");
        }

        // Validar Código de Barras: Se mantiene tu lógica original
        if (p.getCodigoBarras() != null && !p.getCodigoBarras().isEmpty()) {
            boolean existeCodigo = (p.getIdProducto() == null)
                    ? productoRepository.existsByCodigoBarras(p.getCodigoBarras())
                    : productoRepository.existsByCodigoBarrasAndIdProductoNot(p.getCodigoBarras(), p.getIdProducto());

            if (existeCodigo) {
                throw new IllegalArgumentException("El código de barras ya pertenece a otro producto.");
            }
        }
    }

    private void validarDatosProducto(Producto p) {
        if (p.getPrecio() == null || p.getPrecio().compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("Precio inválido.");
        if (p.getStock() == null || p.getStock().compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("Stock inválido.");
        if (p.getCategoria() == null) throw new IllegalArgumentException("Seleccione una categoría.");
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