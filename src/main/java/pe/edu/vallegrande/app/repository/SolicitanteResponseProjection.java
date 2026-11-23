package pe.edu.vallegrande.app.repository;

import java.time.LocalDateTime;

/**
 * Projection for the enriched applicant query.
 *
 * Exposes the getters needed to build {@code SolicitanteResponse}.
 * Includes idSolicitante so the frontend can perform CRUD operations.
 */
public interface SolicitanteResponseProjection {
    Long getIdSolicitante();
    String getTipoDocumento();
    /** Combined location string "District, Province, Department" — kept for legacy compatibility. */
    String getUbicacion();
    /** District resolved from the free-text field or the ubigeo catalog. */
    String getDistrito();
    /** Province resolved from the free-text field or the ubigeo catalog. */
    String getProvincia();
    /** Department resolved from the free-text field or the ubigeo catalog. */
    String getDepartamento();
    String getNumeroDocumento();
    String getNombres();
    String getApellidoPaterno();
    String getApellidoMaterno();
    String getTelefono();
    String getCorreo();
    String getDireccion();
    /** Returns "Activo" or "Inactivo" (CASE in SQL). */
    String getEstadoTexto();
    LocalDateTime getFechaRegistro();
    LocalDateTime getFechaActualizacion();
    LocalDateTime getFechaEliminacion();
    LocalDateTime getFechaRestauracion();
}
