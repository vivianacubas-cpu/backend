package pe.edu.vallegrande.app.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@Schema(description = "Input data for registering or updating an applicant. " +
        "The status and date fields are automatically assigned by the server.")
@JsonPropertyOrder({
    "applicantId",
    "documentTypeId",
    "ubigeoId",
    "department",
    "province",
    "district",
    "documentNumber",
    "firstNames",
    "paternalLastName",
    "maternalLastName",
    "phoneNumber",
    "email",
    "address",
    "status",
    "createdAt",
    "updatedAt",
    "deletedAt",
    "restoredAt"
})
@Entity
@Table(name = "solicitante")
public class Solicitante {

    @Schema(description = "Send 0 or null so the system assigns the ID automatically.", example = "0")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_solicitante")
    @JsonProperty("applicantId")
    private Long idSolicitante;

    @Schema(description = "Document type ID. 1 = DNI, 2 = CNE. See /v1/api/applicant/tipos-documento.",
            example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @Column(name = "id_tipo_documento", nullable = false)
    @JsonProperty("documentTypeId")
    private Integer idTipoDocumento;

    @Schema(description = "Ubigeo ID (department/province/district). See /v1/api/applicant/ubigeos.",
            example = "1")
    @Column(name = "id_ubigeo", nullable = false)
    @JsonProperty("ubigeoId")
    private Integer idUbigeo;

    @Schema(description = "Department. Can be entered freely or left empty if ubigeoId is used.",
            example = "Lima")
    @Column(name = "departamento", length = 100)
    @JsonProperty("department")
    private String departamento;

    @Schema(description = "Province. Can be entered freely or left empty if ubigeoId is used.",
            example = "Cañete")
    @Column(name = "provincia", length = 100)
    @JsonProperty("province")
    private String provincia;

    @Schema(description = "District. Can be entered freely or left empty if ubigeoId is used.",
            example = "San Vicente de Cañete")
    @Column(name = "distrito", length = 100)
    @JsonProperty("district")
    private String distrito;

    @Schema(description = "DNI number (8 digits) or CNE (up to 20 characters).",
            example = "70812345", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "The document number is required.")
    @Size(max = 20, message = "The document number cannot exceed 20 characters.")
    @Column(name = "numero_documento", nullable = false, length = 20)
    @JsonProperty("documentNumber")
    private String numeroDocumento;

    @Schema(description = "Applicant's first names. Letters and single spaces only.",
            example = "Mariana", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "First names are required.")
    @Size(max = 100, message = "First names cannot exceed 100 characters.")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚáéíóúÑñÜü]+([ ][A-Za-zÁÉÍÓÚáéíóúÑñÜü]+)*$",
             message = "First names can only contain letters and single spaces between words.")
    @Column(name = "nombres", nullable = false, length = 100)
    @JsonProperty("firstNames")
    private String nombres;

    @Schema(description = "Paternal last name. Letters and single spaces only.",
            example = "Quispe", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "The paternal last name is required.")
    @Size(max = 100, message = "The paternal last name cannot exceed 100 characters.")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚáéíóúÑñÜü]+([ ][A-Za-zÁÉÍÓÚáéíóúÑñÜü]+)*$",
             message = "The paternal last name can only contain letters and a single space between words.")
    @Column(name = "apellido_paterno", nullable = false, length = 100)
    @JsonProperty("paternalLastName")
    private String apellidoPaterno;

    @Schema(description = "Maternal last name. Letters and single spaces only.",
            example = "Ramirez", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "The maternal last name is required.")
    @Size(max = 100, message = "The maternal last name cannot exceed 100 characters.")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚáéíóúÑñÜü]+([ ][A-Za-zÁÉÍÓÚáéíóúÑñÜü]+)*$",
             message = "The maternal last name can only contain letters and a single space between words.")
    @Column(name = "apellido_materno", nullable = false, length = 100)
    @JsonProperty("maternalLastName")
    private String apellidoMaterno;

    @Schema(description = "Mobile phone. Exactly 9 numeric digits.", example = "987654321")
    @Pattern(regexp = "^\\d{9}$",
             message = "The phone number must contain exactly 9 numeric digits.")
    @Size(max = 9, message = "The phone number cannot exceed 9 digits.")
    @Column(name = "telefono", length = 20)
    @JsonProperty("phoneNumber")
    private String telefono;

    @Schema(description = "Applicant's email address.", example = "mariana.quispe@example.com")
    @Email(message = "The email address format is not valid.")
    @Size(max = 150, message = "The email cannot exceed 150 characters.")
    @Column(name = "correo", length = 150)
    @JsonProperty("email")
    private String correo;

    @Schema(description = "Applicant's home address.", example = "Av. Mariscal Benavides 250")
    @Size(max = 250, message = "The address cannot exceed 250 characters.")
    @Column(name = "direccion", length = 250)
    @JsonProperty("address")
    private String direccion;

    @Schema(description = "Automatically assigned by the server: A = Active, I = Inactive.",
            accessMode = Schema.AccessMode.READ_ONLY, example = "A")
    @Column(name = "estado", nullable = false, length = 1)
    @JsonProperty("status")
    private String estado;

    @Schema(description = "Automatically assigned on registration.", accessMode = Schema.AccessMode.READ_ONLY)
    @Column(name = "fecha_registro")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonProperty("createdAt")
    private LocalDateTime fechaRegistro;

    @Schema(description = "Automatically assigned on update.", accessMode = Schema.AccessMode.READ_ONLY)
    @Column(name = "fecha_actualizacion")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonProperty("updatedAt")
    private LocalDateTime fechaActualizacion;

    @Schema(description = "Automatically assigned on logical deletion.", accessMode = Schema.AccessMode.READ_ONLY)
    @Column(name = "fecha_eliminacion")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonProperty("deletedAt")
    private LocalDateTime fechaEliminacion;

    @Schema(description = "Automatically assigned on restoration.", accessMode = Schema.AccessMode.READ_ONLY)
    @Column(name = "fecha_restauracion")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonProperty("restoredAt")
    private LocalDateTime fechaRestauracion;

    public Solicitante() {}

    public Long getIdSolicitante() { return idSolicitante; }
    public void setIdSolicitante(Long idSolicitante) { this.idSolicitante = idSolicitante; }

    public Integer getIdTipoDocumento() { return idTipoDocumento; }
    public void setIdTipoDocumento(Integer idTipoDocumento) { this.idTipoDocumento = idTipoDocumento; }

    public Integer getIdUbigeo() { return idUbigeo; }
    public void setIdUbigeo(Integer idUbigeo) { this.idUbigeo = idUbigeo; }

    public String getDepartamento() { return departamento; }
    public void setDepartamento(String departamento) { this.departamento = departamento; }

    public String getProvincia() { return provincia; }
    public void setProvincia(String provincia) { this.provincia = provincia; }

    public String getDistrito() { return distrito; }
    public void setDistrito(String distrito) { this.distrito = distrito; }

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
