package pe.edu.vallegrande.app.service.impl;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReaderBuilder;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.vallegrande.app.dto.ImportResult;
import pe.edu.vallegrande.app.model.TipoTramite;
import pe.edu.vallegrande.app.service.TipoTramiteService;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Service that provides bulk import (Excel / CSV) and export (Excel / PDF)
 * for the TipoTramite master table.
 *
 * Import column order expected in both Excel and CSV:
 *   codigo | nombre | categoria | descripcion | requisitos
 *   | plazoEstimadoDias | requiereDocumentos | vigentDesde
 */
@Service
public class TipoTramiteImportExportServiceImpl {

    private static final Logger log = LoggerFactory.getLogger(TipoTramiteImportExportServiceImpl.class);

    private static final String[] EXPECTED_HEADERS = {
            "codigo", "nombre", "categoria", "descripcion",
            "requisitos", "plazoEstimadoDias", "requiereDocumentos", "vigentDesde"
    };

    private static final DateTimeFormatter DT_FMT  = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final TipoTramiteService tipoTramiteService;

    public TipoTramiteImportExportServiceImpl(TipoTramiteService tipoTramiteService) {
        this.tipoTramiteService = tipoTramiteService;
    }

    // ================================================================
    // IMPORT — EXCEL (.xlsx)
    // ================================================================
    @Transactional
    public ImportResult importFromExcel(MultipartFile file) throws IOException {
        log.info("Starting Excel import for TipoTramite — file: {}", file.getOriginalFilename());

        List<ImportResult.RowError> errors = new ArrayList<>();
        int imported = 0;

        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            Iterator<Row> rowIter = sheet.iterator();

            if (!rowIter.hasNext()) {
                return new ImportResult(0, 0, 0,
                        List.of(new ImportResult.RowError(1, "The file is empty.")));
            }
            Row headerRow = rowIter.next();
            List<String> actualHeaders = readHeadersFromRow(headerRow);
            String headerError = validateHeaders(actualHeaders);
            if (headerError != null) {
                return new ImportResult(0, 0, 1,
                        List.of(new ImportResult.RowError(1, headerError)));
            }

            while (rowIter.hasNext()) {
                Row row = rowIter.next();
                if (isBlankRow(row)) continue;
                int fileRow = row.getRowNum() + 1;
                try {
                    TipoTramite tt = rowToEntity(row);
                    tipoTramiteService.save(tt);
                    imported++;
                } catch (Exception ex) {
                    log.warn("TipoTramite row {} rejected: {}", fileRow, ex.getMessage());
                    errors.add(new ImportResult.RowError(fileRow, ex.getMessage()));
                }
            }
        }

