package pe.edu.vallegrande.app.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * Request DTO for registering or updating an official response to a FUT procedure.
 * Accepts both English (new) and Spanish (legacy) field names.
 */
public class RespuestaTramiteRequest {

    /** ID of the procedure request this response belongs to. */
    @JsonAlias({"idSolicitud"})
    private Long idSolicitud;

    /** ID of the user (secretary/principal) issuing the response. Optional. */
    @JsonAlias({"idUsuario"})
    private Integer idUsuario;

    /** Text of the official response. Required. */
    @JsonAlias({"respuesta"})
    private String respuesta;

    public Long getIdSolicitud() { return idSolicitud; }
    public void setIdSolicitud(Long idSolicitud) { this.idSolicitud = idSolicitud; }

    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }

    public String getRespuesta() { return respuesta; }
    public void setRespuesta(String respuesta) { this.respuesta = respuesta; }
}
