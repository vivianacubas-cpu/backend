package pe.edu.vallegrande.app.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing a procedure type (TipoTramite).
 * Maps to the 'tipo_tramite' database table.
 *
 * JSON output uses English field names (@JsonProperty).
 * JSON input accepts both English and Spanish names (@JsonAlias) for
 * backward compatibility with the existing Angular frontend.
 */
@JsonPropertyOrder({
    "procedureTypeId",
    "code",
    "name",
    "category",
    "description",
    "requirements",
    "estimatedDays",
    "requiresDocuments",
    "validFrom",
    "status",
    "createdAt",
    "updatedAt",
    "deletedAt",
    "restoredAt"
})
@Entity
@Table(name = "tipo_tramite")
public class TipoTramite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_tramite")
    @JsonProperty("procedureTypeId")
    @JsonAlias({"idTipoTramite", "id_tipo_tramite"})
    private Integer idTipoTramite;

    @Column(name = "codigo", length = 20, unique = true)
    @JsonProperty("code")
    @JsonAlias({"codigo"})
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 100)
    @JsonProperty("name")
    @JsonAlias({"nombre"})
    private String nombre;

    @Column(name = "categoria", length = 60)
    @JsonProperty("category")
    @JsonAlias({"categoria"})
    private String categoria;

    @Column(name = "descripcion", length = 250)
    @JsonProperty("description")
    @JsonAlias({"descripcion"})
    private String descripcion;

    @Column(name = "requisitos", length = 500)
    @JsonProperty("requirements")
    @JsonAlias({"requisitos"})
    private String requisitos;

    @Column(name = "plazo_estimado_dias")
    @JsonProperty("estimatedDays")
    @JsonAlias({"plazoEstimadoDias", "plazo_estimado_dias"})
    private Integer plazoEstimadoDias;

    @Column(name = "requiere_documentos", nullable = false)
    @JsonProperty("requiresDocuments")
    @JsonAlias({"requiereDocumentos", "requiere_documentos"})
    private Boolean requiereDocumentos = Boolean.FALSE;

    @Column(name = "vigente_desde")
    @JsonProperty("validFrom")
    @JsonAlias({"vigentDesde", "vigente_desde"})
    private LocalDate vigentDesde;

    @Column(name = "estado", nullable = false)
    @JsonProperty("status")
    @JsonAlias({"estado"})
    private Boolean estado = Boolean.TRUE;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "fecha_registro")
    @JsonProperty("createdAt")
    @JsonAlias({"fechaRegistro", "fecha_registro"})
    private LocalDateTime fechaRegistro;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "fecha_actualizacion")
    @JsonProperty("updatedAt")
    @JsonAlias({"fechaActualizacion", "fecha_actualizacion"})
    private LocalDateTime fechaActualizacion;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "fecha_eliminacion")
    @JsonProperty("deletedAt")
    @JsonAlias({"fechaEliminacion", "fecha_eliminacion"})
    private LocalDateTime fechaEliminacion;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "fecha_restauracion")
    @JsonProperty("restoredAt")
    @JsonAlias({"fechaRestauracion", "fecha_restauracion"})
    private LocalDateTime fechaRestauracion;

    @PrePersist
    void prePersist() {
        if (estado == null) estado = Boolean.TRUE;
        if (requiereDocumentos == null) requiereDocumentos = Boolean.FALSE;
        if (fechaRegistro == null) fechaRegistro = LocalDateTime.now();
    }

    public TipoTramite() {}

    public Integer getIdTipoTramite() { return idTipoTramite; }
    public void setIdTipoTramite(Integer idTipoTramite) { this.idTipoTramite = idTipoTramite; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getRequisitos() { return requisitos; }
    public void setRequisitos(String requisitos) { this.requisitos = requisitos; }

    public Integer getPlazoEstimadoDias() { return plazoEstimadoDias; }
    public void setPlazoEstimadoDias(Integer plazoEstimadoDias) { this.plazoEstimadoDias = plazoEstimadoDias; }

    public Boolean getRequiereDocumentos() { return requiereDocumentos; }
    public void setRequiereDocumentos(Boolean requiereDocumentos) { this.requiereDocumentos = requiereDocumentos; }

    public LocalDate getVigentDesde() { return vigentDesde; }
    public void setVigentDesde(LocalDate vigentDesde) { this.vigentDesde = vigentDesde; }

    public Boolean getEstado() { return estado; }
    public void setEstado(Boolean estado) { this.estado = estado; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }

    public LocalDateTime getFechaEliminacion() { return fechaEliminacion; }
    public void setFechaEliminacion(LocalDateTime fechaEliminacion) { this.fechaEliminacion = fechaEliminacion; }

    public LocalDateTime getFechaRestauracion() { return fechaRestauracion; }
    public void setFechaRestauracion(LocalDateTime fechaRestauracion) { this.fechaRestauracion = fechaRestauracion; }
}
