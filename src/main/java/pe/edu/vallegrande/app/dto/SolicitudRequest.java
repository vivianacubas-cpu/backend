package pe.edu.vallegrande.app.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * Request body DTO for registering a FUT (Single Procedure Form) request.
 */
@Schema(name = "FUTRequest", description = "Input body for registering a Single Procedure Form (FUT) request")
public class SolicitudRequest {

    @NotBlank(message = "The procedure type (solicito) is required.")
    @Size(max = 255, message = "The procedure type cannot exceed 255 characters.")
    @JsonAlias({"solicito"})
    private String solicitante;

    @NotBlank(message = "The full name is required.")
    @Size(max = 200, message = "The full name cannot exceed 200 characters.")
    @JsonAlias({"nombreCompleto", "nombre_completo"})
    private String fullName;

    @NotBlank(message = "The document number is required.")
    @Size(max = 20, message = "The document number cannot exceed 20 characters.")
    private String dni;

    @Email(message = "The email address format is not valid.")
    @Size(max = 150, message = "The email cannot exceed 150 characters.")
    @JsonAlias({"correo", "email"})
    private String email;

    @NotBlank(message = "The mobile phone number is required.")
    @Pattern(regexp = "^9\\d{8}$", message = "The mobile number must have 9 digits and start with 9.")
    @JsonAlias({"celular"})
    private String cellphone;

    @NotBlank(message = "The address is required.")
    @Size(max = 250, message = "The address cannot exceed 250 characters.")
    @JsonAlias({"domicilio"})
    private String address;

    @NotBlank(message = "The district is required.")
    @Size(max = 100, message = "The district cannot exceed 100 characters.")
    @JsonAlias({"distrito"})
    private String district;

    @NotBlank(message = "The province is required.")
    @Size(max = 100, message = "The province cannot exceed 100 characters.")
    @JsonAlias({"provincia"})
    private String province;

    @NotBlank(message = "The basis for the request is required.")
    @Size(max = 2000, message = "The basis cannot exceed 2000 characters.")
    @JsonAlias({"fundamento"})
    private String basis;

    @Size(max = 255, message = "Attached documents cannot exceed 255 characters.")
    @JsonAlias({"documentosAdjuntos", "documentos_adjuntos"})
    private String attachedDocuments;

    @JsonAlias({"fechaDeclarada", "fecha_declarada"})
    private String declaredDate;

    @Size(max = 500, message = "The signature note cannot exceed 500 characters.")
    @JsonAlias({"notaFirma", "nota_firma"})
    private String signatureNote;

    private String idempotencyKey;
    private String tipoTramite;
    private String tipoSolicitante;
    private String tipoDocumento;
    private String department;

    @Size(max = 50, message = "The department number cannot exceed 50 characters.")
    private String numeroDepto;

    @Pattern(regexp = "^(\\d{6,9})?$", message = "The landline phone must have between 6 and 9 digits.")
    private String telefonoFijo;

    @Min(value = 0, message = "The number of folios cannot be negative.")
    private Integer folios;

    @Size(max = 255, message = "The file name cannot exceed 255 characters.")
    private String archivoNombre;

    @Size(max = 150, message = "The file type cannot exceed 150 characters.")
    private String archivoTipo;

    private Long archivoTamano;

    // ── Getters and setters ────────────────────────────────────────────────────

    public String getSolicitante() { return solicitante; }
    public void setSolicitante(String solicitante) { this.solicitante = solicitante; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCellphone() { return cellphone; }
    public void setCellphone(String cellphone) { this.cellphone = cellphone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }

    public String getBasis() { return basis; }
    public void setBasis(String basis) { this.basis = basis; }

    public String getAttachedDocuments() { return attachedDocuments; }
    public void setAttachedDocuments(String attachedDocuments) { this.attachedDocuments = attachedDocuments; }

    public String getDeclaredDate() { return declaredDate; }
    public void setDeclaredDate(String declaredDate) { this.declaredDate = declaredDate; }

    public String getSignatureNote() { return signatureNote; }
    public void setSignatureNote(String signatureNote) { this.signatureNote = signatureNote; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String getTipoTramite() { return tipoTramite; }
    public void setTipoTramite(String tipoTramite) { this.tipoTramite = tipoTramite; }

    public String getTipoSolicitante() { return tipoSolicitante; }
    public void setTipoSolicitante(String tipoSolicitante) { this.tipoSolicitante = tipoSolicitante; }

    public String getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getNumeroDepto() { return numeroDepto; }
    public void setNumeroDepto(String numeroDepto) { this.numeroDepto = numeroDepto; }

    public String getTelefonoFijo() { return telefonoFijo; }
    public void setTelefonoFijo(String telefonoFijo) { this.telefonoFijo = telefonoFijo; }

    public Integer getFolios() { return folios; }
    public void setFolios(Integer folios) { this.folios = folios; }

    public String getArchivoNombre() { return archivoNombre; }
    public void setArchivoNombre(String archivoNombre) { this.archivoNombre = archivoNombre; }

    public String getArchivoTipo() { return archivoTipo; }
    public void setArchivoTipo(String archivoTipo) { this.archivoTipo = archivoTipo; }

    public Long getArchivoTamano() { return archivoTamano; }
    public void setArchivoTamano(Long archivoTamano) { this.archivoTamano = archivoTamano; }
}
