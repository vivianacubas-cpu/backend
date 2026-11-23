package pe.edu.vallegrande.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * Input DTO for registering or updating a FUT procedure management record.
 *
 * The values for tipoGestion, prioridad, and estadoGestion must be in uppercase
 * (APROBACION, URGENTE, EN_PROCESO). The backend converts them to human-readable
 * text in the response.
 *
 * Audit dates (fechaRegistro, fechaActualizacion, etc.) are managed
 * automatically by the system and must not be sent.
 */
@Schema(name = "ManagementRequest",
        description = "Data for registering or updating a FUT procedure management record. " +
                "Enum values must be in uppercase. Audit dates are managed by the backend.")
public class GestionTramiteRequest {

    @NotNull(message = "The idSolicitante field is required.")
    @Positive(message = "The idSolicitante must be a positive number.")
    @Schema(description = "Identifier of the registered applicant.", example = "5")
    private Long idSolicitante;

    /** Free-text name of the teacher or responsible person. Optional. */
    @Size(max = 150, message = "The responsible person's name cannot exceed 150 characters.")
    @Pattern(
        regexp = "^$|^[A-Za-zÁÉÍÓÚáéíóúÑñ]+( [A-Za-zÁÉÍÓÚáéíóúÑñ]+)*$",
        message = "The responsible person's name can only contain letters, accented vowels, Ñ, and spaces."
    )
    @Schema(description = "Full name of the responsible person (optional). Calculated automatically if partial fields are omitted.",
            example = "Viviana Cubas Rufasto")
    private String nombreResponsable;

