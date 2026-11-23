package pe.edu.vallegrande.app.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.vallegrande.app.dto.ImportResult;
import pe.edu.vallegrande.app.dto.SolicitanteRequest;
import pe.edu.vallegrande.app.dto.SolicitanteResponse;
import pe.edu.vallegrande.app.model.Solicitante;
import pe.edu.vallegrande.app.service.SolicitanteService;
import pe.edu.vallegrande.app.service.impl.SolicitanteImportExportServiceImpl;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * REST controller for applicant management.
 * Primary path: /v1/api/applicant
 * Legacy alias:  /v1/api/solicitante (kept for Angular frontend compatibility)
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/v1/api/applicant")
@Tag(name = "Applicant API", description = "Master Table — Management of persons who submit FUT procedure requests")
public class SolicitanteRest {

    // Example input body: generic illustrative values, no real personal data.
    // Status and dates are included to show the complete structure; the server ignores them.
    private static final String EXAMPLE_REQUEST = """
            {
              "documentType": "DNI",
              "documentNumber": "00000000",
              "paternalLastName": "Last Name",
              "maternalLastName": "Mother Last Name",
              "firstNames": "First Names",
              "location": "District, Province, Department",
              "address": "Address",
              "phoneNumber": "000000000",
              "email": "email@example.com",
              "status": "",
              "createdAt": null,
              "updatedAt": null,
              "deletedAt": null,
              "restoredAt": null
            }""";

    // Example presentation response: fields in the specified order, empty values.
    private static final String EXAMPLE_APPLICANT = """
            {
              "documentType": "",
              "documentNumber": "",
              "paternalLastName": "",
              "maternalLastName": "",
              "firstNames": "",
              "location": "",
              "address": "",
              "phoneNumber": "",
              "email": "",
              "status": "",
              "createdAt": null,
              "updatedAt": null,
              "deletedAt": null,
              "restoredAt": null
            }""";

    // Endpoints returning lists show an empty array as the example.
    private static final String EXAMPLE_APPLICANT_LIST = "[]";

    private final SolicitanteService solicitanteService;
    private final SolicitanteImportExportServiceImpl importExportService;

