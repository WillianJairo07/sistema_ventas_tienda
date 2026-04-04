package com.sistemaventas.sistema_ventas.service;

import com.sistemaventas.sistema_ventas.model.Categoria;
import com.sistemaventas.sistema_ventas.repository.CategoriaRepository;
import com.sistemaventas.sistema_ventas.repository.ProductoRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoriaService {

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    // 1. Usamos los métodos automáticos de JPA (Mucho más rápidos)
    public List<Categoria> listarTodasOrdenadas() {
        return categoriaRepository.findByEstadoTrueOrderByIdCategoriaAsc();
    }

    public List<Categoria> listarSoloInactivos() {
        return categoriaRepository.findByEstadoFalseOrderByIdCategoriaAsc();
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

    public void guardar(Categoria categoria) {
        String nombreNuevo = categoria.getNombreCategoria().trim();

        // --- BLOQUE AUTO-REVIVIR (Optimizado) ---
        // Usamos el método IgnoreCase de JPA para evitar queries nativas
        Categoria oculta = categoriaRepository.findByNombreCategoriaIgnoreCase(nombreNuevo);

        if (oculta != null) {
            // Caso A: La categoría existe pero está oculta (La revivimos)
            if (!oculta.isEstado() && categoria.getIdCategoria() == null) {
                oculta.setEstado(true);
                oculta.setNombreCategoria(nombreNuevo);
                categoriaRepository.save(oculta);
                return;
            }

            // Caso B: Existe, está activa y no es la que estamos editando (Duplicado)
            if (oculta.isEstado()) {
                if (categoria.getIdCategoria() == null || !categoria.getIdCategoria().equals(oculta.getIdCategoria())) {
                    throw new IllegalArgumentException("La categoría '" + nombreNuevo + "' ya existe.");
                }
            }
        }

        // Si pasó las validaciones, guardamos
        categoria.setNombreCategoria(nombreNuevo);
        // Solo seteamos true si es una creación nueva
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
}
