package pe.edu.vallegrande.app.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Entity for FUT Procedure Management.
 * Maps to the 'gestion_tramite_fut' database table.
 *
 * Human-readable display values (label maps) return Spanish text
 * to preserve compatibility with the existing Angular frontend display.
 */
@Schema(hidden = true)
@Entity
@Table(name = "gestion_tramite_fut")
public class GestionTramiteFut {

    // ── Primary key (internal) ────────────────────────────────────────────────
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_gestion")
    private Integer idGestion;

    // ── FK to applicant (internal) ────────────────────────────────────────────
    @Column(name = "id_solicitante", nullable = false)
    private Long idSolicitante;

    // ── @Transient fields: computed by the service before responding ──────────

    /** Row correlation number assigned by the service when listing. */
    @Transient
    private Integer numero;

    /** Applicant document number (from dbo.solicitante). */
    @Transient
    private String numeroDocumento;

    /** Applicant paternal last name (from dbo.solicitante). */
    @Transient
    private String apellidoPaterno;

    /** Applicant maternal last name (from dbo.solicitante). */
    @Transient
    private String apellidoMaterno;

    /** Applicant first names (from dbo.solicitante). */
    @Transient
    private String nombres;

    /** Full name of the responsible person (firstNames + paternalLastName + maternalLastName). */
    @Transient
    private String responsable;

    /** Name of the responsible area (from dbo.area). */
    @Transient
    private String areaResponsable;

    /** Human-readable status: Pendiente, En proceso, Observado, Aprobado, Rechazado. */
    @Transient
    private String estadoTexto;

    // ── Persisted columns for responsible person ──────────────────────────────
    @Column(name = "nombre_responsable", length = 150)
    private String nombreResponsable;

    @Column(name = "nombres_responsable", length = 100)
    private String nombresResponsable;

    @Column(name = "apellido_paterno_responsable", length = 100)
    private String apellidoPaternoResponsable;

    @Column(name = "apellido_materno_responsable", length = 100)
    private String apellidoMaternoResponsable;

    @Column(name = "id_area_responsable")
    private Integer idAreaResponsable;

    // ── Management columns ────────────────────────────────────────────────────
    @Column(name = "fecha_recepcion", nullable = false)
    private LocalDate fechaRecepcion;

    /** RECEPCION | EVALUACION | DERIVACION | APROBACION | RECHAZO | SUBSANACION */
    @Column(name = "tipo_gestion", nullable = false, length = 30)
    private String tipoGestion;

    /** NORMAL | URGENTE | MUY_URGENTE */
    @Column(name = "prioridad", nullable = false, length = 15)
    private String prioridad = "NORMAL";

    @Column(name = "fecha_limite")
    private LocalDate fechaLimite;

    /** PENDIENTE | EN_PROCESO | OBSERVADO | APROBADO | RECHAZADO */
    @Column(name = "estado_gestion", nullable = false, length = 20)
    private String estadoGestion = "PENDIENTE";

    @Column(name = "decision", length = 500)
    private String decision;

    @Column(name = "fundamento_decision", length = 2000)
    private String fundamentoDecision;

    @Column(name = "fecha_resolucion")
    private LocalDate fechaResolucion;

    @Column(name = "observaciones", length = 2000)
    private String observaciones;

    // ── Audit status (A/I — internal) ─────────────────────────────────────────
    @Column(name = "estado", nullable = false, length = 1)
    private String estado = "A";

