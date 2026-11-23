package pe.edu.vallegrande.app.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO for a FUT procedure management record.
 *
 * All JSON field names are in English.
 * Human-readable display values (managementType, priority) are returned
 * in Spanish to preserve Angular frontend compatibility.
 */
@Schema(description = "Response for a FUT procedure management record.")
@JsonPropertyOrder({
    "managementId",
    "applicantId",
    "documentNumber",
    "paternalLastName",
    "maternalLastName",
    "firstNames",
    "responsible",
    "responsibleFirstNames",
    "responsiblePaternalLastName",
    "responsibleMaternalLastName",
    "responsibleAreaId",
    "responsibleArea",
    "managementType",
    "priority",
    "managementStatus",
    "recordStatus",
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
public record GestionTramiteResponse(

    @Schema(description = "Internal management ID (use for edit, delete, restore operations).", example = "1")
    Integer managementId,

    @Schema(description = "Internal applicant ID.", example = "1")
    Long applicantId,

    @Schema(description = "Applicant's document number.", example = "70812345")
    String documentNumber,

    @Schema(description = "Applicant's paternal last name.", example = "Quispe")
    String paternalLastName,

    @Schema(description = "Applicant's maternal last name.", example = "Ramirez")
    String maternalLastName,

    @Schema(description = "Applicant's first names.", example = "Mariana")
    String firstNames,

    @Schema(description = "Full name of the responsible person.", example = "Viviana Cubas Rufasto")
    String responsible,

    @Schema(description = "First names of the responsible person (for edit form pre-fill).", example = "Viviana")
    String responsibleFirstNames,

    @Schema(description = "Paternal last name of the responsible person (for edit form pre-fill).", example = "Cubas")
    String responsiblePaternalLastName,

    @Schema(description = "Maternal last name of the responsible person (for edit form pre-fill).", example = "Rufasto")
    String responsibleMaternalLastName,

    @Schema(description = "Internal responsible area ID.", example = "2")
    Integer responsibleAreaId,

    @Schema(description = "Name of the responsible area.", example = "SECRETARIA")
    String responsibleArea,

    @Schema(description = "Management type (human-readable display value).", example = "Evaluación")
    String managementType,

    @Schema(description = "Priority (human-readable display value).", example = "Urgente")
    String priority,

    @Schema(description = "Management status code.", example = "OBSERVADO")
    String managementStatus,

    @Schema(description = "Record audit status: ACTIVO or INACTIVO.", example = "ACTIVO")
    String recordStatus,

    @Schema(description = "Reception date.", example = "2026-11-04")
    LocalDate receptionDate,

    @Schema(description = "Deadline date.", example = "2026-11-06")
    LocalDate deadlineDate,

    @Schema(description = "Resolution date.", example = "2026-11-04")
    LocalDate resolutionDate,

    @Schema(description = "Decision.", example = "OBSERVADO")
    String decision,

    @Schema(description = "Basis for the decision.", example = "The request requires correcting the submitted documents.")
    String decisionBasis,

    @Schema(description = "Additional observations.", example = "The applicant was notified.")
    String observations,

    @Schema(description = "Registration date.")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt,

    @Schema(description = "Last update date.")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime updatedAt,

    @Schema(description = "Logical deletion date.")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime deletedAt,

    @Schema(description = "Restoration date.")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime restoredAt
) {}
