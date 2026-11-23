package pe.edu.vallegrande.app.rest;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.vallegrande.app.dto.ImportResult;
import pe.edu.vallegrande.app.model.TipoTramite;
import pe.edu.vallegrande.app.service.TipoTramiteService;
import pe.edu.vallegrande.app.service.impl.TipoTramiteImportExportServiceImpl;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * REST controller for procedure type management.
 * Primary path: /v1/api/procedure-type
 * Legacy alias:  /v1/api/tipo-tramite (kept for Angular frontend compatibility)
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/v1/api/procedure-type")
@Tag(name = "Procedure Type API",
     description = "Master Table — Administration of procedure types available in the FUT system"
)
public class TipoTramiteRest {

    private final TipoTramiteService service;
    private final TipoTramiteImportExportServiceImpl importExportService;

    public TipoTramiteRest(TipoTramiteService service,
                           TipoTramiteImportExportServiceImpl importExportService) {
        this.service             = service;
        this.importExportService = importExportService;
    }

    // Query all procedure types.
    @GetMapping
    @Operation(
        summary = "Query all procedure types",
        description = "Returns the complete list of procedure types (active and inactive).\n\n" +
                      "**Successful response (200):** array with all records.\n\n" +
                      "To retrieve only active ones, use the /estado/true endpoint."
    )
    public List<TipoTramite> findAll() {
        return service.findAll();
    }

    // Query procedure types by status.
    @GetMapping("/estado/{estado}")
    @Operation(
        summary = "Query procedure types by status",
        description = "Filters procedure types by status:\n\n" +
                      "- **true** = Active (available for selection in the FUT form)\n" +
                      "- **false** = Inactive (not available for new requests)\n\n" +
                      "**Successful response (200):** array of procedure types with the specified status."
    )
    public List<TipoTramite> findByEstado(
            @Parameter(description = "Procedure type status. true = active, false = inactive.")
            @Schema(allowableValues = {"true", "false"})
            @PathVariable Boolean estado) {
        return service.findByEstado(estado);
    }

    // Find a procedure type by its identifier.
    @GetMapping("/{id}")
    @Operation(
        summary = "Find procedure type by ID",
        description = "Returns the data of the procedure type with the specified identifier.\n\n" +
                      "**Successful response (200):** object with the procedure type data.\n\n" +
                      "**Error 404:** if no procedure type exists with that ID."
    )
    public TipoTramite findById(
            @Parameter(description = "Numeric identifier of the procedure type (id_tipo_tramite).")
            @PathVariable Integer id) {
        return service.findById(id);
    }

    // Register a new procedure type.
    @PostMapping
    @Operation(
        summary = "Register new procedure type",
        description = "Creates a new procedure type in the system.\n\n" +
                      "**Required fields:** nombre.\n\n" +
                      "**Optional fields:** descripcion, requisitos, plazoEstimadoDias, estado.\n\n" +
                      "**nombre:** procedure name (maximum 100 characters, must be unique).\n\n" +
                      "**plazoEstimadoDias:** estimated number of days to resolve the procedure.\n\n" +
                      "**estado:** true = active (default), false = inactive.\n\n" +
                      "**Successful response (200):** object with the registered procedure type.\n\n" +
                      "**Error 400:** if the name already exists or a required field is missing."
    )
    public ResponseEntity<TipoTramite> save(@RequestBody TipoTramite entrada) {
        return ResponseEntity.ok(service.save(entrada));
    }

    // Compatibility alias: hidden to avoid duplicates in Swagger.
    @Hidden
    @PostMapping({"/", "/save"})
    public ResponseEntity<TipoTramite> saveAlias(@RequestBody TipoTramite entrada) {
        return ResponseEntity.ok(service.save(entrada));
    }

    // Update an existing procedure type.
    @PutMapping("/{id}")
    @Operation(
        summary = "Update procedure type",
        description = "Modifies the data of an existing procedure type.\n\n" +
                      "**Modifiable fields:** nombre, descripcion, requisitos, plazoEstimadoDias, estado.\n\n" +
                      "**Successful response (200):** object with the updated data.\n\n" +
                      "**Error 404:** if no procedure type exists with that ID."
    )
    public TipoTramite update(
            @Parameter(description = "Numeric identifier of the procedure type to update.")
            @PathVariable Integer id,
            @RequestBody TipoTramite entrada) {

        // Validate that the identifier is valid.
        if (id == null || id < 1) {
            throw new IllegalArgumentException(
                    "The procedure type ID must be greater than zero.");
        }

        return service.update(id, entrada);
    }

