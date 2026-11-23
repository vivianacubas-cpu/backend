package pe.edu.vallegrande.app.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * Input DTO for registering or updating a FUT procedure management record.
 *
 * <p>All JSON field names are in English. Spanish aliases are accepted
 * for backward compatibility with the existing Angular frontend.
 *
 * <p>Both internal codes (APROBACION, URGENTE, EN_PROCESO)
 * and human-readable text (Approval, Urgent, In Progress) are accepted.
 * The backend normalizes before persisting.
 *
 * <p>Audit dates are managed automatically by the backend;
 * any values sent in those fields will be ignored.
 *
 * <p>managementId is ignored on registration; on update, the route parameter is used.
 */
@Schema(name = "ManagementFormRequest",
        description = "Request body for registering or updating a FUT procedure management record.")
@JsonPropertyOrder({
    "managementId",
    "applicant",
    "responsibleFirstNames",
    "responsiblePaternalLastName",
    "responsibleMaternalLastName",
    "responsibleArea",
    "managementType",
    "priority",
    "managementStatus",
    "receptionDate",
    "deadlineDate",
    "resolutionDate",
    "decision",
    "decisionBasis",
    "observations",
    "createdAt",
    "updatedAt",
    "deletedAt",
    "restoredAt"
})
public class GestionTramiteFormRequest {

    // ── Fields that are ignored or read-only ──────────────────────────────

    @Schema(description = "Ignored on registration. On update, the route ID parameter is used.",
            example = "null", nullable = true)
    @JsonAlias({"idGestion"})
    private Integer idGestion;

    // ── Applicant: accepts full name (Swagger) or numeric ID (frontend) ───

    @Schema(description = "Full name of the applicant registered in the system. " +
            "Alternative: send applicantId (number). " +
            "Example: 'Mariana Quispe Ramirez'.",
            example = "Mariana Quispe Ramirez")
    @JsonProperty("applicant")
    @JsonAlias({"solicitante"})
    private String solicitante;

    /** Numeric applicant ID — used by the Angular frontend. Takes priority over applicant name. */
    @Schema(description = "Applicant ID (alternative to 'applicant'). Use when the frontend sends the ID directly.",
            example = "5", hidden = true)
    @JsonAlias({"idSolicitante"})
    private Long idSolicitante;

    // ── Responsible person ────────────────────────────────────────────────

