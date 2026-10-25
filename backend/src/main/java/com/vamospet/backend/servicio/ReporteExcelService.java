package com.vamospet.backend.servicio;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
@Service
public class ReporteExcelService {
    public static final String CONTENT_TYPE =
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final int ANCHO_MIN = 10;
    private static final int ANCHO_MAX = 60;
    private static final Map<String, byte[]> SEMAFORO = Map.of(
        "rojo", new byte[]{(byte) 254, (byte) 202, (byte) 202},
        "amarillo", new byte[]{(byte) 253, (byte) 230, (byte) 138},
        "verde", new byte[]{(byte) 187, (byte) 247, (byte) 208});
    public byte[] generar(ReporteService.Tabla t) throws IOException {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet(WorkbookUtil.createSafeSheetName(t.titulo));
            Estilos est = new Estilos(libro);
            int fila = 0;
            fila = escribirEncabezado(hoja, est, t, fila);
            fila = escribirResumen(hoja, est, t, fila);
            escribirTabla(hoja, est, t, fila);
            ajustarAnchos(hoja, t);
            libro.write(salida);
            return salida.toByteArray();
        }
    }
    private int escribirEncabezado(Sheet hoja, Estilos est, ReporteService.Tabla t, int fila) {
        celda(hoja.createRow(fila++), 0, "Vamos Pet — Clínica Veterinaria", est.marca);
        celda(hoja.createRow(fila++), 0, t.titulo, est.titulo);
        celda(hoja.createRow(fila++), 0, t.subtitulo, est.normal);
        celda(hoja.createRow(fila++), 0, "Generado el " + LocalDateTime.now()
        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), est.normal);
        return fila + 1;
    }
    private int escribirResumen(Sheet hoja, Estilos est, ReporteService.Tabla t, int fila) {
        if (t.resumen.isEmpty()) return fila;
        for (List<String> par : t.resumen) {
            Row r = hoja.createRow(fila++);
            celda(r, 0, par.get(0), est.normal);
            celda(r, 1, par.get(1), est.negrita);
        }
        return fila + 1;
    }
    private void escribirTabla(Sheet hoja, Estilos est, ReporteService.Tabla t, int filaCabecera) {
        Row cab = hoja.createRow(filaCabecera);
        for (int j = 0; j < t.columnas.size(); j++) celda(cab, j, t.columnas.get(j), est.cabecera);
        if (t.filas.isEmpty()) {
            Row vacio = hoja.createRow(filaCabecera + 1);
            celda(vacio, 0, "No hay datos para el periodo seleccionado.", est.normal);
            return;
        }
        for (int i = 0; i < t.filas.size(); i++) {
            Row r = hoja.createRow(filaCabecera + 1 + i);
            List<String> datos = t.filas.get(i);
            String color = i < t.colores.size() ? t.colores.get(i) : null;
            for (int j = 0; j < datos.size(); j++) {
                boolean ultima = j == datos.size() - 1;
                CellStyle estilo = (ultima && color != null) ? est.semaforo(color) : est.normal;
                celda(r, j, datos.get(j), estilo);
            }
        }
        hoja.createFreezePane(0, filaCabecera + 1);
        hoja.setAutoFilter(new CellRangeAddress(filaCabecera, filaCabecera + t.filas.size(),
            0, t.columnas.size() - 1));
    }
    private void ajustarAnchos(Sheet hoja, ReporteService.Tabla t) {
        for (int j = 0; j < t.columnas.size(); j++) {
            int max = t.columnas.get(j).length();
            for (List<String> f : t.filas) {
                if (j < f.size() && f.get(j) != null) max = Math.max(max, f.get(j).length());
            }
            hoja.setColumnWidth(j, Math.max(ANCHO_MIN, Math.min(ANCHO_MAX, max + 2)) * 256);
        }
    }
    private void celda(Row fila, int col, String texto, CellStyle estilo) {
        Cell c = fila.createCell(col);
        c.setCellValue(texto == null ? "" : texto);
        c.setCellStyle(estilo);
    }
    private static final class Estilos {
        final CellStyle marca, titulo, normal, negrita, cabecera;
        private final XSSFWorkbook libro;
        private final Map<String, CellStyle> semaforos = new java.util.HashMap<>();
        Estilos(XSSFWorkbook libro) {
            this.libro = libro;
            marca = estilo(fuente(true, 11, IndexedColors.GREEN.getIndex()), null);
            titulo = estilo(fuente(true, 16, IndexedColors.BLACK.getIndex()), null);
            normal = estilo(fuente(false, 10, IndexedColors.BLACK.getIndex()), null);
            negrita = estilo(fuente(true, 10, IndexedColors.BLACK.getIndex()), null);
            cabecera = estilo(fuente(true, 10, IndexedColors.WHITE.getIndex()),
                new byte[]{22, (byte) 120, 80});
        }
        CellStyle semaforo(String color) {
            return semaforos.computeIfAbsent(color, k -> {
                CellStyle e = estilo(fuente(true, 10, IndexedColors.BLACK.getIndex()), SEMAFORO.get(k));
                return e;
            });
        }
        private Font fuente(boolean negrita, int tam, short color) {
            Font f = libro.createFont();
            f.setBold(negrita);
            f.setFontHeightInPoints((short) tam);
            f.setColor(color);
            return f;
        }
        private CellStyle estilo(Font f, byte[] rgb) {
            org.apache.poi.xssf.usermodel.XSSFCellStyle e = libro.createCellStyle();
            e.setFont(f);
            if (rgb != null) {
                e.setFillForegroundColor(new XSSFColor(rgb, null));
                e.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            }
            return e;
        }
    }
}