    @NotBlank(message = "The responsible person's first names are required.")
    @Size(max = 100, message = "The responsible person's first names cannot exceed 100 characters.")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚáéíóúÑñ]+( [A-Za-zÁÉÍÓÚáéíóúÑñ]+)*$",
             message = "First names can only contain letters, accented vowels, Ñ, and spaces.")
    @Schema(description = "First names of the person responsible for the management.", example = "Viviana")
    private String nombresResponsable;

    @NotBlank(message = "The paternal last name is required.")
    @Size(max = 100, message = "The paternal last name cannot exceed 100 characters.")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚáéíóúÑñ]+( [A-Za-zÁÉÍÓÚáéíóúÑñ]+)*$",
             message = "The paternal last name can only contain letters, accented vowels, Ñ, and spaces.")
    @Schema(description = "Paternal last name of the responsible person.", example = "Cubas")
    private String apellidoPaternoResponsable;

    @NotBlank(message = "The maternal last name is required.")
    @Size(max = 100, message = "The maternal last name cannot exceed 100 characters.")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚáéíóúÑñ]+( [A-Za-zÁÉÍÓÚáéíóúÑñ]+)*$",
             message = "The maternal last name can only contain letters, accented vowels, Ñ, and spaces.")
    @Schema(description = "Maternal last name of the responsible person.", example = "Rufasto")
    private String apellidoMaternoResponsable;

    @NotNull(message = "The idAreaResponsable field is required.")
    @Schema(description = "Identifier of the responsible area. See /v1/api/management/catalogo/areas.",
            example = "1")
    private Integer idAreaResponsable;

    @NotBlank(message = "The reception date is required.")
    @Schema(description = "Reception date (format YYYY-MM-DD).", example = "2026-10-28")
    private String fechaRecepcion;

    @NotBlank(message = "The tipoGestion field is required.")
    @Pattern(regexp = "RECEPCION|EVALUACION|DERIVACION|APROBACION|RECHAZO|SUBSANACION",
             message = "Invalid tipoGestion. Allowed values: RECEPCION, EVALUACION, DERIVACION, APROBACION, RECHAZO, SUBSANACION.")
    @Schema(description = "Management type. Values: RECEPCION, EVALUACION, DERIVACION, APROBACION, RECHAZO, SUBSANACION.",
            example = "APROBACION")
    private String tipoGestion;

    @NotBlank(message = "The priority is required.")
    @Pattern(regexp = "NORMAL|URGENTE|MUY_URGENTE",
             message = "Invalid priority. Allowed values: NORMAL, URGENTE, MUY_URGENTE.")
    @Schema(description = "Priority. Values: NORMAL, URGENTE, MUY_URGENTE.", example = "URGENTE")
    private String prioridad;

    @NotBlank(message = "The deadline date is required.")
    @Schema(description = "Deadline date (format YYYY-MM-DD).", example = "2026-10-29")
    private String fechaLimite;

    @NotBlank(message = "The management status is required.")
    @Pattern(regexp = "PENDIENTE|EN_PROCESO|OBSERVADO|APROBADO|RECHAZADO",
             message = "Invalid estadoGestion. Allowed values: PENDIENTE, EN_PROCESO, OBSERVADO, APROBADO, RECHAZADO.")
    @Schema(description = "Management status. Values: PENDIENTE, EN_PROCESO, OBSERVADO, APROBADO, RECHAZADO.",
            example = "EN_PROCESO")
    private String estadoGestion;

    @NotBlank(message = "The decision is required.")
    @Size(max = 500, message = "The decision cannot exceed 500 characters.")
    @Schema(description = "Decision made.", example = "APROBADO")
    private String decision;

    @NotBlank(message = "The decision basis is required.")
    @Size(max = 2000, message = "The decision basis cannot exceed 2000 characters.")
    @Schema(description = "Basis for the decision.",
            example = "The request meets the requirements established by the institution and has the necessary documentation for approval.")
    private String fundamentoDecision;

    @NotBlank(message = "The resolution date is required.")
    @Schema(description = "Resolution date (format YYYY-MM-DD).", example = "2026-10-31")
    private String fechaResolucion;

    @NotBlank(message = "Observations are required.")
    @Size(max = 2000, message = "Observations cannot exceed 2000 characters.")
    @Schema(description = "Additional observations.", example = "No observations.")
    private String observaciones;

    // ── Getters and setters ────────────────────────────────────────────────────

    public Long getIdSolicitante() { return idSolicitante; }
    public void setIdSolicitante(Long idSolicitante) { this.idSolicitante = idSolicitante; }

    public String getNombreResponsable() { return nombreResponsable; }
    public void setNombreResponsable(String nombreResponsable) { this.nombreResponsable = nombreResponsable; }

    public String getNombresResponsable() { return nombresResponsable; }
    public void setNombresResponsable(String nombresResponsable) { this.nombresResponsable = nombresResponsable; }

    public String getApellidoPaternoResponsable() { return apellidoPaternoResponsable; }
    public void setApellidoPaternoResponsable(String apellidoPaternoResponsable) { this.apellidoPaternoResponsable = apellidoPaternoResponsable; }

    public String getApellidoMaternoResponsable() { return apellidoMaternoResponsable; }
    public void setApellidoMaternoResponsable(String apellidoMaternoResponsable) { this.apellidoMaternoResponsable = apellidoMaternoResponsable; }

    public Integer getIdAreaResponsable() { return idAreaResponsable; }
    public void setIdAreaResponsable(Integer idAreaResponsable) { this.idAreaResponsable = idAreaResponsable; }

    public String getFechaRecepcion() { return fechaRecepcion; }
    public void setFechaRecepcion(String fechaRecepcion) { this.fechaRecepcion = fechaRecepcion; }

    public String getTipoGestion() { return tipoGestion; }
    public void setTipoGestion(String tipoGestion) { this.tipoGestion = tipoGestion; }

    public String getPrioridad() { return prioridad; }
    public void setPrioridad(String prioridad) { this.prioridad = prioridad; }

    public String getFechaLimite() { return fechaLimite; }
    public void setFechaLimite(String fechaLimite) { this.fechaLimite = fechaLimite; }

    public String getEstadoGestion() { return estadoGestion; }
    public void setEstadoGestion(String estadoGestion) { this.estadoGestion = estadoGestion; }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public String getFundamentoDecision() { return fundamentoDecision; }
    public void setFundamentoDecision(String fundamentoDecision) { this.fundamentoDecision = fundamentoDecision; }

    public String getFechaResolucion() { return fechaResolucion; }
    public void setFechaResolucion(String fechaResolucion) { this.fechaResolucion = fechaResolucion; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
}