    // Compatibility alias: hidden from Swagger.
    @Hidden
    @PutMapping("/update/{id}")
    public TipoTramite updateAlias(
            @PathVariable Integer id,
            @RequestBody TipoTramite entrada) {
        return service.update(id, entrada);
    }

    // Activate or deactivate a procedure type.
    @PatchMapping("/{id}/estado")
    @Operation(
        summary = "Activate or deactivate procedure type",
        description = "Changes the status of a procedure type.\n\n" +
                      "**Expected body:** `{ \"estado\": true }` to activate or `{ \"estado\": false }` to deactivate.\n\n" +
                      "An inactive procedure type will not be available for new FUT requests.\n\n" +
                      "**Successful response (200):** object with the updated status.\n\n" +
                      "**Error 404:** if no procedure type exists with that ID."
    )
    public TipoTramite changeState(
            @Parameter(description = "Numeric identifier of the procedure type.")
            @PathVariable Integer id,
            @RequestBody Map<String, Boolean> body) {
        return service.changeState(id, body.get("estado"));
    }

    // =========================================================================
    // IMPORTACIÓN MASIVA — EXCEL
    // POST /v1/api/procedure-type/import/excel
    // =========================================================================
    @PostMapping(value = "/import/excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Import procedure types from Excel (.xlsx)",
        description = """
            Loads procedure types in bulk from an Excel file (.xlsx).

            **Expected column order (row 1 = header):**
            codigo | nombre | categoria | descripcion | requisitos
            | plazoEstimadoDias | requiereDocumentos | vigentDesde

            **Rules:**
            - **nombre** is required (max 100 characters, must be unique).
            - **plazoEstimadoDias:** integer (number of calendar days); leave empty if unknown.
            - **requiereDocumentos:** true/false, si/no, 1/0.
            - **vigentDesde:** date in yyyy-MM-dd format.
            - Duplicate names (case-insensitive) are rejected per row.
            - Valid rows are saved; invalid rows are reported without stopping the process.

            **Response:** summary with totalProcessed, totalImported, totalRejected and error details.
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = "Import completed",
                content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Invalid or unreadable file",
                content = @Content)
        }
    )
    public ResponseEntity<ImportResult> importFromExcel(
            @Parameter(description = "Excel file (.xlsx) with procedure type data.")
            @RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new ImportResult(0, 0, 0,
                                List.of(new ImportResult.RowError(0, "No file received."))));
            }
            return ResponseEntity.ok(importExportService.importFromExcel(file));
        } catch (IOException ex) {
            return ResponseEntity.badRequest().body(
                    new ImportResult(0, 0, 1,
                            List.of(new ImportResult.RowError(0, "Could not read file: " + ex.getMessage()))));
        }
    }

    // =========================================================================
    // IMPORTACIÓN MASIVA — CSV
    // POST /v1/api/procedure-type/import/csv
    // =========================================================================
    @PostMapping(value = "/import/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Import procedure types from CSV (.csv)",
        description = """
            Loads procedure types in bulk from a CSV file (.csv).

            **Column order (row 1 = header):**
            codigo | nombre | categoria | descripcion | requisitos
            | plazoEstimadoDias | requiereDocumentos | vigentDesde

            **Delimiter:** comma (,) or semicolon (;) — auto-detected.

            **Encoding:** UTF-8 recommended.
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = "Import completed",
                content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Invalid or unreadable file",
                content = @Content)
        }
    )
    public ResponseEntity<ImportResult> importFromCsv(
            @Parameter(description = "CSV file (.csv) with procedure type data.")
            @RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new ImportResult(0, 0, 0,
                                List.of(new ImportResult.RowError(0, "No file received."))));
            }
            return ResponseEntity.ok(importExportService.importFromCsv(file));
        } catch (IOException ex) {
            return ResponseEntity.badRequest().body(
                    new ImportResult(0, 0, 1,
                            List.of(new ImportResult.RowError(0, "Could not read file: " + ex.getMessage()))));
        }
    }

    // =========================================================================
    // EXPORTACIÓN — EXCEL
    // GET /v1/api/procedure-type/export/excel
    // =========================================================================
    @GetMapping("/export/excel")
    @Operation(
        summary = "Export procedure types to Excel (.xlsx)",
        description = """
            Downloads a Microsoft Excel file (.xlsx) with all procedure types stored in the system.

            **Columns included:** ID, Código, Nombre, Categoría, Descripción, Requisitos,
            Plazo (días), Requiere Docs, Vigente Desde, Estado, Fecha Registro,
            Fecha Actualización, Fecha Eliminación, Fecha Restauración.
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = "Excel file ready to download",
                content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
            @ApiResponse(responseCode = "500", description = "Error generating the file",
                content = @Content)
        }
    )
    public ResponseEntity<byte[]> exportToExcel() {
        try {
            byte[] bytes = importExportService.exportToExcel();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"tipos-tramite.xlsx\"")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes);
        } catch (IOException ex) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // =========================================================================
    // EXPORTACIÓN — PDF
    // GET /v1/api/procedure-type/export/pdf
    // =========================================================================
    @GetMapping("/export/pdf")
    @Operation(
        summary = "Export procedure types to PDF",
        description = """
            Downloads a PDF report with all procedure types in the system.

            **Report includes:** title, generation date, total records, and a table with:
            ID, Código, Nombre, Categoría, Descripción, Requisitos, Plazo, Requiere Docs,
            Vigente Desde, Estado, Fecha Registro.

            Landscape A4, alternating row colours.
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = "PDF ready to download",
                content = @Content(mediaType = "application/pdf")),
            @ApiResponse(responseCode = "500", description = "Error generating the file",
                content = @Content)
        }
    )
    public ResponseEntity<byte[]> exportToPdf() {
        try {
            byte[] bytes = importExportService.exportToPdf();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"tipos-tramite.pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(bytes);
        } catch (IOException ex) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // =========================================================================
    // PLANTILLA — EXCEL
    // GET /v1/api/procedure-type/template/excel
    // =========================================================================
    @GetMapping("/template/excel")
    @Operation(
        summary = "Download procedure type import template (Excel)",
        description = """
            Downloads an Excel template (.xlsx) ready to fill and upload to the import endpoint.

            **Column order:**
            codigo | nombre | categoria | descripcion | requisitos
            | plazoEstimadoDias | requiereDocumentos | vigentDesde

            Row 1 contains the required headers. Row 2 shows example hints (delete before importing).

            **Field rules:**
            - **nombre** is required (max 100 characters, must be unique).
            - **plazoEstimadoDias:** integer or leave empty.
            - **requiereDocumentos:** true/false, si/no, 1/0.
            - **vigentDesde:** format yyyy-MM-dd (e.g. 2024-01-15).

            Use this file with: **POST /v1/api/procedure-type/import/excel**
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = "Template ready to download",
                content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
            @ApiResponse(responseCode = "500", description = "Error generating template",
                content = @Content)
        }
    )
    public ResponseEntity<byte[]> downloadTemplateExcel() {
        try {
            byte[] bytes = importExportService.generateTemplateExcel();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"plantilla-tipos-tramite.xlsx\"")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes);
        } catch (IOException ex) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // =========================================================================
    // PLANTILLA — CSV
    // GET /v1/api/procedure-type/template/csv
    // =========================================================================
    @GetMapping("/template/csv")
    @Operation(
        summary = "Download procedure type import template (CSV)",
        description = """
            Downloads a CSV template (.csv) ready to fill and upload to the import endpoint.

            **Column order:**
            codigo,nombre,categoria,descripcion,requisitos,plazoEstimadoDias,requiereDocumentos,vigentDesde

            The file contains only the header row. Add data rows below it.

            **Field rules:**
            - **nombre** is required.
            - **plazoEstimadoDias:** integer or leave empty.
            - **requiereDocumentos:** true/false, si/no, 1/0.
            - **vigentDesde:** yyyy-MM-dd format.

            Use this file with: **POST /v1/api/procedure-type/import/csv**
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = "Template ready to download",
                content = @Content(mediaType = "text/csv")),
            @ApiResponse(responseCode = "500", description = "Error generating template",
                content = @Content)
        }
    )
    public ResponseEntity<byte[]> downloadTemplateCsv() {
        byte[] bytes = importExportService.generateTemplateCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"plantilla-tipos-tramite.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(bytes);
    }
}
