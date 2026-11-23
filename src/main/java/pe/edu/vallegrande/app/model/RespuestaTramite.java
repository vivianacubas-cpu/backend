package pe.edu.vallegrande.app.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing an official response to a FUT procedure request.
 * Maps to the 'respuesta_tramite' database table.
 */
@JsonPropertyOrder({"responseId", "requestId", "userId", "responseText", "respondedAt"})
@Entity
@Table(name = "respuesta_tramite")
public class RespuestaTramite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_respuesta")
    @JsonProperty("responseId")
    private Long idRespuesta;

    @Column(name = "id_solicitud", nullable = false)
    @JsonProperty("requestId")
    private Long idSolicitud;

    @Column(name = "id_usuario")
    @JsonProperty("userId")
    private Integer idUsuario;

    @Column(name = "respuesta", nullable = false, length = 2000)
    @JsonProperty("responseText")
    private String respuesta;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "fecha_respuesta", nullable = false)
    @JsonProperty("respondedAt")
    private LocalDateTime fechaRespuesta;

    @PrePersist
    void prePersist() {
        if (fechaRespuesta == null) {
            fechaRespuesta = LocalDateTime.now();
        }
    }

    public RespuestaTramite() {}

    public Long getIdRespuesta() { return idRespuesta; }
    public void setIdRespuesta(Long idRespuesta) { this.idRespuesta = idRespuesta; }

    public Long getIdSolicitud() { return idSolicitud; }
    public void setIdSolicitud(Long idSolicitud) { this.idSolicitud = idSolicitud; }

    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }

    public String getRespuesta() { return respuesta; }
    public void setRespuesta(String respuesta) { this.respuesta = respuesta; }

    public LocalDateTime getFechaRespuesta() { return fechaRespuesta; }
    public void setFechaRespuesta(LocalDateTime fechaRespuesta) { this.fechaRespuesta = fechaRespuesta; }
}