    // ── Audit dates ───────────────────────────────────────────────────────────
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "fecha_eliminacion")
    private LocalDateTime fechaEliminacion;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "fecha_restauracion")
    private LocalDateTime fechaRestauracion;

    // ── Human-readable label maps (Spanish display values — used by frontend) ─
    private static final Map<String, String> LABEL_ESTADO = Map.of(
        "PENDIENTE",  "Pendiente",
        "EN_PROCESO", "En proceso",
        "OBSERVADO",  "Observado",
        "APROBADO",   "Aprobado",
        "RECHAZADO",  "Rechazado"
    );

    private static final Map<String, String> LABEL_TIPO = Map.of(
        "RECEPCION",   "Recepción",
        "EVALUACION",  "Evaluación",
        "DERIVACION",  "Derivación",
        "APROBACION",  "Aprobación",
        "RECHAZO",     "Rechazo",
        "SUBSANACION", "Subsanación"
    );

    private static final Map<String, String> LABEL_PRIORIDAD = Map.of(
        "NORMAL",      "Normal",
        "URGENTE",     "Urgente",
        "MUY_URGENTE", "Muy urgente"
    );

    @PrePersist
    void prePersist() {
        if (fechaRegistro  == null) fechaRegistro  = LocalDateTime.now();
        if (fechaRecepcion == null) fechaRecepcion = LocalDate.now();
        if (prioridad     == null || prioridad.isBlank())     prioridad     = "NORMAL";
        if (estadoGestion == null || estadoGestion.isBlank()) estadoGestion = "PENDIENTE";
        if (estado        == null || estado.isBlank())        estado        = "A";
    }

    public GestionTramiteFut() {}

    // ── Internal getters/setters (@JsonIgnore) ────────────────────────────────

    @JsonIgnore
    public Integer getIdGestion() { return idGestion; }
    public void setIdGestion(Integer idGestion) { this.idGestion = idGestion; }

    @JsonIgnore
    public Long getIdSolicitante() { return idSolicitante; }
    public void setIdSolicitante(Long idSolicitante) { this.idSolicitante = idSolicitante; }

    @JsonIgnore
    public String getNombreResponsable() { return nombreResponsable; }
    public void setNombreResponsable(String v) { this.nombreResponsable = v; }

    @JsonIgnore
    public String getNombresResponsable() { return nombresResponsable; }
    public void setNombresResponsable(String v) { this.nombresResponsable = v; }

    @JsonIgnore
    public String getApellidoPaternoResponsable() { return apellidoPaternoResponsable; }
    public void setApellidoPaternoResponsable(String v) { this.apellidoPaternoResponsable = v; }

    @JsonIgnore
    public String getApellidoMaternoResponsable() { return apellidoMaternoResponsable; }
    public void setApellidoMaternoResponsable(String v) { this.apellidoMaternoResponsable = v; }

    @JsonIgnore
    public Integer getIdAreaResponsable() { return idAreaResponsable; }
    public void setIdAreaResponsable(Integer v) { this.idAreaResponsable = v; }

    @JsonIgnore
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    @JsonIgnore
    public String getEstadoGestion() { return estadoGestion; }
    public void setEstadoGestion(String estadoGestion) { this.estadoGestion = estadoGestion; }

    // ── @Transient visible getters ────────────────────────────────────────────

    public Integer getNumero() { return numero; }
    public void setNumero(Integer numero) { this.numero = numero; }

    public String getNumeroDocumento() { return numeroDocumento; }
    public void setNumeroDocumento(String v) { this.numeroDocumento = v; }

    public String getApellidoPaterno() { return apellidoPaterno; }
    public void setApellidoPaterno(String v) { this.apellidoPaterno = v; }

    public String getApellidoMaterno() { return apellidoMaterno; }
    public void setApellidoMaterno(String v) { this.apellidoMaterno = v; }

    public String getNombres() { return nombres; }
    public void setNombres(String v) { this.nombres = v; }

    public String getResponsable() { return responsable; }
    public void setResponsable(String v) { this.responsable = v; }

    @JsonProperty("areaResponsable")
    public String getAreaResponsable() { return areaResponsable; }
    public void setAreaResponsable(String v) { this.areaResponsable = v; }

    @JsonIgnore
    public String getNombreAreaResponsable() { return areaResponsable; }
    public void setNombreAreaResponsable(String v) { this.areaResponsable = v; }

    // ── Persisted visible getters ─────────────────────────────────────────────

    public LocalDate getFechaRecepcion() { return fechaRecepcion; }
    public void setFechaRecepcion(LocalDate v) { this.fechaRecepcion = v; }

    /** Management type as human-readable text. */
    @JsonProperty("tipoGestion")
    public String getTipoGestionTexto() {
        if (tipoGestion == null) return null;
        return LABEL_TIPO.getOrDefault(tipoGestion, tipoGestion);
    }
    @JsonIgnore
    public String getTipoGestion() { return tipoGestion; }
    public void setTipoGestion(String v) { this.tipoGestion = v; }

    /** Priority as human-readable text. */
    @JsonProperty("prioridad")
    public String getPrioridadTexto() {
        if (prioridad == null) return null;
        return LABEL_PRIORIDAD.getOrDefault(prioridad, prioridad);
    }
    @JsonIgnore
    public String getPrioridad() { return prioridad; }
    public void setPrioridad(String v) { this.prioridad = v; }

    public LocalDate getFechaLimite() { return fechaLimite; }
    public void setFechaLimite(LocalDate v) { this.fechaLimite = v; }

    /** Management status as human-readable text. */
    @JsonProperty("estado")
    public String getEstadoTexto() {
        if (estadoGestion == null) return null;
        return LABEL_ESTADO.getOrDefault(estadoGestion, estadoGestion);
    }

    public String getDecision() { return decision; }
    public void setDecision(String v) { this.decision = v; }

    public LocalDate getFechaResolucion() { return fechaResolucion; }
    public void setFechaResolucion(LocalDate v) { this.fechaResolucion = v; }

    public String getFundamentoDecision() { return fundamentoDecision; }
    public void setFundamentoDecision(String v) { this.fundamentoDecision = v; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String v) { this.observaciones = v; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime v) { this.fechaRegistro = v; }

    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime v) { this.fechaActualizacion = v; }

    public LocalDateTime getFechaEliminacion() { return fechaEliminacion; }
    public void setFechaEliminacion(LocalDateTime v) { this.fechaEliminacion = v; }

    public LocalDateTime getFechaRestauracion() { return fechaRestauracion; }
    public void setFechaRestauracion(LocalDateTime v) { this.fechaRestauracion = v; }
}
