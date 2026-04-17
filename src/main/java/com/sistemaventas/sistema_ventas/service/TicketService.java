package com.sistemaventas.sistema_ventas.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfWriter;
import com.sistemaventas.sistema_ventas.model.DetalleVenta;
import com.sistemaventas.sistema_ventas.model.Venta;
import com.sistemaventas.sistema_ventas.repository.PagoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Service
public class TicketService {

    @Autowired
    private PagoRepository pagoRepository;

    public byte[] generarTicketPDF(Venta venta) throws Exception {
        // Tamaño para papel térmico (80mm aprox)
        Rectangle pageSize = new Rectangle(226, 800);
        Document document = new Document(pageSize, 10, 10, 10, 10);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        PdfWriter.getInstance(document, out);
        document.open();

        Font fontBold = FontFactory.getFont(FontFactory.COURIER_BOLD, 9);
        Font fontNormal = FontFactory.getFont(FontFactory.COURIER, 9);
        String separador = "--------------------------------------";

        // --- ENCABEZADO ---
        Paragraph header = new Paragraph();
        header.setAlignment(Element.ALIGN_CENTER);
        header.add(new Chunk("TICKET DE VENTA\n", fontBold));
        header.add(new Chunk("BODEGA SAN MARTÍN\n", fontBold));
        header.add(new Chunk("RUC: 12345678901\n", fontNormal));
        header.add(new Chunk("Calle Zarumilla P-30 San Juan de Dios\n", fontNormal));
        header.add(new Chunk("Jacobo Hunter, Arequipa\n", fontNormal));
        header.add(new Chunk(String.format("Ticket Nº: %03d\n", venta.getIdVenta()), fontNormal));

        String fechaHora = "Fecha: " + venta.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                "  Hora: " + venta.getFecha().format(DateTimeFormatter.ofPattern("HH:mm"));
        header.add(new Chunk(fechaHora + "\n", fontNormal));
        document.add(header);
        document.add(new Paragraph(separador, fontNormal));

        // --- TABLA DE PRODUCTOS ---
        float[] columnWidths = {55f, 18f, 27f};
        PdfPTable table = new PdfPTable(columnWidths);
        table.setWidthPercentage(100);
        table.addCell(crearCelda("Producto", fontBold, Element.ALIGN_LEFT));
        table.addCell(crearCelda("Cant", fontBold, Element.ALIGN_CENTER));
        table.addCell(crearCelda("Total", fontBold, Element.ALIGN_RIGHT));

        for (DetalleVenta det : venta.getDetalles()) {
            String unidad = det.getProducto().getUnidadMedida();
            String cantTxt = ("UNIDAD".equalsIgnoreCase(unidad))
                    ? String.valueOf(det.getCantidad().intValue())
                    : String.format("%.3f", det.getCantidad());

            table.addCell(crearCelda(det.getProducto().getNombreProducto(), fontNormal, Element.ALIGN_LEFT));
            table.addCell(crearCelda(cantTxt, fontNormal, Element.ALIGN_CENTER));
            BigDecimal subtotal = det.getPrecioVenta().multiply(det.getCantidad());
            table.addCell(crearCelda(String.format("%.2f", subtotal), fontNormal, Element.ALIGN_RIGHT));
        }
        document.add(table);
        document.add(new Paragraph(separador, fontNormal));

        // --- SECCIÓN DE TOTALES ---
        BigDecimal total = venta.getTotalVenta();
        BigDecimal pagadoCon = venta.getMontoPagado(); // El efectivo que recibiste en mano

        Paragraph pTotal = new Paragraph("TOTAL VENTA: S/ " + String.format("%.2f", total), fontBold);
        pTotal.setAlignment(Element.ALIGN_RIGHT);
        document.add(pTotal);

        // --- LÓGICA DIFERENCIADA POR TIPO DE VENTA ---

        if ("CONTADO".equalsIgnoreCase(venta.getTipoVenta())) {
            // CASO 1: Es Contado -> Mostramos Efectivo y Vuelto
            if (pagadoCon != null && pagadoCon.compareTo(total) >= 0) {
                BigDecimal vuelto = pagadoCon.subtract(total);

                Paragraph pRecibido = new Paragraph("EFECTIVO:    S/ " + String.format("%.2f", pagadoCon), fontNormal);
                pRecibido.setAlignment(Element.ALIGN_RIGHT);
                document.add(pRecibido);

                Paragraph pVuelto = new Paragraph("VUELTO:      S/ " + String.format("%.2f", vuelto), fontBold);
                pVuelto.setAlignment(Element.ALIGN_RIGHT);
                document.add(pVuelto);
            }
            document.add(new Paragraph(separador, fontNormal));
            Paragraph pEstado = new Paragraph("*** VENTA PAGADA ***", fontBold);
            pEstado.setAlignment(Element.ALIGN_CENTER);
            document.add(pEstado);

        } else {
            // CASO 2: Es Crédito -> Mostramos Abonos y Saldo Pendiente real de la DB
            document.add(new Paragraph(separador, fontNormal));

            BigDecimal totalAbonado = pagoRepository.totalPagadoVenta(venta.getIdVenta());
            if (totalAbonado == null) totalAbonado = BigDecimal.ZERO;
            BigDecimal saldo = total.subtract(totalAbonado);

            Paragraph pAbono = new Paragraph("Abonado Total: S/ " + String.format("%.2f", totalAbonado), fontNormal);
            pAbono.setAlignment(Element.ALIGN_RIGHT);
            document.add(pAbono);

            if (saldo.compareTo(new BigDecimal("0.01")) > 0) {
                Paragraph pSaldo = new Paragraph("SALDO PENDIENTE: S/ " + String.format("%.2f", saldo), fontBold);
                pSaldo.setAlignment(Element.ALIGN_RIGHT);
                document.add(pSaldo);
            } else {
                Paragraph pCancelado = new Paragraph("*** VENTA CANCELADA ***", fontBold);
                pCancelado.setAlignment(Element.ALIGN_CENTER);
                document.add(pCancelado);
            }
        }

        // --- PIE ---
        document.add(new Paragraph("\n"));
        Paragraph pie = new Paragraph("¡Gracias por su compra!", fontNormal);
        pie.setAlignment(Element.ALIGN_CENTER);
        document.add(pie);

        document.close();
        return out.toByteArray();
    }

    private PdfPCell crearCelda(String texto, Font fuente, int alineacion) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, fuente));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(alineacion);
        cell.setPaddingBottom(5f);
        cell.setNoWrap(false);
        return cell;
    }
}