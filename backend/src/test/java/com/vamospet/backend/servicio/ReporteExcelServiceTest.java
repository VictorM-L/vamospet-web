package com.vamospet.backend.servicio;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class ReporteExcelServiceTest {
    private ReporteExcelService servicio;
    @BeforeEach
    void preparar() { servicio = new ReporteExcelService(); }
    private ReporteService.Tabla tablaEjemplo() {
        ReporteService.Tabla t = new ReporteService.Tabla();
        t.titulo = "Ingresos por periodo";
        t.subtitulo = "Del 01/10/2026 al 23/10/2026";
        t.columnas = List.of("Fecha", "N° de pagos", "Total");
        t.fila("10/10/2026", "2", "S/ 105.00");
        t.fila("11/10/2026", "1", "S/ 45.00");
        t.dato("Ingreso neto", "S/ 150.00");
        return t;
    }
    private Sheet abrir(byte[] bytes) throws Exception {
        Workbook libro = new XSSFWorkbook(new ByteArrayInputStream(bytes));
        return libro.getSheetAt(0);
    }
    private Row buscarFila(Sheet hoja, String texto) {
        for (Row r : hoja) {
            Cell c = r.getCell(0);
            if (c != null && texto.equals(c.getStringCellValue())) return r;
        }
        return null;
    }
    @Test
    @DisplayName("Genera un libro .xlsx válido con una hoja")
    void generaLibroValido() throws Exception {
        byte[] bytes = servicio.generar(tablaEjemplo());
        assertTrue(bytes.length > 0);
        try (Workbook libro = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertEquals(1, libro.getNumberOfSheets());
        }
    }
    @Test
    @DisplayName("Escribe título, cabeceras y todas las filas de datos")
    void escribeTituloCabecerasYFilas() throws Exception {
        Sheet hoja = abrir(servicio.generar(tablaEjemplo()));
        assertNotNull(buscarFila(hoja, "Ingresos por periodo"), "falta el título");
        Row cab = buscarFila(hoja, "Fecha");
        assertNotNull(cab, "faltan las cabeceras");
        assertEquals("Total", cab.getCell(2).getStringCellValue());
        Row primera = hoja.getRow(cab.getRowNum() + 1);
        assertEquals("10/10/2026", primera.getCell(0).getStringCellValue());
        assertEquals("S/ 105.00", primera.getCell(2).getStringCellValue());
        assertEquals("11/10/2026", hoja.getRow(cab.getRowNum() + 2).getCell(0).getStringCellValue());
    }
    @Test
    @DisplayName("Incluye el resumen (etiqueta y valor)")
    void incluyeResumen() throws Exception {
        Sheet hoja = abrir(servicio.generar(tablaEjemplo()));
        Row resumen = buscarFila(hoja, "Ingreso neto");
        assertNotNull(resumen);
        assertEquals("S/ 150.00", resumen.getCell(1).getStringCellValue());
    }
    @Test
    @DisplayName("Sin filas muestra el mensaje 'No hay datos' y no falla")
    void sinFilas() throws Exception {
        ReporteService.Tabla t = tablaEjemplo();
        t.filas.clear();
        t.colores.clear();
        Sheet hoja = abrir(servicio.generar(t));
        assertNotNull(buscarFila(hoja, "No hay datos para el periodo seleccionado."));
    }
    @Test
    @DisplayName("La última columna lleva color de semáforo cuando la fila trae color")
    void pintaSemaforo() throws Exception {
        ReporteService.Tabla t = new ReporteService.Tabla();
        t.titulo = "Inventario con semáforo";
        t.subtitulo = "Situación actual";
        t.columnas = List.of("Producto", "Semáforo");
        t.filaColor("rojo", "Desparasitante", "Crítico: sin stock");
        t.fila("Correa", "Normal");
        Sheet hoja = abrir(servicio.generar(t));
        Row cab = buscarFila(hoja, "Producto");
        Cell rojo = hoja.getRow(cab.getRowNum() + 1).getCell(1);
        Cell sinColor = hoja.getRow(cab.getRowNum() + 2).getCell(1);
        assertEquals(FillPatternType.SOLID_FOREGROUND, rojo.getCellStyle().getFillPattern());
        assertEquals(FillPatternType.NO_FILL, sinColor.getCellStyle().getFillPattern());
    }
    @Test
    @DisplayName("Un título con caracteres prohibidos o de más de 31 letras produce un nombre de hoja válido")
    void nombreDeHojaSeguro() throws Exception {
        ReporteService.Tabla t = tablaEjemplo();
        t.titulo = "Citas / atenciones [prueba] por veterinario";
        try (Workbook libro = new XSSFWorkbook(new ByteArrayInputStream(servicio.generar(t)))) {
            String nombre = libro.getSheetName(0);
            assertTrue(nombre.length() <= 31);
            assertFalse(nombre.contains("/") || nombre.contains("[") || nombre.contains("]"));
        }
    }
}
