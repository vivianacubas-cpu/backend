package pe.edu.vallegrande.app.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing the status change history (tracking) of a FUT procedure request.
 * Maps to the 'seguimiento_tramite' database table.
 */
@JsonPropertyOrder({
    "trackingId",
    "requestId",
    "previousStatus",
    "newStatus",
    "observation",
    "changedAt",
    "status",
    "updatedAt",
    "deletedAt",
    "restoredAt"
})
@Entity
@Table(name = "seguimiento_tramite")
public class SeguimientoTramite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_seguimiento")
    @JsonProperty("trackingId")
    private Long idSeguimiento;

    @Column(name = "id_solicitud", nullable = false)
    @JsonProperty("requestId")
    private Long idSolicitud;

    @Column(name = "estado_anterior", length = 30)
    @JsonProperty("previousStatus")
    private String estadoAnterior;

    @Column(name = "estado_nuevo", nullable = false, length = 30)
    @JsonProperty("newStatus")
    private String estadoNuevo;

    @Column(name = "observacion", length = 2000)
    @JsonProperty("observation")
    private String observacion;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "fecha_cambio", nullable = false)
    @JsonProperty("changedAt")
    private LocalDateTime fechaCambio;

    /** A = active (visible), I = inactive (logically deleted) */
    @Column(name = "estado", nullable = false, length = 1)
    @JsonProperty("status")
    private String estado = "A";

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "fecha_actualizacion")
    @JsonProperty("updatedAt")
    private LocalDateTime fechaActualizacion;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "fecha_eliminacion")
    @JsonProperty("deletedAt")
    private LocalDateTime fechaEliminacion;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "fecha_restauracion")
    @JsonProperty("restoredAt")
    private LocalDateTime fechaRestauracion;

    @PrePersist
    void prePersist() {
        if (fechaCambio == null) fechaCambio = LocalDateTime.now();
        if (estado == null || estado.isBlank()) estado = "A";
    }

    public SeguimientoTramite() {}

    public Long getIdSeguimiento() { return idSeguimiento; }
    public void setIdSeguimiento(Long idSeguimiento) { this.idSeguimiento = idSeguimiento; }

    public Long getIdSolicitud() { return idSolicitud; }
    public void setIdSolicitud(Long idSolicitud) { this.idSolicitud = idSolicitud; }

    public String getEstadoAnterior() { return estadoAnterior; }
    public void setEstadoAnterior(String estadoAnterior) { this.estadoAnterior = estadoAnterior; }

    public String getEstadoNuevo() { return estadoNuevo; }
    public void setEstadoNuevo(String estadoNuevo) { this.estadoNuevo = estadoNuevo; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    public LocalDateTime getFechaCambio() { return fechaCambio; }
    public void setFechaCambio(LocalDateTime fechaCambio) { this.fechaCambio = fechaCambio; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }

    public LocalDateTime getFechaEliminacion() { return fechaEliminacion; }
    public void setFechaEliminacion(LocalDateTime fechaEliminacion) { this.fechaEliminacion = fechaEliminacion; }

    public LocalDateTime getFechaRestauracion() { return fechaRestauracion; }
    public void setFechaRestauracion(LocalDateTime fechaRestauracion) { this.fechaRestauracion = fechaRestauracion; }
}
