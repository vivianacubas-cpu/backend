package pe.edu.vallegrande.app.rest;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.vallegrande.app.dto.GestionTramiteFormRequest;
import pe.edu.vallegrande.app.dto.GestionTramiteResponse;
import pe.edu.vallegrande.app.repository.AreaRepository;
import pe.edu.vallegrande.app.service.GestionTramiteFutService;

import java.util.List;

/**
 * REST controller for FUT procedure management operations.
 * Primary path: /v1/api/management
 * Legacy alias:  /v1/api/gestion-tramite (kept for Angular frontend compatibility)
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/v1/api/management")
@Tag(name = "FUT Processing Management API",
     description = "Transactional Table — Internal management of FUT procedure processing")
public class GestionTramiteFutRest {

    private static final String EXAMPLE_REQUEST = """
            {
              "applicant": "Mariana Quispe Ramirez",
              "responsibleFirstNames": "Viviana",
              "responsiblePaternalLastName": "Cubas",
              "responsibleMaternalLastName": "Rufasto",
              "responsibleArea": "ADMINISTRACIÓN",
              "managementType": "EVALUACION",
              "priority": "URGENTE",
              "managementStatus": "PENDIENTE",
              "receptionDate": "2026-10-29",
              "deadlineDate": "2026-10-30",
              "resolutionDate": "2026-10-31",
              "decision": "UNDER EVALUATION",
              "decisionBasis": "The request has been received and is currently under evaluation.",
              "observations": "The submitted documentation needs to be reviewed and validated."
            }""";

    private static final String EXAMPLE_RESPONSE = """
            {
              "managementId": 1,
              "applicantId": 1,
              "documentNumber": "70812345",
              "paternalLastName": "Quispe",
              "maternalLastName": "Ramirez",
              "firstNames": "Mariana",
              "responsible": "Viviana Cubas Rufasto",
              "responsibleFirstNames": "Viviana",
              "responsiblePaternalLastName": "Cubas",
              "responsibleMaternalLastName": "Rufasto",
              "responsibleAreaId": 1,
              "responsibleArea": "DIRECCION",
              "managementType": "Approval",
              "priority": "Urgent",
              "managementStatus": "EN_PROCESO",
              "recordStatus": "ACTIVO",
              "receptionDate": "2026-10-28",
              "deadlineDate": "2026-10-29",
              "resolutionDate": "2026-10-31",
              "decision": "APROBADO",
              "decisionBasis": "The request meets the requirements.",
              "observations": "No observations.",
              "createdAt": "2026-10-28T18:22:15",
              "updatedAt": null,
              "deletedAt": null,
              "restoredAt": null
            }""";

    private final GestionTramiteFutService service;
    private final AreaRepository areaRepository;

    public GestionTramiteFutRest(GestionTramiteFutService service, AreaRepository areaRepository) {
        this.service = service;
        this.areaRepository = areaRepository;
    }

    // ── REGISTER ─────────────────────────────────────────────────────────────

    @PostMapping
    @Operation(
        summary = "Register FUT procedure management",
        description = """
            Creates a new management record associated with a registered applicant.

            **solicitante:** full name (e.g. "Mariana Quispe Ramirez") or use **idSolicitante** (number).

            **areaResponsable:** ADMINISTRACIÓN, DIRECCIÓN, SECRETARÍA — or use **idAreaResponsable** (number).

            **tipoGestion:** Reception, Evaluation, Derivation, Approval, Rejection, Correction
            (or: RECEPCION, EVALUACION, DERIVACION, APROBACION, RECHAZO, SUBSANACION).

            **prioridad:** Normal, Urgent, Very Urgent (or: NORMAL, URGENTE, MUY_URGENTE).

            **estadoGestion:** Pending, In Progress, Observed, Approved, Rejected
            (or: PENDIENTE, EN_PROCESO, OBSERVADO, APROBADO, RECHAZADO).

            **Auto-generated fields:** idGestion, fechaRegistro, fechaActualizacion, fechaEliminacion, fechaRestauracion.
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = " ", content = @Content),
            @ApiResponse(responseCode = "400", description = "Invalid data or missing required field", content = @Content),
            @ApiResponse(responseCode = "404", description = "Applicant or area does not exist", content = @Content)
        }
    )
    public ResponseEntity<GestionTramiteResponse> save(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Data for the new management record.",
                required = true,
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = GestionTramiteFormRequest.class),
                    examples = @ExampleObject(name = "Registration example", value = EXAMPLE_REQUEST)))
            @Valid @RequestBody GestionTramiteFormRequest request) {
        return ResponseEntity.ok(service.save(request));
    }

    // ── LIST ACTIVE RECORDS ───────────────────────────────────────────────────

    @GetMapping
    @Operation(
        summary = "List active management records",
        description = """
            Returns all active management records.
            Optional parameter **estadoGestion**: PENDIENTE, EN_PROCESO, OBSERVADO, APROBADO, RECHAZADO.
            """,
        responses = @ApiResponse(responseCode = "200", description = "List of active management records",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = GestionTramiteResponse.class),
                examples = @ExampleObject(value = "[]")))
    )
    public ResponseEntity<List<GestionTramiteResponse>> findAll(
            @Parameter(
                description = "Filter by management status. Leave empty to retrieve all active records.",
                schema = @Schema(
                    type = "string",
                    allowableValues = {"PENDIENTE", "EN_PROCESO", "OBSERVADO", "APROBADO", "RECHAZADO"}
                )
            )
            @RequestParam(required = false) String estadoGestion) {
        return ResponseEntity.ok(service.findActivas(estadoGestion));
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    @PutMapping("/update/{id}")
    @Operation(
        summary = "Update FUT procedure management",
        description = """
            Modifies the data of an existing management record.

            **How to edit in 2 steps:**

            **Step 1 — Load current data:**
            Leave the Request body as is (with the example) and click **Execute**.
            The response will return the actual data of the management record with that ID.

            **Step 2 — Save changes:**
            Copy the JSON from the response, paste it in the Request body,
            modify the fields you need, and click **Execute** again.

            **Ignored fields:** idGestion, fechaRegistro, fechaActualizacion, fechaEliminacion, fechaRestauracion.
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = "Management record data (current or updated)",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = GestionTramiteResponse.class),
                    examples = @ExampleObject(value = EXAMPLE_RESPONSE))),
            @ApiResponse(responseCode = "400", description = "Invalid data or missing required field", content = @Content),
            @ApiResponse(responseCode = "404", description = "Management record does not exist", content = @Content)
        }
    )
    public ResponseEntity<GestionTramiteResponse> update(
            @Parameter(description = "Numeric ID of the management record to update.", example = "1")
            @PathVariable Integer id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Updated data. The first time, leave the example and execute to see current data.",
                required = true,
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = GestionTramiteFormRequest.class),
                    examples = @ExampleObject(name = "Update example", value = EXAMPLE_REQUEST)))
            @RequestBody GestionTramiteFormRequest request) {

        // Validate that the identifier is valid.
        if (id == null || id < 1) {
            throw new IllegalArgumentException("The management record ID must be greater than zero.");
        }

        boolean bodyIsPlaceholder = request == null
                || request.getSolicitante() == null
                || request.getSolicitante().isBlank()
                || request.getSolicitante().equals("Mariana Quispe Ramirez");

        if (bodyIsPlaceholder) {
            return ResponseEntity.ok(service.findById(id));
        }

        return ResponseEntity.ok(service.update(id, request));
    }

    // ── RESTORE ───────────────────────────────────────────────────────────────

    @PatchMapping("/restore/{id}")
    @Operation(
        summary = "Restore deleted management record",
        description = """
            Changes the status of the management record from inactive (I) to active (A).
            The operation is idempotent.

            **Error 404:** if no management record exists with that ID.
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = "Management record restored",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = GestionTramiteResponse.class),
                    examples = @ExampleObject(value = EXAMPLE_RESPONSE))),
            @ApiResponse(responseCode = "404", description = "Management record does not exist", content = @Content)
        }
    )
    public ResponseEntity<GestionTramiteResponse> restore(
            @Parameter(description = "Numeric ID of the management record to restore.", example = "1")
            @PathVariable Integer id) {
        return ResponseEntity.ok(service.restore(id));
    }

    // ── LOGICAL DELETION ──────────────────────────────────────────────────────

    @PatchMapping("/delete/{id}")
    @Operation(
        summary = "Delete management record (logical deletion)",
        description = """
            Changes the status of the management record to inactive (I).
            The operation is idempotent.

            **Error 404:** if no management record exists with that ID.
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = "Management record deleted",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = GestionTramiteResponse.class),
                    examples = @ExampleObject(value = EXAMPLE_RESPONSE))),
            @ApiResponse(responseCode = "404", description = "Management record does not exist", content = @Content)
        }
    )
    public ResponseEntity<GestionTramiteResponse> delete(
            @Parameter(description = "Numeric ID of the management record to delete.", example = "1")
            @PathVariable Integer id) {
        return ResponseEntity.ok(service.delete(id));
    }

    // ── ENDPOINTS REQUIRED BY THE FRONTEND (hidden from Swagger) ─────────────

    @Hidden
    @GetMapping("/eliminados")
    public ResponseEntity<List<GestionTramiteResponse>> findDeleted() {
        return ResponseEntity.ok(service.findEliminadas());
    }

    @Hidden
    @GetMapping("/{id}")
    public ResponseEntity<GestionTramiteResponse> findById(
            @PathVariable Integer id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @Hidden
    @GetMapping("/catalogo/areas")
    public ResponseEntity<List<java.util.Map<String, Object>>> catalogoAreas() {
        List<java.util.Map<String, Object>> result = areaRepository.findAll().stream()
                .map(a -> {
                    java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("areaId", a.getIdArea());
                    m.put("name", a.getNombre());
                    return m;
                })
                .toList();
        return ResponseEntity.ok(result);
    }
}