        int total = imported + errors.size();
        log.info("TipoTramite Excel import — total: {}, imported: {}, rejected: {}",
                total, imported, errors.size());
        return new ImportResult(total, imported, errors.size(), errors);
    }

    // ================================================================
    // IMPORT — CSV (.csv)
    // ================================================================
    @Transactional
    public ImportResult importFromCsv(MultipartFile file) throws IOException {
        log.info("Starting CSV import for TipoTramite — file: {}", file.getOriginalFilename());

        List<ImportResult.RowError> errors = new ArrayList<>();
        int imported = 0;

        // Read all bytes once so the InputStream is not consumed by delimiter detection
        byte[] rawBytes = file.getBytes();

        char delimiter = detectCsvDelimiterFromBytes(rawBytes);

        try (Reader reader = new InputStreamReader(
                new ByteArrayInputStream(rawBytes), StandardCharsets.UTF_8)) {
            com.opencsv.CSVReader csvReader = new CSVReaderBuilder(reader)
                    .withCSVParser(new CSVParserBuilder().withSeparator(delimiter).build())
                    .build();

            List<String[]> allLines;
            try {
                allLines = csvReader.readAll();
            } catch (com.opencsv.exceptions.CsvException e) {
                throw new IOException("Error reading CSV: " + e.getMessage(), e);
            }

            if (allLines.isEmpty()) {
                return new ImportResult(0, 0, 0,
                        List.of(new ImportResult.RowError(1, "The file is empty.")));
            }

            String[] headerArray = allLines.get(0);
            // Skip the Excel separator hint line "sep=;" or "sep=," if present
            if (allLines.size() > 1 && headerArray.length == 1
                    && headerArray[0].trim().toLowerCase().startsWith("sep=")) {
                headerArray = allLines.get(1);
                List<String> actualHeaders = Arrays.stream(headerArray)
                        .map(h -> stripBom(h).strip())
                        .toList();
                String headerError = validateHeaders(actualHeaders);
                if (headerError != null) {
                    return new ImportResult(0, 0, 1,
                            List.of(new ImportResult.RowError(1, headerError)));
                }
                for (int i = 2; i < allLines.size(); i++) {
                    String[] cols = allLines.get(i);
                    int fileRow = i + 1;
                    if (cols.length == 0 || Arrays.stream(cols).allMatch(String::isBlank)) continue;
                    try {
                        TipoTramite tt = columnsToEntity(cols);
                        tipoTramiteService.save(tt);
                        imported++;
                    } catch (Exception ex) {
                        log.warn("TipoTramite CSV row {} rejected: {}", fileRow, ex.getMessage());
                        errors.add(new ImportResult.RowError(fileRow, ex.getMessage()));
                    }
                }
                int total = imported + errors.size();
                return new ImportResult(total, imported, errors.size(), errors);
            }

            List<String> actualHeaders = Arrays.stream(headerArray)
                    .map(h -> stripBom(h).strip())
                    .toList();
            String headerError = validateHeaders(actualHeaders);
            if (headerError != null) {
                return new ImportResult(0, 0, 1,
                        List.of(new ImportResult.RowError(1, headerError)));
            }

            for (int i = 1; i < allLines.size(); i++) {
                String[] cols = allLines.get(i);
                int fileRow = i + 1;
                if (cols.length == 0 || Arrays.stream(cols).allMatch(String::isBlank)) continue;
                try {
                    TipoTramite tt = columnsToEntity(cols);
                    tipoTramiteService.save(tt);
                    imported++;
                } catch (Exception ex) {
                    log.warn("TipoTramite CSV row {} rejected: {}", fileRow, ex.getMessage());
                    errors.add(new ImportResult.RowError(fileRow, ex.getMessage()));
                }
            }
        }

        int total = imported + errors.size();
        log.info("TipoTramite CSV import — total: {}, imported: {}, rejected: {}",
                total, imported, errors.size());
        return new ImportResult(total, imported, errors.size(), errors);
    }

    // ================================================================
    // EXPORT — EXCEL (.xlsx)
    // ================================================================
    @Transactional(readOnly = true)
    public byte[] exportToExcel() throws IOException {
        log.info("Generating Excel export for TipoTramite");

        List<TipoTramite> list = tipoTramiteService.findAll();

        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = wb.createSheet("TiposTramite");

            // ── Paleta ────────────────────────────────────────────────────────
            byte[] C_RED    = {(byte)139,(byte)0,  (byte)0  };
            byte[] C_WHITE  = {(byte)255,(byte)255,(byte)255};
            byte[] C_DARK   = {(byte)30, (byte)30, (byte)30 };
            byte[] C_BORDER = {(byte)200,(byte)150,(byte)150};
            byte[] C_GBKG   = {(byte)198,(byte)239,(byte)206};
            byte[] C_GFNT   = {(byte)0,  (byte)97, (byte)0  };
            byte[] C_RBKG   = {(byte)255,(byte)235,(byte)235};

            // Título
            XSSFCellStyle sTit = wb.createCellStyle();
            sTit.setFillForegroundColor(new XSSFColor(C_RED, null));
            sTit.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            sTit.setAlignment(HorizontalAlignment.CENTER);
            sTit.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
            { XSSFFont f = wb.createFont(); f.setBold(true); f.setFontHeightInPoints((short)16);
              f.setColor(new XSSFColor(C_WHITE, null)); sTit.setFont(f); }

            // Sede
            XSSFCellStyle sSed = wb.createCellStyle();
            sSed.setFillForegroundColor(new XSSFColor(C_RED, null));
            sSed.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            sSed.setAlignment(HorizontalAlignment.CENTER);
            sSed.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
            { XSSFFont f = wb.createFont(); f.setBold(true); f.setFontHeightInPoints((short)11);
              f.setColor(new XSSFColor(C_WHITE, null)); sSed.setFont(f); }

            // Sistema FUT
            XSSFCellStyle sSis = wb.createCellStyle();
            sSis.setAlignment(HorizontalAlignment.CENTER);
            sSis.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
            { XSSFFont f = wb.createFont(); f.setBold(true); f.setFontHeightInPoints((short)11);
              f.setColor(new XSSFColor(C_RED, null)); sSis.setFont(f); }

            // Sección
            XSSFCellStyle sSec = wb.createCellStyle();
            sSec.setAlignment(HorizontalAlignment.LEFT);
            sSec.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
            { XSSFFont f = wb.createFont(); f.setBold(true); f.setFontHeightInPoints((short)12);
              f.setColor(new XSSFColor(C_RED, null)); sSec.setFont(f); }

            // Periodo
            XSSFCellStyle sPer = wb.createCellStyle();
            sPer.setAlignment(HorizontalAlignment.CENTER);
            sPer.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
            sPer.setBorderTop(BorderStyle.MEDIUM);    sPer.setBorderBottom(BorderStyle.MEDIUM);
            sPer.setBorderLeft(BorderStyle.MEDIUM);   sPer.setBorderRight(BorderStyle.MEDIUM);
            sPer.setTopBorderColor(new XSSFColor(C_RED, null));
            sPer.setBottomBorderColor(new XSSFColor(C_RED, null));
            sPer.setLeftBorderColor(new XSSFColor(C_RED, null));
            sPer.setRightBorderColor(new XSSFColor(C_RED, null));
            { XSSFFont f = wb.createFont(); f.setBold(true); f.setFontHeightInPoints((short)11);
              f.setColor(new XSSFColor(C_RED, null)); sPer.setFont(f); }

            // Encabezado tabla — rojo, BLANCO, bordes blancos
            XSSFCellStyle sHdr = wb.createCellStyle();
            sHdr.setFillForegroundColor(new XSSFColor(C_RED, null));
            sHdr.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            sHdr.setAlignment(HorizontalAlignment.CENTER);
            sHdr.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
            sHdr.setWrapText(true);
            sHdr.setBorderTop(BorderStyle.THIN);    sHdr.setBorderBottom(BorderStyle.THIN);
            sHdr.setBorderLeft(BorderStyle.THIN);   sHdr.setBorderRight(BorderStyle.THIN);
            sHdr.setTopBorderColor(new XSSFColor(C_WHITE, null));
            sHdr.setBottomBorderColor(new XSSFColor(C_WHITE, null));
            sHdr.setLeftBorderColor(new XSSFColor(C_WHITE, null));
            sHdr.setRightBorderColor(new XSSFColor(C_WHITE, null));
            { XSSFFont f = wb.createFont(); f.setBold(true); f.setFontHeightInPoints((short)10);
              f.setColor(new XSSFColor(C_WHITE, null)); sHdr.setFont(f); }

            // Datos normal
            XSSFCellStyle sDat = wb.createCellStyle();
            sDat.setBorderTop(BorderStyle.THIN);    sDat.setBorderBottom(BorderStyle.THIN);
            sDat.setBorderLeft(BorderStyle.THIN);   sDat.setBorderRight(BorderStyle.THIN);
            sDat.setTopBorderColor(new XSSFColor(C_BORDER, null));
            sDat.setBottomBorderColor(new XSSFColor(C_BORDER, null));
            sDat.setLeftBorderColor(new XSSFColor(C_BORDER, null));
            sDat.setRightBorderColor(new XSSFColor(C_BORDER, null));
            sDat.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
            { XSSFFont f = wb.createFont(); f.setFontHeightInPoints((short)10);
              f.setColor(new XSSFColor(C_DARK, null)); sDat.setFont(f); }

            // Badge Activo
            XSSFCellStyle sAct = wb.createCellStyle();
            sAct.setFillForegroundColor(new XSSFColor(C_GBKG, null));
            sAct.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            sAct.setAlignment(HorizontalAlignment.CENTER);
            sAct.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
            sAct.setBorderTop(BorderStyle.THIN);    sAct.setBorderBottom(BorderStyle.THIN);
            sAct.setBorderLeft(BorderStyle.THIN);   sAct.setBorderRight(BorderStyle.THIN);
            sAct.setTopBorderColor(new XSSFColor(C_BORDER, null));
            sAct.setBottomBorderColor(new XSSFColor(C_BORDER, null));
            sAct.setLeftBorderColor(new XSSFColor(C_BORDER, null));
            sAct.setRightBorderColor(new XSSFColor(C_BORDER, null));
            { XSSFFont f = wb.createFont(); f.setBold(true); f.setFontHeightInPoints((short)10);
              f.setColor(new XSSFColor(C_GFNT, null)); sAct.setFont(f); }

            // Badge Inactivo
            XSSFCellStyle sIna = wb.createCellStyle();
            sIna.setFillForegroundColor(new XSSFColor(C_RBKG, null));
            sIna.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            sIna.setAlignment(HorizontalAlignment.CENTER);
            sIna.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
            sIna.setBorderTop(BorderStyle.THIN);    sIna.setBorderBottom(BorderStyle.THIN);
            sIna.setBorderLeft(BorderStyle.THIN);   sIna.setBorderRight(BorderStyle.THIN);
            sIna.setTopBorderColor(new XSSFColor(C_BORDER, null));
            sIna.setBottomBorderColor(new XSSFColor(C_BORDER, null));
            sIna.setLeftBorderColor(new XSSFColor(C_BORDER, null));
            sIna.setRightBorderColor(new XSSFColor(C_BORDER, null));
            { XSSFFont f = wb.createFont(); f.setBold(true); f.setFontHeightInPoints((short)10);
              f.setColor(new XSSFColor(C_RED, null)); sIna.setFont(f); }

            // Relleno rojo
            XSSFCellStyle sRed = wb.createCellStyle();
            sRed.setFillForegroundColor(new XSSFColor(C_RED, null));
            sRed.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // ── Anchos de columna ─────────────────────────────────────────────
            int[] colW = {4,7,14,10,16,16,7,8,10,7,15,15,15,15};
            for (int i = 0; i < colW.length; i++) sheet.setColumnWidth(i, colW[i] * 256);
            int LAST = 13;
            sheet.setZoom(100);

            // Fila 1: vacía
            sheet.createRow(0).setHeightInPoints(8f);

            // Fila 2: título
            Row r1 = sheet.createRow(1); r1.setHeightInPoints(34f);
            for (int c = 0; c <= LAST; c++) r1.createCell(c).setCellStyle(sRed);
            r1.getCell(0).setCellValue("I.E. N.\u00b0 20191 \u201cALFONSO UGARTE\u201d");
            r1.getCell(0).setCellStyle(sTit);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(1,1,0,LAST));

            // Fila 3: sede
            Row r2 = sheet.createRow(2); r2.setHeightInPoints(22f);
            for (int c = 0; c <= LAST; c++) r2.createCell(c).setCellStyle(sRed);
            r2.getCell(0).setCellValue("SAN VICENTE \u2013 CA\u00d1ETE");
            r2.getCell(0).setCellStyle(sSed);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(2,2,0,LAST));

            // Fila 4: sistema FUT
            Row r3 = sheet.createRow(3); r3.setHeightInPoints(22f);
            r3.createCell(0).setCellValue("Sistema de Gesti\u00f3n de Formulario \u00danico de Tr\u00e1mite (FUT)");
            r3.getCell(0).setCellStyle(sSis);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(3,3,0,LAST));

            // Fila 5: vacía
            sheet.createRow(4).setHeightInPoints(10f);

            // Fila 6: sección + periodo
            Row r5 = sheet.createRow(5); r5.setHeightInPoints(26f);
            r5.createCell(0).setCellValue("Registro de Tipos de Tr\u00e1mite");
            r5.getCell(0).setCellStyle(sSec);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(5,5,0,10));
            for (int c = 1; c <= 11; c++) r5.createCell(c);
            r5.createCell(12).setCellValue("\uD83D\uDCC5  Periodo: " + java.time.Year.now().getValue());
            r5.getCell(12).setCellStyle(sPer);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(5,5,12,LAST));

            // Fila 7: vacía
            sheet.createRow(6).setHeightInPoints(6f);

            // Fila 8: encabezados
            String[] hdrs = {
                "ID","C\u00f3digo","Nombre","Categor\u00eda","Descripci\u00f3n","Requisitos",
                "Plazo\n(d\u00edas)","Requiere\nDocs","Vigente\nDesde","Estado",
                "Fecha Registro","Fecha\nActualizaci\u00f3n",
                "Fecha\nEliminaci\u00f3n","Fecha\nRestauraci\u00f3n"
            };
            Row r7 = sheet.createRow(7); r7.setHeightInPoints(32f);
            for (int i = 0; i < hdrs.length; i++) {
                r7.createCell(i).setCellValue(hdrs[i]);
                r7.getCell(i).setCellStyle(sHdr);
            }

            // Filas de datos
            int rowNum = 8;
            for (TipoTramite tt : list) {
                Row row = sheet.createRow(rowNum); row.setHeightInPoints(17f);
                String estadoStr = tt.getEstado() != null ? (tt.getEstado() ? "Activo" : "Inactivo") : "";
                String[] vals = {
                    tt.getIdTipoTramite() != null ? tt.getIdTipoTramite().toString() : "",
                    nvl(tt.getCodigo()), nvl(tt.getNombre()), nvl(tt.getCategoria()),
                    nvl(tt.getDescripcion()), nvl(tt.getRequisitos()),
                    tt.getPlazoEstimadoDias() != null ? tt.getPlazoEstimadoDias().toString() : "",
                    tt.getRequiereDocumentos() != null ? (tt.getRequiereDocumentos() ? "S\u00ed" : "No") : "",
                    tt.getVigentDesde() != null ? tt.getVigentDesde().format(DATE_FMT) : "",
                    estadoStr, fmtDt(tt.getFechaRegistro()), fmtDt(tt.getFechaActualizacion()),
                    fmtDt(tt.getFechaEliminacion()), fmtDt(tt.getFechaRestauracion())
                };
                for (int i = 0; i < vals.length; i++) {
                    row.createCell(i).setCellValue(vals[i]);
                    if (i == 9) {
                        row.getCell(i).setCellStyle("Activo".equalsIgnoreCase(vals[i]) ? sAct : sIna);
                    } else {
                        row.getCell(i).setCellStyle(sDat);
                    }
                }
                rowNum++;
            }

            // Auto-ajustar columnas al contenido
            for (int i = 0; i <= LAST; i++) sheet.autoSizeColumn(i);

            wb.write(out);
            log.info("TipoTramite Excel export — {} rows", list.size());
            return out.toByteArray();
        }
    }
    // ================================================================
    // EXPORT — PDF
    // ================================================================
    @Transactional(readOnly = true)
    public byte[] exportToPdf() throws IOException {
        log.info("Generating PDF export for TipoTramite");

        List<TipoTramite> list = tipoTramiteService.findAll();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter   pdfWriter = new PdfWriter(out);
            PdfDocument pdfDoc    = new PdfDocument(pdfWriter);
            PageSize    pageSize  = PageSize.A4.rotate();
            Document    document  = new Document(pdfDoc, pageSize);

            float pw     = pageSize.getWidth();
            float ph     = pageSize.getHeight();
            float margin = 25f;
            document.setMargins(145f, margin, 50f, margin);

            com.itextpdf.kernel.font.PdfFont boldFont =
                    com.itextpdf.kernel.font.PdfFontFactory.createFont(
                            com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD);
            com.itextpdf.kernel.font.PdfFont normalFont =
                    com.itextpdf.kernel.font.PdfFontFactory.createFont(
                            com.itextpdf.io.font.constants.StandardFonts.HELVETICA);

            // ── Paleta ────────────────────────────────────────────────────────
            DeviceRgb RED_DARK   = new DeviceRgb(139,  0,   0);
            DeviceRgb RED_MID    = new DeviceRgb(180, 20,  20);
            DeviceRgb RED_LIGHT  = new DeviceRgb(220, 60,  60);
            DeviceRgb RED_PALE   = new DeviceRgb(253, 232, 229);
            DeviceRgb RED_BORDER = new DeviceRgb(180, 80,  80);
            DeviceRgb GREEN_BG   = new DeviceRgb(230, 255, 230);
            DeviceRgb GREEN_FG   = new DeviceRgb( 30, 130,  30);

            // ── Forzar creación de la primera página ──────────────────────────
            document.add(new Paragraph(" ").setFontSize(1f).setMargin(0f));
            document.flush();

            // ================================================================
            // HEADER
            // ================================================================
            PdfPage   page   = pdfDoc.getFirstPage();
            PdfCanvas canvas = new PdfCanvas(page);

            float band1H    = 52f;
            float band2H    = 28f;
            float band3H    = 32f;
            float headerTop = ph - 10f;

            canvas.setFillColor(RED_DARK)
                  .rectangle(0, headerTop - band1H, pw, band1H).fill();
            canvas.setFillColor(RED_MID)
                  .moveTo(pw - 80, headerTop).lineTo(pw, headerTop)
                  .lineTo(pw, headerTop - band1H).closePath().fill();
            canvas.setFillColor(RED_MID)
                  .moveTo(0, headerTop).lineTo(70, headerTop)
                  .lineTo(0, headerTop - band1H).closePath().fill();

            float f2Top = headerTop - band1H;
            canvas.setFillColor(RED_MID)
                  .rectangle(0, f2Top - band2H, pw, band2H).fill();

            float f3Top = f2Top - band2H;
            canvas.setFillColor(ColorConstants.WHITE)
                  .rectangle(0, f3Top - band3H, pw, band3H).fill();
            canvas.setFillColor(RED_LIGHT)
                  .rectangle(0, f3Top - band3H, pw, 3f).fill();
            canvas.setFillColor(RED_LIGHT)
                  .moveTo(pw - 60, f3Top).lineTo(pw, f3Top)
                  .lineTo(pw, f3Top - band3H).closePath().fill();
            canvas.release();

            Canvas cv1 = new Canvas(new PdfCanvas(page),
                    new Rectangle(0, headerTop - band1H, pw, band1H));
            cv1.add(new Paragraph("I.E. N.\u00b0 20191 \u201cAlfonso Ugarte\u201d")
                    .setFont(boldFont).setFontSize(22f)
                    .setFontColor(ColorConstants.WHITE)
                    .setTextAlignment(TextAlignment.CENTER).setMarginTop(8f));
            cv1.close();

            Canvas cv2 = new Canvas(new PdfCanvas(page),
                    new Rectangle(0, f2Top - band2H, pw, band2H));
            cv2.add(new Paragraph(
                    "Sistema de Gesti\u00f3n de Formularios \u00danicos de Tr\u00e1mite (FUT)")
                    .setFont(boldFont).setFontSize(10f)
                    .setFontColor(ColorConstants.WHITE)
                    .setTextAlignment(TextAlignment.CENTER).setMarginTop(7f));
            cv2.close();

            Canvas cv3 = new Canvas(new PdfCanvas(page),
                    new Rectangle(0, f3Top - band3H, pw, band3H));
            cv3.add(new Paragraph("Reporte de Tipos de Tr\u00e1mite")
                    .setFont(boldFont).setFontSize(14f)
                    .setFontColor(RED_DARK)
                    .setTextAlignment(TextAlignment.CENTER).setMarginTop(7f));
            cv3.close();

            // ── Banda de metadata ─────────────────────────────────────────────
            String now     = LocalDateTime.now().format(DT_FMT);
            String metaStr = "Sede: San Vicente de Ca\u00f1ete - Lima"
                           + "   |   Generado el: " + now
                           + "   |   Total de registros: " + list.size();

            document.add(new Paragraph(metaStr)
                    .setFont(normalFont).setFontSize(8f)
                    .setFontColor(new DeviceRgb(50, 50, 50))
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBorder(new SolidBorder(RED_BORDER, 1f))
                    .setBackgroundColor(new DeviceRgb(255, 252, 252))
                    .setPadding(6f)
                    .setMarginBottom(12f));

            // ── Tabla ─────────────────────────────────────────────────────────
            float[] colWidths = {3f, 5f, 10f, 7f, 12f, 12f, 5f, 5f, 7f, 5f, 8f};
            Table table = new Table(UnitValue.createPercentArray(colWidths))
                    .useAllAvailableWidth()
                    .setMarginBottom(0f);

            String[] pdfHeaders = {
                    "ID", "C\u00f3digo", "Nombre", "Categor\u00eda", "Descripci\u00f3n",
                    "Requisitos", "Plazo", "Req. Docs", "Vigente Desde", "Estado", "Fecha Registro"
            };
            for (String h : pdfHeaders) {
                table.addHeaderCell(
                        new Cell()
                                .add(new Paragraph(h).setFont(boldFont).setFontSize(7f))
                                .setBackgroundColor(RED_DARK)
                                .setFontColor(ColorConstants.WHITE)
                                .setTextAlignment(TextAlignment.CENTER)
                                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                                .setBorder(new SolidBorder(RED_MID, 0.5f))
                                .setPadding(5f));
            }

            int rowNum = 0;
            for (TipoTramite tt : list) {
                DeviceRgb rowBg = (rowNum % 2 == 0) ? new DeviceRgb(255,255,255) : RED_PALE;

                String estadoStr = tt.getEstado() != null
                        ? (tt.getEstado() ? "Activo" : "Inactivo") : "";

                String[] vals = {
                        tt.getIdTipoTramite() != null ? tt.getIdTipoTramite().toString() : "",
                        nvl(tt.getCodigo()),
                        nvl(tt.getNombre()),
                        nvl(tt.getCategoria()),
                        nvl(tt.getDescripcion()),
                        nvl(tt.getRequisitos()),
                        tt.getPlazoEstimadoDias() != null
                                ? tt.getPlazoEstimadoDias().toString() : "",
                        tt.getRequiereDocumentos() != null
                                ? (tt.getRequiereDocumentos() ? "S\u00ed" : "No") : "",
                        tt.getVigentDesde() != null
                                ? tt.getVigentDesde().format(DATE_FMT) : "",
                        estadoStr,
                        fmtDt(tt.getFechaRegistro())
                };
                for (int i = 0; i < vals.length; i++) {
                    Cell cell;
                    if (i == 9) {
                        boolean isActive = "Activo".equalsIgnoreCase(vals[i]);
                        DeviceRgb badgeBg = isActive ? GREEN_BG : new DeviceRgb(255,235,235);
                        DeviceRgb badgeFg = isActive ? GREEN_FG : RED_DARK;
                        cell = new Cell()
                                .add(new Paragraph(vals[i]).setFont(boldFont).setFontSize(6.5f)
                                        .setFontColor(badgeFg))
                                .setBackgroundColor(badgeBg)
                                .setTextAlignment(TextAlignment.CENTER)
                                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                                .setBorder(new SolidBorder(RED_BORDER, 0.4f))
                                .setPadding(3f);
                    } else {
                        cell = new Cell()
                                .add(new Paragraph(vals[i]).setFont(normalFont).setFontSize(6.5f)
                                        .setFontColor(new DeviceRgb(40, 40, 40)))
                                .setBackgroundColor(rowBg)
                                .setTextAlignment(i == 0 ? TextAlignment.CENTER : TextAlignment.LEFT)
                                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                                .setBorder(new SolidBorder(RED_BORDER, 0.4f))
                                .setPadding(3f);
                    }
                    table.addCell(cell);
                }
                rowNum++;
            }

            document.add(table);

            // ── Footer en todas las páginas ───────────────────────────────────
            document.flush();
            int totalPages = pdfDoc.getNumberOfPages();
            for (int p = 1; p <= totalPages; p++) {
                PdfPage   fp  = pdfDoc.getPage(p);
                PdfCanvas fpc = new PdfCanvas(fp);
                fpc.setFillColor(RED_DARK).rectangle(0, 0, pw, 18f).fill();
                fpc.setFillColor(RED_LIGHT).rectangle(0, 18f, pw, 4f).fill();
                fpc.release();
                Canvas footerCv = new Canvas(new PdfCanvas(fp),
                        new Rectangle(0, 0, pw, 18f));
                footerCv.add(new Paragraph(
                        "I.E. N.\u00b0 20191 \u201cAlfonso Ugarte\u201d  \u2014  P\u00e1gina " + p + " de " + totalPages)
                        .setFont(normalFont).setFontSize(7f)
                        .setFontColor(ColorConstants.WHITE)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginTop(3f));
                footerCv.close();
            }

            document.close();
            log.info("TipoTramite PDF export — {} rows", list.size());
            return out.toByteArray();
        }
    }

    // ================================================================
    // PRIVATE HELPERS
    // ================================================================

    private List<String> readHeadersFromRow(Row row) {
        List<String> headers = new ArrayList<>();
        for (org.apache.poi.ss.usermodel.Cell cell : row) {
            headers.add(stripBom(getCellStringValue(cell)).strip());
        }
        return headers;
    }

    private String validateHeaders(List<String> actual) {
        for (String expected : EXPECTED_HEADERS) {
            boolean found = actual.stream().anyMatch(h -> h.equalsIgnoreCase(expected));
            if (!found) {
                return "Missing required column: '" + expected
                       + "'. Expected columns: " + Arrays.toString(EXPECTED_HEADERS);
            }
        }
        return null;
    }

    private TipoTramite rowToEntity(Row row) {
        TipoTramite tt = new TipoTramite();
        String codigo = getCellStringValue(row.getCell(0));
        tt.setCodigo(codigo.isBlank() ? null : codigo);
        tt.setNombre(getCellStringValue(row.getCell(1)));
        String cat = getCellStringValue(row.getCell(2));
        tt.setCategoria(cat.isBlank() ? null : cat);
        String desc = getCellStringValue(row.getCell(3));
        tt.setDescripcion(desc.isBlank() ? null : desc);
        String req = getCellStringValue(row.getCell(4));
        tt.setRequisitos(req.isBlank() ? null : req);
        String plazo = getCellStringValue(row.getCell(5));
        if (!plazo.isBlank()) {
            try { tt.setPlazoEstimadoDias(Integer.parseInt(plazo)); }
            catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "plazoEstimadoDias must be an integer: " + plazo);
            }
        }
        String reqDocs = getCellStringValue(row.getCell(6));
        tt.setRequiereDocumentos(parseBool(reqDocs));
        String vigDesde = getCellStringValue(row.getCell(7));
        if (!vigDesde.isBlank()) {
            try { tt.setVigentDesde(LocalDate.parse(vigDesde, DATE_FMT)); }
            catch (Exception e) {
                try { tt.setVigentDesde(LocalDate.parse(vigDesde)); }
                catch (Exception e2) { /* leave null — non-critical field */ }
            }
        }
        return tt;
    }

    private TipoTramite columnsToEntity(String[] cols) {
        TipoTramite tt = new TipoTramite();
        tt.setCodigo(col(cols, 0));
        tt.setNombre(col(cols, 1));
        tt.setCategoria(col(cols, 2));
        tt.setDescripcion(col(cols, 3));
        tt.setRequisitos(col(cols, 4));
        String plazo = col(cols, 5);
        if (plazo != null && !plazo.isBlank()) {
            try { tt.setPlazoEstimadoDias(Integer.parseInt(plazo)); }
            catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "plazoEstimadoDias must be an integer: " + plazo);
            }
        }
        String reqDocs = col(cols, 6);
        tt.setRequiereDocumentos(reqDocs != null ? parseBool(reqDocs) : Boolean.FALSE);
        String vigDesde = col(cols, 7);
        if (vigDesde != null && !vigDesde.isBlank()) {
            try { tt.setVigentDesde(LocalDate.parse(vigDesde, DATE_FMT)); }
            catch (Exception e) {
                try { tt.setVigentDesde(LocalDate.parse(vigDesde)); }
                catch (Exception e2) { /* leave null */ }
            }
        }
        return tt;
    }

    private Boolean parseBool(String v) {
        if (v == null || v.isBlank()) return Boolean.FALSE;
        return v.equalsIgnoreCase("true") || v.equalsIgnoreCase("si")
                || v.equalsIgnoreCase("s\u00ed") || v.equals("1")
                || v.equalsIgnoreCase("yes");
    }

    private String col(String[] cols, int idx) {
        if (idx >= cols.length) return null;
        String v = cols[idx].trim();
        return v.isEmpty() ? null : v;
    }

    private String getCellStringValue(org.apache.poi.ss.usermodel.Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toLocalDate().format(DATE_FMT);
                }
                double d = cell.getNumericCellValue();
                yield (d == Math.floor(d) && !Double.isInfinite(d))
                        ? String.valueOf((long) d)
                        : String.valueOf(d);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try { yield cell.getStringCellValue().trim(); }
                catch (Exception e) { yield String.valueOf(cell.getNumericCellValue()); }
            }
            default -> "";
        };
    }

    private boolean isBlankRow(Row row) {
        if (row == null) return true;
        for (org.apache.poi.ss.usermodel.Cell cell : row) {
            if (cell != null
                    && cell.getCellType() != CellType.BLANK
                    && !getCellStringValue(cell).isBlank()) return false;
        }
        return true;
    }

    /**
     * Detects the CSV delimiter from an already-read byte array.
     * This avoids consuming the MultipartFile InputStream before parsing.
     */
    private char detectCsvDelimiterFromBytes(byte[] rawBytes) throws IOException {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(rawBytes), StandardCharsets.UTF_8))) {
            String firstLine = br.readLine();
            if (firstLine == null) return ',';
            long semicolons = firstLine.chars().filter(c -> c == ';').count();
            long commas     = firstLine.chars().filter(c -> c == ',').count();
            return semicolons >= commas ? ';' : ',';
        }
    }

    /** Removes the UTF-8 BOM character (U+FEFF) from the start of a string if present. */
    private String stripBom(String s) {
        if (s != null && !s.isEmpty() && s.charAt(0) == '\uFEFF') {
            return s.substring(1);
        }
        return s != null ? s : "";
    }

    private void poiCreateCell(Row row, int col, String value, CellStyle style) {
        org.apache.poi.ss.usermodel.Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private String nvl(String s) { return s != null ? s : ""; }

    private String fmtDt(LocalDateTime dt) { return dt != null ? dt.format(DT_FMT) : ""; }

    // ================================================================
    // TEMPLATE — EXCEL (.xlsx)
    // ================================================================
    public byte[] generateTemplateExcel() throws IOException {
        log.info("Generating Excel template for TipoTramite");

        // ── Paleta de colores ─────────────────────────────────────────────
        byte[] C_RED    = {(byte)139,(byte)0,  (byte)0  };
        byte[] C_RED2   = {(byte)180,(byte)20, (byte)20 };
        byte[] C_WHITE  = {(byte)255,(byte)255,(byte)255};
        byte[] C_DARK   = {(byte)30, (byte)30, (byte)30 };
        byte[] C_SILVER = {(byte)245,(byte)245,(byte)245};
        byte[] C_RULE   = {(byte)255,(byte)243,(byte)205};
        byte[] C_RULEFG = {(byte)92, (byte)64, (byte)0  };
        byte[] C_BORDER = {(byte)200,(byte)150,(byte)150};

        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet = wb.createSheet("Plantilla TiposTramite");
            sheet.setZoom(100);

            // ── Estilos ───────────────────────────────────────────────────

            XSSFCellStyle sTit = wb.createCellStyle();
            sTit.setFillForegroundColor(new XSSFColor(C_RED, null));
            sTit.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            sTit.setAlignment(HorizontalAlignment.CENTER);
            sTit.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
            { XSSFFont f = wb.createFont(); f.setBold(true); f.setFontHeightInPoints((short)16);
              f.setColor(new XSSFColor(C_WHITE, null)); sTit.setFont(f); }

            XSSFCellStyle sSub = wb.createCellStyle();
            sSub.setFillForegroundColor(new XSSFColor(C_RED2, null));
            sSub.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            sSub.setAlignment(HorizontalAlignment.CENTER);
            sSub.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
            { XSSFFont f = wb.createFont(); f.setBold(true); f.setFontHeightInPoints((short)10);
              f.setColor(new XSSFColor(C_WHITE, null)); sSub.setFont(f); }

            XSSFCellStyle sRed = wb.createCellStyle();
            sRed.setFillForegroundColor(new XSSFColor(C_RED, null));
            sRed.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            XSSFCellStyle sRed2 = wb.createCellStyle();
            sRed2.setFillForegroundColor(new XSSFColor(C_RED2, null));
            sRed2.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            XSSFCellStyle sHdr = wb.createCellStyle();
            sHdr.setFillForegroundColor(new XSSFColor(C_RED, null));
            sHdr.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            sHdr.setAlignment(HorizontalAlignment.CENTER);
            sHdr.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
            sHdr.setBorderBottom(BorderStyle.MEDIUM); sHdr.setBorderTop(BorderStyle.THIN);
            sHdr.setBorderLeft(BorderStyle.THIN);     sHdr.setBorderRight(BorderStyle.THIN);
            sHdr.setTopBorderColor(new XSSFColor(C_WHITE, null));
            sHdr.setBottomBorderColor(new XSSFColor(C_WHITE, null));
            sHdr.setLeftBorderColor(new XSSFColor(C_WHITE, null));
            sHdr.setRightBorderColor(new XSSFColor(C_WHITE, null));
            { XSSFFont f = wb.createFont(); f.setBold(true); f.setFontHeightInPoints((short)10);
              f.setColor(new XSSFColor(C_WHITE, null)); sHdr.setFont(f); }

            XSSFCellStyle sRule = wb.createCellStyle();
            sRule.setFillForegroundColor(new XSSFColor(C_RULE, null));
            sRule.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            sRule.setAlignment(HorizontalAlignment.CENTER);
            sRule.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
            sRule.setWrapText(true);
            sRule.setBorderBottom(BorderStyle.THIN); sRule.setBorderTop(BorderStyle.THIN);
            sRule.setBorderLeft(BorderStyle.THIN);   sRule.setBorderRight(BorderStyle.THIN);
            sRule.setTopBorderColor(new XSSFColor(C_BORDER, null));
            sRule.setBottomBorderColor(new XSSFColor(C_BORDER, null));
            sRule.setLeftBorderColor(new XSSFColor(C_BORDER, null));
            sRule.setRightBorderColor(new XSSFColor(C_BORDER, null));
            { XSSFFont f = wb.createFont(); f.setItalic(true); f.setFontHeightInPoints((short)8);
              f.setColor(new XSSFColor(C_RULEFG, null)); sRule.setFont(f); }

            XSSFCellStyle sDat = wb.createCellStyle();
            sDat.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
            sDat.setBorderBottom(BorderStyle.THIN); sDat.setBorderTop(BorderStyle.THIN);
            sDat.setBorderLeft(BorderStyle.THIN);   sDat.setBorderRight(BorderStyle.THIN);
            sDat.setTopBorderColor(new XSSFColor(C_BORDER, null));
            sDat.setBottomBorderColor(new XSSFColor(C_BORDER, null));
            sDat.setLeftBorderColor(new XSSFColor(C_BORDER, null));
            sDat.setRightBorderColor(new XSSFColor(C_BORDER, null));
            { XSSFFont f = wb.createFont(); f.setFontHeightInPoints((short)10);
              f.setColor(new XSSFColor(C_DARK, null)); sDat.setFont(f); }

            XSSFCellStyle sDat2 = wb.createCellStyle();
            sDat2.setFillForegroundColor(new XSSFColor(C_SILVER, null));
            sDat2.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            sDat2.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
            sDat2.setBorderBottom(BorderStyle.THIN); sDat2.setBorderTop(BorderStyle.THIN);
            sDat2.setBorderLeft(BorderStyle.THIN);   sDat2.setBorderRight(BorderStyle.THIN);
            sDat2.setTopBorderColor(new XSSFColor(C_BORDER, null));
            sDat2.setBottomBorderColor(new XSSFColor(C_BORDER, null));
            sDat2.setLeftBorderColor(new XSSFColor(C_BORDER, null));
            sDat2.setRightBorderColor(new XSSFColor(C_BORDER, null));
            { XSSFFont f = wb.createFont(); f.setFontHeightInPoints((short)10);
              f.setColor(new XSSFColor(C_DARK, null)); sDat2.setFont(f); }

            int LAST = 7;

            // ── Fila 1 vacía ──────────────────────────────────────────────
            sheet.createRow(0).setHeightInPoints(6f);

            // ── Fila 2: título ────────────────────────────────────────────
            Row r1 = sheet.createRow(1); r1.setHeightInPoints(32f);
            for (int c = 0; c <= LAST; c++) r1.createCell(c).setCellStyle(sRed);
            r1.getCell(0).setCellValue("I.E. N.\u00b0 20191 \u201cALFONSO UGARTE\u201d");
            r1.getCell(0).setCellStyle(sTit);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(1,1,0,LAST));

            // ── Fila 3: subtítulo ─────────────────────────────────────────
            Row r2 = sheet.createRow(2); r2.setHeightInPoints(20f);
            for (int c = 0; c <= LAST; c++) r2.createCell(c).setCellStyle(sRed2);
            r2.getCell(0).setCellValue("Plantilla de Importaci\u00f3n \u2014 Tipos de Tr\u00e1mite   |   Sistema FUT");
            r2.getCell(0).setCellStyle(sSub);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(2,2,0,LAST));

            // ── Fila 4 vacía ──────────────────────────────────────────────
            sheet.createRow(3).setHeightInPoints(6f);

            // ── Fila 5: encabezados ───────────────────────────────────────
            String[] headers = EXPECTED_HEADERS;
            Row hRow = sheet.createRow(4); hRow.setHeightInPoints(24f);
            for (int i = 0; i <= LAST; i++) {
                org.apache.poi.ss.usermodel.Cell cell = hRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(sHdr);
            }

            // ── Fila 6: reglas ────────────────────────────────────────────
            String[] rules = {
                "Opcional (ej: TRM-001)",
                "Obligatorio. M\u00e1x. 100 car. \u00danico.",
                "Opcional (ej: Acad\u00e9mico)",
                "Opcional. M\u00e1x. 250 car.",
                "Opcional. M\u00e1x. 500 car.",
                "N\u00famero entero (opcional)",
                "true/false \u00f3 si/no \u00f3 1/0",
                "Formato: yyyy-MM-dd"
            };
            Row rRow = sheet.createRow(5); rRow.setHeightInPoints(28f);
            for (int i = 0; i <= LAST; i++) {
                org.apache.poi.ss.usermodel.Cell cell = rRow.createCell(i);
                cell.setCellValue(rules[i]);
                cell.setCellStyle(sRule);
            }

            // ── Filas 7-11: filas vacías con estilo alterno ───────────────
            for (int r = 6; r <= 10; r++) {
                Row row = sheet.createRow(r); row.setHeightInPoints(18f);
                XSSFCellStyle style = (r % 2 == 0) ? sDat : sDat2;
                for (int c = 0; c <= LAST; c++) {
                    row.createCell(c).setCellStyle(style);
                }
            }

            // ── Anchos de columna ─────────────────────────────────────────
            int[] widths = {16, 32, 20, 32, 32, 20, 24, 18};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256);
            }

            sheet.createFreezePane(0, 5);

            wb.write(out);
            log.info("Excel template generated for TipoTramite");
            return out.toByteArray();
        }
    }

    // ================================================================
    // TEMPLATE — CSV (.csv)
    // ================================================================
    public byte[] generateTemplateCsv() {
        log.info("Generating CSV template for TipoTramite");
        // sep=; indica a Excel (Windows/español) que use punto y coma como separador,
        // lo que permite abrir el archivo con doble clic y ver columnas separadas.
        // El importador detecta automáticamente coma o punto y coma.
        String content = "sep=;\n" + String.join(";", EXPECTED_HEADERS);
        return content.getBytes(StandardCharsets.UTF_8);
    }
}
