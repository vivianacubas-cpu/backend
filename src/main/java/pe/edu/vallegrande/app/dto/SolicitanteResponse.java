package pe.edu.vallegrande.app.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Response DTO for the Applicant module.
 * Exposes presentation fields as English JSON property names.
 */
@Schema(description = "Applicant data as displayed by the frontend.")
@JsonPropertyOrder({
    "applicantId",
    "documentType",
    "documentNumber",
    "paternalLastName",
    "maternalLastName",
    "firstNames",
    "district",
    "province",
    "department",
    "address",
    "phoneNumber",
    "email",
    "status",
    "createdAt",
    "updatedAt",
    "deletedAt",
    "restoredAt"
})
public record SolicitanteResponse(
        @Schema(description = "Internal applicant identifier.", example = "1")
        Long applicantId,

        @Schema(description = "Document type (DNI, CNE).", example = "DNI")
        String documentType,

        @Schema(description = "Document number (DNI 8 digits or CNE up to 20 characters).", example = "76543456")
        String documentNumber,

        @Schema(description = "Paternal last name.", example = "Cubas")
        String paternalLastName,

        @Schema(description = "Maternal last name.", example = "Rufasto")
        String maternalLastName,

        @Schema(description = "Applicant's first names.", example = "Viviana")
        String firstNames,

        @Schema(description = "District.", example = "San Vicente de Cañete")
        String district,

        @Schema(description = "Province.", example = "Cañete")
        String province,

        @Schema(description = "Department.", example = "Lima")
        String department,

        @Schema(description = "Home address.", example = "Jr. Los Álamos 123")
        String address,

        @Schema(description = "Mobile phone (9 digits).", example = "925554572")
        String phoneNumber,

        @Schema(description = "Email address.", example = "viviana@example.com")
        String email,

        @Schema(description = "Human-readable status: \"Activo\" or \"Inactivo\".", example = "Activo")
        String status,

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