    public SolicitanteRest(SolicitanteService solicitanteService,
                           SolicitanteImportExportServiceImpl importExportService) {
        this.solicitanteService  = solicitanteService;
        this.importExportService = importExportService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /v1/api/applicant/update/{id} — Update applicant data
    // ─────────────────────────────────────────────────────────────────────────
    @PutMapping("/update/{id}")
    @Operation(
        summary = "Update applicant data",
        description = """
            Modifies the data of an existing applicant.

            **How to edit in 2 steps:**

            **Step 1 — Load current data:**
            Leave the Request body as is (with the example) and click **Execute**.
            The response will return the actual data of the applicant with that ID.
            If the applicant was **Inactive**, they are automatically restored to **Active**.

            **Step 2 — Save changes:**
            Copy the JSON from the response, paste it in the Request body,
            modify the fields you need, and click **Execute** again.

            **Ignored fields:** estado, fechaRegistro, fechaActualizacion, fechaEliminacion, fechaRestauracion.

            **Error 404:** if no applicant exists with that ID.
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = " ",
                content = @Content),
            @ApiResponse(responseCode = "404", description = " ",
                content = @Content)
        }
    )
    public ResponseEntity<SolicitanteResponse> update(
            @Parameter(description = "Numeric identifier of the applicant to update.", example = "1")
            @PathVariable Long id,
            @RequestBody(
                description = "Updated data. The first time, leave the example and execute to see current data.",
                required = true,
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = SolicitanteRequest.class),
                    examples = @ExampleObject(
                        name = "Update example",
                        value = EXAMPLE_REQUEST)))
            @org.springframework.web.bind.annotation.RequestBody SolicitanteRequest request) {

        // Validate the applicant identifier.
        if (id == null || id < 1) {
            throw new IllegalArgumentException("The applicant ID must be greater than zero.");
        }

        // If the body is empty or contains the placeholder, return the current data for editing.
        boolean bodyIsPlaceholder = request == null
                || request.getNumeroDocumento() == null
                || request.getNumeroDocumento().isBlank()
                || request.getNumeroDocumento().equals("00000000");

        if (bodyIsPlaceholder) {
            SolicitanteResponse current = solicitanteService.findById(id)
                    .orElse(null);

            if (current == null) {
                return ResponseEntity.notFound().build();
            }

            // If inactive, automatically restore and return active data.
            if ("Inactivo".equalsIgnoreCase(current.status())) {
                SolicitanteResponse restored = solicitanteService.restore(id);
                return ResponseEntity.ok(restored);
            }

            return ResponseEntity.ok(current);
        }

        return ResponseEntity.ok(solicitanteService.updateFromRequest(id, request));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /v1/api/applicant/save — Register new applicant
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/save")
    @Operation(
        summary = "Register new applicant",
        description = """
            Registers a new person in the applicant master.

            **Required fields:** idTipoDocumento, numeroDocumento, nombres, apellidoPaterno, apellidoMaterno.

            **Optional fields:** idUbigeo, departamento, provincia, distrito, telefono, correo, direccion.

            **idSolicitante:** send 0 or null so the system assigns the identifier automatically.

            **idTipoDocumento:** 1 = DNI, 2 = CNE. See /v1/api/solicitante/tipos-documento for the full list.

            **idUbigeo:** ubigeo identifier. See /v1/api/solicitante/ubigeos.
            If omitted, the system will use the generic ubigeo and take departamento/provincia/distrito from free text.

            **Successful response (200):** object with the 14 presentation properties.

            **Error 400:** if a required field is missing or the data format is incorrect.

            **Error 409:** if the document number already belongs to another applicant.
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = "Applicant registered",
                content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = EXAMPLE_APPLICANT))),
            @ApiResponse(responseCode = "400", description = "Invalid data or missing required field",
                content = @Content),
            @ApiResponse(responseCode = "409", description = "Duplicate document number",
                content = @Content)
        }
    )
    public ResponseEntity<SolicitanteResponse> save(@Valid
            @RequestBody(
                description = "New applicant data. Status and date fields are assigned by the server.",
                required = true,
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = SolicitanteRequest.class),
                    examples = @ExampleObject(
                        name = "Registration example",
                        value = EXAMPLE_REQUEST)))
            @org.springframework.web.bind.annotation.RequestBody SolicitanteRequest request) {
        return ResponseEntity.ok(solicitanteService.saveFromRequest(request));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PATCH /v1/api/applicant/restore/{id} — Restore applicant
    // ─────────────────────────────────────────────────────────────────────────
    @PatchMapping("/restore/{id}")
    @Operation(
        summary = "Restore applicant",
        description = """
            Changes the applicant's status from **Inactive** to **Active**.

            **Successful response (200):** object with status "Activo" and fechaRestauracion recorded.

            **Error 404:** if no applicant exists with that ID.
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = "Applicant restored",
                content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = EXAMPLE_APPLICANT))),
            @ApiResponse(responseCode = "404", description = "Applicant not found",
                content = @Content)
        }
    )
    public ResponseEntity<SolicitanteResponse> restore(
            @Parameter(description = "Numeric identifier of the applicant to restore.", example = "1")
            @PathVariable Long id) {
        SolicitanteResponse result = solicitanteService.restore(id);
        return ResponseEntity.ok(result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PATCH /v1/api/solicitante/delete/{id} — Deactivate (logical deletion)
    // ─────────────────────────────────────────────────────────────────────────
    @PatchMapping("/delete/{id}")
    @Operation(
        summary = "Deactivate applicant (logical deletion)",
        description = """
            Changes the applicant's status from **Active** to **Inactive**.

            The record is preserved in the database and can be restored later.

            **Successful response (200):** object with status "Inactive" and fechaEliminacion recorded.

            **Error 404:** if no applicant exists with that ID.
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = "Applicant deactivated",
                content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = EXAMPLE_APPLICANT))),
            @ApiResponse(responseCode = "404", description = "Applicant not found",
                content = @Content)
        }
    )
    public ResponseEntity<SolicitanteResponse> delete(
            @Parameter(description = "Numeric identifier of the applicant to deactivate.", example = "1")
            @PathVariable Long id) {
        SolicitanteResponse result = solicitanteService.delete(id);
        return ResponseEntity.ok(result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /v1/api/solicitante — Query all applicants
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping
    @Operation(
        summary = "Query all applicants",
        description = """
            Returns the complete list of applicants registered in the system (active and inactive).

            Each record includes exactly the presentation properties in this order:
            tipoDocumento, numeroDocumento, apellidoPaterno, apellidoMaterno, nombres,
            distrito, provincia, departamento, direccion, telefono, correo,
            estado (Active/Inactive), fechaRegistro, fechaActualizacion,
            fechaEliminacion, fechaRestauracion.

            **Successful response (200):** array with all records.
            **No records:** empty array [].
            """,
        responses = @ApiResponse(
            responseCode = "200", description = "List of applicants",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = EXAMPLE_APPLICANT_LIST)))
    )
    public ResponseEntity<List<SolicitanteResponse>> findAll() {
        return ResponseEntity.ok(solicitanteService.findAll());
    }

    // ─── Endpoints required by the frontend (hidden from Swagger with @Hidden) ───

    @Hidden
    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<SolicitanteResponse>> findByEstado(@PathVariable String estado) {
        return ResponseEntity.ok(solicitanteService.findByEstado(estado));
    }

    @Hidden
    @GetMapping("/{id}")
    public ResponseEntity<SolicitanteResponse> findById(@PathVariable Long id) {
        return solicitanteService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Hidden
    @GetMapping("/documento/{numeroDocumento}")
    public ResponseEntity<SolicitanteResponse> findByNumeroDocumento(@PathVariable String numeroDocumento) {
        return solicitanteService.findByNumeroDocumento(numeroDocumento)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Hidden
    @GetMapping("/tipos-documento")
    public ResponseEntity<java.util.List<java.util.Map<String, Object>>> listTiposDocumento() {
        return ResponseEntity.ok(solicitanteService.listTiposDocumento());
    }

    @Hidden
    @GetMapping("/ubigeos")
    public ResponseEntity<java.util.List<java.util.Map<String, Object>>> listUbigeos() {
        return ResponseEntity.ok(solicitanteService.listUbigeos());
    }

    // =========================================================================
    // IMPORTACIÓN MASIVA — EXCEL
    // POST /v1/api/applicant/import/excel
    // =========================================================================
    @PostMapping(value = "/import/excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Import applicants from Excel (.xlsx)",
        description = """
            Loads applicants in bulk from an Excel file (.xlsx).

            **Expected column order (row 1 = header):**
            tipoDocumento | numeroDocumento | nombres | apellidoPaterno | apellidoMaterno
            | telefono | correo | direccion | departamento | provincia | distrito

            **tipoDocumento:** DNI or CNE.

            **Rules:**
            - Required columns: tipoDocumento, numeroDocumento, nombres, apellidoPaterno, apellidoMaterno.
            - Duplicate document numbers are rejected with an error per row.
            - Valid rows are saved; invalid rows are reported without stopping the process.

            **Response:** summary with totalProcessed, totalImported, totalRejected and per-row error details.
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = "Import completed (check totals for errors)",
                content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Invalid or unreadable file",
                content = @Content)
        }
    )
    public ResponseEntity<ImportResult> importFromExcel(
            @Parameter(description = "Excel file (.xlsx) with applicant data.")
            @org.springframework.web.bind.annotation.RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new ImportResult(0, 0, 0,
                                List.of(new ImportResult.RowError(0, "No file received."))));
            }
            ImportResult result = importExportService.importFromExcel(file);
            return ResponseEntity.ok(result);
        } catch (IOException ex) {
            return ResponseEntity.badRequest().body(
                    new ImportResult(0, 0, 1,
                            List.of(new ImportResult.RowError(0, "Could not read file: " + ex.getMessage()))));
        }
    }

    // =========================================================================
    // IMPORTACIÓN MASIVA — CSV
    // POST /v1/api/applicant/import/csv
    // =========================================================================
    @PostMapping(value = "/import/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Import applicants from CSV (.csv)",
        description = """
            Loads applicants in bulk from a CSV file (.csv).

            **Column order (row 1 = header):**
            tipoDocumento | numeroDocumento | nombres | apellidoPaterno | apellidoMaterno
            | telefono | correo | direccion | departamento | provincia | distrito

            **Delimiter:** comma (,) or semicolon (;) — auto-detected.

            **Encoding:** UTF-8 recommended.

            **Response:** summary with totalProcessed, totalImported, totalRejected and error details.
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = "Import completed",
                content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Invalid or unreadable file",
                content = @Content)
        }
    )
    public ResponseEntity<ImportResult> importFromCsv(
            @Parameter(description = "CSV file (.csv) with applicant data.")
            @org.springframework.web.bind.annotation.RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new ImportResult(0, 0, 0,
                                List.of(new ImportResult.RowError(0, "No file received."))));
            }
            ImportResult result = importExportService.importFromCsv(file);
            return ResponseEntity.ok(result);
        } catch (IOException ex) {
            return ResponseEntity.badRequest().body(
                    new ImportResult(0, 0, 1,
                            List.of(new ImportResult.RowError(0, "Could not read file: " + ex.getMessage()))));
        }
    }

    // =========================================================================
    // EXPORTACIÓN — EXCEL
    // GET /v1/api/applicant/export/excel
    // =========================================================================
    @GetMapping("/export/excel")
    @Operation(
        summary = "Export applicants to Excel (.xlsx)",
        description = """
            Downloads a Microsoft Excel file (.xlsx) with all applicants registered in the system.

            **Columns included:** ID, Tipo Documento, Nº Documento, Apellido Paterno, Apellido Materno,
            Nombres, Distrito, Provincia, Departamento, Dirección, Teléfono, Correo, Estado,
            Fecha Registro, Fecha Actualización, Fecha Eliminación, Fecha Restauración.

            The file uses professional formatting with bold headers and alternating row colours.
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = "Excel file generated and ready to download",
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
                            "attachment; filename=\"solicitantes.xlsx\"")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes);
        } catch (IOException ex) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // =========================================================================
    // EXPORTACIÓN — PDF
    // GET /v1/api/applicant/export/pdf
    // =========================================================================
    @GetMapping("/export/pdf")
    @Operation(
        summary = "Export applicants to PDF",
        description = """
            Downloads a PDF file with a professional report of all applicants registered in the system.

            **Report includes:** title, generation date, total records, and a table with the main fields
            of each applicant (ID, Tipo Documento, Nº Documento, Apellidos, Nombres, Location, Dirección,
            Teléfono, Correo, Estado, Fecha Registro).

            The PDF uses landscape A4 orientation with alternating row colours for readability.
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = "PDF file generated and ready to download",
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
                            "attachment; filename=\"solicitantes.pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(bytes);
        } catch (IOException ex) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // =========================================================================
    // PLANTILLA — EXCEL
    // GET /v1/api/applicant/template/excel
    // =========================================================================
    @GetMapping("/template/excel")
    @Operation(
        summary = "Download applicant import template (Excel)",
        description = """
            Downloads an Excel template (.xlsx) ready to fill and upload to the import endpoint.

            **Column order:**
            tipoDocumento | numeroDocumento | nombres | apellidoPaterno | apellidoMaterno
            | telefono | correo | direccion | departamento | provincia | distrito

            Row 1 contains the required headers. Row 2 shows example hints (delete before importing).

            Use this file with: **POST /v1/api/applicant/import/excel**
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
                            "attachment; filename=\"plantilla-solicitantes.xlsx\"")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes);
        } catch (IOException ex) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // =========================================================================
    // PLANTILLA — CSV
    // GET /v1/api/applicant/template/csv
    // =========================================================================
    @GetMapping("/template/csv")
    @Operation(
        summary = "Download applicant import template (CSV)",
        description = """
            Downloads a CSV template (.csv) ready to fill and upload to the import endpoint.

            **Column order:**
            tipoDocumento,numeroDocumento,nombres,apellidoPaterno,apellidoMaterno,
            telefono,correo,direccion,departamento,provincia,distrito

            The file contains only the header row. Add data rows below it, separated by commas or semicolons.

            Use this file with: **POST /v1/api/applicant/import/csv**
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
                        "attachment; filename=\"plantilla-solicitantes.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(bytes);
    }
}
