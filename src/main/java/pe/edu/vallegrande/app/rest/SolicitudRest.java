package pe.edu.vallegrande.app.rest;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.vallegrande.app.dto.SolicitudRequest;
import pe.edu.vallegrande.app.service.SolicitudService;

import java.util.List;
import java.util.Map;

/**
 * REST controller responsible for managing FUT (Single Procedure Form) requests.
 *
 * Allows registering, querying, editing, updating statuses,
 * deleting, and restoring requests.
 *
 * Primary path: /v1/api/request
 * Legacy alias:  /v1/api/solicitud (kept for Angular frontend compatibility)
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/v1/api/request")
@Tag(
    name = "FUT Request API",
    description = "Transactional Table — Registration and querying of Single Procedure Form (FUT) requests"
)
public class SolicitudRest {

    private final SolicitudService service;

    /**
     * Constructor that injects the request service.
     *
     * @param service service responsible for business logic
     */
    public SolicitudRest(SolicitudService service) {
        this.service = service;
    }

    /**
     * Registers a new FUT request.
     *
     * @param request data submitted to register the request
     * @return information about the registered request
     */
    @PostMapping
    @Operation(
        summary = "Register FUT request",
        description = "Registers a new FUT request in the system.\n\n" +
                      "**Required fields:** solicitante (procedure type), fullName, dni, cellphone, address, district, province, basis, declaredDate.\n\n" +
                      "**solicitante:** name of the procedure to be carried out (e.g. CERTIFICADO DE ESTUDIOS).\n\n" +
                      "**fullName:** first names and last names in format: firstName paternalLastName maternalLastName.\n\n" +
                      "**dni:** document number (8 digits for DNI, 9-12 for CNE).\n\n" +
                      "**tipoDocumento:** DNI (default) or CNE.\n\n" +
                      "**declaredDate:** date in YYYY-MM-DD format.\n\n" +
                      "**basis:** free text with the basis or reason for the request.\n\n" +
                      "The system automatically generates the tracking code and creates the first history entry.\n\n" +
                      "**Successful response (200):** object with the registered request, including trackingCode.\n\n" +
                      "**Error 400:** if a required field is missing or the data format is incorrect.\n\n" +
                      "**Error 500:** if the document type does not exist in the database."
    )
    public ResponseEntity<Map<String, Object>> create(
            @Valid @RequestBody SolicitudRequest request) {
        return ResponseEntity.ok(service.create(request));
    }

    /**
     * Registers a request using alternative paths
     * to maintain compatibility with older clients.
     *
     * @param request request data
     * @return information about the registered request
     */
    @Hidden
    @PostMapping({"/", "/save"})
    public ResponseEntity<Map<String, Object>> createAlias(
            @RequestBody SolicitudRequest request) {
        return ResponseEntity.ok(service.create(request));
    }

    /**
     * Retrieves the active requests in the system.
     *
     * @param estado optional filter by request status
     * @return list of registered requests
     */
    @GetMapping
    @Operation(
        summary = "Query registered requests",
        description = "Returns active (non-deleted) FUT requests.\n\n" +
                      "Optionally filter by status using the **estado** parameter:\n\n" +
                      "- **RECIBIDO** = Request received and pending review\n" +
                      "- **EN_REVISION** = Under review by the secretary\n" +
                      "- **OBSERVADO** = Has observations requiring applicant action\n" +
                      "- **ATENDIDO** = Procedure resolved and delivered\n" +
                      "- **RECHAZADO** = Not approved; request rejected\n\n" +
                      "If no status is specified, returns all active requests.\n\n" +
                      "**Successful response (200):** array of requests with their main data."
    )
    public List<Map<String, Object>> list(
            @Parameter(description = "Filter by request status. Leave empty to retrieve all.")
            @Schema(allowableValues = {
                "RECIBIDO", "EN_REVISION", "OBSERVADO", "ATENDIDO", "RECHAZADO"
            })
            @RequestParam(required = false) String estado) {
        return service.list(estado);
    }

    /**
     * Queries the detail of a request by its identifier.
     *
     * @param id request identifier
     * @return full information about the request and its tracking history
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "Query request detail",
        description = "Returns the full detail of a FUT request, including all its data and status change history.\n\n" +
                      "**Successful response (200):** complete object with request data and tracking history.\n\n" +
                      "**Error 404:** if no request exists with that ID."
    )
    public Map<String, Object> detail(
            @Parameter(description = "Numeric identifier of the request (id_solicitud in the database).")
            @PathVariable Long id) {
        return service.detail(id);
    }

    /**
     * Queries a request using its tracking code.
     *
     * @param codigo unique tracking code of the request
     * @return status and information about the request
     */
    @GetMapping("/seguimiento/{codigo}")
    @Operation(
        summary = "Query request by tracking code",
        description = "Allows the citizen to check the current status of their request using the code received when registering.\n\n" +
                      "The code has the format FUT-YYYY-XXXXXXXXXX (e.g. FUT-2025-ABCDE12345).\n\n" +
                      "**Successful response (200):** object with the code, current status, and registration date.\n\n" +
                      "**Error 404:** if no request exists with that code."
    )
    public Map<String, Object> track(
            @Parameter(description = "Request tracking code (e.g. FUT-2025-ABCDE12345).")
            @PathVariable String codigo) {
        return service.track(codigo);
    }

    /**
     * Queries requests that were logically deleted.
     *
     * @return list of deleted requests
     */
    @GetMapping("/eliminadas")
    @Operation(
        summary = "Query deleted requests",
        description = "Returns the list of logically deleted requests.\n\n" +
                      "These requests can be restored via the /restaurar endpoint.\n\n" +
                      "**Successful response (200):** array of deleted requests."
    )
    public List<Map<String, Object>> deleted() {
        return service.deleted();
    }

    /**
     * Updates the data of an existing request.
     *
     * @param id request identifier
     * @param request new request data
     * @return updated request information
     */
    @PutMapping("/{id}")
    @Operation(
        summary = "Edit request data",
        description = "Updates the data of an existing FUT request.\n\n" +
                      "**Important:** a deleted request cannot be edited; it must be restored first.\n\n" +
                      "**Successful response (200):** object with the updated data.\n\n" +
                      "**Error 400:** if the request is deleted or the data is invalid.\n\n" +
                      "**Error 404:** if no request exists with that ID."
    )
    public Map<String, Object> update(
            @Parameter(description = "Numeric identifier of the request to edit.")
            @PathVariable Long id,
            @Valid @RequestBody SolicitudRequest request) {
        return service.update(id, request);
    }

    /**
     * Updates the status of a request and records the change
     * in its tracking history.
     *
     * @param id request identifier
     * @param body new status and observation data
     * @return updated request information
     */
    @PatchMapping("/{id}/estado")
    @Operation(
        summary = "Change request status",
        description = "Updates the status of a FUT request and automatically records the change in the tracking history.\n\n" +
                      "**Expected body:**\n```json\n{\n  \"estado\": \"EN_REVISION\",\n  \"observacion\": \"Documents received, under verification.\"\n}\n```\n\n" +
                      "**Valid statuses:**\n" +
                      "- RECIBIDO — request received and pending\n" +
                      "- EN_REVISION — under review\n" +
                      "- OBSERVADO — has observations the applicant must address\n" +
                      "- ATENDIDO — procedure resolved\n" +
                      "- RECHAZADO — not approved\n\n" +
                      "**Successful response (200):** object with the updated status and full history.\n\n" +
                      "**Error 400:** if the submitted status is not one of the valid values.\n\n" +
                      "**Error 404:** if no request exists with that ID."
    )
    public Map<String, Object> updateStatus(
            @Parameter(description = "Numeric identifier of the request.")
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return service.updateStatus(
            id,
            body.get("estado"),
            body.get("observacion")
        );
    }

    /**
     * Restores a logically deleted request.
     *
     * @param id request identifier
     * @return restored request information
     */
    @PatchMapping("/{id}/restaurar")
    @Operation(
        summary = "Restore deleted request",
        description = "Recovers a logically deleted request.\n\n" +
                      "**Successful response (200):** object with the restored request.\n\n" +
                      "**Error 404:** if no request exists with that ID."
    )
    public Map<String, Object> restore(
            @Parameter(description = "Numeric identifier of the request to restore.")
            @PathVariable Long id) {
        return service.restore(id);
    }

    /**
     * Logically deletes a request, preserving
     * its data in the database.
     *
     * @param id identifier of the request to delete
     * @return empty response
     */
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete request (logical deletion)",
        description = "Marks the request as deleted without removing the record from the database.\n\n" +
                      "The request can be recovered with the /restaurar endpoint.\n\n" +
                      "**Successful response (200):** no content.\n\n" +
                      "**Error 404:** if no request exists with that ID."
    )
    public ResponseEntity<Void> delete(
            @Parameter(description = "Numeric identifier of the request to delete.")
            @PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok().build();
    }
}
