package com.sistemaventas.sistema_ventas.service;

import com.sistemaventas.sistema_ventas.model.Categoria;
import com.sistemaventas.sistema_ventas.repository.CategoriaRepository;
import com.sistemaventas.sistema_ventas.repository.ProductoRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Service
public class CategoriaService {

    @Autowired
    private CategoriaRepository categoriaRepository;


    private String normalizarTexto(String texto) {
        if (texto == null) return "";
        // 1. Quitar tildes (NFD separa la letra del acento, luego el regex borra el acento)
        String normalizado = java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD);
        normalizado = normalizado.replaceAll("\\p{M}", "");
        // 2. Quitar espacios y pasar a minúsculas
        return normalizado.replace(" ", "").toLowerCase();
    }

    public Page<Categoria> listarPaginado(boolean activos, int page, int size, String buscar) {
        Pageable pageable = PageRequest.of(page, size);

        if (buscar != null && !buscar.trim().isEmpty()) {
            return categoriaRepository.findByNombreCategoriaContainingIgnoreCaseAndEstadoOrderByIdCategoriaDesc(buscar.trim(), activos, pageable);
        }

        return activos ?
                categoriaRepository.findByEstadoTrueOrderByIdCategoriaDesc(pageable) :
                categoriaRepository.findByEstadoFalseOrderByIdCategoriaDesc(pageable);
    }

    @Transactional
    public void restaurar(Integer id) {
        // .findById ya encuentra categorías inactivas por defecto
        Categoria cat = categoriaRepository.findById(id).orElse(null);
        if (cat != null) {
            cat.setEstado(true);
            categoriaRepository.save(cat);
        } else {
            throw new RuntimeException("No se encontró la categoría para restaurar.");
        }
    }

    @Transactional
    public void guardar(Categoria categoria) {
        // 1. Limpieza de espacios
        String nombreNuevo = categoria.getNombreCategoria().trim().replaceAll("\\s+", " ");

        // 2. VALIDACIÓN OPTIMIZADA (Sin usar findAll)

        // Escenario A: NUEVA CATEGORÍA (ID es null)
        if (categoria.getIdCategoria() == null) {
            // Buscamos si existe por nombre (ignora mayúsculas/minúsculas)
            Categoria existente = categoriaRepository.findByNombreCategoriaIgnoreCase(nombreNuevo);

            if (existente != null) {
                // Lógica de Auto-revivir: si existe pero está inactiva, la activamos
                if (!existente.isEstado()) {
                    existente.setEstado(true);
                    existente.setNombreCategoria(nombreNuevo);
                    categoriaRepository.save(existente);
                    return;
                }
                // Si está activa, es un duplicado
                throw new IllegalArgumentException("Ya existe una categoría similar");
            }
        }
        // Escenario B: EDITANDO CATEGORÍA (ID ya existe)
        else {
            // Validamos que el nuevo nombre no lo tenga OTRA categoría distinta a la actual
            if (categoriaRepository.existsByNombreCategoriaIgnoreCaseAndIdCategoriaNot(nombreNuevo, categoria.getIdCategoria())) {
                throw new IllegalArgumentException("Ya existe una categoría similar");
            }
        }

        // 3. GUARDADO O ACTUALIZACIÓN FINAL
        categoria.setNombreCategoria(nombreNuevo);

        // Solo forzamos estado true si es un registro totalmente nuevo
        if (categoria.getIdCategoria() == null) {
            categoria.setEstado(true);
        }

        categoriaRepository.save(categoria);
    }

    public Categoria buscarPorId(Integer id) {
        return categoriaRepository.findById(id).orElse(null);
    }

    // ELIMINACIÓN LÓGICA: Para que se vaya a "Inactivos"
    public void eliminar(Integer id) {
        Categoria cat = buscarPorId(id);
        if (cat != null) {
            cat.setEstado(false);
            categoriaRepository.save(cat);
        }
    }


    public List<Categoria> listarParaCombos() {
        // Usamos el repositorio para traer solo las activas, ordenadas por nombre, en una LISTA
        return categoriaRepository.findByEstadoTrueOrderByNombreCategoriaAsc();
    }
}