    @NotBlank(message = "The responsible person's first names are required.")
    @Size(max = 100, message = "The responsible person's first names cannot exceed 100 characters.")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚáéíóúÑñ]+( [A-Za-zÁÉÍÓÚáéíóúÑñ]+)*$",
             message = "First names can only contain letters, accented vowels, Ñ, and spaces.")
    @Schema(description = "First names of the person responsible for the management.", example = "Viviana")
    @JsonProperty("responsibleFirstNames")
    @JsonAlias({"nombresResponsable"})
    private String nombresResponsable;

    @NotBlank(message = "The paternal last name is required.")
    @Size(max = 100, message = "The paternal last name cannot exceed 100 characters.")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚáéíóúÑñ]+( [A-Za-zÁÉÍÓÚáéíóúÑñ]+)*$",
             message = "The paternal last name can only contain letters, accented vowels, Ñ, and spaces.")
    @Schema(description = "Paternal last name of the responsible person.", example = "Cubas")
    @JsonProperty("responsiblePaternalLastName")
    @JsonAlias({"apellidoPaternoResponsable"})
    private String apellidoPaternoResponsable;

    @NotBlank(message = "The maternal last name is required.")
    @Size(max = 100, message = "The maternal last name cannot exceed 100 characters.")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚáéíóúÑñ]+( [A-Za-zÁÉÍÓÚáéíóúÑñ]+)*$",
             message = "The maternal last name can only contain letters, accented vowels, Ñ, and spaces.")
    @Schema(description = "Maternal last name of the responsible person.", example = "Rufasto")
    @JsonProperty("responsibleMaternalLastName")
    @JsonAlias({"apellidoMaternoResponsable"})
    private String apellidoMaternoResponsable;

    // ── Area: accepts name (Swagger) or numeric ID (frontend) ─────────────

    @Schema(description = "Name of the responsible area: ADMINISTRACION, DIRECCION, SECRETARIA. " +
            "Alternative: send responsibleAreaId (number).",
            example = "SECRETARIA")
    @JsonProperty("responsibleArea")
    @JsonAlias({"areaResponsable"})
    private String areaResponsable;

    /** Numeric area ID — used by the Angular frontend. Takes priority over area name. */
    @Schema(description = "Responsible area ID (alternative to 'responsibleArea'). " +
            "Use when the frontend sends the ID directly.",
            example = "1", hidden = true)
    @JsonProperty("responsibleAreaId")
    @JsonAlias({"idAreaResponsable"})
    private Integer idAreaResponsable;

    // ── Management fields ─────────────────────────────────────────────────

    @NotBlank(message = "The managementType field is required.")
    @Schema(description = "Management type. Values: RECEPCION, EVALUACION, DERIVACION, APROBACION, RECHAZO, SUBSANACION.",
            example = "EVALUACION")
    @JsonProperty("managementType")
    @JsonAlias({"tipoGestion"})
    private String tipoGestion;

    @NotBlank(message = "The priority is required.")
    @Schema(description = "Priority. Values: NORMAL, URGENTE, MUY_URGENTE.",
            example = "URGENTE")
    @JsonProperty("priority")
    @JsonAlias({"prioridad"})
    private String prioridad;

    @NotBlank(message = "The management status is required.")
    @Schema(description = "Management status. Values: PENDIENTE, EN_PROCESO, OBSERVADO, APROBADO, RECHAZADO.",
            example = "PENDIENTE")
    @JsonProperty("managementStatus")
    @JsonAlias({"estadoGestion"})
    private String estadoGestion;

    @NotBlank(message = "The reception date is required.")
    @Schema(description = "Reception date (format YYYY-MM-DD).", example = "2026-10-28")
    @JsonProperty("receptionDate")
    @JsonAlias({"fechaRecepcion"})
    private String fechaRecepcion;

    @NotBlank(message = "The deadline date is required.")
    @Schema(description = "Deadline date (format YYYY-MM-DD).", example = "2026-10-29")
    @JsonProperty("deadlineDate")
    @JsonAlias({"fechaLimite"})
    private String fechaLimite;

    @NotBlank(message = "The resolution date is required.")
    @Schema(description = "Resolution date (format YYYY-MM-DD).", example = "2026-10-31")
    @JsonProperty("resolutionDate")
    @JsonAlias({"fechaResolucion"})
    private String fechaResolucion;

    @NotBlank(message = "The decision is required.")
    @Size(max = 500, message = "The decision cannot exceed 500 characters.")
    @Schema(description = "Decision made.", example = "APROBADO")
    @JsonAlias({"decision"})
    private String decision;

    @NotBlank(message = "The decision basis is required.")
    @Size(max = 2000, message = "The decision basis cannot exceed 2000 characters.")
    @Schema(description = "Basis for the decision.",
            example = "The request meets the requirements established by the institution.")
    @JsonProperty("decisionBasis")
    @JsonAlias({"fundamentoDecision"})
    private String fundamentoDecision;

    @NotBlank(message = "Observations are required.")
    @Size(max = 2000, message = "Observations cannot exceed 2000 characters.")
    @Schema(description = "Additional observations.", example = "No observations.")
    @JsonProperty("observations")
    @JsonAlias({"observaciones"})
    private String observaciones;

    // ── Audit dates (read-only — managed by the backend) ─────────────────

    @Schema(description = "Ignored. The backend assigns the registration date automatically.",
            example = "null", nullable = true, accessMode = Schema.AccessMode.READ_ONLY)
    @JsonProperty("createdAt")
    @JsonAlias({"fechaRegistro"})
    private String fechaRegistro;

    @Schema(description = "Ignored. The backend assigns the update date automatically.",
            example = "null", nullable = true, accessMode = Schema.AccessMode.READ_ONLY)
    @JsonProperty("updatedAt")
    @JsonAlias({"fechaActualizacion"})
    private String fechaActualizacion;

    @Schema(description = "Ignored. The backend assigns the deletion date automatically.",
            example = "null", nullable = true, accessMode = Schema.AccessMode.READ_ONLY)
    @JsonProperty("deletedAt")
    @JsonAlias({"fechaEliminacion"})
    private String fechaEliminacion;

    @Schema(description = "Ignored. The backend assigns the restoration date automatically.",
            example = "null", nullable = true, accessMode = Schema.AccessMode.READ_ONLY)
    @JsonProperty("restoredAt")
    @JsonAlias({"fechaRestauracion"})
    private String fechaRestauracion;

    // ── Getters and setters ───────────────────────────────────────────────

    public Integer getIdGestion() { return idGestion; }
    public void setIdGestion(Integer idGestion) { this.idGestion = idGestion; }

    public String getSolicitante() { return solicitante; }
    public void setSolicitante(String solicitante) { this.solicitante = solicitante; }

    public Long getIdSolicitante() { return idSolicitante; }
    public void setIdSolicitante(Long idSolicitante) { this.idSolicitante = idSolicitante; }

    public String getNombresResponsable() { return nombresResponsable; }
    public void setNombresResponsable(String v) { this.nombresResponsable = v; }

    public String getApellidoPaternoResponsable() { return apellidoPaternoResponsable; }
    public void setApellidoPaternoResponsable(String v) { this.apellidoPaternoResponsable = v; }

    public String getApellidoMaternoResponsable() { return apellidoMaternoResponsable; }
    public void setApellidoMaternoResponsable(String v) { this.apellidoMaternoResponsable = v; }

    public String getAreaResponsable() { return areaResponsable; }
    public void setAreaResponsable(String areaResponsable) { this.areaResponsable = areaResponsable; }

    public Integer getIdAreaResponsable() { return idAreaResponsable; }
    public void setIdAreaResponsable(Integer idAreaResponsable) { this.idAreaResponsable = idAreaResponsable; }

    public String getTipoGestion() { return tipoGestion; }
    public void setTipoGestion(String tipoGestion) { this.tipoGestion = tipoGestion; }

    public String getPrioridad() { return prioridad; }
    public void setPrioridad(String prioridad) { this.prioridad = prioridad; }

    public String getEstadoGestion() { return estadoGestion; }
    public void setEstadoGestion(String estadoGestion) { this.estadoGestion = estadoGestion; }

    public String getFechaRecepcion() { return fechaRecepcion; }
    public void setFechaRecepcion(String fechaRecepcion) { this.fechaRecepcion = fechaRecepcion; }

    public String getFechaLimite() { return fechaLimite; }
    public void setFechaLimite(String fechaLimite) { this.fechaLimite = fechaLimite; }

    public String getFechaResolucion() { return fechaResolucion; }
    public void setFechaResolucion(String fechaResolucion) { this.fechaResolucion = fechaResolucion; }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public String getFundamentoDecision() { return fundamentoDecision; }
    public void setFundamentoDecision(String fundamentoDecision) { this.fundamentoDecision = fundamentoDecision; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public String getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(String fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public String getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(String fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }

    public String getFechaEliminacion() { return fechaEliminacion; }
    public void setFechaEliminacion(String fechaEliminacion) { this.fechaEliminacion = fechaEliminacion; }

    public String getFechaRestauracion() { return fechaRestauracion; }
    public void setFechaRestauracion(String fechaRestauracion) { this.fechaRestauracion = fechaRestauracion; }
}
