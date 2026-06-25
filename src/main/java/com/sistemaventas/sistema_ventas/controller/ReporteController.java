package com.sistemaventas.sistema_ventas.controller;

import com.sistemaventas.sistema_ventas.dto.ReporteCompraDTO;
import com.sistemaventas.sistema_ventas.dto.ReporteVentaDTO;
import com.sistemaventas.sistema_ventas.service.ReporteService;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/reportes")
public class ReporteController {

    @Autowired
    private ReporteService reporteService;

    @GetMapping("/exportar")
    public void exportar(
            @RequestParam String periodo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam String tipo,
            HttpServletResponse response) throws Exception {

        // 1. Lógica de Fechas
        LocalDate hoy = LocalDate.now();
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime inicio;

        switch (periodo) {
            case "semanal" -> inicio = ahora.minusWeeks(1);
            case "mensual" -> inicio = hoy.withDayOfMonth(1).atStartOfDay();
            case "personalizado" -> {
                if (fechaInicio == null || fechaFin == null) {
                    response.sendRedirect("/home?error=Debe+seleccionar+un+rango+de+fechas");
                    return;
                }
                inicio = fechaInicio.atStartOfDay();
                ahora = fechaFin.atTime(23, 59, 59);
            }
            default -> inicio = hoy.atStartOfDay();
        }

        List<?> datos = reporteService.obtenerDatos(tipo, inicio, ahora);

        if (datos.isEmpty()) {
            response.sendRedirect("/home?error=No+existen+registros+para+el+periodo+seleccionado");
            return;
        }

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=Reporte_" + tipo + ".pdf");

        Document doc = new Document(PageSize.A4, 36, 36, 50, 50); // Márgenes
        PdfWriter.getInstance(doc, response.getOutputStream());
        doc.open();

        // Título con estilo
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, java.awt.Color.DARK_GRAY);
        doc.add(new Paragraph("Reporte de " + tipo.toUpperCase(), titleFont));
        doc.add(new Paragraph("Periodo: " + periodo + " | Generado el: " + ahora.toLocalDate(), FontFactory.getFont(FontFactory.HELVETICA, 10)));
        doc.add(Chunk.NEWLINE);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1f, 2f, 2f, 3f}); // Ajuste de columnas para mejor visualización

        // Encabezados profesionales
        String[] headers = tipo.equals("ventas") ? new String[]{"ID", "Fecha", "Total", "Cliente"} : new String[]{"ID", "Fecha", "Total", "Proveedor"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, java.awt.Color.WHITE)));
            cell.setBackgroundColor(new java.awt.Color(41, 128, 185)); // Azul corporativo
            cell.setPadding(8);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        // Datos con formato profesional
        BigDecimal sumaTotal = BigDecimal.ZERO;
        boolean alternate = false;
        java.awt.Color lightGray = new java.awt.Color(240, 240, 240);

        for (Object item : datos) {
            alternate = !alternate;
            java.awt.Color rowColor = alternate ? lightGray : java.awt.Color.WHITE;

            // Extraer datos comunes
            Integer id = (item instanceof ReporteVentaDTO v) ? v.id() : ((ReporteCompraDTO) item).id();
            LocalDate fecha = (item instanceof ReporteVentaDTO v) ? v.fecha().toLocalDate() : ((ReporteCompraDTO) item).fecha();
            BigDecimal total = (item instanceof ReporteVentaDTO v) ? v.total() : ((ReporteCompraDTO) item).total();
            String nombre = (item instanceof ReporteVentaDTO v) ? v.cliente() : ((ReporteCompraDTO) item).proveedor();

            sumaTotal = sumaTotal.add(total);

            // Crear celdas
            addCell(table, String.valueOf(id), rowColor);
            addCell(table, fecha.toString(), rowColor);
            addCell(table, "S/ " + String.format("%,.2f", total), rowColor);
            addCell(table, nombre, rowColor);
        }

        // Fila Total
        PdfPCell totalLabel = new PdfPCell(new Phrase("TOTAL GENERAL", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
        totalLabel.setColspan(2);
        totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalLabel.setPadding(8);
        table.addCell(totalLabel);

        PdfPCell totalValue = new PdfPCell(new Phrase("S/ " + String.format("%,.2f", sumaTotal), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
        totalValue.setBackgroundColor(new java.awt.Color(241, 196, 15)); // Amarillo oro
        totalValue.setPadding(8);
        table.addCell(totalValue);

        table.addCell(new PdfPCell()); // Celda vacía para completar el layout

        doc.add(table);
        doc.close();
    }

    // Método auxiliar para evitar repetición
    private void addCell(PdfPTable table, String text, java.awt.Color color) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA, 11)));
        cell.setBackgroundColor(color);
        cell.setPadding(6);
        table.addCell(cell);
    }
}