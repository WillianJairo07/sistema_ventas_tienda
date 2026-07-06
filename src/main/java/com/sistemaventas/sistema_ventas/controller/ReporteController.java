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
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/reportes")
public class ReporteController {

    @Autowired
    private ReporteService reporteService;

    private final java.awt.Color BLUE_DARK = new java.awt.Color(15, 23, 42);

    @GetMapping("/exportar")
    public void exportar(
            @RequestParam String periodo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam String tipo,
            @RequestParam(required = false) Boolean esComprobantePropio,
            @RequestParam(required = false) String tipoComprobante,
            @RequestParam(required = false) String tipoVenta,
            HttpServletResponse response) throws Exception {

        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime inicio = switch (periodo) {
            case "semanal" -> ahora.minusWeeks(1);
            case "mensual" -> LocalDate.now().withDayOfMonth(1).atStartOfDay();
            case "personalizado" -> (fechaInicio != null) ? fechaInicio.atStartOfDay() : ahora.minusDays(1);
            default -> LocalDate.now().atStartOfDay();
        };
        LocalDateTime fin = (periodo.equals("personalizado") && fechaFin != null) ? fechaFin.atTime(23, 59, 59) : ahora;

        boolean esPropio = "compras".equals(tipo) ? (esComprobantePropio != null ? esComprobantePropio : true) : true;
        String filtroExtra = "ventas".equals(tipo) ? tipoVenta : tipoComprobante;
        List<?> datos = reporteService.obtenerDatos(tipo, inicio, fin, esPropio, filtroExtra);

        if (datos == null || datos.isEmpty()) {
            response.sendRedirect("/home?error=No+existen+registros");
            return;
        }

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=Reporte_" + tipo + ".pdf");
        Document doc = new Document(PageSize.A4.rotate(), 36, 36, 50, 50);
        PdfWriter.getInstance(doc, response.getOutputStream());
        doc.open();

        doc.add(new Paragraph("Reporte de " + tipo.toUpperCase(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, java.awt.Color.DARK_GRAY)));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String textoPeriodo = "Periodo: " + periodo.substring(0, 1).toUpperCase() + periodo.substring(1) +
                " (" + inicio.format(formatter) + " al " + fin.format(formatter) + ")";
        doc.add(new Paragraph(textoPeriodo + " | Generado: " + ahora.format(formatter),
                FontFactory.getFont(FontFactory.HELVETICA, 10)));        doc.add(Chunk.NEWLINE);

        PdfPTable table = new PdfPTable("ventas".equals(tipo) ? 7 : 5);
        table.setWidthPercentage(100);

        if ("ventas".equals(tipo)) {
            table.setWidths(new float[]{0.8f, 1.5f, 1f, 2f, 2f, 2f, 2.5f});
            renderHeaders(table, new String[]{"ID", "Fecha", "Tipo", "Total", "Abonado", "Pendiente", "Cliente"}, BLUE_DARK);
        } else {
            table.setWidths(new float[]{1f, 2f, 2f, 2f, 3f});
            renderHeaders(table, new String[]{"ID", "Fecha", "Comprobante", "Total", "Proveedor"}, BLUE_DARK);
        }

        BigDecimal sVenta = BigDecimal.ZERO, sAbonado = BigDecimal.ZERO, sPendiente = BigDecimal.ZERO;

        for (Object item : datos) {
            if (item instanceof ReporteVentaDTO v) {
                boolean esContado = "CONTADO".equalsIgnoreCase(v.tipoVenta());
                BigDecimal totalV = v.totalVenta();
                BigDecimal abonadoF = esContado ? totalV : v.cobrado();
                BigDecimal pendienteF = esContado ? BigDecimal.ZERO : v.pendiente();

                sVenta = sVenta.add(totalV);
                sAbonado = sAbonado.add(abonadoF);
                sPendiente = sPendiente.add(pendienteF);

                addCell(table, String.valueOf(v.id()));
                addCell(table, v.fecha().toLocalDate().toString());
                addCell(table, v.tipoVenta().toUpperCase());
                addCell(table, "S/ " + String.format("%,.2f", totalV));
                addCell(table, "S/ " + String.format("%,.2f", abonadoF));

                // Fondo rojo solo si hay deuda
                boolean hayDeuda = pendienteF.compareTo(BigDecimal.ZERO) > 0;
                addCellRedConditioned(table, hayDeuda ? "S/ " + String.format("%,.2f", pendienteF) : "Pago completo", hayDeuda);

                addCell(table, v.cliente() != null ? v.cliente() : "N/A");
            } else if (item instanceof ReporteCompraDTO c) {
                sVenta = sVenta.add(c.total());
                addCell(table, String.valueOf(c.id()));
                addCell(table, c.fecha().toString());
                addCell(table, c.tipoComprobante());
                addCell(table, "S/ " + String.format("%,.2f", c.total()));
                addCell(table, c.proveedor());
            }
        }

        // Fila de Totales
        PdfPCell label = new PdfPCell(new Phrase("TOTALES", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
        label.setColspan("ventas".equals(tipo) ? 3 : 3);
        label.setHorizontalAlignment(Element.ALIGN_CENTER);
        label.setPadding(8);
        table.addCell(label);

        table.addCell(createTotalCell("S/ " + String.format("%,.2f", sVenta), new java.awt.Color(178, 227, 197)));
        if ("ventas".equals(tipo)) {
            table.addCell(createTotalCell("S/ " + String.format("%,.2f", sAbonado), new java.awt.Color(237, 218, 158)));
            table.addCell(createTotalCell("S/ " + String.format("%,.2f", sPendiente), new java.awt.Color(241, 174, 169)));
            table.addCell(new PdfPCell(new Phrase(""))); // Espacio cliente
        } else {
            table.addCell(new PdfPCell());
        }

        doc.add(table);
        doc.close();
    }

    private void renderHeaders(PdfPTable table, String[] h, java.awt.Color c) {
        for (String s : h) {
            PdfPCell cell = new PdfPCell(new Phrase(s, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, java.awt.Color.WHITE)));
            cell.setBackgroundColor(c);
            cell.setPadding(6);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }
    }

    private void addCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA, 9)));
        cell.setPadding(5);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addCellRedConditioned(PdfPTable table, String text, boolean red) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA, 9, red ? java.awt.Color.BLACK : java.awt.Color.BLACK)));
        if (red) cell.setBackgroundColor(new java.awt.Color(241, 174, 169));
        cell.setPadding(5);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private PdfPCell createTotalCell(String text, java.awt.Color color) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, java.awt.Color.BLACK)));
        cell.setBackgroundColor(color);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(8);
        return cell;
    }
}