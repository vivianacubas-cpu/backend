package pe.edu.vallegrande.app.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Input DTO for registering or updating an applicant.
 *
 * Accepts two formats in the same JSON:
 *
 * A) Native format (Angular frontend):
 *    { documentTypeId: 1, ubigeoId: 1, department: "", province: "",
 *      district: "", documentNumber: "", firstNames: "",
 *      paternalLastName: "", maternalLastName: "", phoneNumber: "",
 *      email: "", address: "" }
 *
 * B) Presentation format (Swagger / manual testing):
 *    { documentType: "", location: "",
 *      documentNumber: "", firstNames: "", paternalLastName: "", maternalLastName: "",
 *      phoneNumber: "", email: "", address: "" }
 *
 * The service resolves: if documentTypeId is provided → use it directly.
 *                       if documentType (text) is provided → look up the ID in dbo.tipo_documento.
 *                       Same for ubigeoId vs. location + department/province/district.
 *
 * The status and date fields are ignored — the server assigns them automatically.
 */
@Schema(description = "Data for registering or updating an applicant. " +
        "You may use documentTypeId/ubigeoId (native format) or documentType/location (text).")
public class SolicitanteRequest {

    // ── Document type identification ──────────────────────────────────────────

    @Schema(description = "Numeric document type ID (1=DNI, 2=CNE). " +
            "Takes priority over documentType if both are provided.", example = "1")
    @JsonAlias({"idTipoDocumento", "id_tipo_documento"})
    private Integer idTipoDocumento;

    @Schema(description = "Document type text code (DNI, CNE). " +
            "Used when documentTypeId is not provided.", example = "DNI")
    @JsonProperty("documentType")
    @JsonAlias({"tipoDocumento"})
    private String tipoDocumento;

    // ── Ubigeo identification ─────────────────────────────────────────────────

    @Schema(description = "Numeric ubigeo ID. " +
            "Takes priority over location/department/province/district if provided.", example = "1")
    @JsonAlias({"idUbigeo", "id_ubigeo"})
    private Integer idUbigeo;

    @Schema(description = "Department (free text). Used when ubigeoId is not provided.", example = "Lima")
    @JsonProperty("department")
    @JsonAlias({"departamento"})
    private String departamento;

    @Schema(description = "Province (free text). Used when ubigeoId is not provided.", example = "Cañete")
    @JsonProperty("province")
    @JsonAlias({"provincia"})
    private String provincia;

    @Schema(description = "District (free text). Used when ubigeoId is not provided.", example = "San Vicente de Cañete")
    @JsonProperty("district")
    @JsonAlias({"distrito"})
    private String distrito;

    @Schema(description = "Location as 'District, Province, Department' string. " +
            "Parsed when department/province/district are not provided separately.", example = "")
    @JsonProperty("location")
    @JsonAlias({"ubicacion"})
    private String ubicacion;

    // ── Personal data ─────────────────────────────────────────────────────────

    @Schema(description = "DNI number (8 digits) or CNE (up to 20 characters).",
            example = "70812345", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("documentNumber")
    @JsonAlias({"numeroDocumento", "numero_documento"})
    private String numeroDocumento;

    @Schema(description = "Applicant's first names.", example = "Mariana",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("firstNames")
    @JsonAlias({"nombres"})
    private String nombres;

    @Schema(description = "Paternal last name.", example = "Quispe",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("paternalLastName")
    @JsonAlias({"apellidoPaterno", "apellido_paterno"})
    private String apellidoPaterno;

    @Schema(description = "Maternal last name.", example = "Ramirez",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("maternalLastName")
    @JsonAlias({"apellidoMaterno", "apellido_materno"})
    private String apellidoMaterno;

    @Schema(description = "Mobile phone. Exactly 9 numeric digits.", example = "987654321")
    @JsonProperty("phoneNumber")
    @JsonAlias({"telefono"})
    private String telefono;

    @Schema(description = "Email address.", example = "mariana.quispe@example.com")
    @JsonProperty("email")
    @JsonAlias({"correo"})
    private String correo;

    @Schema(description = "Home address.", example = "Av. Mariscal Benavides 250")
    @JsonProperty("address")
    @JsonAlias({"direccion"})
    private String direccion;

    // ── Read-only fields (ignored on input) ───────────────────────────────────

    @Schema(description = "Ignored — automatically assigned by the server.",
            accessMode = Schema.AccessMode.READ_ONLY)
    @JsonProperty("status")
    @JsonAlias({"estado"})
    private String estado;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonProperty("createdAt")
    @JsonAlias({"fechaRegistro"})
    private LocalDateTime fechaRegistro;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonProperty("updatedAt")
    @JsonAlias({"fechaActualizacion"})
    private LocalDateTime fechaActualizacion;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonProperty("deletedAt")
    @JsonAlias({"fechaEliminacion"})
    private LocalDateTime fechaEliminacion;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonProperty("restoredAt")
    @JsonAlias({"fechaRestauracion"})
    private LocalDateTime fechaRestauracion;

    public SolicitanteRequest() {}

    public Integer getIdTipoDocumento() { return idTipoDocumento; }
    public void setIdTipoDocumento(Integer idTipoDocumento) { this.idTipoDocumento = idTipoDocumento; }

    public String getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }

    public Integer getIdUbigeo() { return idUbigeo; }
    public void setIdUbigeo(Integer idUbigeo) { this.idUbigeo = idUbigeo; }

    public String getDepartamento() { return departamento; }
    public void setDepartamento(String departamento) { this.departamento = departamento; }

    public String getProvincia() { return provincia; }
    public void setProvincia(String provincia) { this.provincia = provincia; }

    public String getDistrito() { return distrito; }
    public void setDistrito(String distrito) { this.distrito = distrito; }

    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }

    public String getNumeroDocumento() { return numeroDocumento; }
    public void setNumeroDocumento(String numeroDocumento) { this.numeroDocumento = numeroDocumento; }

    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }

    public String getApellidoPaterno() { return apellidoPaterno; }
    public void setApellidoPaterno(String apellidoPaterno) { this.apellidoPaterno = apellidoPaterno; }

    public String getApellidoMaterno() { return apellidoMaterno; }
    public void setApellidoMaterno(String apellidoMaterno) { this.apellidoMaterno = apellidoMaterno; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }

    public LocalDateTime getFechaEliminacion() { return fechaEliminacion; }
    public void setFechaEliminacion(LocalDateTime fechaEliminacion) { this.fechaEliminacion = fechaEliminacion; }

    public LocalDateTime getFechaRestauracion() { return fechaRestauracion; }
    public void setFechaRestauracion(LocalDateTime fechaRestauracion) { this.fechaRestauracion = fechaRestauracion; }
}
