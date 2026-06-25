package com.sistemaventas.sistema_ventas.service;

import com.sistemaventas.sistema_ventas.model.Categoria;
import com.sistemaventas.sistema_ventas.repository.CategoriaRepository;
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
        String normalizado = java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD);
        normalizado = normalizado.replaceAll("\\p{M}", "");
        return normalizado.toLowerCase().trim().replaceAll("\\s+", " ");
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
        String nombreNuevo = categoria.getNombreCategoria().trim(); // Mantenemos el formato original para guardarlo bonito

        if (categoria.getIdCategoria() == null) {
            // Buscamos ignorando espacios y tildes
            Categoria existente = categoriaRepository.findByNombreSinTildesNiEspacios(nombreNuevo);

            if (existente != null) {
                if (!existente.isEstado()) {
                    existente.setEstado(true);
                    categoriaRepository.save(existente);
                    return;
                }
                throw new IllegalArgumentException("Ya existe una categoría similar");
            }
        } else {
            // Validación para edición
            if (categoriaRepository.existsByNombreSinTildesNiEspaciosYIdNot(nombreNuevo, categoria.getIdCategoria())) {
                throw new IllegalArgumentException("Ya existe otra categoría similar");
            }
        }

        categoria.setNombreCategoria(nombreNuevo);
        if (categoria.getIdCategoria() == null) categoria.setEstado(true);
        categoriaRepository.save(categoria);
    }

    public Categoria buscarPorId(Integer id) {
        return categoriaRepository.findById(id).orElse(null);
    }

    public void eliminar(Integer id) {
        Categoria cat = buscarPorId(id);
        if (cat != null) {
            cat.setEstado(false);
            categoriaRepository.save(cat);
        }
    }

    public List<Categoria> listarParaCombos() {
        return categoriaRepository.findByEstadoTrueOrderByNombreCategoriaAsc();
    }
}