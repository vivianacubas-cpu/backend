package pe.edu.vallegrande.app.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.LocalDate;

/**
 * Entity representing a FUT (Single Procedure Form) request.
 * Maps to the 'tramite_fut' database table.
 */
@Entity
@Table(name = "tramite_fut")
public class Solicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_solicitud")
    @JsonProperty("requestId")
    private Long idSolicitud;

    @Column(name = "id_solicitante")
    @JsonProperty("applicantId")
    private Long idSolicitante;

    @Column(name = "codigo_seguimiento", nullable = false, unique = true, length = 60)
    @JsonProperty("trackingCode")
    private String codigoSeguimiento;

    @Column(name = "solicito", nullable = false, length = 500)
    @JsonProperty("procedureRequested")
    private String solicito;

    @Column(name = "nombre_completo", nullable = false, length = 200)
    @JsonProperty("fullName")
    private String nombreCompleto;

    @Column(name = "dni", nullable = false, length = 20)
    @JsonProperty("documentNumber")
    private String dni;

    @Column(name = "tipo_documento", nullable = false, length = 10)
    @JsonProperty("documentType")
    private String tipoDocumento = "DNI";

    @Column(name = "tipo_solicitante", length = 100)
    @JsonProperty("applicantType")
    private String tipoSolicitante;

    @Column(name = "departamento", length = 100)
    @JsonProperty("department")
    private String departamento;

    @Column(name = "numero_depto", length = 50)
    @JsonProperty("deptNumber")
    private String numeroDepto;

    @Column(name = "telefono_fijo", length = 20)
    @JsonProperty("landlinePhone")
    private String telefonoFijo;

    @Column(name = "folios")
    @JsonProperty("folios")
    private Integer folios;

    @Column(name = "archivo_nombre", length = 255)
    @JsonProperty("fileName")
    private String archivoNombre;

    @Column(name = "archivo_tipo", length = 150)
    @JsonProperty("fileType")
    private String archivoTipo;

    @Column(name = "archivo_tamano")
    @JsonProperty("fileSize")
    private Long archivoTamano;

    @Column(name = "idempotency_key", length = 80, unique = true)
    @JsonProperty("idempotencyKey")
    private String idempotencyKey;

    @Column(name = "correo", length = 150)
    @JsonProperty("email")
    private String correo;

    @Column(name = "celular", nullable = false, length = 9)
    @JsonProperty("mobilePhone")
    private String celular;

    @Column(name = "domicilio", nullable = false, length = 250)
    @JsonProperty("homeAddress")
    private String domicilio;

    @Column(name = "distrito", nullable = false, length = 100)
    @JsonProperty("district")
    private String distrito;

    @Column(name = "provincia", nullable = false, length = 100)
    @JsonProperty("province")
    private String provincia;

    @Column(name = "fundamento", nullable = false, length = 2000)
    @JsonProperty("basis")
    private String fundamento;

    @Column(name = "documentos_adjuntos", length = 1000)
    @JsonProperty("attachedDocuments")
    private String documentosAdjuntos;

    @Column(name = "fecha_declarada", nullable = false)
    @JsonProperty("declaredDate")
    private LocalDate fechaDeclarada;

    @Column(name = "id_tipo_tramite")
    @JsonProperty("procedureTypeId")
    private Integer idTipoTramite;

    @Column(name = "nota_firma", length = 500)
    @JsonProperty("signatureNote")
    private String notaFirma;

    @Column(name = "estado", nullable = false, length = 30)
    @JsonProperty("status")
    private String estado = "RECIBIDO";

    @Column(name = "observacion", length = 2000)
    @JsonProperty("observation")
    private String observacion;

    @Column(name = "fecha_eliminacion")
    @JsonProperty("deletedAt")
    private LocalDateTime fechaEliminacion;

    @Column(name = "fecha_actualizacion")
    @JsonProperty("updatedAt")
    private LocalDateTime fechaActualizacion;

    @Column(name = "fecha_restauracion")
    @JsonProperty("restoredAt")
    private LocalDateTime fechaRestauracion;

    @Column(name = "fecha_registro", nullable = false)
    @JsonProperty("createdAt")
    private LocalDateTime fechaRegistro;

    @PrePersist
    void prePersist() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }
        if (estado == null || estado.isBlank()) {
            estado = "RECIBIDO";
        }
    }

    public Solicitud() {}

    public Long getIdSolicitud() { return idSolicitud; }
    public void setIdSolicitud(Long idSolicitud) { this.idSolicitud = idSolicitud; }

    public Long getIdSolicitante() { return idSolicitante; }
    public void setIdSolicitante(Long idSolicitante) { this.idSolicitante = idSolicitante; }

    public String getCodigoSeguimiento() { return codigoSeguimiento; }
    public void setCodigoSeguimiento(String codigoSeguimiento) { this.codigoSeguimiento = codigoSeguimiento; }

    public String getSolicito() { return solicito; }
    public void setSolicito(String solicito) { this.solicito = solicito; }

    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }

    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }

    public String getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }

    public String getTipoSolicitante() { return tipoSolicitante; }
    public void setTipoSolicitante(String tipoSolicitante) { this.tipoSolicitante = tipoSolicitante; }

    public String getDepartamento() { return departamento; }
    public void setDepartamento(String departamento) { this.departamento = departamento; }

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

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getCelular() { return celular; }
    public void setCelular(String celular) { this.celular = celular; }

    public String getDomicilio() { return domicilio; }
    public void setDomicilio(String domicilio) { this.domicilio = domicilio; }

    public String getDistrito() { return distrito; }
    public void setDistrito(String distrito) { this.distrito = distrito; }

    public String getProvincia() { return provincia; }
    public void setProvincia(String provincia) { this.provincia = provincia; }

    public String getFundamento() { return fundamento; }
    public void setFundamento(String fundamento) { this.fundamento = fundamento; }

    public String getDocumentosAdjuntos() { return documentosAdjuntos; }
    public void setDocumentosAdjuntos(String documentosAdjuntos) { this.documentosAdjuntos = documentosAdjuntos; }

    public LocalDate getFechaDeclarada() { return fechaDeclarada; }
    public void setFechaDeclarada(LocalDate fechaDeclarada) { this.fechaDeclarada = fechaDeclarada; }

    public Integer getIdTipoTramite() { return idTipoTramite; }
    public void setIdTipoTramite(Integer idTipoTramite) { this.idTipoTramite = idTipoTramite; }

    public String getNotaFirma() { return notaFirma; }
    public void setNotaFirma(String notaFirma) { this.notaFirma = notaFirma; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    public LocalDateTime getFechaEliminacion() { return fechaEliminacion; }
    public void setFechaEliminacion(LocalDateTime fechaEliminacion) { this.fechaEliminacion = fechaEliminacion; }

    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }

    public LocalDateTime getFechaRestauracion() { return fechaRestauracion; }
    public void setFechaRestauracion(LocalDateTime fechaRestauracion) { this.fechaRestauracion = fechaRestauracion; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
}
